# Distributed system pipeline

## Description 

This repository contains a template of a complete distributed system that combines [Apache Kafka](https://kafka.apache.org/), [Apache Flink](https://flink.apache.org/), [Apache Druid](https://druid.apache.org/) and [Grafana](https://grafana.com/) with a "dummy" source of sensors for deployment on two servers.

The objective of the system is
1. Gather the metrics produced by the sensors and publish them to Kafka.
2. Use Flink for runtime processing to subscribe to the metrics on Kafka and remove outliers and compute the total number of outliers for each 10-minute tumbling window.
3. Publish from Flink to Kafka the clean values and the outliers stats. 
4. Store the raw and clean metrics as well as the outlier stats in Druid.
5. Use Grafana to visualize the timeseries and outliers

The system also includes the Kafdrop docker image to monitor the Kafka brokers and topics.

## Preliminaries

### Docker images

One server with the most resources should act as the main and the other as the worker. Transfer the `docker-compose.yml` file on the main server and the `docker-compose-worker.yml` file on the worker server.
Pull the images with the following command:

`docker-compose pull`

Add the correct hostnames on the `.env` file for the `SERVER1` and `SERVER2` environment variables.

### Run the containers

First run the containers on the main server and after each one is deployed without errors run the containers on the worker server.

`Main server: docker-compose up`
`Worker server: docker-compose -f docker-compose-worker.yml up`

### Run the sensor producer

After every container is deployed without errors start the sensor producer from the folder `Sensors` by running the command `python producer.py`. The same `.env` file should be present in the folder to correctly identify the Kafka brokers.

You can also run the sensor producer by using the `docker-compose-sensors.yml` file to start the producer with the command `docker-compose -f docker-compose-sensors.yml`.

### Run the Flink job

The folder `Runtime-processing` contains the Flink job for outlier detection. The job is packaged into the jar file in `target/runtimeprocessing-0.1.jar`.

To run the job: 
1. Open the Flink UI using the url `MAIN_SERVER:8081`.
2. Go to `Submit new job`.
3. Add the jar file
4. Click on the job, choose a parallelism level and start.

### Connect Kafka to Druid

Using Druid's UI in `MAIN_SERVER:8808` setup 3 data sources for each topic (`raw_sensors`, `clean_sensors`, `outlier_stats`) to start storing data.

### Connect Grafana to Druid

Grafana is used to visualize the data from Druid. The following steps need to take place to connect the two systems and create visualizations.

1. Using Grafana's UI in `MAIN_SERVER:3003` setup the Druid plugin.
2. Setup the Druid as a data source using the URL `MAIN_SERVER:8808`.
3. Create a dashboard
4. Create a visualization.