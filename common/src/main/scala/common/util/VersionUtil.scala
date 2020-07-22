package common.util

import java.nio.file.{Files, Paths}

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

import scala.collection.JavaConverters._

/**
  * Retrieves the version from an SBT generated config file.
  *
  * See project/Version.scala for the generator.
  */
object VersionUtil {

  /**
    * Returns the name of a project's conf file.
    *
    * For "my-project", returns "my-project-version.conf".
    */
  def versionConf(projectName: String): String = s"$projectName-version.conf"

  /**
    * Returns the name of a project's version property.
    *
    * For "my-project", returns "my.project.version".
    */
  def versionProperty(projectName: String): String = s"${projectName.replace("-", ".")}.version"

  /**
    * Returns the version for a project with a possible fallback when the version conf cannot be loaded.
    *
    * @param projectName The hyphenated name of the project, ex: "my-project"
    * @param default     What to return when the version cannot be found. The parameter passed is the `projectName`.
    * @return The version from the conf or the default
    */
  def getVersion(projectName: String, default: String => String = defaultMessage): String = {
    ConfigFactory
      .load(versionConf(projectName))
      .as[Option[String]](versionProperty(projectName))
      .getOrElse(default(projectName))
  }

  /**
    * Instead of returning a version, states that the version conf will be generated by sbt.
    */
  def defaultMessage(projectName: String): String = {
    s"${versionConf(projectName)}-to-be-generated-by-sbt"
  }

  /**
    * A regex compatible with the dependency constants in project/Dependencies.scala.
    */
  private val SbtDependencyVersionRegex = """\s+private val (\w+)V = "([^"]+)"""".r

  /**
    * Returns the dependency version grepped from project/Dependencies.scala.
    *
    * @param dependencyName The dependency name, ex: "myDependency"
    * @param projectName    The hyphenated name of the dependency/project, ex: "my-dependency"
    * @return The dependency version from project/Dependencies.scala
    * @throws RuntimeException If the dependency cannot be found
    */
  def sbtDependencyVersion(dependencyName: String)(projectName: String): String = {
    try {
      val dependencies = Paths.get("project/Dependencies.scala").toAbsolutePath
      val lines = Files.readAllLines(dependencies).asScala
      val versionOption = lines collectFirst {
        case SbtDependencyVersionRegex(name, version) if name == dependencyName => version
      }
      versionOption getOrElse {
        throw new RuntimeException(s"Did not parse a version for '${dependencyName}V' from $dependencies")
      }
    } catch {
      case e: Exception =>
        throw new RuntimeException(
          s"${e.getMessage} (This occurred after ${versionConf(projectName)} was not found.)",
          e
        )
    }
  }

}