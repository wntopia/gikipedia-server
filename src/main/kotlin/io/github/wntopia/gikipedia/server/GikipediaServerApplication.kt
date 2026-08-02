package io.github.wntopia.gikipedia.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GikipediaServerApplication

fun main(args: Array<String>) {
    runApplication<GikipediaServerApplication>(*args)
}
