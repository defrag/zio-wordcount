package zio.wordcount

import zio.json._

final case class EventPayload(payload: String)

sealed trait EventType 
object EventType {
  case object Foo extends EventType
  case object Bar extends EventType
  implicit val decoder: JsonDecoder[EventType] = DeriveJsonDecoder.gen[EventType]
}

final case class Event(event_type: String, data: String, timestamp: Long) 
object Event {
  implicit val decoder: JsonDecoder[Event] = DeriveJsonDecoder.gen[Event]
  def fromPayload(ep: EventPayload) : Either[String, Event] = ep.payload.fromJson[Event]
}