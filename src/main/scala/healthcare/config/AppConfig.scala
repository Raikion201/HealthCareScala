package healthcare.config

import zio._
import java.io.File
import scala.io.Source
import java.lang.{System => JSystem}

case class DeepSeekConfig(
  apiKey: String,
  baseUrl: String = "https://api.deepseek.com/",
  model: String = "deepseek-chat"
)

case class AppConfig(
  deepSeek: DeepSeekConfig,
  webServerPort: Int = 8080,
  mcpServerPort: Int = 8081
)

object AppConfig {
  
  private def loadEnvFile(): Unit = {
    val envFile = new File(".env")
    if (envFile.exists()) {
      val source = Source.fromFile(envFile)
      try {
        source.getLines()
          .filter(line => line.trim.nonEmpty && !line.trim.startsWith("#"))
          .foreach { line =>
            val parts = line.split("=", 2)
            if (parts.length == 2) {
              val key = parts(0).trim
              val value = parts(1).trim
              JSystem.setProperty(key, value)
            }
          }
      } finally {
        source.close()
      }
    }
  }
  
  val live: ZLayer[Any, SecurityException, AppConfig] = ZLayer.fromZIO {
    for {
      _ <- ZIO.attempt(loadEnvFile()).orDie
      apiKey <- ZIO.fromOption(
        Option(JSystem.getProperty("DEEPSEEK_API_KEY"))
          .orElse(Option(JSystem.getenv("DEEPSEEK_API_KEY")))
      ).mapError(_ => new SecurityException("DEEPSEEK_API_KEY environment variable is required"))
    } yield AppConfig(
      deepSeek = DeepSeekConfig(apiKey = apiKey)
    )
  }
}

