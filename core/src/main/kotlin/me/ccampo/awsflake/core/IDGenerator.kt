package me.ccampo.awsflake.core

import com.amazonaws.util.EC2MetadataUtils
import mu.KLogger
import mu.KotlinLogging
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Generates k-sorted, 77 bit IDs, which are guaranteed to be unique in an AWS
 * environment without any node coordination. To limit the possibility of
 * duplicate ID generations (and hence collisions), this class is a singleton,
 * and can only be instantiated once, using the `getInstance` factory method.
 * It is not possible to change the epoch, ip, or region during runtime after
 * instantiation. An instance should either be created directly using the
 * `getInstance` method, or by using the provided `Builder`.
 *
 * See `getInstance` and `Builder` for more detailed descriptions of the
 * parameters and general usage.
 *
 * @param region: the AWS region this instance is running in.
 * @param epoch: the epoch which timestamps will be generated in reference to.
 * @param ip: the private IPv4 address of the instance.
 */
class IDGenerator private constructor(private val region: AWSRegion, epoch: Instant, ip: String) {

    companion object {
        const val TIME_BIT_LEN = 41
        const val REGION_BIT_LEN = 5
        const val IP_BIT_LEN = 16
        const val SEQ_BIT_LEN = 15
        const val SEQUENCE_MAX = 32768 // 2^15
        val DEFAULT_EPOCH = Instant.parse("2020-01-01T00:00:00Z")

        private val lock = ReentrantLock()

        // Enforces that IDGenerator is a singleton. See: https://stackoverflow.com/a/45943282
        @Volatile
        private var INSTANCE: IDGenerator? = null

        /**
         * Instantiates the single instance of `IDGenerator` and returns it if the instance
         * does not exist. Otherwise, returns the single instance of `IDGenerator` which
         * has already been instantiated.
         */
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
         *
         * See `nextId` for information about how to sequentially generate an ID.
         */
        fun generate(timestamp: Long, regionOrdinal: Int, machineId: Int, seq: Int): BigInteger {
            val part1: Long = timestamp.shl(REGION_BIT_LEN).shl(IP_BIT_LEN)
                    .or(regionOrdinal.toLong().shl(IP_BIT_LEN))
                    .or(machineId.toLong())
            return BigInteger.valueOf(part1).shiftLeft(SEQ_BIT_LEN).or(BigInteger.valueOf(seq.toLong()))
        }
    }

    /**
     * A traditional builder used to construct an instance of `IDGenerator`.
     */
    class Builder {
        private var region: AWSRegion? = null
        private var epoch: Instant? = null
        private var ip: String? = null

        /**
         * Set the generator's region (`AWSRegion`).
         */
        fun region(region: AWSRegion): Builder = apply { this.region = region }

        /**
         * Set the generator's epoch.
         */
        fun epoch(epoch: Instant): Builder = apply { this.epoch = epoch }

        /**
         * Set the generator's IP (IPv4).
         */
        fun ip(ip: String): Builder = apply { this.ip = ip }

        /**
         * Build an instance of `IDGenerator` using the configured values.
         * Note that `IDGenerator` is a singleton by design, so only one
         * instance can ever be created.
         */
        fun build(): IDGenerator {
            return getInstance(
                    region ?: AWSRegion.parse(EC2MetadataUtils.getEC2InstanceRegion()),
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
     * Thread-safe method to generate unique, 77-bit, time-sorted IDs
     * (AWSFlakes). A new unique ID is generated each time this method is
     * called.
     *
     * See `generate` for more details about the ID generation scheme
     * in general.
     */
    fun nextId(): BigInteger {
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

/**
 * Thrown to indicate the maximum timestamp possible has been exceeded. The
 * first 41 bits of AWSFlake's generated IDs are the timestamp since the epoch
 * in milliseconds. This means that a maximum timestamp of 2^41 - 1
 * (2199023255551) milliseconds is possible (~69 years). When this number is
 * exceeded, this exception is thrown. It can be avoided by using a more
 * recent epoch to generate IDs.
 */
class MaxTimestampExceededException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)

/**
 * Thrown to indicate that the current timestamp is less than the previous
 * timestamp, and thus an ID cannot be generated without the possibility of
 * collision with a previous ID. For example, imagine that the system has
 * been generating IDs every millisecond for two days, and then on the third
 * day, the system clock gets set to two days in the past. All IDs generated
 * on the third day and onward will be the same as the previous two days of
 * IDs, and are no longer unique.
 */
class ClockMovedBackwardsException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
