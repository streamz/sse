@file:JvmName("Main")
package io.streamz.sse.consumer

import com.launchdarkly.eventsource.ConnectionErrorHandler
import com.launchdarkly.eventsource.EventHandler
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.MessageEvent
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

class Args(parser: ArgParser) {
    val url by parser.storing("-u", "--url", help = "url")
        .default("http://localhost:8000/sub")

    val topic by parser
        .storing("-t", "--topic", help = "topic to subscribe to")
        .default("test")

    val lastEventId by parser
        .storing("-f", "--from", help = "from last event id")
        .default("-1")
}

fun main(args: Array<String>) = mainBody {
    val argv = ArgParser(args).parseInto(::Args)

    argv.run {
        println("""
        url = $url
        topic = $topic""".trimIndent())
    }
    val running = AtomicBoolean(true)
    val source = EventSource.Builder(object: EventHandler {
        override fun onOpen() {
            println("waiting for events on topic ${argv.topic} " +
                "from event id: ${argv.lastEventId}...")
        }
        override fun onComment(comment: String?) {}
        override fun onClosed() {}
        override fun onError(t: Throwable?) {
            t!!.printStackTrace()
        }
        override fun onMessage(event: String?, me: MessageEvent?) {
            println("event: $event")
            println("data: ${me?.data}")
            println("lastEventId: ${me?.lastEventId}")
            println("---")
        }
    }, URI(argv.url + "?topic=${argv.topic}"))
        .connectionErrorHandler(object: ConnectionErrorHandler {
            override fun onConnectionError(t: Throwable?): ConnectionErrorHandler.Action {
                println(t?.message)
                running.set(false)
                return ConnectionErrorHandler.Action.SHUTDOWN
            }
        }).build()

    source.setLastEventId(argv.lastEventId)
    source.start()

    Runtime.getRuntime().addShutdownHook(Thread {source.close()})

    do {
        val line = readLine()
        if (line == "quit" || line == "exit") {
            println("bye")
            break
        }
    } while (running.get())
}