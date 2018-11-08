package fs2.kops.producing
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}

case class KafkaSendResult[K, V](record: ProducerRecord[K, V],
                                 meta: RecordMetadata)
