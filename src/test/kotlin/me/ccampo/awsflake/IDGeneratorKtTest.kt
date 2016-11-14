package me.ccampo.awsflake

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDateTime

/**
 * @author Chris Campo
 */
class IDGeneratorKtTest {

    @Test
    fun testGenerate() {
        val time = 1234L // Binary: 10011010010
        val reg = 1      // Binary: 1
        val mach = 1023  // Binary: 1111111111
        val seq = 9876   // Binary: 10011010010100

        // 41 bits: time
        //  5 bits: region identifier
        // 16 bits: machine ID
        // 15 bits: sequence number
        // Expected ID: 00000000000000000000000000000010011010010 00001 0000001111111111 010011010010100
        // Expected ID (decimal): 84802015307412

        val id = generate(time, reg, mach, seq)
        val expected = BigInteger.valueOf(84802015307412L)

        assertThat(id).isEqualTo(expected)
    }

    @Test
    fun testThatIDsAreSorted() {
        val reg = 1
        val ip = Pair(128, 255)
        val epoch = LocalDateTime.of(2016, 1, 1, 0, 0, 0)
        val ids = (1..50).map { i -> encode(generate(reg, ip, epoch), 13) }
        val sorted = ids.sorted()
        assertThat(ids).isEqualTo(sorted)
    }
}
