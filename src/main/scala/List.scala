package net.fyrie.redis
package bench

import Commands._

trait ListBench {
  val testvals = Iterator.continually("bar")
}

class AkkaListBench(iterations: Int)(implicit conn: RedisClient) extends BenchIterations(iterations) with ListBench {
  val key = "akkalistbench"

  override def before {
    conn send flushdb
    conn.resetStats
  }
  override def after {
    conn.printStats
    conn send flushdb
  }

  def run = {
    iterate { i => conn ! rpush(key, testvals.next) }
    assert ((conn send llen(key)) == iterations)
    //iterate { i => conn ! lpop(key) }
    (1 to iterations).map{ i => conn !!! lpop(key) }.foreach(x => assert(x.await.result.get.get == "bar"))
    assert ((conn send llen(key)) == 0)
  }
}
