import sbt._
import sbt.CompileOrder._

class AkkaRedisClientBenchProject(info: ProjectInfo) extends DefaultProject(info) with AkkaBaseProject
{
  override def compileOptions = Optimize :: Unchecked :: super.compileOptions.toList

  val akkaRedisClient         = "net.fyrie" %% "fyrie-redis" % "0.1-SNAPSHOT"

  val fyrieReleases           = "Fyrie releases" at "http://repo.fyrie.net/releases"
  val fyrieSnapshots          = "Fyrie snapshots" at "http://repo.fyrie.net/snapshots"
  val scalaToolsSnapshots     = ScalaToolsSnapshots

  val akkaModuleConfig        = ModuleConfiguration("se.scalablesolutions.akka", AkkaRepositories.AkkaRepo)
}
