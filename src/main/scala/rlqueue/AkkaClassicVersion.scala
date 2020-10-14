package rlqueue

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future, Promise}

object AkkaClassicVersion {

  private case class LazyFuture(t: () => Future[Unit])
  private case object ScheduledRunQueue

  private class RateLimiterActor(maxRuns: Int, per: FiniteDuration)
      extends Actor
      with ActorLogging {

    // mutable actor state: the current rate limiter queue; the queue itself is
    // immutable, but the reference is mutable and access to it is protected by
    // the actor. // GUIGA: made it immutable
    private val queue = RateLimiterQueue[LazyFuture](maxRuns, per.toMillis)
    override def receive: Receive = onMessage(queue)

    private def onMessage(queue: RateLimiterQueue[LazyFuture]): Receive = {
      case lf: LazyFuture =>
        // enqueueing the new computation and checking if any computations can be run
        context.become(onMessage(queue.enqueue(lf)))
        runQueue()

      case ScheduledRunQueue =>
        // clearing the `scheduled` flag, as we are in a scheduled run right now, so it's
        // possible a new one has to be scheduled
        context.become(onMessage(queue.notScheduled))
        runQueue()
    }

    private def runQueue(): Unit = {
      val now = System.currentTimeMillis()

      val (tasks, queue2) = queue.run(now)
      // Updating the mutable reference to store the new queue.
      context.become(onMessage(queue2))
      // Each task returned by `queue.run` is turned into a side-effect: either running
      // the lazy future (which amounts to running the block of code which creates the
      // future - and hence makes the computation run), or scheduling a `rlqueue.ScheduledRunQueue`
      // message to be sent to the actor after the given delay.
      import context.dispatcher
      tasks.foreach {
        case RateLimiterQueue.Run(LazyFuture(f)) => f()
        case RateLimiterQueue.RunAfter(millis) =>
          context.system.scheduler.scheduleOnce(
            millis.millis,
            self,
            ScheduledRunQueue
          )
      }
    }
  }

  class AkkaRateLimiter(rateLimiterActor: ActorRef) {
    def runLimited[T](
        f: => Future[T]
    )(implicit ec: ExecutionContext): Future[T] = {
      val p = Promise[T]()
      val msg =
        LazyFuture(() =>
          f.andThen { case r => p.complete(r) }
            .map(_ => ())
        )
      rateLimiterActor ! msg
      p.future
    }
  }

  object AkkaRateLimiter {
    def create(maxRuns: Int, per: FiniteDuration)(implicit
        actorSystem: ActorSystem
    ): AkkaRateLimiter = {

      val rateLimiterActor =
        actorSystem.actorOf(Props(new RateLimiterActor(maxRuns, per)))
      new AkkaRateLimiter(rateLimiterActor)
    }
  }

  def main(args: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val actorSystem: ActorSystem = ActorSystem("system")
    val arl = AkkaRateLimiter.create(3, 1.microseconds)
    arl.runLimited(Future { 1 })
    arl.runLimited(Future { 2 })
    arl.runLimited(Future { 3 })
    arl.runLimited(Future { 4 })
    arl.runLimited(Future { 5 })
  }

}
