package io.github.wntopia.gikipedia.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication @ConfigurationPropertiesScan
class GikipediaServerApplication

fun main(args: Array<String>) {
    runApplication<GikipediaServerApplication>(*args)
}
