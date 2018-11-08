package fs2.kops.processors

import cats.effect.{Async, Effect}
import fs2.Sink
import fs2.async.Ref
import fs2.async.mutable.Topic
import io.prometheus.client.Counter
import fs2.kops.consuming.{
  Consuming,
  KafkaConsumeFailure,
  KafkaConsumeSuccess,
  KafkaProcessResult
}

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger

trait Sinks {
  def commitOrSeekBackSink[F[_]] = new CommitOrSeekBackSink[F]
  def logSink[F[_]] = new LogSink[F]
  def commitAllSink[F[_]] = new CommitAllSink[F]
  def prometheusSink[F[_]] = new PrometheusResultSink[F]
  def topicPublishSink[F[_]] = new TopicPublishSink[F]
}

final private[kops] class CommitOrSeekBackSink[F[_]] extends Consuming {
  def apply[K, V](
      consumer: Consumer[K, V]
  )(implicit F: Async[F]): Sink[F, KafkaProcessResult[K, V]] = _.evalMap {
    case KafkaConsumeSuccess(record, _) => commit(consumer, record)
    case KafkaConsumeFailure(record, _) =>
      F.delay(
        consumer.seek(
          new TopicPartition(record.topic, record.partition),
          record.offset() - 1
        )
      )
  }
}

final private[kops] class LogSink[F[_]] {
  def apply[K, V]()(implicit F: Effect[F],
                    L: Logger): Sink[F, KafkaProcessResult[K, V]] =
    _.evalMap {
      case KafkaConsumeSuccess(r, _) =>
        F.delay(L.info(s"Consumed ${r}"))
      case KafkaConsumeFailure(r, _) =>
        F.delay(L.error(s"Failed to consume ${r}"))
    }
}

final private[kops] class CommitAllSink[F[_]] extends Consuming {
  def apply[K, V](
      consumer: Consumer[K, V]
  )(implicit F: Async[F]): Sink[F, KafkaProcessResult[K, V]] = _.evalMap {
    case KafkaConsumeSuccess(record, _) => commit(consumer, record)
    case KafkaConsumeFailure(record, _) => commit(consumer, record)
  }
}

final private[kops] class PrometheusResultSink[F[_]] {
  import cats.syntax.flatMap._
  def apply[K, V](
      counter: Ref[F, Counter]
  )(implicit F: Effect[F]): Sink[F, KafkaProcessResult[K, V]] =
    _.evalMap {
      case _ @KafkaConsumeFailure(_, _) =>
        counter.get >>=
          (c => F.delay(c.labels("consumer", "error").inc()))
      case _ @KafkaConsumeSuccess(_, _) =>
        counter.get >>=
          (c => F.delay(c.labels("consumer", "success").inc()))
    }
}

final private[kops] class TopicPublishSink[F[_]] {
  def apply[E](topic: Topic[F, E]): Sink[F, E] =
    _.evalMap { topic.publish1 }
}
