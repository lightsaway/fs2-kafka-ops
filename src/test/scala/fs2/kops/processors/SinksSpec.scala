package fs2.kops.processors

import cats.effect.IO
import fs2.Stream
import fs2.async.{Ref, topic}
import fs2.kops.consuming.{KafkaConsumeFailure, KafkaConsumeSuccess}
import fs2.kops.{
  commitAllSink,
  commitOrSeekBackSink,
  prometheusSink,
  topicPublishSink
}
import io.prometheus.client.Counter
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition
import org.markushauck.mockito.MockitoSugar
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

class SinksSpec extends FunSuite with Matchers with MockitoSugar {

  test("testTopicPublishSink") {
    val t = topic[IO, Int](0).unsafeRunSync()
    val sink = topicPublishSink[IO](t)
    val stream = fs2.Stream(1, 2, 3, 4, 5).observe(sink)
    val values =
      t.subscribe(5).take(6).concurrently(stream).compile.toList.unsafeRunSync()
    values.size shouldBe 6
    values.head shouldBe 0
    values.last shouldBe 5
  }

  test("testPrometheusSink") {
    val counter = Counter
      .build()
      .name("foo")
      .help("Foo")
      .labelNames("name", "type")
      .create()

    val ref = Ref[IO, Counter](counter).unsafeRunSync()
    val record = new ConsumerRecord("", 0, 0, "", "")
    val stream = fs2
      .Stream(
        KafkaConsumeSuccess(record, 1),
        KafkaConsumeSuccess(record, 1),
        KafkaConsumeFailure(
          record,
          new RuntimeException()
        )
      )
      .covary[IO]

    stream.observe(prometheusSink[IO](ref)).compile.toList.unsafeRunSync()

    val samples = counter.collect().asScala.toList.head.samples.asScala
    samples
      .filter(_.labelValues == List("consumer", "success").asJava)
      .head
      .value shouldBe 2.0
    samples
      .filter(_.labelValues == List("consumer", "error").asJava)
      .head
      .value shouldBe 1.0

  }

  test("testCommitOrSeekBackSink") {

    val consumer = mock[Consumer[String, String]]

    val commitCaptor = ArgumentCaptor.forClass(
      classOf[java.util.Map[TopicPartition, OffsetAndMetadata]]
    )

    val topicCaptor = ArgumentCaptor.forClass(classOf[TopicPartition])
    val offsetCapotr = ArgumentCaptor.forClass(classOf[Long])

    val record = new ConsumerRecord("", 0, 25, "", "")
    val failing = new ConsumerRecord("", 1, 55, "", "")

    Stream(
      KafkaConsumeSuccess(record, 1),
      KafkaConsumeFailure(failing, 0)
    ).observe(commitOrSeekBackSink[IO](consumer))
      .compile
      .drain
      .unsafeRunSync()

    verify(consumer).commitSync(commitCaptor.capture())
    verify(consumer).seek(topicCaptor.capture(), offsetCapotr.capture())

    commitCaptor.getValue.get(new TopicPartition("", 0)).offset shouldBe 25
    topicCaptor.getValue.partition shouldBe 1
    offsetCapotr.getValue shouldBe (failing.offset() - 1)

  }

  test("testCommitAllSink") {

    val consumer = mock[Consumer[String, String]]

    val commitCaptor = ArgumentCaptor.forClass(
      classOf[java.util.Map[TopicPartition, OffsetAndMetadata]]
    )

    val record = new ConsumerRecord("", 0, 25, "", "")
    val failing = new ConsumerRecord("", 1, 55, "", "")

    Stream(
      KafkaConsumeSuccess(record, 1),
      KafkaConsumeFailure(failing, 0)
    ).observe(commitAllSink[IO](consumer))
      .compile
      .drain
      .unsafeRunSync()

    verify(consumer, times(2)).commitSync(commitCaptor.capture())

    val args: List[java.util.Map[TopicPartition, OffsetAndMetadata]] =
      commitCaptor.getAllValues.asScala.toList

    args.head.get(new TopicPartition("", 0)).offset shouldBe 25
    args.last.get(new TopicPartition("", 1)).offset shouldBe 55

  }

}
