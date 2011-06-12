package net.fyrie.redis
package bench

import akka.actor.Actor._
import akka.dispatch._

class AkkaLatencyBench(iterations: Int, connections: Int)(implicit conn: RedisClient) extends BenchIterations(iterations) {
  val key = "akkalatencybench"

  override def before {
    conn.sync.flushdb
    conn.sync.set(key, 0)
  }
  override def after {
    conn.sync.flushdb
  }

  def run = {
    val connIterations = iterations / connections
    val totalIterations = connIterations * connections
    val futures = (1 to connections).map(i => Promise[Boolean]()).toList
    val threads = futures map { f =>
      new Thread {
        override def run() {
          (1 to connIterations) foreach {i => conn.sync.incr(key)}
          f.completeWithResult(true)
        }
      }
    }
    threads.foreach(_.start())
    futures.foreach(f => f.await)
    assert (conn.sync.get(key).parse[Int].get == totalIterations)
  }
}

