package rlqueue

import cats.implicits.{catsStdInstancesForList, toFoldableOps}
import monix.catnap.MVar
import monix.eval.{Fiber, Task}

import scala.concurrent.duration._

object MonixVersion {

  sealed trait RateLimiterMsg
  case object ScheduledRunQueue extends RateLimiterMsg
  case class Schedule(t: Task[Unit]) extends RateLimiterMsg

  private def runQueue(
      data: RateLimiterQueue[Task[Unit]],
      queue: MVar[Task, RateLimiterMsg]
  ): Task[Unit] = {
    queue
      // (1) take a message from the queue (or wait until one is available)
      .take
      // (2) modify the data structure accordingly
      .map {
        case ScheduledRunQueue => data.notScheduled
        case Schedule(t)       => data.enqueue(t)
      }
      // (3) run the rate limiter queue: obtain the rate-limiter-tasks to be run
      .map(_.run(System.currentTimeMillis()))
      .flatMap { case (tasks, d) =>
        tasks // Seq[RateLimiterQueue.RateLimiterTask[Task[Unit]]]
          // (4) convert each rate-limiter-task to a Monix-Task
          .map {
            case RateLimiterQueue.Run(run) => run
            case RateLimiterQueue.RunAfter(millis) =>
              Task
                .sleep(millis.millis)
                .flatMap(_ => queue.put(ScheduledRunQueue))
          } // Seq[Task[Unit]]
          // (5) fork each converted Monix-Task so that it runs in the background
          .map(x => x.start)
          // (6) sequence a list of tasks which spawn background fibers
          // into one big task which, when run, will spawn all of them
//            .seq
          .sequence_
          .map(_ => d)
      }
      // (7) recursive call to handle the next message,
      // using the updated data structure
      .flatMap(d => runQueue(d, queue))
  }

  class MonixRateLimiter(
      queue: MVar[Task, RateLimiterMsg],
      queueFiber: Fiber[Unit]
  ) {
    def runLimited[T](f: Task[T]): Task[T] = {
      for {
        mv <- MVar.empty[Task, T]()
        _ <- queue.put(Schedule(f.flatMap(mv.put)))
        r <- mv.take
      } yield r
    }
    def stop(): Task[Unit] = {
      queueFiber.cancel
    }
  }

  object MonixRateLimiter {
    def create(maxRuns: Int, per: FiniteDuration): Task[MonixRateLimiter] =
      for {
        queue <- MVar.empty[Task, RateLimiterMsg]()
        runQueueFiber <-
          runQueue(
            RateLimiterQueue[Task[Unit]](maxRuns, per.toMillis),
            queue
          ).start
      } yield new MonixRateLimiter(queue, runQueueFiber)
  }

  def main(args: Array[String]): Unit = {
    val arl = MonixRateLimiter.create(3, 1.microseconds)
//    for {
//    _ <- 1
//    }
  }

}
