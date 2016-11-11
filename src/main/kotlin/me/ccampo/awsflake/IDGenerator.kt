package me.ccampo.awsflake

import org.slf4j.Logger
import java.math.BigInteger
import java.time.Duration
import java.time.LocalDateTime


const val TIME_BIT_LEN = 41
const val REGION_BIT_LEN = 5
const val IP_BIT_LEN = 16
const val SEQ_BIT_LEN = 15
const val SEQUENCE_MAX = 32768 // 2^15

private val defaultEpoch = LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0)
private var lastTimestamp = -1L
private var sequence = 0

/**
 * The ID is an 77 bit integer of the following composition:
 *
 * 41 bits: time since epoch, in millis
 *  5 bits: region identifier (1-32, see "regions")
 * 16 bits: last two octets of private IP (unique per VPC /16 netmask)
 * 15 bits: sequence number
 *
 * @author Chris Campo
 */
fun generate(region: Int, ip: Pair<Int, Int>, epoch: LocalDateTime = defaultEpoch, logger: Logger? = null): BigInteger {
    var timestamp = Duration.between(epoch, LocalDateTime.now()).toMillis()

    // Clock moved backwards - refuse to generate ID
    if (timestamp < lastTimestamp) {
        TODO("Clock moved backwards - need a better error condition -ccampo 2016-11-10")
    }

    // TODO: is locking on sequence sufficient? -ccampo 2016-11-10
    synchronized(sequence) {
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) % SEQUENCE_MAX
            if (sequence == 0) {
                // Basically a sleep - at this point we've exceeded the max sequence number,
                // so we need to until the clock counts up one millisecond before we can
                // generate any more IDs. Should be an exceedingly rare scenario, but we
                // account for it nonetheless.
                do {
                    timestamp = System.currentTimeMillis()
                } while (timestamp <= lastTimestamp)
            }
        } else {
            sequence = 0
        }
        lastTimestamp = timestamp
    }

    if (logger != null && logger.isDebugEnabled) {
        logger.debug("IP octets: {}, {}", ip.first, ip.second)
        logger.debug("IP octet 1 binary: {}", Integer.toBinaryString(ip.first))
        logger.debug("IP octet 2 binary: {}", Integer.toBinaryString(ip.second))
    }

    val machineId: Int = (ip.first shl 8) or ip.second
    return generate(timestamp, region, machineId, sequence, logger = logger)
}

private fun generate(timestamp: Long, regionOrdinal: Int, machineId: Int, seq: Int, logger: Logger?):
        BigInteger {
    val part1: Long = timestamp.shl(REGION_BIT_LEN).shl(IP_BIT_LEN)
            .or(regionOrdinal.toLong().shl(IP_BIT_LEN))
            .or(machineId.toLong())
    val id: BigInteger = BigInteger.valueOf(part1).shiftLeft(SEQ_BIT_LEN).or(BigInteger.valueOf(seq.toLong()))

    if (logger != null && logger.isDebugEnabled) {
        logger.debug("Timestamp : {}", timestamp)
        logger.debug("Timestamp binary: {}", BigInteger.valueOf(timestamp).toString(2))
        logger.debug("Region ordinal: {}", regionOrdinal)
        logger.debug("Region ordinal binary: {}", Integer.toBinaryString(regionOrdinal))
        logger.debug("Machine ID: {}", machineId)
        logger.debug("Machine ID binary: {}", Integer.toBinaryString(machineId))
        logger.debug("Sequence: {}", seq)
        logger.debug("Sequence binary: {}", Integer.toBinaryString(seq))
        logger.debug("Part 1: {}", part1)
        logger.debug("Part 1 binary: {}",
                String.format("%41s", BigInteger.valueOf(part1).toString(2)).replace(" ", "0"))
        logger.debug("ID: {}", id)
        logger.debug("ID binary: {}", String.format("%77s", id.toString(2)).replace(" ", "0"))
    }
    return id
}
