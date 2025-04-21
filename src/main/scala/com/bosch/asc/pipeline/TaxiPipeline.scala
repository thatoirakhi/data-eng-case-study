package com.bosch.asc.pipeline

import org.apache.spark.internal.Logging
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.functions.round
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object TaxiPipeline extends Logging {

  def main(args: Array[String]): Unit = {

    val conf: PipelineProperties = loadConfig()
    val sparkSession: SparkSession = createSparkSession(conf)

    val taxiDataDF = loadTaxiData(sparkSession)
    val taxiZoneLookupDF = loadTaxiZoneLookup(sparkSession)

    val enrichedTaxiDataDF = enrichTaxiData(taxiDataDF, taxiZoneLookupDF)
    val transformedDF = transformTaxiData(enrichedTaxiDataDF)

    val aggregatedDF = aggregateTaxiData(transformedDF)
    writeHighDemandZones(aggregatedDF, conf)
    writeDemandTrends(aggregatedDF, conf)

    val sharedRideDF = calculateSharedRideFeasibility(transformedDF)
    val finalPriceDF = calculateDynamicPricing(sharedRideDF)
    writeSharedRidePricing(finalPriceDF, conf)

    sparkSession.stop()
  }

  private def loadConfig(): PipelineProperties = {
    ConfigSource.default.loadOrThrow[PipelineProperties]
  }

  private def createSparkSession(conf: PipelineProperties): SparkSession = {
    SparkSession.builder()
      .appName("taxi-data-pipeline")
      .config("spark.hadoop.fs.s3a.path.style.access", "true")
      .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .config("fs.s3a.endpoint", conf.minioEndpoint)
      .config("fs.s3a.access.key", conf.minioAccessKey)
      .config("fs.s3a.secret.key", conf.minioSecretKey)
      .master("local[*]")
      .getOrCreate()
  }

  private def loadTaxiData(spark: SparkSession): DataFrame = {
    spark.read.parquet("s3a://taxi-bucket/taxi-data/")
  }

  private def loadTaxiZoneLookup(spark: SparkSession): DataFrame = {
    spark.read.format("csv").option("header", "true").load("s3a://taxi-bucket/reference-data/taxi_zone_lookup.csv")
  }

   def enrichTaxiData(taxiDataDF: DataFrame, taxiZoneLookupDF: DataFrame): DataFrame = {
    taxiDataDF.join(taxiZoneLookupDF, taxiDataDF("VendorID") === taxiZoneLookupDF("LocationID"))
  }

   def transformTaxiData(enrichedTaxiDataDF: DataFrame): DataFrame = {
    enrichedTaxiDataDF
      .withColumn("pickup_hour", hour(col("tpep_pickup_datetime")))
      .withColumn("pickup_day", date_format(col("tpep_pickup_datetime"), "EEEE"))
      .withColumn("trip_duration_minutes",
        (unix_timestamp(col("tpep_dropoff_datetime")) - unix_timestamp(col("tpep_pickup_datetime"))) / 60)
  }

   def aggregateTaxiData(transformedDF: DataFrame): DataFrame = {
    transformedDF
      .groupBy("Zone", "pickup_hour", "pickup_day")
      .agg(
        count("*").alias("total_trips"),
        round(avg("fare_amount").cast("double"), 2).alias("avg_fare"),
        avg("passenger_count").alias("avg_passenger_count"),
        avg("trip_duration_minutes").alias("avg_trip_duration"),
        (sum("fare_amount") / sum("trip_duration_minutes")).alias("avg_fare_per_minute")
      )
  }

  private def writeHighDemandZones(aggregatedDF: DataFrame, conf: PipelineProperties): Unit = {
    val windowSpec = Window.partitionBy("pickup_day", "pickup_hour").orderBy(desc("total_trips"))
    val rankedDF = aggregatedDF
      .withColumn("rank", rank().over(windowSpec))
      .filter(col("rank") <= 5)

    rankedDF.write
      .mode("append")
      .jdbc(
        url = conf.jdbcUrl,
        table = "high_demand_zones",
        connectionProperties = conf.connectionProperties
      )
  }

  private def writeDemandTrends(aggregatedDF: DataFrame, conf: PipelineProperties): Unit = {
    val trendAnalysisDF = aggregatedDF
      .groupBy("Zone", "pickup_day")
      .agg(
        sum("total_trips").alias("weekly_trips"),
        avg("avg_fare").alias("weekly_avg_fare")
      )
      .orderBy(desc("weekly_trips"))

    trendAnalysisDF.write
      .mode("append")
      .jdbc(
        url = conf.jdbcUrl,
        table = "demand_trends",
        connectionProperties = conf.connectionProperties
      )
  }
   def calculateSharedRideFeasibility(transformedDF: DataFrame): DataFrame = {
    transformedDF
      .withColumn("pickup_interval", concat(col("pickup_day"), lit("_"), col("pickup_hour")))
      .groupBy("Zone", "pickup_interval")
      .agg(
        count("*").alias("total_trips"),
        sum("passenger_count").alias("total_passengers"),
        avg("fare_amount").alias("avg_fare")
      )
      .withColumn("shared_ride_feasibility", when(col("total_passengers") > 4, lit(1)).otherwise(lit(0)))
  }

   def calculateDynamicPricing(sharedRideDF: DataFrame): DataFrame = {
    sharedRideDF
      .withColumn("dynamic_price_multiplier", when(col("total_trips") > 50, 1.5)
        .when(col("total_trips") > 30, 1.2)
        .otherwise(1.0))
      .withColumn("final_price", round( col("avg_fare") * col("dynamic_price_multiplier") *
        when(col("shared_ride_feasibility") === 1, 0.8).otherwise(1.0), 2))
  }

  private def writeSharedRidePricing(finalPriceDF: DataFrame, conf: PipelineProperties): Unit = {
    finalPriceDF.write
      .mode("append")
      .jdbc(
        url = conf.jdbcUrl,
        table = "shared_ride_pricing",
        connectionProperties = conf.connectionProperties
      )
  }
}
