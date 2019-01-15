package fs2.kops.processors

import cats.effect.IO
import fs2.{Pipe, Sink}
import org.scalatest.{FunSuite, Matchers}

import fs2.kops.syntax.pipe._
class ProcessorExtentionsSpec extends FunSuite with Matchers {

  test("testPipeImprovements 'and composition'") {

    val p1: Pipe[IO, String, String] = identity
    val p2: Pipe[IO, String, Int] = _.map(_.toInt)

    val p3: Pipe[IO, String, Int] = p1 and p2
    fs2
      .Stream("1", "2", "3")
      .covary[IO]
      .through(p3)
      .compile
      .toList
      .unsafeRunSync() shouldBe List(1, 2, 3)
  }

  test("testPipeImprovements 'observe' composition") {
    import scala.concurrent.ExecutionContext.Implicits.global
    var flag: String = ""
    val p1: Pipe[IO, String, String] = identity
    val p2: Sink[IO, String] = _.evalMap(x => IO { flag = x })

    val p3: Pipe[IO, String, String] = p1 observe p2
    fs2
      .Stream("1", "2", "3")
      .covary[IO]
      .through(p3)
      .compile
      .toList
      .unsafeRunSync() shouldBe List("1", "2", "3")
    flag shouldBe "3"
  }

  test("testPipeImprovements 'takeWhile' syntax") {

    val p1: Pipe[IO, Int, Int] = identity
    fs2
      .Stream(1, 2, 3)
      .covary[IO]
      .through(p1.takeWhile(_ < 3))
      .compile
      .toList
      .unsafeRunSync() shouldBe List(1, 2)
  }

  test("testSinkImprovements") {

    import scala.concurrent.ExecutionContext.Implicits.global
    var flag1: String = ""
    var flag2: String = ""
    val s1: Sink[IO, String] = _.evalMap(x => IO { flag1 = x })
    val s2: Sink[IO, String] = _.evalMap(x => IO { flag2 = x })

    val composed = s1 and s2

    fs2
      .Stream("1", "2", "3")
      .covary[IO]
      .observe(composed)
      .compile
      .toList
      .unsafeRunSync() shouldBe List("1", "2", "3")
    flag1 shouldBe "3"
    flag2 shouldBe "3"
  }

}
