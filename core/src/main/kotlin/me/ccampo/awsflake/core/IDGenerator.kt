package me.ccampo.awsflake.core

import com.amazonaws.util.EC2MetadataUtils
import mu.KLogger
import mu.KotlinLogging
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class IDGenerator private constructor(private val region: AWSRegion, epoch: Instant, ip: String) {

    companion object {
        const val TIME_BIT_LEN = 41
        const val REGION_BIT_LEN = 5
        const val IP_BIT_LEN = 16
        const val SEQ_BIT_LEN = 15
        const val SEQUENCE_MAX = 32768 // 2^15
        val DEFAULT_EPOCH = Instant.parse("2020-01-01T00:00:00Z")
        val DEFAULT_REGION = AWSRegion.US_EAST_1

        private val lock = ReentrantLock()

        // Enforces that IDGenerator is a singleton. See: https://stackoverflow.com/a/45943282
        @Volatile
        private var INSTANCE: IDGenerator? = null

        fun getInstance(region: AWSRegion, epoch: Instant, ip: String): IDGenerator =
                INSTANCE ?: lock.withLock {
                    INSTANCE ?: IDGenerator(region, epoch, ip).also { INSTANCE = it }
                }

        /**
         * Generate a 77 bit integer of the following composition:
         *
         * <timestamp (41 bits)><region (5 bits)><octet1 (8 bits)><octet2 (8 bits)><seq (15 bits)>
         *
         * The value bits (in order) are designed to represent the following:
         *
         * 41 bits: time since epoch, in millis (this gives us a max of ~69.7 years from the epoch)
         *  5 bits: region identifier (1-32, see "regions")
         * 16 bits: last two octets of private IP (unique per VPC /16 netmask)
         * 15 bits: sequence number
         *
         * While this method does not enforce that any of the values passed actually map
         * to their intended usage (for example, the `timestamp` argument can be any arbitrary
         * long), all of the logic to compute the value is captured here. Also note that
         * there is no thread safety guaranteed here.
         *
         * Use `IDGenerator.generate()` to more properly generate a unique identifier.
         */
        fun generate(timestamp: Long, regionOrdinal: Int, machineId: Int, seq: Int): BigInteger {
            val part1: Long = timestamp.shl(REGION_BIT_LEN).shl(IP_BIT_LEN)
                    .or(regionOrdinal.toLong().shl(IP_BIT_LEN))
                    .or(machineId.toLong())
            return BigInteger.valueOf(part1).shiftLeft(SEQ_BIT_LEN).or(BigInteger.valueOf(seq.toLong()))
        }
    }

    class Builder {
        private var region: AWSRegion? = null
        private var epoch: Instant? = null
        private var ip: String? = null

        fun region(region: AWSRegion): Builder = apply { this.region = region }
        fun epoch(epoch: Instant): Builder = apply { this.epoch = epoch }
        fun ip(ip: String): Builder = apply { this.ip = ip }

        fun build(): IDGenerator {
            return getInstance(
                    region ?: DEFAULT_REGION,
                    epoch ?: DEFAULT_EPOCH,
                    ip ?: EC2MetadataUtils.getPrivateIpAddress()
            )
        }
    }

    private val log: KLogger = KotlinLogging.logger {}
    private val octets: Pair<Int, Int>
    private val epochMillis = epoch.toEpochMilli()
    private val lock = ReentrantLock()
    private var lastTimestamp = -1L
    private var sequence = 0

    init {
        val octetsList = ip.split(".").takeLast(2).map { Integer.parseInt(it) }
        octets = Pair(octetsList[0], octetsList[1])
        log.info { "Region = $region, Octets = $octets, Epoch = $epoch" }
    }

    /**
     * Thread-safe
     */
    fun generate(): BigInteger {
        lock.withLock {
            var timestamp = timeGen()

            if (timestamp >= 1L shl TIME_BIT_LEN) {
                throw MaxTimestampExceededException("Max timestamp exceeded - use a more recent epoch.")
            }

            /*
             * Clock moved backwards - refuse to generate ID. Obviously impossible in real life
             * (barring any quantum fluctuations of course), but this can occur if for whatever
             * reason the system's clock is set to some time in the past while the application
             * is running.
             */
            if (timestamp < lastTimestamp) {
                throw ClockMovedBackwardsException("Clock moved backwards - ID cannot be generated.")
            }

            if (lastTimestamp == timestamp) {
                sequence = (sequence + 1) % SEQUENCE_MAX
                if (sequence == 0) {
                    /*
                     * This is basically a sleep. At this point we've exceeded the max sequence
                     * number, so we need to until the clock counts up one millisecond before we
                     * can generate any more IDs. This should be an exceedingly rare scenario,
                     * but we account for it nonetheless.
                     */
                    do {
                        timestamp = timeGen()
                    } while (timestamp <= lastTimestamp)
                }
            } else {
                sequence = 0
            }
            lastTimestamp = timestamp

            val machineId: Int = (octets.first shl 8) or octets.second
            return generate(timestamp, region.ordinal, machineId, sequence)
        }
    }

    private fun timeGen(): Long = System.currentTimeMillis() - epochMillis
}

class MaxTimestampExceededException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)

class ClockMovedBackwardsException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
