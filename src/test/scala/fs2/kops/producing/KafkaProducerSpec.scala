package fs2.kops.producing

import java.util.concurrent.Executors

import cats.effect.{IO, Timer}
import fs2.Pipe
import fs2.internal.ThreadFactories
import fs2.kops.{
  DualExecutionContext,
  produceOne,
  produceTransacted,
  subscribedProducer
}
import org.apache.kafka.clients.producer.{MockProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
class KafkaProducerSpec extends FlatSpec with Matchers {

  behavior.of("subscribed kafka producer stream")

  val blocking = Executors.newCachedThreadPool(
    ThreadFactories.named("blocking", false, false))
  implicit val DC: DualExecutionContext =
    DualExecutionContext(global, ExecutionContext.fromExecutor(blocking))

  val transformer: Pipe[IO, Int, ProducerRecord[String, String]] = i =>
    i.map(i => new ProducerRecord[String, String]("", "foo", i.toString))

  it should "produce transformed messages" in {

    val topic = fs2.async.topic[IO, Int](0).unsafeRunSync()

    val publisherStream = fs2.Stream.eval(topic.publish1(1))

    val producer = new MockProducer[String, String](true,
                                                    new StringSerializer,
                                                    new StringSerializer)

    val subscriber = subscribedProducer[IO](producer, topic, transformer, 1)

    subscriber
      .take(1)
      .concurrently(publisherStream)
      .compile
      .toList
      .unsafeRunSync()
    producer.history.size() shouldBe 1

  }

  it should "throw exception when failing to communicate with kafka" in {

    val topic = fs2.async.topic[IO, Int](0).unsafeRunSync()
    import cats.syntax.all._

    val producer = new MockProducer[String, String](false,
                                                    new StringSerializer,
                                                    new StringSerializer)
    val errorEmiter =
      fs2.Stream.eval(
        Timer[IO].sleep(300.millis) *> IO(
          producer.errorNext(new RuntimeException("༼ つ ◕_◕ ༽つ"))))

    val eventPublisher = fs2.Stream.eval(topic.publish1(1))
    val subscriber = subscribedProducer[IO](producer, topic, transformer, 1)

    an[KafkaSendException[_, _]] should be thrownBy subscriber
      .concurrently(eventPublisher >> errorEmiter)
      .compile
      .toList
      .unsafeRunSync()
  }

  behavior.of("produce one function")
  it should "produce proper messages" in {

    val mock = new MockProducer[String, String](true,
                                                new StringSerializer,
                                                new StringSerializer)
    val mapper = (value: String) =>
      new ProducerRecord[String, String]("topic", "foo", s"$value-changed")
    produceOne[IO](mock, mapper).apply("value").unsafeRunSync()

    val sendouts = mock.history.asScala.toList
    sendouts.size shouldBe 1
    sendouts.head.key() shouldBe "foo"
    sendouts.head.value() shouldBe "value-changed"
  }

  behavior.of("produce transacted function")
  it should "should not produce if there is an error in the middle of transaction" in {

    val mock = new MockProducer[String, String](true,
                                                new StringSerializer,
                                                new StringSerializer)
    mock.initTransactions()

    val mapper = (value: String) => {
      if (value == "3")
        new ProducerRecord[String, String]("topic", "foo", s"$value-changed")
      else throw new RuntimeException("")
    }

    produceTransacted[IO](mock, mapper)
      .apply(List("1", "2", "3"))
      .unsafeRunSync()

    val sendouts = mock.history.asScala.toList
    sendouts.size shouldBe 0
  }

  it should "should produce if transaction is successfull" in {
    val mock = new MockProducer[String, String](true,
                                                new StringSerializer,
                                                new StringSerializer)
    mock.initTransactions

    val mapper = (value: String) =>
      new ProducerRecord[String, String](
        "topic",
        "foo",
        s"$value-changed-${Thread.currentThread}")

    produceTransacted[IO](mock, mapper)
      .apply(List("1", "2", "3"))
      .unsafeRunSync()

    val sendouts = mock.history.asScala.toList
    sendouts.size shouldBe 3

    sendouts.head.key() shouldBe "foo"
    sendouts.head.value() should startWith("1-changed-")
    sendouts.head.value() should include("blocking")

    sendouts.last.key() shouldBe "foo"
    sendouts.last.value() should startWith("3-changed-")
    sendouts.last.value() should include("blocking")
  }

}
