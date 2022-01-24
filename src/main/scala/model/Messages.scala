package model

import java.time.LocalDate
import java.util.UUID

object Messages {
  case object StartReadingTyres
  case object StartReadingMovies
  case class ReadFile(location: String, filePattern: String)
  case class Item(itemId: Int, brand: String, itemType: String, description: String, dimensions: String, price: Float)
  case class Movie(video_id: UUID,
                   avg_rating: String,
                   description: String,
                   encoding: String,
                   genres: java.util.Set[String],
                   mpaa_rating: String,
                   preview_thumbnails: String,
                   release_date: LocalDate,
                   tags: java.util.Set[String],
                   title: String,
                   movie_type: String,
                   url: String,
                   user_id: UUID
  )
}
