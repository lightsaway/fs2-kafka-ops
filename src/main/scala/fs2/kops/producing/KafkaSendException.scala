package fs2.kops.producing
import org.apache.kafka.clients.producer.ProducerRecord

case class KafkaSendException[K, V](record: ProducerRecord[K, V],
                                    error: Throwable)
    extends RuntimeException(error)
