/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package example;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchemaBuilder;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Skeleton code for the datastream walkthrough
 */
public class RuntimeProcessing {
	public static void main(String[] args) throws Exception {
		String server1 = Helpers.readEnvVariable("SERVER1");
		String kafkaServer = server1 + ":9092";

		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		JsonDeserializationSchema<Sensor> jsonFormat = new JsonDeserializationSchema<>(Sensor.class);
		JsonSerializationSchema<Sensor> jsonFormatOutput=new JsonSerializationSchema<>();
		//Kafka source
		KafkaSource<Sensor> source = KafkaSource.<Sensor>builder()
				.setBootstrapServers(kafkaServer)
				.setTopics("raw_sensors")
				.setGroupId("runtimeprocessing")
				.setValueOnlyDeserializer(jsonFormat)
				.setStartingOffsets(OffsetsInitializer.latest())
				.build();
		//Kafka sink
		KafkaSink<Sensor> sink = KafkaSink.<Sensor>builder()
				.setRecordSerializer(
						new KafkaRecordSerializationSchemaBuilder<>()
								.setValueSerializationSchema(jsonFormatOutput)
								.setTopic("clean_sensors")
								.build()
				)
				.setBootstrapServers(kafkaServer)
				.build();
		//Kafka sink2
		KafkaSink<String> sink2 = KafkaSink.<String>builder()
				.setRecordSerializer(
						new KafkaRecordSerializationSchemaBuilder<>()
								.setValueSerializationSchema(new SimpleStringSchema())
								.setTopic("outlier_stats")
								.build()
				)
				.setBootstrapServers(kafkaServer)
				.build();
		//Get the data from kafka and deserialize to class
		DataStream<Sensor> input = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");
		//Filter out the outliers
		DataStream<Sensor> cleanInput = input.filter(s -> s.getValue() <= s.getMax());
		cleanInput.sinkTo(sink);
		//Get stats for outliers on a 10 -in window per sensor type
		DataStream<Tuple4<Long, Long, Integer, Integer>> stats = input
				.filter(s -> s.getValue() > s.getMax())
				.keyBy(Sensor::getSensor_id)
				.window(TumblingProcessingTimeWindows.of(Time.minutes(10)))
				.process(new ProcessWindowFunction<>() {
                    @Override
                    public void process(Integer integer, ProcessWindowFunction<Sensor, Tuple4<Long, Long, Integer, Integer>, Integer, TimeWindow>.Context context, Iterable<Sensor> iterable, Collector<Tuple4<Long, Long, Integer, Integer>> collector) {
                        long windowStart = context.window().getStart();
						long duration = context.window().getEnd() - windowStart;
                        AtomicInteger counter = new AtomicInteger();
                        iterable.forEach(s -> counter.addAndGet(1));
                        collector.collect(Tuple4.of(windowStart, duration, integer, counter.get()));
                    }
                });
		DataStream<String> deserialized = stats
				.map(s -> "{\"time\": " + s.f0 + ", \"duration\": " + s.f1 + ", \"sensor_id\": " + s.f2 + ", \"outliers\": " + s.f3 + "}");
		deserialized.sinkTo(sink2);
		env.execute("Runtime processing");
	}
}
