package fs2

import fs2.kops.configuration.ConfigurationExtention
import fs2.kops.consuming.{ConsumerBuilder, Consumers, Consuming}
import fs2.kops.processors.{Pipes, ProcessorExtentions, Sinks}
import fs2.kops.producing.{ProducerBuilder, Producers, Producing}

package object kops
    extends ConfigurationExtention
    with ApacheKafkaExtentions
    with ProcessorExtentions
    with ProducerBuilder
    with Producing
    with Producers
    with ConsumerBuilder
    with Consuming
    with Consumers
    with Sinks
    with Pipes
