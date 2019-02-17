package fs2.kops.excontext

import java.util.concurrent.Executors

import cats.effect.Sync
import cats.syntax.all._
import fs2.Stream

import scala.concurrent.ExecutionContext

case class ConsumerExecutionContext(e: ExecutionContext)

trait ConsumerContextBuilder {
  def consumerExecutionContext[F[_]] = new ConsumerExecutionContextBuilder[F]
  def fixedThreadPool[F[_]] = new FixedThreadPool[F]
}

final private[kops] class ConsumerExecutionContextBuilder[F[_]] {
  def create(poolSize: Int = 1)(
      implicit F: Sync[F]): F[ConsumerExecutionContext] =
    new FixedThreadPool[F]
      .create(poolSize)
      .map(ConsumerExecutionContext(_))

  def stream(poolSize: Int = 1)(
      implicit F: Sync[F]): Stream[F, ConsumerExecutionContext] =
    Stream.eval(create(poolSize))
}
