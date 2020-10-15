import monix.bio.{IO, UIO}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
// Monix uses scheduler instead of the execution context
// from scala standard library, to learn more check
// https://monix.io/docs/3x/execution/scheduler.html
import monix.execution.Scheduler.Implicits.global

object MonixLab {
  def main(args: Array[String]): Unit = {
    // Unlike Future, IO is lazy and nothing gets executed at this point
    val bio = IO { println("Hello, World!"); "Hi" }

    // We can run it as a Scala's Future
    val scalaFuture: Future[String] = bio.runToFuture

    // Used only for demonstration,
    // think three times before you use `Await` in production code!
    val result: String = Await.result(scalaFuture, 5.seconds)

    println(result) // Hi is printed

    val taskA: UIO[Int] = IO
      .now(10)
      .delayExecution(2.seconds)
      // executes the finalizer on cancelation
      .doOnCancel(UIO(println("taskA has been cancelled")))

  }
}
