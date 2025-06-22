package healthcare

import healthcare.model._
import healthcare.service._
import healthcare.web.WebServer
import zio._
import zio.Console._
import zio.http.Server

object Main extends ZIOAppDefault {

  override def run = {
    val program = for {
      _ <- printLine("Starting Health Care System web server on http://localhost:8080")
      _ <- Server.serve(WebServer.app)
    } yield ()

    program.provideLayer(HealthService.live ++ Server.default)
  }
}
