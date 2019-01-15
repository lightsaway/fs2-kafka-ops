package fs2.kops.processors

import cats.effect.IO
import fs2.Stream
import fs2.kops.consuming.{KafkaConsumeFailure, KafkaConsumeSuccess}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.scalatest.{FunSuite, Matchers}

class PipesSpec extends FunSuite with Pipes with Matchers {

  test("testTakeWhileSuccessPipe") {
    val record = new ConsumerRecord("", 0, 25, "", "")
    val stream = Stream(
      KafkaConsumeSuccess(record, 1),
      KafkaConsumeSuccess(record, 1),
      KafkaConsumeFailure(record, new RuntimeException)
    ).covary[IO]

    val result = (stream ++ stream ++ stream)
      .through(takeWhileSuccessPipe[IO][String, String]())
      .compile
      .toList
      .unsafeRunSync()

    result.size shouldBe 2
    result.find(!_.isValid) shouldBe None
  }

}
