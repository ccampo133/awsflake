package me.ccampo.awsflake.core

import java.math.BigInteger

// Base62 charset sorted to quickly calculate decimal equivalency by compensating.
private val charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()


/**
 * Return a base-62 encoded string representing an arbitrary length
 * big-integer `value`.
 *
 * @param minLen: The minimum length of the encoded string to return. If the
 * encoded value is less than `minLen`, it is padded with zeros.
 */
fun encode(value: BigInteger, minLen: Int? = null): String {
    var num = value
    val sb = StringBuilder(1)
    do {
        val i = num.mod(BigInteger.valueOf(62)).toInt()
        sb.insert(0, charset[i])
        num = num.divide(BigInteger.valueOf(62))
    } while (num > BigInteger.ZERO)

    // Pad with zeros if necessary
    if (minLen != null && sb.length < minLen) {
        return sb.padStart(minLen, '0').toString()
    }

    return sb.toString()
}

/**
 * Decodes a base-62 encoded value into an arbitrary length big-integer.
 */
fun decode(value: String): BigInteger {
    var result = BigInteger.ZERO
    var power = BigInteger.ONE
    for (i in value.length - 1 downTo 0) {
        var digit = value[i].toInt() - 48
        if (digit > 42) digit -= 13 else if (digit > 9) digit -= 7
        result = result.plus(power.times(BigInteger.valueOf(digit.toLong())))
        power = power.times(BigInteger.valueOf(62))
    }
    return result
}
