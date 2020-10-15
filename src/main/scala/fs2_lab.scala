class fs2_lab {
  import cats.effect.IO
  import fs2._

  Stream(1, 0).repeat.take(6).toList

  Stream(1, 2, 3).drain.toList

  Stream.eval_(IO(println("!!"))).compile.toVector.unsafeRunSync()

  val o3: List[Either[Throwable, Int]] =
    (Stream(1, 2) ++ Stream(3)
      .map(_ => throw new Exception("nooo!!!"))).attempt.toList

  def tk[F[_], O](n: Long): Pipe[F, O, O] =
    (in: Stream[F, O]) =>
      in.scanChunksOpt(n) { l: Long =>
        if (l <= 0) None
        else
          Some(c =>
            c.size match {
              case m if m < l => (l - m, c)
              case m          => (0, c.take(l.toInt))
            }
          )
      }

  val o4: List[Int] = Stream(1, 2, 3, 4).through(tk(2)).toList

}
