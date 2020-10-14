import scala.reflect.runtime.universe._

def show[T](value: T)(implicit tag: TypeTag[T]) = tag.toString()

println(show(1))

trait Nat

class _0 extends Nat

class Succ[N <: Nat] extends Nat

val seq: Seq[Int] = Seq(1, 2, 3)
seq.isInstanceOf[Seq[Int]]
seq.isInstanceOf[Seq[Boolean]]
seq.isInstanceOf[Seq[Float]]
