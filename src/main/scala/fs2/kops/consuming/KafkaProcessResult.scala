package fs2.kops.consuming

import org.apache.kafka.clients.consumer.ConsumerRecord

sealed trait KafkaProcessResult[K, V] {
  val rawRecord: ConsumerRecord[K, V]
  val isValid: Boolean
}

case class KafkaConsumeSuccess[K, V, R](rawRecord: ConsumerRecord[K, V],
                                        result: R)
    extends KafkaProcessResult[K, V] {
  override val isValid: Boolean = true
}

case class KafkaConsumeFailure[K, V, E](rawRecord: ConsumerRecord[K, V],
                                        error: E)
    extends KafkaProcessResult[K, V] {
  override val isValid: Boolean = false
}
