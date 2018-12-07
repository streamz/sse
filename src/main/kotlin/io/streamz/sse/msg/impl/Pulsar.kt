package io.streamz.sse.msg.impl

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import io.streamz.sse.msg.*
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import java.util.*
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.Consumer as PulsarConsumer
import org.apache.pulsar.client.api.Producer as PulsarProducer

object Pulsar {
    fun <A> producer(hostPort: String, f: (a: A) -> String): Producer<A> {
        return object : Producer<A> {
            val pulsar = PulsarClient.builder()
                .serviceUrl(mkUri("pulsar://", hostPort, 6650))
                .build()

            val producers = HashMap<String, PulsarProducer<String>>()

            override
            fun publish(topic: String, value: A) {
                producers.getOrPut(topic) { producer(topic) }.send(f(value))
            }

            override fun close() {
                producers.forEach { _, u -> u.close() }
                pulsar.close()
                pulsar.shutdown()
            }

            private fun producer(topic: String): PulsarProducer<String> {
                return pulsar
                    .newProducer(Schema.STRING)
                    .topic(topic)
                    .create()
            }
        }
    }

    fun consumer(hostPort: String): Consumer {
        return object : Consumer {
            val pulsar = PulsarClient.builder()
                .serviceUrl(mkUri("pulsar://", hostPort, 6650))
                .build()

            val consumers = HashMap<String, PulsarConsumer<String>>()

            override fun consumeAll(s: Subscription, from: Option<Id>):
                Option<Pair<List<Msg>, (Boolean) -> Unit>> {
                val c = consumers.getOrPut(s.toString()) { pulsarConsumer(s) }
                from.map { f -> seek(c, f) }
                val t = c.receive(0, TimeUnit.MILLISECONDS) ?: return None
                val m = listOf(Msg(mid2id(t.messageId), t.value))
                return Some(Pair(m, { b -> if (b) c.acknowledge(t.messageId) }))
            }

            override fun consume(s: Subscription, from: Option<Id>) {
                val c = consumers.getOrPut(s.toString()) { pulsarConsumer(s) }
                from.map { f -> seek(c, f) }
                val t = c.receive(0, TimeUnit.MILLISECONDS) ?: return
                s.cb.fold({Unit}){
                    if (it(Msg(mid2id(t.messageId), t.value)))
                        c.acknowledge(t.messageId)
                }
            }

            override fun close() {
                consumers.forEach { _, u -> u.close() }
                pulsar.close()
                pulsar.shutdown()
            }

            private fun pulsarConsumer(s: Subscription): PulsarConsumer<String> {
                return pulsar
                    .newConsumer(Schema.STRING)
                    .subscriptionName(s.name.value)
                    .topic(s.topic.value)
                    .subscribe()
            }

            private fun seek(c: PulsarConsumer<String>, f: Id) {
                val mid = str2mid(f.value)
                c.seek(mid)
                // force to skip last received message
                c.receive(0, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun mid2id(id: MessageId): Id {
        return Id(Base64.getEncoder().encodeToString(id.toByteArray()))
    }

    private fun str2mid(id: String): MessageId {
        return MessageId.fromByteArray(Base64.getDecoder().decode(id))
    }
}