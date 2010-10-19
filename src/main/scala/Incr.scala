package net.fyrie.redis
package bench

import Commands._
import serialization.Parse.Implicits._

class AkkaIncrBench(iterations: Int)(implicit conn: RedisClient) extends BenchIterations(iterations) {
  val key = "akkaincrbench"

  override def before {
    conn send flushdb
    conn.resetStats
  }
  override def after {
    conn.printStats
    conn send flushdb
  }

  def run = {
    conn ! set(key, 0L)
    val msg = incr(key)
    iterate { i => conn ! msg }
    assert ((conn send get[Int](key)).get == iterations)
  }
}
