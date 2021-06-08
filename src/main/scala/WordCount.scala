package zio.wordcount

import java.time.Instant
import scala.util.Try
import java.time.Duration

final case class WordCountObservation(word: String, registeredAt: Instant, sourcedAt: Instant)
object WordCountObservation {
  def fromEvent(e: Event) = Try(WordCountObservation(e.data, Instant.now(), Instant.ofEpochSecond(e.timestamp))).toOption
}

final case class StreamStateSummary(private val map: Map[String, Int]) {
  def prettyPrint = 
    if (map.isEmpty) "No occurences of any word happened yet. Refresh the page in a bit!"
    else map.foldLeft("Word Count summary: \n\n")( (s, i) => s + s"Word ${i._1} occured ${i._2} time(s).\n")
}
object StreamStateSummary {
  def empty = StreamStateSummary(Map.empty)
}

final case class StreamState(private val observations: List[WordCountObservation]) {
  val window = Duration.ofMinutes(5)
  def summarize : StreamStateSummary = {
    val summary = 
      observations
        .foldLeft(Map.empty[String, Int])( (s, o) => {
          val count = s.get(o.word).map(_ + 1).getOrElse(1)
          s.updated(o.word, count)
        })

    StreamStateSummary(summary)
  }

  def withObservations(o: List[WordCountObservation], tick: java.time.Instant) = {
    val filtered = (observations ++ o).filter(_.registeredAt.isAfter(tick.minusMillis(window.toMillis())))
    StreamState(filtered)
  }
}
