package zio.wordcount

import zio.App
import zio.console._
import zio.stream._
import zio.Schedule
import java.time.Duration
import zio.clock._
import zhttp.http._
import zhttp.service.Server
import zio._

object Application extends App {

  def run(args: List[String]) = 
    for {
      ref <- Ref.make(StreamStateSummary.empty)
      _   <- createStream(ref).fork
      ec  <- Server.start(8090, createApp(ref)).exitCode
    } yield ec  
  
  val ticks = ZStream.repeatEffectWith(instant, Schedule.spaced(Duration.ofSeconds(5)))

  def createStream(state: Ref[StreamStateSummary]) = 
    CLIProcess.createBlackBoxStream("~/Downloads/blackbox.macosx")
      .tap(i => putStrLn(s"Received from BlackBox: '${i}'"))
      .map(p => Event.fromPayload(EventPayload(p)))
      .collect { case Right(v) => v }
      .map(WordCountObservation.fromEvent)
      .collect { case Some(v) => v }
      .grouped(5)
      .zipWithLatest(ticks) ( (o, i) => (o.toList, i) )
      .scan(StreamState(List.empty))(  (s, o) => s.withObservations(o._1, o._2) )
      .tap(s => putStrLn(s"Current State Debug: ${s.summarize}"))
      .mapM(s => state.set(s.summarize))
      .runDrain
      
  def createApp(state: Ref[StreamStateSummary]) = Http.collectM[Request] {
    case Method.GET -> Root => state.get.map(s => Response.text(s.prettyPrint))
  }      
}
