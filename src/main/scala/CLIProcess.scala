package zio.wordcount

import scala.sys.process._
import zio.stream._

object CLIProcess {
  def createBlackBoxStream(executablePath: String) = {
    val process = Process(s"/bin/bash -c ${executablePath}")

    ZStream.fromIterator(process.lazyLines.iterator)
  }
}