package actor

import akka.actor.{Actor, ActorLogging, Props}
import org.apache.kafka.clients.producer.{ProducerRecord}
import service.Reader
import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import model.Messages._

class CsvProducer extends Actor with ActorLogging {
  import utils.KafkaSetUp.{avroRecord, producer}

  implicit val ec = ExecutionContext.global
  val interval = Duration(5000, MILLISECONDS)
  val delay = Duration(100, "millis")
  val reader = context.actorOf(Props[Reader], "FileReader")

  context.watch(reader)

  override def receive: Receive = {
    case item@Item(id, brand, itemType, desc, dimensions, price) =>
      log.info(s"Received Item with: $id, $brand, $itemType, $desc, $dimensions, $price")
      writeItem(item)
    case StartReading =>
      log.info("Start reading...")
      context.system.scheduler.scheduleWithFixedDelay(
        delay,
        interval,
        reader,
        ReadFile("src/main/resources/", "csv")
      )
  }

  def writeItem(item: Item): Unit  = {
    avroRecord.put("itemId", item.itemId)
    avroRecord.put("brand", item.brand)
    avroRecord.put("itemType", item.itemType)
    avroRecord.put("description", item.description)
    avroRecord.put("dimensions", item.dimensions)
    avroRecord.put("price", item.price)
    val key = randomUUID().toString
    val record = new ProducerRecord("tyres", key, avroRecord)
    val ack = producer.send(record).get()
    log.info(s"${ack.toString} written to partition ${ack.partition.toString}")
  }

}