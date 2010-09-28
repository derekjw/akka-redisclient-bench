package net.fyrie.redis
package akka
package bench

import Commands._
import net.fyrie.redis.akka.collection._

trait ListBench {
  val testvals = Iterator.continually("bar")
}

class AkkaListBench(iterations: Long)(implicit conn: AkkaRedisClient) extends BenchIterations(iterations) with ListBench {
  val key = "akkalistbench"

  override def before { conn send flushdb }
  override def after { conn send flushdb }

  def run = {
    val n = RedisList[String](key)
    iterate { i => n += testvals.next }
    assert (n.length == iterations) // remove this line for even more performance
    val futures = (1L to iterations).map(i => n.lpopFuture)
    futures.foreach(f => assert(f.await.result.get.get == "bar"))
    assert (n.length == 0)
  }
}

class AkkaWorkerListBench(iterations: Long)(implicit conn: AkkaRedisWorkerPool) extends BenchIterations(iterations) with ListBench {
  val key = "worklistbench"

  override def before { conn send flushdb }
  override def after { conn send flushdb }

  def run = {
    (1 to iterations.toInt).map{ i => conn !!! rpush(key, testvals.next) }.foreach(_.await)
    assert ((conn send llen(key)) == iterations)
    (1 to iterations.toInt).map{ i => conn !!! lpop(key) }.foreach(x => assert(x.await.result.get.get == "bar"))
    assert ((conn send llen(key)) == 0)
  }
}

class StdListBench(iterations: Long)(implicit conn: RedisClient) extends BenchIterations(iterations) with ListBench {
  val key = "std-listbench"

  override def before { conn send flushdb }
  override def after { conn send flushdb }

  def run = {
    iterate { i => conn send rpush(key, testvals.next) }
    assert ((conn send llen(key)) == iterations)
    iterate { i => assert((conn send lpop(key)).get == "bar") }
    assert ((conn send llen(key)) == 0)
  }
}
/*
class OldListBench(iterations: Long)(implicit conn: com.redis.RedisClient) extends BenchIterations(iterations) with ListBench with StringImplicits {
  val key = "old-listbench"

  override def before { conn.flushdb }
  override def after { conn.flushdb }

  def run = {
    iterate { i => conn.rpush(key, testvals.next) }
    assert (((conn.llen(key)).get).toLong == iterations)
    iterate { i => assert((conn.lpop(key)).get == "bar") }
    assert (((conn.llen(key)).get).toLong == 0)
  }
}
*/
