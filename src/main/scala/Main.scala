package net.fyrie.redis
package akka
package bench

object Main {
  import Clients._
  
  def main(args: Array[String]) {

    benchIncr

    Clients.stop
  }

  def benchIncr {
    val title = "Incr"
    val akkaTitle = "Akka"
    val stdTitle = "Std"
    val results = List(100000,10000,1000,100,10).flatMap(i => List(new AkkaIncrBench(i), new StdIncrBench(i))).map(_.result)
    val grouped = results.grouped(2).toList
    val (maxTitle, maxAkka, maxStd) = grouped.foldLeft((title.length,akkaTitle.length,stdTitle.length)){
      case ((l1, l2, l3), List(a, s)) => (l1 max a.iterations.toString.length, l2 max a.perSec.toInt.toString.length, l3 max s.perSec.toInt.toString.length)
    }
    val formatStr = "%-"+maxTitle+"."+maxTitle+"s | %"+maxAkka+"."+maxAkka+"s  %"+maxStd+"."+maxStd+"s"
    println(formatStr format (title, akkaTitle, stdTitle))
    println(formatStr format ("-" * maxTitle, "-" * maxAkka, "-" * maxStd))
    grouped.foreach {
      case List(a,s) => println(formatStr format (a.iterations.toString, a.perSec.toInt.toString, s.perSec.toInt.toString))
    }
  }
}

object Clients {
  implicit val akkaRedisClient: AkkaRedisClient = new AkkaRedisClient("localhost", 16379)
  implicit val redisClient: RedisClient = new RedisClient("localhost", 16379)

  def stop {
    akkaRedisClient.stop
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
