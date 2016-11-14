package me.ccampo.awsflake

import com.amazonaws.util.EC2MetadataUtils
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
        val region by stringType
        val ip by stringType
        val epoch by stringType
    }
}

fun main(args: Array<String>) {
    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("defaults.properties")

    // Load some configuration properties
    port(config[Config.server.port])

    val region: AWSRegion = when {
        config[Config.server.region].isBlank() -> AWSRegion.parse(EC2MetadataUtils.getEC2InstanceRegion())
        else -> {
            log.warn("Overriding node region with value {}", config[Config.server.region])
            AWSRegion.parse(config[Config.server.region])
        }
    }

    val ip: String = when {
        config[Config.server.ip].isBlank() -> EC2MetadataUtils.getPrivateIpAddress()
        else -> {
            log.warn("Overriding node private IP address with value {}", config[Config.server.ip])
            config[Config.server.ip]
        }
    }

    val epoch: LocalDateTime? = when {
        config[Config.server.epoch].isBlank() -> null
        else -> {
            LocalDateTime.parse(config[Config.server.epoch], DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
    }

    val (oct1, oct2) = ip.split(".").takeLast(2).map { Integer.parseInt(it) }
    log.info("Region = {}, IP = {}, Octets = {}, {}, Epoch = {}", region, ip, oct1, oct2, epoch)

    get("/id") { req, resp ->
        val minLen: Int? = req.queryParams("minLength")?.toInt()
        encode(generate(region.ordinal, Pair(oct1, oct2), epoch = epoch, logger = log), minLen = minLen)
    }
}
