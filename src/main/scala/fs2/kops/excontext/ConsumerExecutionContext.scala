package fs2.kops.excontext

import java.util.concurrent.Executors

import cats.effect.Sync
import cats.syntax.all._
import fs2.Stream

import scala.concurrent.ExecutionContext

case class ConsumerExecutionContext(e: ExecutionContext)

trait ConsumerContextBuilder {
  def consumer[F[_]] = new KafkaConsumerExecutionContextBuilder[F]
}

final private[kops] class KafkaConsumerExecutionContextBuilder[F[_]] {
  def create(poolSize: Int = 1)(implicit F: Sync[F]): F[ConsumerExecutionContext] = F.delay(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(poolSize: Int))).map(ConsumerExecutionContext(_))

  def stream(poolSize: Int = 1)(implicit F: Sync[F]): Stream[F, ConsumerExecutionContext] = Stream.eval(create(poolSize))
}

