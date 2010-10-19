package net.fyrie.redis
package bench

import se.scalablesolutions.akka.dispatch.Dispatchers

object Main {
  import Clients._
  
  def main(args: Array[String]) {

    warmup

    benchIncr
    benchList
    benchHash
    benchLatency

    Clients.stop
  }

  // Only Akka seems to benefit from warmup, the others take too long anyways
  def warmup {
    (new AkkaIncrBench(100000)).result
    println("Warm up: 1/2")
    (new AkkaHashBench(10000)).result
    println("Warm up: 2/2")
  }

  def benchLatency {
    printTable(List("latency", "Fyrie (10 clients)", "Fyrie (50 clients)") :: List(100000, 10000,1000).map{
      i => List(i, (new AkkaLatencyBench(i, 10)).result.perSec,
                   (new AkkaLatencyBench(i, 50)).result.perSec).map(_.toInt.toString)})
  }

  def benchIncr {
    printTable(List("incr (req/s)", "Fyrie Redis") :: List(100000, 10000,1000,100,10,2).map{
      i => List(i, (new AkkaIncrBench(i)).result.perSec).map(_.toInt.toString)})
  }

  def benchList {
    printTable(List("list (req/s)", "Fyrie Redis") :: List(100000, 10000,1000,100,10,1).map{
      i => List(i, (new AkkaListBench(i)).result.perSec).map(_.toInt.toString)})
  }

  def benchHash {
    printTable(List("sort (ms)", "Fyrie Redis") :: List(10000,1000,100).map{
      i => List(i, (new AkkaHashBench(i)).result.millis).map(_.toInt.toString)})
  }

  def printTable(data: Seq[Seq[String]]) {
    val cols = data.foldLeft(0){ case (m, row) => m max row.length }
    val maxWidths = data.foldLeft(List.fill(cols)(0)){ case (ml, row) => ml.zipAll(row, "", "").map{ case (m: Int, s: String) => m max s.length } }
    val formatStr = maxWidths.tail.foldLeft("  %"+maxWidths.head+"s  |"){ case (s, m) => s+"  %"+m+"s  "}
    val divider = maxWidths.tail.foldLeft(" "+("-"*(maxWidths.head+2))+" |"){case (s, m) => s+" "+("-"*(m+2))+" "}
    println(divider)
    println(formatStr format (data.head: _*))
    println(divider)
    data.tail.foreach(row => println(formatStr format (row.padTo(cols, ""): _*)))
    println(divider)
    println("")
  }
}

object Clients {
  implicit val redisClient: RedisClient = new RedisClient
  redisClient.startStats()

  def stop {
    redisClient.stop
  }
}

trait Result {
  def nanos: Long
}

trait Bench[T <: Result] {
  def run: Unit
  def before: Unit = ()
  def after: Unit = ()
  def execute: Long = {
    before
    val start = System.nanoTime
    this.run
    val nanos = System.nanoTime - start
    after
    nanos
  }
  def result: T
}

abstract class BenchIterations(iterations: Int) extends Bench[BenchIterResult] {

  def iterate(f: (Int) => Unit): Unit = {
    var i = 0
    while (i < iterations) {
      i += 1
      f(i)
    }
  }

  def result = BenchIterResult(this.execute, iterations)
}

case class BenchIterResult(nanos: Long, iterations: Int) extends Result {
  def perSec = iterations / seconds
  def millis = nanos / 1000000.0
  def seconds = nanos / 1000000000.0
}
