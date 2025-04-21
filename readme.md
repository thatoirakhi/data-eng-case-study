# Assignment
In this assignment, your task is to build a data pipeline based on the open source NYC taxi data set.
The preferred coding language is Scala.

In this repository, you will find starter code for the assignment. You do not have to use this code.
It is just a suggestion to get you started.

We use MinIO as storage for the source data and PostgreSQL as data sink for transformed data. Both are provided as Docker containers.
You will use Apache Spark to load data, transform it and persist it into the PostgreSQL database.

## Before you get started

* you should have Docker & Docker compose installed on your machine
* you should have a database client installed that lets you execute SQL code. Some IDEs come with plugins allowing to do that. Other options include DBeaver or DataGrip.
* you should have an IDE installed that lets you open and edit Scala projects.

## Getting started instructions

* download [Yellow Taxi Trip Records](https://www.nyc.gov/site/tlc/about/tlc-trip-record-data.page) for June, July & August 2024
* place the data into the `setup/data` directory
* also download the `Taxi Zone Lookup Table` further down on the same page
* place it into the `setup/reference-data` directory
* in the project's root, you can now run `docker compose up --build -d`. This will start docker containers with the services you need to run this project locally.
* verify that your containers are up and running:
  * open `localhost:9001` in your browser --> this should bring up the MinIO login page. `src\main\resources\application.conf` contains the login credentials
  * you should see the `taxi-bucket` containing `reference-data` and `taxi-data`
  * you should also be able to connect to the PostgreSQL container from your SQL client / editor (connection string, database & credentials are in `src\main\resources\application.conf` as well)
  * if you want to run the example pipeline, note that you have to create the sink table first (see `scripts\test_ddl.sql`)
* If all of that worked, you are good to go!

## Tasks

* Task 1: run the main method in the `DataPipeline` class. This will read the data from the MinIO bucket and write it to the PostgreSQL database. Note that this is just a small example to verify if everything works properly.
* Task 2: familiarize yourself with the dataset and formulate analytical questions that could be interesting. Document these questions.
* Task 3: design a suitable data model for the given data set and document it together with a basic design document for your project.
* Task 3a: build a job which reads the source data and transforms it according to your data model
* Task 3b: write the transformed data to a PostgreSQL DB and query it

## Hints

* you can deviate from the provided code as you see fit
* you can use other sources / sinks if desired (but not required)
* you can also write back to MinIO, read and write from PostgreSQL etc

## Evaluation Criteria

* Overall system and application design
* Analytical questions: appropriate level of difficulty and relevance to the business problem (imagine you work for the city of NYC and have to regulate taxi traffic)
* Data model: ability to design an appropriate data model for the business context
* Data quality considerations
* Software development best practices (testing, decomposition, clean code, ...)
* (optional) This assignment is brand new. Bonus points if you find ways to make it better or easier to get started with.