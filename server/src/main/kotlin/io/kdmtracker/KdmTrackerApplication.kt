package io.kdmtracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KdmTrackerApplication

fun main(args: Array<String>) {
    runApplication<KdmTrackerApplication>(*args)
}
