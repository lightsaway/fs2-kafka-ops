package fs2.kops.configuration

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.{SaslConfigs, SslConfigs}
import org.scalatest.{FlatSpec, Matchers}

class ConfigurationExtentionSpec extends FlatSpec with Matchers {

  behavior of "ConfigurationExtention"

  it should "convert kafka configration without ssl to java map" in {
    KafkaConfiguration("foo:443").asMap shouldBe Map[String, AnyRef](
      CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> "foo:443")
  }

  it should "convert kafka configration with ssl to java map" in {
    KafkaConfiguration(
      "foo:443",
      Some(Sasl("user", "pass", (Ssl("foo", "bar"))))).asMap shouldBe Map[
      String,
      AnyRef](
      CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> "foo:443",
      CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> "SASL_SSL",
      SaslConfigs.SASL_MECHANISM -> "SCRAM-SHA-512",
      SaslConfigs.SASL_JAAS_CONFIG -> s"""org.apache.kafka.common.security.scram.ScramLoginModule required username="user" password="pass";""",
      SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG -> "foo",
      SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG -> "bar",
    )

  }

}
