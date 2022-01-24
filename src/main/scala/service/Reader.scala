package service

import akka.Done
import akka.actor.{Actor, ActorLogging}
import scala.jdk.CollectionConverters._
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.FileIO
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import org.apache.kafka.common.serialization.Serializer
import java.io.File
import scala.util.{Failure, Success}
import model.Messages._
import org.apache.avro.Schema.Parser
import org.apache.avro.generic.{GenericRecord, GenericData}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import java.util.UUID.randomUUID
import scala.concurrent.Future

class Reader extends Actor with ActorLogging {
  implicit val executionContext = context.system.dispatcher
  implicit val mat: Materializer = Materializer(context)

  override def receive: Receive = {
    case ReadFile(location, extension) =>
      getListOfFiles(new File(location), extension)
        .map { file => {
          sourceProducer(file).onComplete {
            case Failure(exception) => throw exception
            case _: Success[_] =>
              if (renameFile(file)) {
                log.info(s"$file was processed with success!")
              } else {
                log.error(s"Failed to rename file $file")
              }
          }
        }
        }
    case _ => log.error(s"Unrecognized message from ${sender()}")
  }

  private val bootstrapServer: String = ConfigFactory.load().getString("kafka.bootstrap-server")
  private val schemaRegistry: String = ConfigFactory.load().getString("kafka.schema-registry")
  private val topic: String = ConfigFactory.load().getString("kafka.topic")
  private val kafkaProducerSettings = genericProducerSettings(bootstrapServer, schemaRegistry)
  private val schemaParser = new Parser
  private val valueSchemaAvro = schemaParser.parse(new File("src/main/resources/movie.avsc"))

  private def genericProducerSettings(broker: String, schemaRegistry: String): ProducerSettings[String, GenericRecord] = {
    val kafkaAvroSerDeConfig = Map[String, Any] {
      AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistry
//      AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS -> false
    }

    val kafkaAvroSerializer = new KafkaAvroSerializer()
    kafkaAvroSerializer.configure(kafkaAvroSerDeConfig.asJava, false)

    val serializer = kafkaAvroSerializer.asInstanceOf[Serializer[GenericRecord]]
    ProducerSettings(context.system, new StringSerializer, serializer)
      .withBootstrapServers(broker)
  }

  private def sourceProducer(fileName: File): Future[Done] = {
    FileIO.fromFile(fileName)
      .via(CsvParsing.lineScanner())
      .via(CsvToMap.toMapAsStrings())
      .map(row => {
        log.info(s"ROW: $row")
        rowToProducerRecord(row)
      })
      .runWith(Producer.plainSink(kafkaProducerSettings))
  }

  private def rowToProducerRecord(movieMap: Map[String, String]): ProducerRecord[String, GenericRecord]  = {
    val avroRecord = new GenericData.Record(valueSchemaAvro)
    val encodingRecord = new GenericData.Record(valueSchemaAvro.getField("encoding").schema())
    movieMap.foreach {
      case (key: String, value: String) if key == "encoding" =>
        val encoding = createEncodingRecord(encodingRecord, value)
        avroRecord.put(key, encoding)
      case (key: String, value: String) => avroRecord.put(key, value)
      case _ => log.error("Invalid map combination")
    }
    new ProducerRecord(topic, randomUUID().toString, avroRecord)
  }

  private def createEncodingRecord(encodingRecord: GenericRecord, value: String): GenericRecord = {
    val encodings = value
      .drop(1)
      .dropRight(1)
      .split(",")
      .map(_.split(":"))
      .flatMap(_.drop(1))
    encodingRecord.put("encoding", encodings(0))
    encodingRecord.put("height", encodings(1))
    encodingRecord.put("width", encodings(2))
    encodingRecord
  }

  private def renameFile(file: File): Boolean = file.renameTo(new File (file.getAbsolutePath + ".OK"))

  private def getListOfFiles(dir: File, extension: String): List[File] = {
    dir.listFiles.filter(_.isFile).toList.filter { file =>
      file.getName.endsWith(extension)
    }
  }

}