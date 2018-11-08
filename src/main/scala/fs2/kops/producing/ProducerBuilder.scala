package fs2.kops.producing

import cats.effect.Sync
import fs2.Stream
import org.apache.kafka.clients.producer.{
  Producer,
  KafkaProducer => ApacheKafkaProducer
}
import org.apache.kafka.common.serialization.{Serializer, StringSerializer}

import scala.collection.JavaConverters._

trait ProducerBuilder {
  def producer[F[_]] = new KafkaProducerBuilder[F]
  def simpleProducer[F[_]] = new SimpleProducerBuilder[F]
  def simpleTransactionalProducer[F[_]] = new SimpleTransactionalProducer[F]
}

final private[kops] class KafkaProducerBuilder[F[_]] {
  def create[K, V](
      settings: Map[String, AnyRef],
      keySerializer: Serializer[K],
      valueSerializer: Serializer[V]
  )(implicit F: Sync[F]): F[Producer[K, V]] =
    F.delay(
      new ApacheKafkaProducer[K, V](
        settings.asJava,
        keySerializer,
        valueSerializer
      )
    )

  def stream[K, V](
      settings: Map[String, AnyRef],
      keySerializer: Serializer[K],
      valueSerializer: Serializer[V]
  )(implicit F: Sync[F]): Stream[F, Producer[K, V]] =
    fs2.Stream.bracket(create[K, V](settings, keySerializer, valueSerializer))(
      p => Stream.emit(p).covary[F],
      p => Sync[F].delay(p.close())
    )

}

final private[kops] class SimpleTransactionalProducer[F[_]] {
  import cats.implicits._

  def create(settings: Map[String, AnyRef])(implicit F: Sync[F]) =
    new SimpleProducerBuilder[F]
      .create(settings)
      .flatMap(p => F.delay(p.initTransactions()) *> F.delay(p))

  def stream(
      settings: Map[String, AnyRef]
  )(implicit F: Sync[F]): Stream[F, Producer[String, String]] =
    new SimpleProducerBuilder[F]
      .stream(settings)
      .evalMap(p => F.delay(p.initTransactions()).as(p))
}

final private[kops] class SimpleProducerBuilder[F[_]] {
  def create(
      settings: Map[String, AnyRef]
  )(implicit F: Sync[F]): F[Producer[String, String]] =
    new KafkaProducerBuilder[F]
      .create(settings, new StringSerializer, new StringSerializer)

  def stream(
      settings: Map[String, AnyRef]
  )(implicit F: Sync[F]): Stream[F, Producer[String, String]] =
    new KafkaProducerBuilder[F]
      .stream(settings, new StringSerializer, new StringSerializer)

}
