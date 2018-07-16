package io.streamz.sse.msg.impl

import io.streamz.sse.msg.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import java.util.*
import kotlin.concurrent.getOrSet
import org.apache.kafka.clients.consumer.KafkaConsumer as KConsumer
import org.apache.kafka.clients.producer.KafkaProducer as KProducer

object Kafka : WithLogging() {
    fun <A> producer(hostPort: String, f: (a: A) -> String): Producer<A> {
        return object : Producer<A> {
            val producer =
                KProducer<String, String>(
                    bootstrap(mkUri("kafka://", hostPort, 29092)))

            override
            fun publish(topic: String, value: A) {
                producer.send(ProducerRecord<String, String>(topic, f(value)))
                { t, e ->
                    if (e != null) e.printStackTrace()
                    else log.debug("The offset is: " + t.offset())
                }
            }

            override fun close() {
                producer.close()
            }
        }
    }

    // hardwired to partition 0, consumer per topic per thread
    fun consumer(hostPort: String): Consumer {
        return object : Consumer {
            val consumers = ThreadLocal<HashMap<String, KConsumer<String, String>>>()
            val consumerRefs = ArrayList<KConsumer<String, String>>()

            override fun consume(
                topic: String,
                from: Optional<Id>):
                Optional<Pair<List<Msg>, (Boolean) -> Unit>> {
                val partition = listOf(TopicPartition(topic, 0))
                val c = consumers.getOrSet { HashMap() }.getOrPut(topic) {
                    consumer(topic)
                }
                c.resume(partition)
                from.map { f ->
                    c.seek(TopicPartition(topic, 0), f.value.toLong() + 1)
                }
                val m = c.poll(0).fold(emptyList<Msg>()) { a, b ->
                    a + Msg(Id(b.offset().toString()), b.value())
                }
                c.pause(partition)
                return if (m.isEmpty()) Optional.empty() else Optional.of(
                    Pair(m, { b ->
                        if (b) c.commitSync()
                    }))
            }

            // assumes a single partition
            override fun consume(
                topic: String,
                from: Optional<Id>,
                cb: (value: Msg) -> Boolean) {
                val partition = listOf(TopicPartition(topic, 0))
                val c = consumers.getOrSet { HashMap() }.getOrPut(topic) {
                    consumer(topic)
                }
                c.resume(partition)
                from.map { f ->
                    c.seek(TopicPartition(topic, 0), f.value.toLong() + 1)
                }
                val m = c.poll(0)
                c.pause(partition)
                m.forEach { f ->
                    if (cb(Msg(Id(f.offset().toString()), f.value()))) c.commitAsync()
                }
            }

            override fun close() {
                consumerRefs.forEach { f -> f.close() }
            }

            private fun consumer(topic: String): KConsumer<String, String> {
                val c = KConsumer<String, String>(
                    bootstrap(mkUri("kafka://", hostPort, 29092)))
                c.assign(listOf(TopicPartition(topic, 0)))
                consumerRefs.add(c)
                return c
            }
        }
    }

    fun bootstrap(servers: String): Properties {
        val props = Properties()
        props["bootstrap.servers"] = servers
        props["enable.auto.commit"] = "false"
        props["auto.offset.reset"] = "earliest"
        props["acks"] = "all"
        props["group.id"] = "default"
        props["retries"] = 0
        props["batch.size"] = 16384
        props["linger.ms"] = 1
        props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        return props
    }
}