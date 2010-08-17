package net.fyrie.redis
package akka
package bench

object Main {
  import Clients._
  
  def main(args: Array[String]) {

    benchIncr
    benchList

    Clients.stop
  }

  def benchIncr {
    printTable(List("incr (req/s)", "Akka", "Standard", "Old") :: List(100000,10000,1000,100,10).map{
      i => List(i, (new AkkaIncrBench(i)).result.perSec,
                   (new StdIncrBench(i)).result.perSec,
                   (new OldIncrBench(i)).result.perSec).map(_.toInt.toString)})
  }

  def benchList {
    printTable(List("list (req/s)", "Akka", "Standard", "Old") :: List(100000,10000,1000,100,10).map{
      i => List(i, (new AkkaListBench(i)).result.perSec,
                   (new StdListBench(i)).result.perSec,
                   (new OldListBench(i)).result.perSec).map(_.toInt.toString)})
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
  implicit val redisClient: RedisClient = new RedisClient("localhost", 16379)
  implicit val oldRedisClient: com.redis.RedisClient = new com.redis.RedisClient("localhost", 16379)

  def stop {
    akkaRedisClient.stop
    redisClient.disconnect
    oldRedisClient.disconnect
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

abstract class BenchIterations(iterations: Long) extends Bench[BenchIterResult] {

  def iterate(f: (Long) => Unit): Unit = {
    var i = 0L
    while (i < iterations) {
      i += 1L
      f(i)
    }
  }

  def result = BenchIterResult(this.execute, iterations)
}

case class BenchIterResult(nanos: Long, iterations: Long) extends Result {
  def perSec = iterations / seconds
  def millis = nanos / 1000000.0
  def seconds = nanos / 1000000000.0
}
