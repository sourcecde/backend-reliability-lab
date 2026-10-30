package com.backendreliabilitylab

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class BackendReliabilityLabApplication

fun main(args: Array<String>) {
	runApplication<BackendReliabilityLabApplication>(*args)
}
