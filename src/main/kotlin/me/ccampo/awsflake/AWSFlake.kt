package me.ccampo.awsflake

import com.amazonaws.util.EC2MetadataUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.Spark.get

val log: Logger = LoggerFactory.getLogger("AWSFlake")

fun main(args: Array<String>) {
    val region = AWSRegion.parse(EC2MetadataUtils.getEC2InstanceRegion())
    val ip = EC2MetadataUtils.getPrivateIpAddress()
    val (oct1, oct2) = ip.split(".").takeLast(2).map { Integer.parseInt(it) }
    log.info("region = {}, ip = {}, octets = {}, {}", region, ip, oct1, oct2)

    get("/id") { req, resp ->
        val minLen: Int? = req.queryParams("minLength")?.toInt()
        encode(generate(region.ordinal, Pair(oct1, oct2), logger = log), minLen = minLen)
    }
}
