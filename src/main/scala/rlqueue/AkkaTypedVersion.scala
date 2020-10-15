package rlqueue

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorSystem, Behavior}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

object AkkaTypedVersion {

  sealed trait RateLimiterMsg
  case class LazyFuture(t: () => Future[Unit]) extends RateLimiterMsg
  case object ScheduledRunQueue extends RateLimiterMsg

  def rateLimit(
      timer: TimerScheduler[RateLimiterMsg],
      data: RateLimiterQueue[LazyFuture]
  ): Behavior[RateLimiterMsg] =
    Behaviors.receiveMessage {
      case lf: LazyFuture => rateLimit(timer, runQueue(timer, data.enqueue(lf)))
      case ScheduledRunQueue =>
        rateLimit(timer, runQueue(timer, data.notScheduled))
    }

  def runQueue(
      timer: TimerScheduler[RateLimiterMsg],
      data: RateLimiterQueue[LazyFuture]
  ): RateLimiterQueue[LazyFuture] = {

    val now = System.currentTimeMillis()

    val (tasks, data2) = data.run(now)
    tasks.foreach {
      case RateLimiterQueue.Run(LazyFuture(f)) => f()
      case RateLimiterQueue.RunAfter(millis) =>
        timer.startSingleTimer((), ScheduledRunQueue, millis.millis)
    }

    data2
  }

  class AkkaTypedRateLimiter(actorSystem: ActorSystem[RateLimiterMsg]) {

    def runLimited[T](
        f: => Future[T]
    )(implicit ec: ExecutionContext): Future[T] = {
      val p = Promise[T]
      actorSystem ! LazyFuture(() =>
        f.andThen { case r => p.complete(r) }.map(_ => ())
      )
      p.future
    }
  }

  object AkkaTypedRateLimiter {
    def create(maxRuns: Int, per: FiniteDuration): AkkaTypedRateLimiter = {
      val behavior = Behaviors.withTimers[RateLimiterMsg] { timer =>
        rateLimit(timer, RateLimiterQueue(maxRuns, per.toMillis))
      }
      new AkkaTypedRateLimiter(ActorSystem(behavior, "rate-limiter"))
    }
  }

  def main(args: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val arl = AkkaTypedRateLimiter.create(1, 1.microseconds)
    arl.runLimited(Future { Thread.sleep(1000); println("Teste 1"); 1 })
    arl.runLimited(Future { Thread.sleep(2000); println("Teste 2"); 2 })
    arl.runLimited(Future { Thread.sleep(3000); println("Teste 3"); 3 })
    arl.runLimited(Future { Thread.sleep(4000); println("Teste 4"); 4 })

  }

}
