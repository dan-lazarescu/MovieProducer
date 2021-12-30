package service

import akka.actor.{Actor, ActorLogging}

import java.io.File
import scala.io.{BufferedSource, Source}
import model.Messages._

class Reader extends Actor with ActorLogging {

  override def receive: Receive = {
    case ReadFile(location, extension) =>
      // 1. obtain list of files with extension
      // 2. for each file get contents
      // 3. for each item content send a message to caller
      // 4. after file is processed rename file
      val items = for {
        file <- getListOfFiles(new File(location), extension)
        items <- getFilesContent(file)
      } yield items
      items.foreach(item => sender() ! item)
  }

  private def renameFile(file: File): Boolean = {
    file.renameTo(new File (file.getAbsolutePath + ".OK"))
  }

  private def getListOfFiles(dir: File, extension: String): List[File] = {
    dir.listFiles.filter(_.isFile).toList.filter { file =>
      file.getName.endsWith(extension)
    }
  }

  private def withSourceFile(file: File)(op: BufferedSource => List[Item]): List[Item] = {
    val dataSource = Source.fromFile(file)
    try {
      op(dataSource)
    } finally {
      dataSource.close()
      renameFile(file)
    }
  }

  private def getFilesContent(file: File): List[Item] = {
    withSourceFile(file) { source =>
      val contents = for {
        line <- source.getLines
        Array(itemId, brand, itemType, description, dimensions, price) = line.split(",").map(_.trim)
      } yield Item(
          itemId.toInt,
          brand,
          itemType,
          description,
          dimensions,
          price.toFloat
        )
      contents.toList
    }

  }

}