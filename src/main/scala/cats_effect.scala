import java.util.UUID

import cats.effect.{Blocker, Clock, ExitCode, IO, IOApp, Timer}
import cats.implicits.{catsSyntaxTuple2Parallel, catsSyntaxTuple2Semigroupal}

import scala.concurrent.duration._

object cats_effect extends IOApp {

  val rndUUID: IO[UUID] = IO(UUID.randomUUID())
  val helloIO: IO[Unit] = rndUUID.flatMap(uuid => IO(println(s"Hello $uuid")))
  helloIO.unsafeRunSync()
  helloIO.unsafeRunSync()

  Timer[IO].sleep(5.seconds)
  Clock[IO].realTime(NANOSECONDS)

  IO.race(helloIO, helloIO)
  helloIO.timeout(1.second)
  helloIO.timeoutTo(1.second, helloIO)

  val result: IO[(Unit, Unit)] =
    (helloIO, helloIO).tupled
  val parResult: IO[(Unit, Unit)] =
    (helloIO, helloIO).parTupled

  import concurrent.ExecutionContext.Implicits.global
  val blocker: Blocker = Blocker.liftExecutionContext(global)

  override def run(args: List[String]): IO[ExitCode] = {

    IO.pure(ExitCode.Success)
  }
}
