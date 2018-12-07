package io.streamz.sse.msg

import arrow.core.Option
import arrow.optics.Lens
import arrow.optics.PLens
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class Id(val value: String) {
    override fun toString(): String {
        return value
    }
}
data class Msg(val id: Id, val payload: String)
data class Topic(val value: String)
data class SubscriptionName(val value: String)
data class Subscription(
    val topic: Topic,
    val name: SubscriptionName,
    val cb: Option<(Msg) -> Boolean>) {
    override fun toString(): String {
        return "${topic.value}:${name.value}"
    }
}

object optics {
    val subscriptionLens: Lens<Subscription, SubscriptionName> = PLens(
        get = { i -> i.name },
        set = { t -> { i -> i.copy(name = t) } }
    )
}

fun mkUri(proto: String, hostPort: String, port: Int): String {
    val p = if (!hostPort.contains(proto)) "$proto$hostPort" else hostPort
    return if (!p.contains(":[0-9]{1,5}\$".toRegex())) "$p:$port" else p
}

interface Loggable
fun Loggable.logger(): Logger = LoggerFactory.getLogger(this.javaClass)
abstract class WithLogging: Loggable {
    val log = logger()
}