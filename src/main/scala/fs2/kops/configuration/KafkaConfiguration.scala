package fs2.kops.configuration

case class KafkaConfiguration(
    bootstrapServers: String,
    sasl: Option[Sasl] = None
)
