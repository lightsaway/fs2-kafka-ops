package fs2.kops.processors

import fs2.Pipe
import fs2.kops.consuming.KafkaProcessResult

trait Pipes {
  def takeWhileSuccessPipe[F[_]] = new TakeWhileSuccessPipe[F]
}

final private[kops] class TakeWhileSuccessPipe[F[_]] {
  def apply[K, V]()
    : Pipe[F, KafkaProcessResult[K, V], KafkaProcessResult[K, V]] =
    _.takeWhile(_.isValid)
}
