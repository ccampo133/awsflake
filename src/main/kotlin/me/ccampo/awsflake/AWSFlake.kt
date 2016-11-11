package me.ccampo.awsflake

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.Spark.get

val log: Logger = LoggerFactory.getLogger("AWSFlake")

fun main(args: Array<String>) {
    //val region = AWSRegion.parse(EC2MetadataUtils.getEC2InstanceRegion())
    //val ip = EC2MetadataUtils.getPrivateIpAddress()

    val region = AWSRegion.US_EAST_1
    val ip = "127.0.0.1"
    val (oct1, oct2) = ip.split(".").takeLast(2).map { Integer.parseInt(it) }
    log.info("region = {}, ip = {}, octets = {}, {}", region, ip, oct1, oct2)

    get("/id") { req, res -> encode(generate(region.ordinal, Pair(oct1, oct2), logger = log)) }
}
