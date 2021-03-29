import akka.util.ByteString

val listGood = List(ByteString("a"),ByteString("b"),ByteString("c"))
val listEmpty = List(ByteString(""))
val listMaybeEmpty = List(ByteString(" "))
val listNothing = List.empty


def checkThing(list: List[ByteString]) = {
  val startTime = System.currentTimeMillis()

  (1 to 200000).foreach { _ =>
    if (list.length > 1) {
      true
    } else if (list.isEmpty) {
      false
    } else {
      list.exists(_.utf8String.trim.nonEmpty)
    }
  }

  println("time taken was " + (System.currentTimeMillis() - startTime))
}

println("".nonEmpty)