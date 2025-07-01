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

  override def run = {
    val program = for {
      config <- ZIO.service[AppConfig]
      runtime <- ZIO.runtime[HealthService with AIAssistantService]

      // Create MCP tools
      tools = HealthMCPTools.createTools(runtime)
      chimpEndpoint = mcpEndpoint(tools, List("mcp"))

      // Start MCP server in the background
      _ <- ZIO.attemptBlocking {
        new Thread(() => {
          println(s"Starting MCP server on http://localhost:${config.mcpServerPort}/mcp")
          NettySyncServer()
            .port(config.mcpServerPort)
            .addEndpoint(chimpEndpoint)
            .startAndWait()
        }).start()
      }

      _ <- printLine(s"Starting AI-Powered Health Care System web server on http://localhost:${config.webServerPort}")
      _ <- printLine(s"MCP server available at http://localhost:${config.mcpServerPort}/mcp")
      _ <- printLine("✅ DEEPSEEK_API_KEY loaded - AI features enabled!")

      // Start web server with both services
      _ <- Server.serve(WebServer.app)
        .provide(
          Server.defaultWithPort(config.webServerPort),
          HealthService.live,
          AIAssistantService.live,
          AppConfig.live
        )
    } yield ()

    program.provideLayer(
      AppConfig.live ++
        HealthService.live ++
        (AppConfig.live >>> AIAssistantService.live)
    )
  }
}