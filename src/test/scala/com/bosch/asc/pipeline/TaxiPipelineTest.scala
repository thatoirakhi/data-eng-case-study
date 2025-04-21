package com.bosch.asc.pipeline

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import com.bosch.asc.pipeline.TaxiPipeline

class TaxiPipelineTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  lazy val spark: SparkSession = SparkSession.builder()
    .appName("TaxiPipelineTest")
    .master("local[*]")
    .getOrCreate()

  override def afterAll(): Unit = {
    if (spark != null) {
      spark.stop()
    }
    super.afterAll()
  }

  test("Test enrichTaxiData method") {
    import spark.implicits._

    // Mock input data
    val taxiData = Seq(
      ("1", 1, "2024-06-01 10:00:00", "2024-06-01 10:30:00", 2, 20.0),
      ("2", 2, "2024-06-01 11:00:00", "2024-06-01 11:20:00", 1, 15.0)
    ).toDF("trip_id", "VendorID", "tpep_pickup_datetime", "tpep_dropoff_datetime", "passenger_count", "fare_amount")

    val taxiZoneLookup = Seq(
      (1, "ABC", "Zone A", "XYZ"),
      (2, "DEF", "Zone B", "UVW")
    ).toDF("LocationID", "Borough", "Zone", "service_zone")

    // Call enrichTaxiData
    val enrichedTaxiDataDF = TaxiPipeline.enrichTaxiData(taxiData, taxiZoneLookup)

    // Validate the results
    val actualData = enrichedTaxiDataDF
      .select("trip_id", "Zone")
      .collect()
      .map(row => (row.getString(0), row.getString(1)))

    actualData should contain theSameElementsAs Seq(
      ("1", "Zone A"),
      ("2", "Zone B")
    )
  }

  test("Test transformTaxiData method") {
    import spark.implicits._

    // Mock input data
    val enrichedTaxiData = Seq(
      ("1", "Zone A", "2024-06-01 10:00:00", "2024-06-01 10:30:00", 20.0),
      ("2", "Zone B", "2024-06-01 11:00:00", "2024-06-01 11:20:00", 15.0)
    ).toDF("trip_id", "Zone", "tpep_pickup_datetime", "tpep_dropoff_datetime", "fare_amount")

    // Call transformTaxiData
    val transformedDF = TaxiPipeline.transformTaxiData(enrichedTaxiData)

    // Validate the results
    val actualData = transformedDF
      .select("Zone", "pickup_hour", "trip_duration_minutes")
      .collect()
      .map(row => (row.getString(0), row.getInt(1), row.getDouble(2)))

    actualData should contain theSameElementsAs Seq(
      ("Zone A", 10, 30.0),
      ("Zone B", 11, 20.0)
    )
  }

  test("Test aggregateTaxiData method") {
    import spark.implicits._

    // Mock input data
    val transformedData = Seq(
      ("Zone A", 10, "Monday", 20.0, 2, 30.0),
      ("Zone B", 11, "Monday", 15.0, 1, 20.0)
    ).toDF("Zone", "pickup_hour", "pickup_day", "fare_amount", "passenger_count", "trip_duration_minutes")

    // Call aggregateTaxiData
    val aggregatedDF = TaxiPipeline.aggregateTaxiData(transformedData)

    // Validate the results
    val actualData = aggregatedDF
      .select("Zone", "pickup_hour", "total_trips", "avg_fare", "avg_trip_duration")
      .collect()
      .map(row => (
        row.getString(0),
        row.getInt(1),
        row.getLong(2),
        row.getDouble(3),
        row.getDouble(4)
      ))

    actualData should contain theSameElementsAs Seq(
      ("Zone A", 10, 1, 20.0, 30.0),
      ("Zone B", 11, 1, 15.0, 20.0)
    )
  }
}