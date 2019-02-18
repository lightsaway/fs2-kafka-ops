package fs2

import fs2.kops.configuration.ConfigurationExtention
import fs2.kops.consuming.{ConsumerActions, ConsumerBuilder, Consumers}
import fs2.kops.excontext.ContextBuilder
import fs2.kops.processors.{Pipes, ProcessorExtentions}
import fs2.kops.producing.{ProducerActions, ProducerBuilder, Producers}

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
    with Pipes
    with ContextBuilder
