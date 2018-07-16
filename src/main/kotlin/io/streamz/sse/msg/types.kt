package io.streamz.sse.msg

import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class Id(val value: String) {
    override fun toString(): String {
        return value
    }
}
data class Msg(val id: Id, val payload: String)
data class Subscription(val topic: String, val cb: (Msg) -> Boolean)

fun mkUri(proto: String, hostPort: String, port: Int): String {
    val p = if (!hostPort.contains(proto)) "$proto$hostPort" else hostPort
    return if (!p.contains(":[0-9]{1,5}\$".toRegex())) "$p:$port" else p
}

interface Loggable
fun Loggable.logger(): Logger = LoggerFactory.getLogger(this.javaClass)
abstract class WithLogging: Loggable {
    val log = logger()
}