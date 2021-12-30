package model

object Messages {
  case object StartReading
  case class ReadFile(location: String, filePattern: String)
  case class Item(itemId: Int, brand: String, itemType: String, description: String, dimensions: String, price: Float)
}
