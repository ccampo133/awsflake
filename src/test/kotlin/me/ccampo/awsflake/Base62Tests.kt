package me.ccampo.awsflake

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigInteger
import java.util.*

class Base62Tests {

    @Test
    fun testEncode() {
        val num = 62 * 62L
        val s = encode(BigInteger.valueOf(num))
        assertThat(s).isEqualTo("100")
    }

    @Test
    fun testEncodeWithPadding() {
        val num = 62 * 62L
        val s = encode(BigInteger.valueOf(num), minLen = 20)
        assertThat(s).isEqualTo("00000000000000000100")
    }

    @Test
    fun testDecode() {
        val s = "100"
        val n = decode(s)
        assertThat(n.toLong()).isEqualTo(62 * 62L)
    }

    @Test
    fun testEncodeAndDecodeAreConsistent() {
        val random = Random()
        for (i in 0..100) {
            // Bitwise AND to only use positive values
            val n = BigInteger.valueOf(random.nextLong() and Long.MAX_VALUE)
            val s = encode(n)
            val n2 = decode(s)
            assertThat(n).isEqualTo(n2)
        }
    }
}
