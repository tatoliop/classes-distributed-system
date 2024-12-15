import json
import os
import random
import threading
import time

from confluent_kafka import Producer
from confluent_kafka.admin import AdminClient
from confluent_kafka.cimpl import NewTopic

kafka_topic = 'raw_sensors'
# Each location sends to a different kafka server
server1 = os.getenv('SERVER1', 'localhost')
server2 = os.getenv('SERVER2', 'localhost')

def current_milli_time():
    return round(time.time() * 1000)


def get_temp_values():
    # First get whether the measurements will be an outlier or not
    outlier_check = random.randrange(100)
    if outlier_check <= 5:  # Outlier with 5% chance
        return random.uniform(45.0, 50.0)
    else:  # Normal
        return random.uniform(18.0, 23.0)


def get_moisture_values():
    # First get whether the measurements will be an outlier or not
    outlier_check = random.randrange(100)
    if outlier_check <= 5:  # Outlier with 5% chance
        return random.uniform(80.0, 90.0)
    else:  # Normal
        return random.uniform(20.0, 60.0)


def msg_format(loc_id, sensor_id, value):
    current_time = current_milli_time()
    msg = {"time": current_time,
           "location_id": loc_id,
           "sensor_id": sensor_id
           }
    if sensor_id == 1:
        msg["sensor"] = "temperature"
        msg["value"] = value
        msg["max"] = 23.0
    elif sensor_id == 2:
        msg["sensor"] = "moisture"
        msg["value"] = value
        msg["max"] = 60.0
    else:
        msg["sensor"] = "N/A"
        msg["value"] = None
        msg["max"] = None
    return json.dumps(msg)


def get_loc1_sensor():
    # Conf for kafka connection
    conf = {'bootstrap.servers':  f'{server1}:9092', 'client.id': 'location_1'}
    # Create the producer
    producer = Producer(conf)
    while True:
        temp_value = get_temp_values()
        moisture_value = get_moisture_values()
        msg_temp = msg_format(1, 1, temp_value)
        producer.produce(kafka_topic, msg_temp)
        msg_moisture = msg_format(1, 2, moisture_value)
        producer.produce(kafka_topic, msg_moisture)
        time.sleep(1)  # W8 1 sec and send new measurement


def get_loc2_sensor():
    # Conf for kafka connection
    conf = {'bootstrap.servers':  f'{server2}:9092', 'client.id': 'location_2'}
    # Create the producer
    producer = Producer(conf)
    while True:
        temp_value = get_temp_values()
        moisture_value = get_moisture_values()
        msg_temp = msg_format(2, 1, temp_value)
        producer.produce(kafka_topic, msg_temp)
        msg_moisture = msg_format(2, 2, moisture_value)
        producer.produce(kafka_topic, msg_moisture)
        time.sleep(1)  # W8 1 sec and send new measurement


if __name__ == "__main__":
    # First create topics with partitions
    admin_client = AdminClient({
        "bootstrap.servers": f'{server1}:9092'
    })

    topic_list = []
    topic_list.append(NewTopic(kafka_topic, 2, 1))
    topic_list.append(NewTopic("clean_sensors", 1, 1))
    topic_list.append(NewTopic("outlier_stats", 1, 1))
    admin_client.create_topics(topic_list)
    time.sleep(5)

    t1 = threading.Thread(target=get_loc1_sensor)
    t2 = threading.Thread(target=get_loc2_sensor)

    t1.start()
    t2.start()

    t1.join()
    t2.join()