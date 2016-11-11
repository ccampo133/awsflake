package me.ccampo.awsflake

import com.amazonaws.util.EC2MetadataUtils

private val region = AWSRegion.parse(EC2MetadataUtils.getEC2InstanceRegion()).ordinal

private val ip = getIpOctets()

private fun getIpOctets(): Pair<Int, Int> {
    val (oct1, oct2) = EC2MetadataUtils.getPrivateIpAddress()
            .split(".")
            .takeLast(2)
            .map { Integer.parseInt(it) }
    return Pair(oct1, oct2)
}

fun main(args: Array<String>) {
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
