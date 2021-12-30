package utils

import org.apache.avro.Schema.Parser
import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.producer.KafkaProducer

import java.io.File
import java.util.Properties

object KafkaSetUp {
  //Serialization setup
  private val kafkaProducerProps: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("schema.registry.url", "http://localhost:8081")
    props.put("key.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer")
    props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer")
    //    props.put("auto.register.schemas", false)
    props
  }

  val producer = new KafkaProducer[String, GenericData.Record](kafkaProducerProps)
  private val schemaParser = new Parser
  private val valueSchemaAvro = schemaParser.parse(new File("src/main/resources/item.avsc"))
  val avroRecord = new GenericData.Record(valueSchemaAvro)
}
