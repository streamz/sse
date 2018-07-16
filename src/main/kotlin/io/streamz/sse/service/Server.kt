package io.streamz.sse.service

import io.streamz.sse.msg.Msg
import io.streamz.sse.msg.Subscription
import io.streamz.sse.msg.impl.Kafka
import io.streamz.sse.msg.impl.Pulsar
import io.streamz.sse.msg.impl.Redis
import io.undertow.Handlers
import io.undertow.Handlers.path
import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.AttachmentKey
import io.undertow.util.Headers
import io.undertow.util.StringReadChannelListener
import java.io.IOException
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Server(argv: Args) : AutoCloseable {
    private val protocol = argv.bootstrap.substringBefore("://").toLowerCase()
    private val hostPort = argv.bootstrap.substringAfter("://")
    private val consumer = {
        when (protocol) {
            "redis" -> Optional.of(Redis.consumer(hostPort))
            "kafka" -> Optional.of(Kafka.consumer(hostPort))
            "pulsar" -> Optional.of(Pulsar.consumer(hostPort))
            else -> Optional.empty()
        }
    }().get()

    private val producer = {
        when (protocol) {
            "redis" -> Optional.of(Redis.producer(hostPort) { s -> s })
            "kafka" -> Optional.of(Kafka.producer(hostPort) { s -> s })
            "pulsar" -> Optional.of(Pulsar.producer<String>(hostPort) { s -> s })
            else -> Optional.empty()
        }
    }().get()

    private val consumerH = BlockingHandler(HttpHandler { e ->
        // get some data if available
        e.queryParameters["topic"]?.map { topic ->
            val o = consumer.consume(topic)
            var status = 204
            var messageStr = ""
            o.map { t ->
                val resp = t.first.map { msg ->
                    "${msg.id.value}, ${msg.payload}"
                }

                messageStr = resp.joinToString("\n")
                status = if (messageStr.isEmpty()) 204 else 200
                t.second(true)
            }
            e.statusCode = status
            e.responseHeaders.put(Headers.CONTENT_TYPE, "text/plain")
            e.responseContentLength = messageStr.length.toLong()
            e.responseSender.send(messageStr)
        }
    })

    private val producerH = HttpHandler { e ->
        object : StringReadChannelListener(e.connection.byteBufferPool) {
            override fun stringDone(string: String) {
                e.queryParameters["topic"]?.map { f ->
                    producer.publish(f, string)
                }
            }

            override fun error(ex: IOException) {
                //
            }
        }.setup(e.requestChannel)
    }

    private val attachmentKey = AttachmentKey.create(Subscription::class.java)
    private val sseH = Handlers.serverSentEvents { connection, lastEventId ->
        connection.queryParameters["topic"]?.map { topic ->
            // store the subscription metadata (topic, callback)
            val cb: (Msg) -> Boolean = { msg ->
                try {
                    connection.send(
                        msg.payload,
                        topic,
                        msg.id.toString(), null)
                    true
                } catch (t: Throwable) {
                    false
                }
            }
            // 1st time only
            if (lastEventId != null && lastEventId != "-1")
                consumer.consume(topic, lastEventId, cb)
            connection.putAttachment(
                attachmentKey,
                Subscription(topic, cb))

            connection.addCloseTask {
                connection.removeAttachment(attachmentKey)
            }
        }
    }

    private val sseTimer = ScheduledThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors())

    private val server = Undertow.builder()
        .addHttpListener(argv.port, argv.host)
        .setHandler(
            path()
                .addPrefixPath("/pub", producerH)
                .addPrefixPath("/poll", consumerH)
                .addPrefixPath("/sub", sseH))
        .build()

    override fun close() {
        server.stop()
    }

    init {
        sseTimer.scheduleAtFixedRate({
            // iterate through SSE Connections and call consume
            sseH.connections.forEach { connection ->
                if (connection.isOpen) {
                    val sub = connection.getAttachment(attachmentKey)
                    if (sub != null) consumer.consume(sub.topic, sub.cb)
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS)

        server.start()
    }
}
