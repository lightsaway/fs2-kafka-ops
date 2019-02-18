package fs2.kops.producing

import cats.effect.{Async, ContextShift}
import cats.implicits._
import fs2.concurrent.Topic
import fs2.{Pipe, Stream}
import org.apache.kafka.clients.producer.{Producer, ProducerRecord}

trait Producers {
  def subscribedProducer[F[_]]: SubscribedProducing[F] =
    new SubscribedProducing[F]

  def produceOne[F[_]]: ProduceOne[F] = new ProduceOne[F]

  def produceTransacted[F[_]]: ProduceTransacted[F] = new ProduceTransacted[F]
}

final private[kops] class ProduceOne[F[_]] extends ProducerActions {
  def apply[K, V, B](
      p: Producer[K, V],
      f: B => ProducerRecord[K, V] = identity _
  )(implicit F: Async[F]): B => F[Either[Throwable, KafkaSendResult[K, V]]] =
    event => unsafeProduce[F, K, V](p, f(event)).attempt
}

final private[kops] class ProduceTransacted[F[_]] extends ProducerActions {
  import cats.implicits._
  def apply[K, V, B](p: Producer[K, V],
                     f: B => ProducerRecord[K, V] = identity _)(
      implicit F: Async[F],
      C: ContextShift[F],
      P: ProducerExecutionContext
  ): List[B] => F[Either[Throwable, List[KafkaSendResult[K, V]]]] =
    events => {
      val transaction: F[Either[Throwable, List[KafkaSendResult[K, V]]]] =
        (for {
          _ <- F.delay(p.beginTransaction())
          transformed = events.map(f)
          result <- transformed.map(e => unsafeProduce(p, e)).sequence
        } yield result).attempt.flatMap {
          case r @ Right(_) => F.delay(p.commitTransaction()).as(r)
          case l @ Left(_)  => F.delay(p.abortTransaction()).as(l)
        }
      C.evalOn(P.e)(transaction)
    }
}

final private[kops] class SubscribedProducing[F[_]] extends ProducerActions {
  def apply[K, V, B](
      p: Producer[K, V],
      topic: Topic[F, B],
      pipe: Pipe[F, B, ProducerRecord[K, V]],
      maxQueueSize: Int = 500
  )(implicit F: Async[F]): Stream[F, KafkaSendResult[K, V]] =
    topic
      .subscribe(maxQueueSize)
      .through(pipe)
      .evalMap(record => unsafeProduce(p, record))
}
