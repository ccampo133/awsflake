package me.ccampo.awsflake

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.Spark.get
import spark.Spark.port
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val log: Logger = LoggerFactory.getLogger("AWSFlake")

class Config {
    companion object server : PropertyGroup() {
        val port by intType
        val nodeId by intType
        val epoch by stringType
    }
}

fun main(args: Array<String>) {
    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("defaults.properties")

    // Load some configuration properties
    port(config[Config.server.port])

    val nodeId: Short = config[Config.server.nodeId].toShort()

    if (nodeId > NODE_ID_MAX || nodeId < 0) {
        throw MaxNodeIdException("Node ID must be a value between 0 - $NODE_ID_MAX")
    }

    val epoch: LocalDateTime? = when {
        config[Config.server.epoch].isBlank() -> null
        else -> {
            LocalDateTime.parse(config[Config.server.epoch], DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
    }

    log.info("Node ID = {}", nodeId)
    if (epoch != null) log.info("Epoch = {}", epoch)

    get("/id") { req, resp -> generate(nodeId, epoch = epoch, logger = log) }
}
