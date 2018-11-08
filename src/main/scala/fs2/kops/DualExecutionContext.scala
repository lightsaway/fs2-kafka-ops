package fs2.kops

import cats.effect.Async

import scala.concurrent.ExecutionContext

case class DualExecutionContext(nonblocking: ExecutionContext,
                                blocking: ExecutionContext)

object DualExecutionContext {
  import cats.implicits._
  implicit class ShiftOps[F[_], A](val fa: F[A]) extends AnyVal {

    def shifted(to: ExecutionContext)(implicit from: ExecutionContext,
                                      F: Async[F]): F[A] =
      for {
        _ <- Async.shift[F](to)
        res <- fa
        _ <- Async.shift[F](from)
      } yield res

    @inline
    def blocking(implicit DEC: DualExecutionContext, F: Async[F]): F[A] = {
      shifted(DEC.blocking)(DEC.nonblocking, F)
    }
  }
}
