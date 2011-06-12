package net.fyrie.redis
package bench

trait ListBench {
  val testvals = Iterator.continually("bar")
}

class AkkaListBench(iterations: Int)(implicit conn: RedisClient) extends BenchIterations(iterations) with ListBench {
  val key = "akkalistbench"

  override def before {
    conn.sync.flushdb
  }
  override def after {
    conn.sync.flushdb
  }

  def run = {
    val q = conn.quiet
    iterate { i => q.rpush(key, testvals.next) }
    assert (conn.sync.llen(key) == iterations)
    iterate { i => q.lpop(key) }
    //(1 to iterations).map{ i => conn !!! lpop(key) }.foreach(x => assert(x.await.result.get.get == "bar"))
    assert (conn.sync.llen(key) == 0)
  }
}

