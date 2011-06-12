package net.fyrie.redis
package bench

class AkkaIncrBench(iterations: Int)(implicit conn: RedisClient) extends BenchIterations(iterations) {
  val key = "akkaincrbench"

  override def before {
    conn.sync.flushdb
  }
  override def after {
    conn.sync.flushdb
  }

  def run = {
    val q = conn.quiet
    q.set(key, 0L)
    iterate { i => q.incr(key) }
    assert(conn.sync.get(key).parse[Long].get == iterations)
  }
}
