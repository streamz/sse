package io.streamz.sse.msg.impl

import io.lettuce.core.RedisClient
import io.streamz.sse.msg.*
import java.util.*

object Redis {
    fun <A> producer(pwAtHostPort: String, f: (a: A) -> String): Producer<A> {
        return object : Producer<A> {
            val redis = RedisClient
                .create(mkUri("redis://", pwAtHostPort, 6379))
            val connection = redis.connect()

            override
            fun publish(topic: String, value: A) {
                synchronized(this) {
                    val i = connection.sync().incr("$topic:id").toString()
                    connection.sync().hset(topic, i, f(value))
                }
            }

            override fun close() {
                connection.close()
                redis.shutdown()
            }
        }
    }

    fun consumer(pwAtHostPort: String): Consumer {
        return object : Consumer {
            val redis = RedisClient
                .create(mkUri("redis://", pwAtHostPort, 6379))
            val connection = redis.connect()
            override
            fun consume(topic: String, from: Optional<Id>):
               Optional<Pair<List<Msg>, (Boolean) -> Unit>> {
                val items = connection.sync().hgetall(topic)
                return Optional.of(Pair(items.map { f ->
                    Msg(Id(f.key), f.value) }, { b ->
                    if (b) items.forEach { i ->
                        connection
                            .sync()
                            .hdel(topic, i.key)
                    }
                }))
            }

            override fun consume(
                topic: String,
                from: Optional<Id>,
                cb: (value: Msg) -> Boolean) {
                val items = connection.sync().hgetall(topic)
                items.map { f ->
                    if (cb(Msg(Id(f.key), f.value)))
                        connection.sync().hdel(topic, f.key)
                }
            }

            override fun close() {
                connection.close()
                redis.shutdown()
            }
        }
    }
}