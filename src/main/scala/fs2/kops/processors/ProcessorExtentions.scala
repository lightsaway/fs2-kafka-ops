package fs2.kops.processors
import cats.effect.Effect
import fs2.{Pipe, Sink}

import scala.concurrent.ExecutionContext

trait ProcessorExtentions {
  implicit class SinkImprovements[F[_], A](sink: Sink[F, A]) {
    def and(other: Sink[F, A])(implicit F: Effect[F],
                               EC: ExecutionContext): Sink[F, A] =
      _.observe(sink).observe(other).drain

  }

  implicit class PipeImprovements[F[_], A, E, C](pipe: Pipe[F, A, E]) {
    def observe(sink: Sink[F, E])(implicit EF: Effect[F],
                                  EC: ExecutionContext): Pipe[F, A, E] =
      _.through(pipe).observe(sink)

    def and(other: Pipe[F, E, C]): Pipe[F, A, C] =
      _.through(pipe).through(other)

    def takeWhile(f: E => Boolean): Pipe[F, A, E] = _.through(pipe).takeWhile(f)
  }
}
