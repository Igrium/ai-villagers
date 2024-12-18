package com.igrium.aivillagers

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object KotlinTest {
    private var Logger = LoggerFactory.getLogger(javaClass)

    @JvmStatic
    public fun printTestString() {
        Logger.info("This is from Kotlin!")
    }
}