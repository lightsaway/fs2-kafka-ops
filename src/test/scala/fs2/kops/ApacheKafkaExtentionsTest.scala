package fs2.kops

import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords}
import org.apache.kafka.common.TopicPartition
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._
class ApacheKafkaExtentionsTest extends FlatSpec with Matchers {

  "ConsumerRecordsSugar" should "partition properly" in {

    val part1 = List(new ConsumerRecord("1",1,2L, "key1","val2"), new ConsumerRecord("1",1,3L, "key2","val3"))
    val part2 = List(new ConsumerRecord("1",2,2L, "key0","val0"), new ConsumerRecord("1",2,3L, "key1","val1"))
    val cr = new ConsumerRecords(Map(
                 new TopicPartition("",1) -> part1.asJava,
                 new TopicPartition("",2) -> part2.asJava).asJava)

    cr.partitioned.size shouldBe 2
    cr.partitioned.flatten.size shouldBe 4
    cr.partitioned.head shouldBe part1
    cr.partitioned.last shouldBe part2

    cr.all shouldBe cr.partitioned.flatten

  }

}
