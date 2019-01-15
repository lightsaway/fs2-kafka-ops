package fs2

import fs2.kops.configuration.ConfigurationExtention
import fs2.kops.consuming.{ConsumerBuilder, Consumers, ConsumerActions}
import fs2.kops.processors.{Pipes, ProcessorExtentions, Sinks}
import fs2.kops.producing.{ProducerBuilder, Producers, ProducerActions}

package object kops
    extends ConfigurationExtention
    with ApacheKafkaExtentions
    with ProcessorExtentions
    with ProducerBuilder
    with ProducerActions
    with Producers
    with ConsumerBuilder
    with ConsumerActions
    with Consumers
    with Sinks
    with Pipes
