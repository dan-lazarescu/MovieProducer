package actor

import akka.actor.{Actor, ActorLogging, Props}
import service.Reader
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import model.Messages._

class CsvProducer extends Actor with ActorLogging {

  implicit val ec = ExecutionContext.global
  val interval = Duration(5000, MILLISECONDS)
  val delay = Duration(100, "millis")
  val reader = context.actorOf(Props[Reader], "FileReader")

  context.watch(reader)

  override def receive: Receive = {
    case StartReadingMovies =>
      log.info("Start reading...")
      context.system.scheduler.scheduleWithFixedDelay(
        delay,
        interval,
        reader,
        ReadFile("src/main/resources/", "csv")
      )
    case _ => log.info("Received something nasty...")
  }

}