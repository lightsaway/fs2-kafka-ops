package fs2.kops.producing
import cats.effect.Async
import org.apache.kafka.clients.producer.{
  Producer,
  ProducerRecord,
  RecordMetadata
}
import cats.implicits._

trait Producing {

  def unsafeProduce[F[_], K, V](
      producer: Producer[K, V],
      record: ProducerRecord[K, V]
  )(implicit F: Async[F]): F[KafkaSendResult[K, V]] = {
    F.async[RecordMetadata] { cb =>
        producer.send(
          record,
          (metadata: RecordMetadata, exception: Exception) =>
            Option(exception) match {
              case Some(e) => cb(Left(KafkaSendException(record, e)))
              case None    => cb(Right(metadata))
          }
        )
        ()
      }
      .map(meta => KafkaSendResult[K, V](record, meta))
  }

}
