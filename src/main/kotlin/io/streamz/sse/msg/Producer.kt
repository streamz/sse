package io.streamz.sse.msg

interface Producer<A> : AutoCloseable {
    fun publish(topic: String, value: A)
}