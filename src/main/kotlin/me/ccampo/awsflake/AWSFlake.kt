package me.ccampo.awsflake

import com.amazonaws.util.EC2MetadataUtils
import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.Spark.get
import spark.Spark.port

val log: Logger = LoggerFactory.getLogger("AWSFlake")

class Config {
    companion object server : PropertyGroup() {
        val port by intType
        val region by stringType
        val ip by stringType
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

    val (oct1, oct2) = ip.split(".").takeLast(2).map { Integer.parseInt(it) }
    log.info("Region = {}, IP = {}, Octets = {}, {}", region, ip, oct1, oct2)

    get("/id") { req, resp ->
        val minLen: Int? = req.queryParams("minLength")?.toInt()
        encode(generate(region.ordinal, Pair(oct1, oct2), logger = log), minLen = minLen)
    }
}
