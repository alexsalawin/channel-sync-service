package org.ecommerce

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ChannelSyncApplication

fun main(args: Array<String>) {
    runApplication<ChannelSyncApplication>(*args)
}