package fs2.kops.consuming

import cats.effect.Effect
import fs2.{Pipe, Stream}
import fs2.kops.{ApacheKafkaExtentions, StreamSyntax}
import org.apache.kafka.clients.consumer._

import scala.concurrent.ExecutionContext

trait Consumers {
  def consumeAndProcessUnchunked[F[_]] = new ConsumeAndProcessUnchunked[F]
}

final private[kops] class ConsumeAndProcessUnchunked[F[_]]
    extends ApacheKafkaExtentions
    with StreamSyntax
    with Consuming {
  def apply[K, V](
      consumer: Consumer[K, V],
      pipe: Pipe[F, ConsumerRecord[K, V], KafkaProcessResult[K, V]],
      timeout: Long = 500L
  )(implicit F: Effect[F], EC: ExecutionContext) =
    consume(consumer, timeout).flatMap(
      _.partitioned
        .map(records => {
          Stream
            .emits(records)
            .covary[F]
            .unchunk
            .through(pipe)
        })
        .join
    )
}
