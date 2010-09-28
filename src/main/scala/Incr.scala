package net.fyrie.redis
package akka
package bench

import Commands._
import net.fyrie.redis.akka.collection._

class AkkaIncrBench(iterations: Long)(implicit conn: AkkaRedisClient) extends BenchIterations(iterations) {
  val key = "akkaincrbench"

  override def before { conn send flushdb }
  override def after { conn send flushdb }

  def run = {
    val n = RedisLongVar(key, Some(0L))
    iterate { i => n.incrFast }
    assert (n.get == iterations)
  }
}

class StdIncrBench(iterations: Long)(implicit conn: RedisClient) extends BenchIterations(iterations) {
  val key = "std-incrbench"

  import serialization.Parse.Implicits.parseLong

  override def before { conn send flushdb }
  override def after { conn send flushdb }

  def run = {
    conn send set(key, 0L)
    val msg = incr(key)
    iterate { i => conn send msg }
    assert ((conn send get(key)).get == iterations)
  }
}

class AkkaWorkerIncrBench(iterations: Long)(implicit conn: AkkaRedisWorkerPool) extends BenchIterations(iterations) {
  val key = "workincrbench"

  import serialization.Parse.Implicits.parseLong

  override def before { conn send flushdb }
  override def after { conn send flushdb }

  def run = {
    conn ! set(key, 0L)
    val msg = incr(key)
    (1 to iterations.toInt).map{ i => conn !!! msg }.foreach(_.awaitBlocking.result)
    assert ((conn send get(key)).get == iterations)
  }
}
/*
class OldIncrBench(iterations: Long)(implicit conn: com.redis.RedisClient) extends BenchIterations(iterations) {
  val key = "old-incrbench"

  override def before { conn.flushdb }
  override def after { conn.flushdb }

  def run = {
    conn.set(key, "0")
    iterate { i => conn.incr(key) }
    assert (((conn.get(key)).get).toLong == iterations)
  }
}
*/
