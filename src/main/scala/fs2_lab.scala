object fs2_lab extends App {
  import cats.effect.IO
  import fs2._

  Stream(1, 0).repeat.take(6).toList

  Stream(1, 2, 3).drain.toList

  Stream.eval_(IO(println("!!"))).compile.toVector.unsafeRunSync()

  val o3: List[Either[Throwable, Int]] =
    (Stream(1, 2) ++ Stream(3)
      .map(_ => throw new Exception("nooo!!!"))).attempt.toList

  def tk[F[_], O](initialState: Long): Pipe[F, O, O] =
    (in: Stream[F, O]) => {

      val out: Stream[F, O] =
        in
          .scanChunksOpt(initialState) { currentState: Long =>
            if (currentState <= 0) None
            else
              Some((c: Chunk[O]) => {
                val tup: (Long, Chunk[O]) = c.size match {
                  case m if m < currentState => (currentState - m, c)
                  case m                     => (0, c.take(currentState.toInt))
                }
                tup
              })
          }

      out

    }

  def tk2[F[_], O](n: Long): Pipe[F, O, O] = {
    def go(s: Stream[F, O], n: Long): Pull[F, O, Unit] = {
      s.pull.uncons.flatMap {
        case Some((hd, tl)) =>
          hd.size match {
            case m if m <= n => Pull.output(hd) >> go(tl, n - m)
            case m           => Pull.output(hd.take(n.toInt)) >> Pull.done
          }
        case None => Pull.done
      }
    }
    in => go(in, n).stream
  }

  val o4: List[Int] = Stream(1, 2, 3, 4, 5, 6, 7, 8).through(tk(3)).toList
  println(o4)
  println(Stream(1, 2, 3, 4, 5, 6, 7, 8).through(tk2(3)).toList)

}
