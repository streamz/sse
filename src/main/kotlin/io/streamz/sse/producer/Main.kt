@file:JvmName("Main")
package io.streamz.sse.producer

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

class Args(parser: ArgParser) {
    val url by parser.storing("-u", "--url", help = "url")
        .default("http://localhost:8000/pub")

    val topic by parser
        .storing("-t", "--topic", help = "topic to subscribe to")
        .default("test")

    val message by parser
        .storing("-m", "--message", help = "message to send")
        .default("Hello World: ")

    val count by parser
        .storing("-c", "--count", help = "number of messages to send")
        .default("1")
}

fun main(args: Array<String>) = mainBody {
    val argv = ArgParser(args).parseInto(::Args)

    val serviceUrl = argv.url + "?topic=${argv.topic.trim()}"
    argv.run {
        println("""
        url = $serviceUrl
        topic = $topic""".trimIndent())
    }

    val client = OkHttpClient()
    val body = RequestBody.create(MediaType.parse("text/plain"), argv.message)
    val request = Request.Builder()
        .url(serviceUrl)
        .post(body)
        .build()
    val count = argv.count.toInt()

    (0 until count).forEach { i ->
        client
            .newCall(request)
            .execute()
            .use { response ->
                println("$i: sending: ${argv.message}; response code: ${response.code()}")
            }
    }
}