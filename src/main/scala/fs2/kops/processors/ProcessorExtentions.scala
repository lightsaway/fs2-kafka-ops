package fs2.kops.processors
import cats.effect.{Concurrent}
import fs2.{Pipe}

trait ProcessorExtentions {
  implicit class SinkImprovements[F[_], A](sink: Pipe[F, A, Unit]) {
    def and(other: Pipe[F, A, Unit])(
        implicit F: Concurrent[F]): Pipe[F, A, Unit] =
      _.observe(sink).observe(other).drain

  }

  implicit class PipeImprovements[F[_], A, E, C](pipe: Pipe[F, A, E]) {
    def observe(sink: Pipe[F, E, Unit])(
        implicit EF: Concurrent[F]): Pipe[F, A, E] =
      _.through(pipe).observe(sink)

    def and(other: Pipe[F, E, C]): Pipe[F, A, C] =
      _.through(pipe).through(other)

    def takeWhile(f: E => Boolean): Pipe[F, A, E] = _.through(pipe).takeWhile(f)
  }
}
