package net.fyrie.redis
package akka
package bench

import se.scalablesolutions.akka.dispatch.Dispatchers

object Main {
  import Clients._
  
  def main(args: Array[String]) {

    warmup

    benchIncr
    benchList
    benchHash

    Clients.stop
  }

  // Only Akka seems to benefit from warmup, the others take too long anyways
  def warmup {
    (new AkkaIncrBench(100000)).result
    println("Warm up: 1/4")
    (new AkkaIncrBench(100000)(akkaRedisClientHawt)).result
    println("Warm up: 2/4")
    (new AkkaHashBench(10000)).result
    println("Warm up: 3/4")
    (new StdStreamHashBench(10000)).result
    println("Warm up: 4/4, Done!")
  }

  def benchIncr {
    printTable(List("incr (req/s)", "Akka Pipeline", "Akka w/ Hawt", "Standard") :: List(10000,1000,100,10,1).map{
      i => List(i, (new AkkaIncrBench(i)).result.perSec,
                   (new AkkaIncrBench(i))(akkaRedisClientHawt).result.perSec,
                   (new StdIncrBench(i)).result.perSec).map(_.toInt.toString)})
  }

  def benchList {
    printTable(List("list (req/s)", "Akka Pipeline", "Akka w/ Hawt", "Standard") :: List(10000,1000,100,10,1).map{
      i => List(i, (new AkkaListBench(i)).result.perSec,
                   (new AkkaListBench(i))(akkaRedisClientHawt).result.perSec,
                   (new StdListBench(i)).result.perSec).map(_.toInt.toString)})
  }

  def benchHash {
    printTable(List("hash / sort", "Akka Pipeline", "Akka w/ Hawt", "Standard", "Std Stream") :: List(10000,1000,100,10,1).map{
      i => List(i, (new AkkaHashBench(i)).result.perSec,
                   (new AkkaHashBench(i))(akkaRedisClientHawt).result.perSec,
                   (new StdHashBench(i)).result.perSec,
                   (new StdStreamHashBench(i)).result.perSec).map(_.toInt.toString)})
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
  implicit val akkaRedisClient: AkkaRedisClient = new AkkaRedisClient("localhost", 16379)
  val akkaRedisClientHawt: AkkaRedisClient = new AkkaRedisClient("localhost", 16379)(Dispatchers.newHawtDispatcher(false))
  implicit val redisClient: RedisClient = new RedisClient("localhost", 16379)

  def stop {
    akkaRedisClient.stop
    akkaRedisClientHawt.stop
    redisClient.disconnect
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
