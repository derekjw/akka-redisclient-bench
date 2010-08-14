package net.fyrie.redis
package akka
package bench

import commands._
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

  implicit def toBytes(in: Any): Array[Byte] = in.toString.getBytes
  implicit def fromBytes(in: Array[Byte]): String = new String(in)

  override def before { conn send flushdb }
  override def after { conn send flushdb }

  def run = {
    conn send set(key, 0L)
    val msg = incr(key)
    iterate { i => conn send msg }
    assert (fromBytes((conn send get(key)).get).toLong == iterations)
  }
}
