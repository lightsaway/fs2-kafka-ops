package fs2.kops

import cats.effect.{Concurrent, Effect}
import fs2.Stream
import fs2.kops.configuration.ConfigurationExtention
import fs2.kops.processors.ProcessorExtentions
import org.apache.kafka.clients.consumer.{
  Consumer,
  ConsumerRecord,
  ConsumerRecords
}
import org.apache.kafka.clients.producer.Producer

trait ApacheKafkaExtentions {

  import scala.collection.JavaConverters._

  implicit class ConsumerRecordsSugar[K, V](r: ConsumerRecords[K, V]) {
    def partitioned: List[List[ConsumerRecord[K, V]]] =
      r.partitions().asScala.map(tp => r.records(tp).asScala.toList).toList

    def all: List[ConsumerRecord[K, V]] =
      r.partitioned.flatten
  }

  implicit class ConsumerSugar[K, V, F[_]](c: Consumer[K, V]) {
    final private val CONNECTION_COUNT_METRIC = "connection-count"

    def checkConnections(implicit F: Effect[F]) =
      F.delay(for {
        connectionCount <- c
          .metrics()
          .asScala
          .map(t => (t._1.name(), t._2.metricValue()))
          .get(CONNECTION_COUNT_METRIC)
          .toRight(s"metric '$CONNECTION_COUNT_METRIC' is not available")
        status <- connectionCount
          .asInstanceOf[Double] > 0.0 match { //TODO figure out how not to cast
          case true  => Right(s"number of active connections ${connectionCount}")
          case false => Left("unable to connect to kafka")
        }
      } yield status)
  }

  implicit class ProducerExtentions[K, V](p: Producer[K, V]) {
    //TODO: check topic
  }

}

trait StreamSyntax {
  implicit class ListOfStreamOps[F[_], A](val lst: List[Stream[F, A]]) {
    def join(implicit F: Concurrent[F]): Stream[F, A] =
      Stream(lst: _*).parJoin(lst.size)
  }

}

object syntax {
  object pipe extends ProcessorExtentions
  object client extends ApacheKafkaExtentions
  object configuration extends ConfigurationExtention
}
