package net.fyrie.redis
package bench

import akka.util.ByteString
import serialization.Store

object TestData {
  def rot(n: Int, s: String): String =
    s.map[Char, String]{
      case c if c.isLower => (((c - 'a' + n) % 26) + 'a').toChar
      case c if c.isUpper => (((c - 'A' + n) % 26) + 'A').toChar
      case c => c
    }

  def gen(seed: Int): TestData = {
    val text = rot(seed, "Lorem ipsum dolor sit amet")
    TestData(rot(seed, "Lorem::" + seed),
             seed,
             text,
             Some(text.reverse).filter(x => seed % 3 != 0),
             text.slice(0,4).map(_.toInt).product,
             Some(text.reverse.slice(0,4).map(_.toInt).product).filter(x => seed % 5 != 0),
             text.slice(0,4).map(_.toDouble).product / text.slice(4,5).map(_.toDouble).product,
             Some(text.reverse.slice(0,4).map(_.toDouble).product / text.reverse.slice(5,6).map(_.toDouble).product).filter(x => seed % 2 != 0))
  }
}

case class TestData(key: String, rank: Int, text: String, otext: Option[String], integer: Int, ointeger: Option[Int], decimal: Double, odecimal: Option[Double]) {
  def toMap: Map[String, ByteString] = Map("text" -> Some(Store(text)),
                                           "otext" -> otext.map(Store(_)),
                                           "integer" -> Some(Store(integer)),
                                           "ointeger" -> ointeger.map(Store(_)),
                                           "decimal" -> Some(Store(decimal)),
                                           "odecimal" -> odecimal.map(Store(_)),
                                           "rank" -> Some(Store(rank))).collect{ case (k, Some(v)) => (k, v) }
}

trait HashBench {
  def testvalstream = Stream.iterate(1)(1 + _).map(TestData.gen)
}

class AkkaHashBench(iterations: Int)(implicit conn: RedisClient) extends BenchIterations(iterations) with HashBench {
  val key = "akkahashbench"

  override def before {
    conn.sync.flushdb
    testvals foreach { t =>
      conn.quiet.hmset(t.key, t.toMap)
      conn.quiet.sadd(key, t.key)
    }
    assert (conn.sync.scard(key) == iterations)
  }
  override def after {
    conn.sync.flushdb
  }

  val testvals = testvalstream.take(iterations)

  def run = {
    val result = conn.sync.sort(
      key,
      get = Seq("#", "*->rank", "*->text", "*->otext", "*->integer", "*->ointeger", "*->decimal", "*->odecimal"),
      by = Some("*->rank"),
      limit = Limit(0, 100)).parse[String, Int, String, String, Int, Int, Double, Double] flatMap {
        case (Some(k), Some(r), Some(t), ot, Some(i), oi, Some(d), od) => Some(TestData(k, r, t, ot, i, oi, d, od))
        case _ => None
    }
    assert (result == testvals.take(100).force)
  }
}

