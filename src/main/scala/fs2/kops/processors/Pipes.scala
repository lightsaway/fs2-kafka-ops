package fs2.kops.processors

import cats.effect.concurrent.Ref
import cats.effect.{Async, Concurrent, Effect}
import fs2.Pipe
import fs2.concurrent.Topic
import fs2.kops.consuming.{
  ConsumerActions,
  KafkaConsumeFailure,
  KafkaConsumeSuccess,
  KafkaProcessResult
}
import io.prometheus.client.Counter
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger

trait Pipes {
  def takeWhileSuccessPipe[F[_]] = new TakeWhileSuccessPipe[F]
  def commitOrSeekBackSink[F[_]] = new CommitOrSeekBackSink[F]
  def logSink[F[_]] = new LogSink[F]
  def commitAllSink[F[_]] = new CommitAllSink[F]
  def prometheusSink[F[_]] = new PrometheusResultSink[F]
  def topicPublishSink[F[_]] = new TopicPublishSink[F]
}

final private[kops] class TakeWhileSuccessPipe[F[_]] {
  def apply[K, V]()
    : Pipe[F, KafkaProcessResult[K, V], KafkaProcessResult[K, V]] =
    _.takeWhile(_.isValid)
}

final private[kops] class CommitOrSeekBackSink[F[_]] extends ConsumerActions {
  def apply[K, V](
      consumer: Consumer[K, V]
  )(implicit F: Concurrent[F]): Pipe[F, KafkaProcessResult[K, V], Unit] =
    _.evalMap {
      case KafkaConsumeSuccess(record, _) => commit(consumer, record)
      case KafkaConsumeFailure(record, _) =>
        seek(consumer,
             new TopicPartition(record.topic, record.partition),
             record.offset() - 1)
    }
}

final private[kops] class LogSink[F[_]] {
  def apply[K, V]()(implicit F: Effect[F],
                    L: Logger): Pipe[F, KafkaProcessResult[K, V], Unit] =
    _.evalMap {
      case KafkaConsumeSuccess(r, _) =>
        F.delay(L.info(s"Consumed ${r}"))
      case KafkaConsumeFailure(r, _) =>
        F.delay(L.error(s"Failed to consume ${r}"))
    }
}

final private[kops] class CommitAllSink[F[_]] extends ConsumerActions {
  def apply[K, V](
      consumer: Consumer[K, V]
  )(implicit F: Async[F]): Pipe[F, KafkaProcessResult[K, V], Unit] =
    _.evalMap(x => commit(consumer, x.rawRecord))
}

final private[kops] class PrometheusResultSink[F[_]] {
  import cats.syntax.flatMap._
  def apply[K, V](
      counter: Ref[F, Counter]
  )(implicit F: Effect[F]): Pipe[F, KafkaProcessResult[K, V], Unit] =
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
  def apply[E](topic: Topic[F, E]): Pipe[F, E, Unit] =
    _.evalMap { topic.publish1 }
}
