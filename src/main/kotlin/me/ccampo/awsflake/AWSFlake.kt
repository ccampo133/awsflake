package me.ccampo.awsflake

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.Spark.get


fun main(args: Array<String>) {
    //val region = AWSRegion.parse(EC2MetadataUtils.getEC2InstanceRegion()).ordinal
    //val ip = EC2MetadataUtils.getPrivateIpAddress().split(".").takeLast(2).map { Integer.parseInt(it) }
    val log: Logger = LoggerFactory.getLogger("AWSFlake")
    get("/id") { req, res -> encode(generate(1, Pair(10, 128), logger = log)) }

/*
    val t = (1L shl 41) - 1
    val r = (1 shl 5) - 1
    val o1 = (1 shl 8) - 1
    val o2 = (1 shl 8) - 1
    val seq = (1 shl 15) - 1
    val id = generate(t, r, Pair(o1, o2), seq, debug = true)
    val encoded = encode(id)
    println(encoded + "\n")
    var dt = 0L
    for (i in 1..10000) {
        dt += measureTimeMillis { generate({ 1 }, { Pair(o1, o2) }) }
    }
    println(dt)
*/
}
