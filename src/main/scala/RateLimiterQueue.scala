import akka.actor.{Actor, ActorLogging}

import scala.collection.immutable.Queue
import scala.concurrent.Future
import scala.concurrent.duration._

sealed trait RateLimiterTask[F]
case class Run[F](run: F) extends RateLimiterTask[F]
case class RunAfter[F](millis: Long) extends RateLimiterTask[F]

/** Queue of rate-limited computations. The computations will be *started* so that at
  * any time, there's at most `maxRuns` in any time `perMillis` window.
  *
  * Note that this does not take into account the duration of the computations, when
  * they end or when they reach a remote server.
  *
  * @param scheduled Is an invocation of `run` already scheduled (by returning an
  *                  appropriate task in the previous invocation): used to prevent
  *                  scheduling too much runs; it's enough if there's only one run
  *                  scheduled at any given time.
  * @tparam F Type of computations. Should be a lazy wrapper, so that computations can
  *           be enqueued for later execution.
  */
case class RateLimiterQueue[F](
    maxRuns: Int,
    perMillis: Long,
    lastTimestamps: Queue[Long] = Queue(), // started computations
    waiting: Queue[F] = Queue(), // waiting computations
    scheduled: Boolean = false
) {

  /** Given the timestamp, obtain a list of task which might include running a
    * computation or scheduling a `run` invocation in the future, and an updated
    * queue.
    */
  def run(now: Long): (List[RateLimiterTask[F]], RateLimiterQueue[F]) = {
    pruneTimestamps(now).doRun(now)
  }

  /** Add a request to the queue. Doesn't run any pending requests.
    */
  def enqueue(f: F): RateLimiterQueue[F] = copy(waiting = waiting.enqueue(f))

  /** Before invoking a scheduled `run`, clear the scheduled flag.
    * If needed, the next `run` invocation might include a `RunAfter` task.
    */
  def notScheduled: RateLimiterQueue[F] = copy(scheduled = false)

  private def doRun(
      now: Long
  ): (List[RateLimiterTask[F]], RateLimiterQueue[F]) = {
    if (lastTimestamps.size < maxRuns) {
      waiting.dequeueOption match {
        case Some((io, w)) =>
          val (tasks, next) =
            copy(lastTimestamps = lastTimestamps.enqueue(now), waiting = w)
              .run(now)
          (Run(io) :: tasks, next)
        case None =>
          (Nil, this)
      }
    } else if (!scheduled) {
      val nextAvailableSlot = perMillis - (now - lastTimestamps.head)
      (List(RunAfter(nextAvailableSlot)), this.copy(scheduled = true))
    } else {
      (Nil, this)
    }
  }

  /** Remove timestamps which are outside of the current time window, that is
    * timestamps which are further from `now` than `timeMillis`.
    */
  private def pruneTimestamps(now: Long): RateLimiterQueue[F] = {
    val threshold = now - perMillis
    copy(lastTimestamps = lastTimestamps.filter(_ >= threshold))
  }

}

// ====================================================================================================================

private case class LazyFuture(t: () => Future[Unit])
private case object ScheduledRunQueue

private class RateLimiterActor(maxRuns: Int, per: FiniteDuration)
    extends Actor
    with ActorLogging {

  import context.dispatcher

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
    // future - and hence makes the computation run), or scheduling a `ScheduledRunQueue`
    // message to be sent to the actor after the given delay.
    tasks.foreach {
      case Run(LazyFuture(f)) => f()
      case RunAfter(millis) =>
        context.system.scheduler.scheduleOnce(
          millis.millis,
          self,
          ScheduledRunQueue
        )
    }
  }

}
