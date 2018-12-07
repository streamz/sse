package io.streamz.sse.msg

import arrow.core.None
import arrow.core.Option

interface Consumer : AutoCloseable {
    fun consume(s: Subscription, from: Option<Id>)
    fun consumeAll(s: Subscription, from: Option<Id> = None):
        Option<Pair<List<Msg>, (Boolean) -> Unit>>
}