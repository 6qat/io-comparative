import java.io.IOException
import java.nio.file.{Files, Path, Paths}

import zio._
import zio.blocking.{Blocking, effectBlocking}
import zio.console._

//import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Try

object ZioLab extends zio.App {

  // : URIO[Console, ExitCode]
  def run(args: List[String]): URIO[Blocking with Console, ExitCode] = {
    //myAppLogic2.exitCode
    countAllWords("/home/guiga/").map(println(_)).exitCode // .as(0)

  }

  val myAppLogic: ZIO[Console, IOException, Unit] =
    for {
      _ <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello, $name, welcome to ZIO!")
    } yield ()

  val myAppLogic2: ZIO[Console, IOException, Unit] =
    putStrLn(line = "Oi! Qual o seu nome?").flatMap { _ =>
      getStrLn.flatMap { str =>
        putStrLn(s"Oi, $str")
      }
    }

  val constant: ZIO[Any, Nothing, Int] = ZIO.succeed(11)
  val neverFails: ZIO[Any, Nothing, String] =
    ZIO.effectTotal("function returning string")
  val mayFail: ZIO[Any, Throwable, String] = ZIO.effect("read file function")
  val fromTry: ZIO[Any, Throwable, String] = ZIO.fromTry(Try("OK!")) // Task

  val anEither: Either[String, Boolean] = Right(true)
  val fromEither: ZIO[Any, String, Boolean] = ZIO.fromEither(anEither)

  def loadUser(id: Int)(implicit ec: ExecutionContext): Future[String] =
    Future {
      "user"
    }

  val fromFuture: Task[String] = ZIO.fromFuture(ec => loadUser(1)(ec))

  def listFiles(dir: String): ZIO[Blocking, Throwable, List[Path]] =
    effectBlocking {
      Files.list(Paths.get(dir)).iterator().asScala.toList
    }

  def readContents(path: Path): ZIO[Blocking, Throwable, String] =
    effectBlocking {
      new String(Files.readAllBytes(path), "UTF-8")
    }

  def countAllWords0(dir: String): ZIO[Blocking, Throwable, Long] = {
    for {
      paths <- listFiles(dir)
      counts <- ZIO.foreach(paths) { path =>
        readContents(path).map { contents =>
          contents.split(' ').length
        }
      }
    } yield counts.sum
  }

  def mapReduce[A, B](dir: String)(
      map: String => A
  )(z: B)(reduce: (B, A) => B): ZIO[Blocking, Throwable, B] = {
    for {
      paths <- listFiles(dir)
      mapped <- ZIO.foreach(paths) { path =>
        readContents(path).map { contents =>
          map(contents)
        }
      }
    } yield mapped.foldLeft(z)(reduce)
  }

  def mapReduceParallel[A, B](dir: String, workers: Int)(
      map: String => A
  )(z: B)(reduce: (B, A) => B): ZIO[Blocking, Throwable, B] = {
    for {
      paths <- listFiles(dir)
      mapped <- ZIO.foreachParN(workers)(paths) { path =>
        readContents(path).map { contents =>
          map(contents)
        }
      }
    } yield mapped.foldLeft(z)(reduce)
  }

  def countAllWords(dir: String): ZIO[Blocking, Throwable, Long] =
    //mapReduce(dir)(_.split(' ').length)(0L)(_ + _)
    mapReduceParallel(dir, 4)(_.split(' ').length)(0L)(_ + _)

  //IO.collectAll(List(IO.never))
  type UserId = Int
  trait DBConnection
  case class User(id: UserId, name: String)

  def getUser(userId: UserId): ZIO[DBConnection, Nothing, Option[User]] =
    UIO(???)
  def createUser(user: User): URIO[DBConnection, Unit] = UIO(???)

  val user: User = User(1, "Blah")

  val created: URIO[DBConnection, Boolean] = for {
    maybeUser <- getUser(user.id)
    res <- maybeUser.fold(createUser(user).as(true))(_ => ZIO.succeed(false))
  } yield res

  //  val dbConnection: DBConnection = ???
  //  val runnable: UIO[Boolean] = created.provide(dbConnection)

  //  val finallyCreated: Any = runtime.unsafeRun(runnable)

}
