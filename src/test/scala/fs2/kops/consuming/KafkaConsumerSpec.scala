package fs2.kops.consuming

import cats.effect.IO
import fs2.Pipe
import fs2.kops.{commitOrSeekBackSink, consumeAndProcessUnchunked, logSink}
import org.apache.kafka.clients.consumer.{
  ConsumerRecord,
  MockConsumer,
  OffsetResetStrategy
}
import org.apache.kafka.common.TopicPartition
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

class KafkaConsumerSpec extends FlatSpec with Matchers {
  import DataBuilder._

  behavior.of("subscribed kafka producer stream")

  val topic = "test-topic"
  val partitions = 3
  val messagesPerPartition = 10

  val mockConsumer =
    new MockConsumer[String, String](OffsetResetStrategy.LATEST)
  mockConsumer.subscribe(List(topic).asJava)
  mockConsumer.rebalance(
    (0 until partitions)
      .map(idx => new TopicPartition(topic, idx))
      .toList
      .asJava
  )

  mockConsumer.updateEndOffsets(
    (0 until partitions)
      .map(
        partition =>
          new TopicPartition(topic, partition) -> long2Long(partition.toLong)
      )
      .toMap
      .asJava
  )

  mockConsumer.schedulePollTask(() =>
    (0 until partitions).map(partition => {
      (0 until messagesPerPartition)
        .map(offset =>
          mockConsumer.addRecord(succesRecord(partition, offset, topic)))
      mockConsumer.addRecord(failRecord(partition, 10, topic))
      mockConsumer.addRecord(succesRecord(partition, 11, topic))
      mockConsumer.addRecord(succesRecord(partition, 12, topic))
    }))

  val transformer: Pipe[IO,
                        ConsumerRecord[String, String],
                        KafkaProcessResult[String, String]] =
    _.evalMap {
      case i if i.value().contains("fail") =>
        IO(KafkaConsumeFailure(i, new RuntimeException))
      case i => IO(KafkaConsumeSuccess(i, "success"))
    }
  implicit val logger = org.slf4j.LoggerFactory.getLogger("")
  val process = transformer
    .observe(commitOrSeekBackSink[IO](mockConsumer))
    .observe(logSink[IO]())
    .takeWhile(_.isValid)

  it should "consume" in {
    val consumerStream =
      consumeAndProcessUnchunked[IO](mockConsumer, process).compile.toList
    consumerStream.unsafeRunSync

    (0 until partitions).map(partition => {
      val commited =
        mockConsumer.committed(new TopicPartition(topic, partition))
      commited.offset shouldBe 9
    })
  }

  object DataBuilder {
    val succesRecord = (partition: Int, offset: Int, topic: String) =>
      new ConsumerRecord[String, String](
        topic,
        partition,
        offset.toLong,
        "test-key",
        "test-value-success"
    )
    val failRecord = (partition: Int, offset: Int, topic: String) =>
      new ConsumerRecord[String, String](
        topic,
        partition,
        offset.toLong,
        "test-key",
        "test-value-fail"
    )
  }
}
