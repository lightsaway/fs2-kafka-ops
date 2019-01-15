package fs2.kops.consuming

import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import org.apache.kafka.clients.consumer.{
  Consumer,
  KafkaConsumer => ApacheKafkaConsumer
}
import org.apache.kafka.common.serialization.{Deserializer, StringDeserializer}

import scala.collection.JavaConverters._

trait ConsumerBuilder {
  def consumer[F[_]] = new KafkaConsumerBuilder[F]
  def subscribedConsumer[F[_]] = new SubscribedKafkaConsumerBuilder[F]
  def simpleConsumer[F[_]] = new SimpleKafkaConsumerBuilder[F]
  def simpleSubscribedConsumer[F[_]] =
    new SimpleSubscribedKafkaConsumerBuilder[F]
}

final private[kops] class KafkaConsumerBuilder[F[_]] {
  def create[K, V](
      settings: Map[String, AnyRef],
      keyDeserializer: Deserializer[K],
      valueDeserializer: Deserializer[V]
  )(implicit F: Sync[F]): F[Consumer[K, V]] =
    F.delay {
      new ApacheKafkaConsumer[K, V](
        settings.asJava,
        keyDeserializer,
        valueDeserializer
      )
    }

  def stream[K, V](
      settings: Map[String, AnyRef],
      keyDeserializer: Deserializer[K],
      valueDeserializer: Deserializer[V]
  )(implicit F: Sync[F]): Stream[F, Consumer[K, V]] =
    Stream.bracket(create(settings, keyDeserializer, valueDeserializer))(
      consumer => Stream.emit(consumer).covary[F],
      consumer => F.delay(consumer.close())
    )
}

final private[kops] class SubscribedKafkaConsumerBuilder[F[_]]
    extends ConsumerActions {
  def create[K, V](
      settings: Map[String, AnyRef],
      keyDeserializer: Deserializer[K],
      valueDeserializer: Deserializer[V],
      topic: String
  )(implicit F: Sync[F]): F[Consumer[K, V]] =
    new KafkaConsumerBuilder[F]
      .create(settings, keyDeserializer, valueDeserializer)
      .flatMap(c => subscribe(c, topic) *> F.delay(c))

  def stream[K, V](
      settings: Map[String, AnyRef],
      keyDeserializer: Deserializer[K],
      valueDeserializer: Deserializer[V],
      topic: String
  )(
      implicit F: Sync[F]
  ): Stream[F, Consumer[K, V]] =
    new KafkaConsumerBuilder[F]
      .stream(settings, keyDeserializer, valueDeserializer)
      .evalMap(c => subscribe(c, topic) *> F.delay(c))
}

final private[kops] class SimpleKafkaConsumerBuilder[F[_]] {
  def create(
      settings: Map[String, AnyRef]
  )(implicit F: Sync[F]): F[Consumer[String, String]] =
    new KafkaConsumerBuilder[F]
      .create(settings, new StringDeserializer, new StringDeserializer)

  def stream(
      settings: Map[String, AnyRef]
  )(implicit F: Sync[F]): Stream[F, Consumer[String, String]] =
    new KafkaConsumerBuilder[F]
      .stream(settings, new StringDeserializer, new StringDeserializer)
}

final private[kops] class SimpleSubscribedKafkaConsumerBuilder[F[_]]
    extends ConsumerActions {
  def create(settings: Map[String, AnyRef], topic: String)(
      implicit F: Sync[F]): F[Consumer[String, String]] =
    new KafkaConsumerBuilder[F]
      .create(settings, new StringDeserializer, new StringDeserializer)
      .flatMap(c => subscribe(c, topic) *> F.delay(c))

  def stream(settings: Map[String, AnyRef], topic: String)(
      implicit F: Sync[F]
  ): Stream[F, Consumer[String, String]] =
    new KafkaConsumerBuilder[F]
      .stream(settings, new StringDeserializer, new StringDeserializer)
      .evalMap(c => subscribe(c, topic) *> F.delay(c))
}
