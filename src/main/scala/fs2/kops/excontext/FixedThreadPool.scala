package fs2.kops.excontext

import java.util.concurrent.Executors

import cats.effect.Sync
import fs2.Stream

import scala.concurrent.ExecutionContext

final private[kops] class FixedThreadPool[F[_]] {
  def create(poolSize: Int)(implicit F: Sync[F]): F[ExecutionContext] =
    F.delay(
      ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(poolSize: Int)))

  def stream(poolSize: Int)(implicit F: Sync[F]): Stream[F, ExecutionContext] =
    Stream.bracket(F.delay(Executors.newFixedThreadPool(poolSize: Int)))(
      pool => Stream.emit(ExecutionContext.fromExecutor(pool)),
      pool => F.delay(pool.shutdown()))
}
