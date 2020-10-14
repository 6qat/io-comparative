val aList = List(1, 2, 3)
val bList = List(4, 5, 6)
val cList = List(7, 8, 9)

for {
  a <- aList
  b <- bList
  c <- cList
} {
  println(a + b + c)
}

aList.foreach(a => {
  bList.foreach(b => {
    cList.foreach(c => {
      println(a + b + c)
    })
  })
})

for {
  a <- aList
  b <- bList
  c <- cList
} yield {
  a + b + c
}

aList.flatMap(a => {
  bList.flatMap(b => {
    cList.map(c => {
      a + b + c
    })
  })
})
