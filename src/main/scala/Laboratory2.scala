object Laboratory2 {
  def main(args: Array[String]): Unit = {
    case class Thing[T](value: T)

    def processThing(thing: Thing[_]) = {
      thing match {
        case Thing(value: Int)      => "Thing of int"
        case Thing(value: String)   => "Thing of string"
        case Thing(value: Seq[Int]) => "Thing of Seq[int]"
        case _                      => "Thing of something else"
      }
    }

    import scala.reflect.runtime.universe._
    def processThing2[T: TypeTag](thing: Thing[T]) = {
      typeOf[T] match {
        case t if t =:= typeOf[Seq[Int]]    => "Thing of Seq[Int]"
        case t if t =:= typeOf[Seq[String]] => "Thing of Seq[String]"
        case t if t =:= typeOf[Int]         => "Thing of Int"
        case _                              => "Thing of other"
      }
    }

    def processThing3[T: TypeTag](thing: Thing[T]) = {
      thing match {
        case Thing(value: Int) => "Thing of int " + value.toString
        case Thing(value: Seq[Int]) if typeOf[T] =:= typeOf[Seq[Int]] =>
          "Thing of seq of int" + value.sum
        case _ => "Thing of something else"
      }
    }

    println(processThing(Thing(1)))
    println(processThing(Thing("hello")))
    println(processThing(Thing(1.2)))
    println(processThing(Thing(List(2))))
    println(processThing(Thing(List("olá"))))

  }
}
