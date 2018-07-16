@file:JvmName("Main")
package io.streamz.sse.service

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody

fun main(args: Array<String>): Unit = mainBody {
    val argv = ArgParser(args).parseInto(::Args)
    argv.run {
        println("Sync Server")
        println("""
        host = $host
        port = $port
        bootstrap = $bootstrap
        topic = $topic""".trimIndent())
        Server(argv)
    }
}