package fs2.kops.excontext

import java.util.concurrent.Executors

import cats.effect.Sync
import fs2.Stream
import fs2.internal.ThreadFactories

import scala.concurrent.ExecutionContext

final private[kops] class CachedhreadPool[F[_]] {

  def create(
      prefix: String = "manually-created",
      daemon: Boolean = false,
      exitOnFatal: Boolean = false)(implicit F: Sync[F]): F[ExecutionContext] =
    F.delay(
      ExecutionContext.fromExecutor(Executors.newCachedThreadPool(
        ThreadFactories.named(prefix, daemon, exitOnFatal))))

  def stream(prefix: String = "manually-created",
             daemon: Boolean = false,
             exitOnFatal: Boolean = false)(
      implicit F: Sync[F]): Stream[F, ExecutionContext] =
    Stream
      .bracket(
        F.delay(Executors.newCachedThreadPool(
          ThreadFactories.named(prefix, daemon, exitOnFatal))))(pool =>
        F.delay(pool.shutdown()))
      .flatMap(pool => Stream.emit(ExecutionContext.fromExecutor(pool)))
}
