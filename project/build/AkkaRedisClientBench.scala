import sbt._
import sbt.CompileOrder._

class AkkaRedisClientBenchProject(info: ProjectInfo) extends DefaultProject(info) with AkkaBaseProject
{
  override def compileOptions = Optimize :: Unchecked :: super.compileOptions.toList

  val akkaRedisClient         = "net.fyrie" %% "akka-redisclient" % "0.1-SNAPSHOT"
  //val redis                   = "com.redis" % "redisclient" % "2.8.0-1.4"

  val fyrieReleases           = "Fyrie releases" at "http://repo.fyrie.net/releases"
  val fyrieSnapshots          = "Fyrie snapshots" at "http://repo.fyrie.net/snapshots"
  val scalaToolsSnapshots     = ScalaToolsSnapshots

}
