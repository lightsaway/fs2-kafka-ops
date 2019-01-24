package fs2.kops.consuming

import java.util.UUID

import cats.effect.IO
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.{FlatSpec, Matchers}
import fs2.kops._
import fs2.kops.configuration.{
  KafkaConfiguration,
  SimpleConsumerConfiguration,
  Topic
}
import org.apache.kafka.common.serialization.{StringDeserializer}
import scala.collection.JavaConverters._

class ConsumerBuilderISpec extends FlatSpec with EmbeddedKafka with Matchers {

  behavior.of("subscribed consumer")

  it should "create and subscribe in effect" in {
    val kConfig = EmbeddedKafkaConfig(kafkaPort = 0, zooKeeperPort = 0)

    withRunningKafkaOnFoundPort(kConfig) { implicit actualConfig =>
      val kafkaLocationConfig =
        KafkaConfiguration(s"localhost:${actualConfig.kafkaPort}").asMap
      val topicName = UUID.randomUUID().toString
      val consumerConfig =
        SimpleConsumerConfiguration(UUID.randomUUID().toString,
                                    Topic(topicName)).asMap

      createCustomTopic(topicName)

      val c = subscribedConsumer[IO]
        .create(kafkaLocationConfig ++ consumerConfig,
                new StringDeserializer(),
                new StringDeserializer(),
                topicName)
        .unsafeRunSync()
      c.subscription().isEmpty shouldBe false
      c.subscription().asScala.head shouldBe topicName
    }
  }

  it should "create and subscribe in stream" in {
    val kConfig = EmbeddedKafkaConfig(kafkaPort = 0, zooKeeperPort = 0)

    withRunningKafkaOnFoundPort(kConfig) { implicit actualConfig =>
      val kafkaLocationConfig =
        KafkaConfiguration(s"localhost:${actualConfig.kafkaPort}").asMap
      val topicName = UUID.randomUUID().toString
      val consumerConfig =
        SimpleConsumerConfiguration(UUID.randomUUID().toString,
                                    Topic(topicName)).asMap
      createCustomTopic(topicName)
      subscribedConsumer[IO]
        .stream(kafkaLocationConfig ++ consumerConfig,
                new StringDeserializer(),
                new StringDeserializer(),
                topicName)
        .map(c => {
          c.subscription().isEmpty shouldBe false
          c.subscription().asScala.head shouldBe topicName
        })
        .compile
        .drain
        .unsafeRunSync()
    }
  }
}
