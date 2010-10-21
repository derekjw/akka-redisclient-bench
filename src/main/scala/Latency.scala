package net.fyrie.redis
package bench

import Commands._
import serialization.Parse.Implicits._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.dispatch._

class AkkaLatencyBench(iterations: Int, connections: Int)(implicit conn: RedisClient) extends BenchIterations(iterations) {
  val key = "akkalatencybench"

  override def before {
    conn send flushdb
    conn send set(key, 0)
  }
  override def after {
    conn send flushdb
  }

  def run = {
    val msg = incr(key)
    val connIterations = iterations / connections
    val totalIterations = connIterations * connections
    val futures = (1 to connections).map(i => new DefaultCompletableFuture[Boolean](5000)).toList
    val threads = futures map { f =>
      new Thread {
        override def run() {
          (1 to connIterations) foreach {i => (conn send msg)}
          f.completeWithResult(true)
        }
      }
    }
    threads.foreach(_.start())
    futures.foreach(f => f.awaitBlocking)
    assert ((conn send get[Int](key)).get == totalIterations)
  }
}
