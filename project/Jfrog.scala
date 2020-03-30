package build.play.grpc

import sbt.AutoPlugin
import sbt.Keys.credentials
import sbt.Keys.isSnapshot
import sbt.Keys.publishArtifact
import sbt.Keys.publishTo
import sbt.Keys.resolvers
import sbt.Keys.version

object Jfrog extends AutoPlugin {
  import sbt._

  def getEnvWithWarning(key: String): String = {
    sys.env.get(key) match {
      case Some(value) => value
      case None        =>
        // scalastyle:off println
        println(s"**** WARNING: ENV VAR '$key' MISSING")
        // scalastyle:on println
        ""
    }
  }

  lazy val jfrogHost: String = "jfrog.namely.land"
  lazy val jfrogUser: String = getEnvWithWarning("JFROG_USERNAME")
  lazy val jfrogPass: String = getEnvWithWarning("JFROG_PASSWORD")

  def getPublishTo(isSnapshot: Boolean): Option[MavenRepository] = {
    val repo: String = if (isSnapshot) "data-sbt-snapshot" else "data-sbt-release"
    val props        = if (isSnapshot) s";build.timestamp=" + new java.util.Date().getTime else ""
    val url          = s"https://$jfrogHost/artifactory/$repo$props"
    Some("Artifactory Realm".at(url))
  }

  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  override def projectSettings = Seq(
    version := sys.env.getOrElse("VERSION", "development"),
    isSnapshot := !version.value.matches("^\\d+\\.\\d+\\.\\d+$"),
    credentials ++= Seq(
      Credentials(
        realm = "Artifactory Realm",
        host = jfrogHost,
        userName = jfrogUser,
        passwd = jfrogPass,
      ),
    ),
    resolvers += "Artima Maven Repository".at("https://repo.artima.com/releases"),
    resolvers += Resolver.jcenterRepo,
    resolvers ++= Seq(
      "Artifactory".at(s"https://$jfrogHost/artifactory/data-sbt-release/"),
    ),
    publishTo := getPublishTo(isSnapshot.value),
    publishArtifact := true,
  )
}
