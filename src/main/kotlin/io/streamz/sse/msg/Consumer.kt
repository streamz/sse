package io.streamz.sse.msg

import java.util.*

interface Consumer : AutoCloseable {
    fun consume(topic: String, from: Optional<Id> = Optional.empty()):
        Optional<Pair<List<Msg>, (Boolean) -> Unit>>

    fun consume(topic: String, cb: (value: Msg) -> Boolean) {
        consume(topic, Optional.empty(), cb)
    }

    fun consume(topic: String, from: String, cb: (value: Msg) -> Boolean) {
        consume(topic, Optional.of(Id(from)), cb)
    }

    fun consume(topic: String, from: Optional<Id>, cb: (value: Msg) -> Boolean)
}