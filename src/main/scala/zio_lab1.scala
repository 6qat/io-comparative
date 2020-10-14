import java.io.IOException

import zio.console.{Console, getStrLn, putStrLn}
import zio.{ExitCode, URIO, ZIO}

object zio_lab1 extends zio.App {

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    myAppLogic.exitCode

  val myAppLogic: ZIO[Console, IOException, Unit] =
    for {
      _ <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello, $name, welcome to ZIO!")
    } yield ()
}
