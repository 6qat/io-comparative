import zio._

/** type IO[+E, +A]   = ZIO[Any, E, A]         // Succeed with an `A`, may fail with `E`        , no requirements.
  * type Task[+A]     = ZIO[Any, Throwable, A] // Succeed with an `A`, may fail with `Throwable`, no requirements.
  * type RIO[-R, +A]  = ZIO[R, Throwable, A]   // Succeed with an `A`, may fail with `Throwable`, requires an `R`.
  * type UIO[+A]      = ZIO[Any, Nothing, A]   // Succeed with an `A`, cannot fail              , no requirements.
  * type URIO[-R, +A] = ZIO[R, Nothing, A]     // Succeed with an `A`, cannot fail              , requires an `R`.
  */

val s1 = ZIO.succeed(42)
val s2: Task[Int] = Task.succeed(42)

val now = ZIO.effectTotal(System.currentTimeMillis())

val f1 = ZIO.fail("Uh oh!")
val f2 = Task.fail(new Exception("Uh oh!"))

val zoption = ZIO.fromOption(Some(42.0))
val zoption2: IO[String, Double] = zoption.mapError(_ => "It wasn't there!")

case class User(teamId: String)

class Team

val maybeId: IO[Option[Nothing], String] = ZIO.fromOption(Some("abc123"))
def getUser(userId: String): IO[Throwable, Option[User]] = ???
def getTeam(teamId: String): IO[Throwable, Team] = ???

val result: IO[Throwable, Option[(User, Team)]] = (for {
  id <- maybeId
  user <- getUser(id).some
  team <- getTeam(user.teamId).asSomeError
} yield (user, team)).optional

val zeither = ZIO.fromEither(Right("Success!"))

import scala.util.Try

val ztry = ZIO.fromTry(Try(42 / 0))

val zfun: URIO[Int, Int] =
  ZIO.fromFunction((i: Int) => i * i)

import scala.concurrent.Future

lazy val future = Future.successful("Hello!")
val zfuture: Task[String] =
  ZIO.fromFuture { implicit ec =>
    future.map(_ => "Goodbye!")
  }

import scala.io.StdIn

val getStrLn: Task[String] =
  ZIO.effect(StdIn.readLine())

def putStrLn(line: String): UIO[Unit] =
  ZIO.effectTotal(println(line))

import java.io.IOException

val getStrLn2: IO[IOException, String] =
  ZIO.effect(StdIn.readLine()).refineToOrDie[IOException]

trait AuthError

object legacy {
  def login(onSuccess: User => Unit, onFailure: AuthError => Unit): Unit = ???
}

val login: IO[AuthError, User] =
  IO.effectAsync[AuthError, User] { callback =>
    legacy.login(
      user => callback(IO.succeed(user)),
      err => callback(IO.fail(err))
    )
  }

import zio.blocking._

val sleeping =
  effectBlocking(Thread.sleep(Long.MaxValue))

import java.net.ServerSocket

import zio.UIO

def accept(l: ServerSocket) =
  effectBlockingCancelable(l.accept())(UIO.effectTotal(l.close()))

import scala.io.{Codec, Source}

def download(url: String) =
  Task.effect(
    Source.fromURL(url)(Codec.UTF8).mkString
  )

def safeDownload(url: String) =
  blocking(download(url))
