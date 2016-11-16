package me.ccampo.awsflake

import org.slf4j.Logger
import java.math.BigInteger
import java.time.Duration
import java.time.LocalDateTime


const val TIME_BIT_LEN = 41
const val NODE_ID_LEN = 10
const val SEQ_BIT_LEN = 12
const val SEQUENCE_MAX = 4096 // 2^12
const val NODE_ID_MAX = 1023

private val defaultEpoch = LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0)
private var lastTimestamp = -1L
private var sequence = 0

/**
 * The ID is an 64 bit signed integer of the following composition:
 *
 * 41 bits: time since epoch, in millis (this gives us a max of ~69.7 years from the epoch)
 * 10 bits: the manually assigned node ID (0-1024)
 * 12 bits: sequence number
 *
 * @author Chris Campo
 */
fun generate(nodeId: Short, epoch: LocalDateTime? = null, logger: Logger? = null): Long {

    if (nodeId > NODE_ID_MAX || nodeId < 0) {
        throw MaxNodeIdException("Node ID must be a value between 0 - $NODE_ID_MAX")
    }

    var timestamp = Duration.between(epoch ?: defaultEpoch, LocalDateTime.now()).toMillis()

    if (timestamp >= 1L shl TIME_BIT_LEN ) {
        throw MaxTimestampExceededException("Max timestamp exceeded - please restart with a more recent epoch")
    }

    // Clock moved backwards - refuse to generate ID
    if (timestamp < lastTimestamp) {
        throw ClockMovedBackwardsException("Clock moved backwards - ID cannot be generated")
    }

    // TODO: is locking on sequence sufficient? -ccampo 2016-11-10
    synchronized(sequence) {
        if (lastTimestamp == timestamp) {
            sequence =  (sequence + 1) % SEQUENCE_MAX
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

    return generate(timestamp, nodeId, sequence, logger = logger)
}

internal fun generate(timestamp: Long, nodeId: Short, seq: Int, logger: Logger? = null): Long {
    val id: Long = (((timestamp shl NODE_ID_LEN) shl SEQ_BIT_LEN) or (nodeId.toLong() shl SEQ_BIT_LEN)) or seq.toLong()

    if (logger != null && logger.isDebugEnabled) {
        logger.debug("Timestamp: {}", timestamp)
        logger.debug("Timestamp binary: {}", BigInteger.valueOf(timestamp).toString(2))
        logger.debug("Node ID: {}", nodeId)
        logger.debug("Node ID binary: {}", Integer.toBinaryString(nodeId.toInt()))
        logger.debug("Sequence: {}", seq)
        logger.debug("Sequence binary: {}", Integer.toBinaryString(seq))
        logger.debug("ID: {}", id)
        logger.debug("ID binary: {}", String.format("%$64s", BigInteger.valueOf(id).toString(2)).replace(" ", "0"))
    }

    return id
}

class MaxNodeIdException(msg: String? = null, cause: Throwable? = null): Exception(msg, cause)
class MaxTimestampExceededException(msg: String? = null, cause: Throwable? = null): Exception(msg, cause)
class ClockMovedBackwardsException(msg: String? = null, cause: Throwable? = null): Exception(msg, cause)
