import actor.CsvProducer
import akka.actor.{ActorSystem, Props}
import model.Messages._

object StartReader extends App {

  val system = ActorSystem("CsvFileReader")
  val csvProducer = system.actorOf(Props[CsvProducer], "CsvProducer")
  csvProducer ! StartReadingMovies

}
