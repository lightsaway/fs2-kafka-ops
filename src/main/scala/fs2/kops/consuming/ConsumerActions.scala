package fs2.kops.consuming
import cats.effect.{Async, Sync}
import fs2.Stream
import org.apache.kafka.clients.consumer.{
  Consumer,
  ConsumerRecord,
  ConsumerRecords,
  OffsetAndMetadata
}
import org.apache.kafka.common.TopicPartition
import scala.collection.JavaConverters._

trait ConsumerActions {
  def subscribeAndConsume[F[_], K, V](
      consumer: Consumer[K, V],
      topic: String,
      timeout: Long
  )(implicit F: Async[F]): Stream[F, ConsumerRecords[K, V]] = {
    for {
      _ <- Stream.eval(subscribe[F, K, V](consumer, topic))
      batch <- consume[F, K, V](consumer, timeout)
        .filter(_.count() > 0)
        .repeat
    } yield batch
  }

  def consume[F[_], K, V](
      consumer: Consumer[K, V],
      timeout: Long
  )(implicit F: Async[F]): Stream[F, ConsumerRecords[K, V]] = {
    Stream.eval(F.delay(consumer.poll(timeout))).filter(_.count() > 0)
  }

  def subscribe[F[_], K, V](
      consumer: Consumer[K, V],
      topic: String
  )(implicit F: Sync[F]): F[Unit] = {
    F.delay(consumer.subscribe(List(topic).asJava))
  }

  def commit[F[_], K, V](
      consumer: Consumer[K, V],
      record: ConsumerRecord[K, V]
  )(implicit F: Async[F]): F[Unit] = F.delay {
    consumer.commitSync(
      Map(
        new TopicPartition(record.topic(), record.partition()) -> new OffsetAndMetadata(
          record.offset(),
          Metadata().toString
        )
      ).asJava
    )
  }

}
