package fs2.kops.configuration
import java.util.UUID

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.{SaslConfigs, SslConfigs}

trait ConfigurationExtention {
  val DEFAULT_CONSUMER_PROPS = Map[String, AnyRef](
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> Boolean.box(false),
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest"
  )

  val DEFAULT_TRANSACTIONAL_PRODUCER_PROPS = Map[String, AnyRef](
    ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG -> Boolean.box(true),
    ProducerConfig.TRANSACTIONAL_ID_CONFIG -> UUID
      .randomUUID()
      .toString // seems to be a good idea to make random per runtime
  )

  implicit class ConfigToNativeProps[A](config: KafkaConfiguration) {
    def asMap: Map[String, AnyRef] = {
      Map[String, AnyRef](
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> config.bootstrapServers) ++
        config.sasl
          .map(
            sasl =>
              collection.mutable.Map(
                SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG -> sasl.ssl.truststoreLocation,
                SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG -> sasl.ssl.truststorePassword,
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> "SASL_SSL",
                SaslConfigs.SASL_MECHANISM -> "SCRAM-SHA-512",
                SaslConfigs.SASL_JAAS_CONFIG -> s"""org.apache.kafka.common.security.scram.ScramLoginModule required username="${sasl.username}" password="${sasl.password}";"""
            )
          )
          .getOrElse(Map.empty)
    }
  }

  implicit class ConsumerConfigToNativeProps[A](
      config: SimpleConsumerConfiguration
  ) {
    def asMap: Map[String, AnyRef] = {
      Map[String, AnyRef]("group.id" -> config.groupId)
    }
  }

}
