package io.streamz.sse.service

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class Args(parser: ArgParser) {
    val host by parser.storing("-o", "--host", help = "host").default("localhost")

    val port by parser
        .storing("-p", "--port", help = "port") { toInt() }.default(8000)

    val bootstrap by parser
        .storing("-b", "--bootstrap",
            help = "bootstrap service uri, redis://pw@host, kafka://host, pulsar://host")

    val topic by parser
        .storing("-t", "--topic", help = "topic to subscribe to")
        .default("test")
}