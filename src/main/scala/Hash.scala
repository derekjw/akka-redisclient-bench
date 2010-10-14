package net.fyrie.redis
package bench

import Commands._

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
  def toMap: Map[String, Any] = Map("text" -> Some(text),
                                    "otext" -> otext,
                                    "integer" -> Some(integer),
                                    "ointeger" -> ointeger,
                                    "decimal" -> Some(decimal),
                                    "odecimal" -> odecimal,
                                    "rank" -> Some(rank)).collect{ case (k, Some(v)) => (k, v) }
}

trait HashBench {
  def testvalstream = Stream.iterate(1)(1 + _).map(TestData.gen)
}

class AkkaHashBench(iterations: Int)(implicit conn: RedisClient) extends BenchIterations(iterations) with HashBench {
  import serialization.Parse.Implicits._

  val key = "akkahashbench"

  override def before { conn send flushdb }
  override def after { conn send flushdb }

  val testvals = testvalstream.take(iterations)

  def run = {
    testvals foreach (t => conn ! multiexec(Seq(hmset(t.key, t.toMap), sadd(key, t.key))))
    assert ((conn send scard(key)) == iterations)
    val result = conn send sort8[String, Int, String, String, Int, Int, Double, Double](
      key,
      get = ("#", "*->rank", "*->text", "*->otext", "*->integer", "*->ointeger", "*->decimal", "*->odecimal"),
      by = Some("*->rank")) map { _.collect{
      case (Some(k), Some(r), Some(t), ot, Some(i), oi, Some(d), od) => TestData(k, r, t, ot, i, oi, d, od)
    }} getOrElse (Stream.empty)
    assert (result.take(100).force == testvals.take(100).force)
  }
}
