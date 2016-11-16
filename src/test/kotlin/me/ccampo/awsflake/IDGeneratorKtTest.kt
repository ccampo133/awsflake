package me.ccampo.awsflake

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.LocalDateTime

/**
 * @author Chris Campo
 */
class IDGeneratorKtTest {

    @Test
    fun testGenerate() {
        val time = 1234L // Binary: 10011010010
        val node = 1023  // Binary: 1111111111
        val seq = 3212   // Binary: 110010001100

        // 41 bits: time
        // 10 bits: node ID
        // 12 bits: sequence number
        // Expected ID: 00000000000000000000000000000010011010010 1111111111 110010001100
        // Expected ID (decimal): 5179964556

        val id = generate(time, node.toShort(), seq)
        val expected = 5179964556L

        assertThat(id).isEqualTo(expected)
    }

    @Test
    fun testThatIDsAreSorted() {
        val node: Short = 1
        val epoch = LocalDateTime.of(2016, 1, 1, 0, 0, 0)
        val ids = (1..50).map { i -> generate(node, epoch = epoch) }
        val sorted = ids.sorted()
        assertThat(ids).isEqualTo(sorted)
    }
}
