package healthcare

import healthcare.model._
import healthcare.service._
import healthcare.web.WebServer
import healthcare.mcp.HealthMCPTools
import healthcare.config.AppConfig
import zio._
import zio.Console._
import zio.http.Server
import chimp._
import sttp.tapir.server.netty.sync.NettySyncServer

object Main extends ZIOAppDefault {

  private def startMCPServer(config: AppConfig): Task[Unit] =
    ZIO.attemptBlocking {
      val tools = HealthMCPTools.createTools
      val chimpEndpoint = mcpEndpoint(tools, List("mcp"))

      new Thread(() => {
        println(s"Starting MCP server on http://localhost:${config.mcpServerPort}/mcp")
        NettySyncServer()
          .port(config.mcpServerPort)
          .addEndpoint(chimpEndpoint)
          .startAndWait()
      }).start()
    }

  override def run = {
    val program = for {
      config <- ZIO.service[AppConfig]

      _ <- startMCPServer(config)

      _ <- printLine(s"Starting AI-Powered Health Care System web server on http://localhost:${config.webServerPort}")
      _ <- printLine(s"MCP server available at http://localhost:${config.mcpServerPort}/mcp")
      _ <- printLine("✅ DEEPSEEK_API_KEY loaded - AI features enabled!")

      _ <- Server.serve(WebServer.app)
        .provide(
          Server.defaultWithPort(config.webServerPort),
          ZLayer.succeed(config)
        )
    } yield ()

    program.provide(
      AppConfig.live.mapError(_.asInstanceOf[Throwable])
    )
  }
}