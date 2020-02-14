package me.ccampo.awsflake.core

import org.assertj.core.api.Assertions.assertThat
import java.math.BigInteger
import java.time.Instant
import kotlin.test.Test

class IDGeneratorKtTest {

    @Test
    fun testGenerate() {
        val time = 1234L // Binary: 10011010010
        val reg = 1      // Binary: 1
        val mach = 1023  // Binary: 1111111111
        val seq = 9876   // Binary: 10011010010100

        /*
         * 41 bits: time
         *  5 bits: region identifier
         * 16 bits: machine ID
         * 15 bits: sequence number
         * Expected ID: 00000000000000000000000000000010011010010 00001 0000001111111111 010011010010100
         * Expected ID (decimal): 84802015307412
         */
        val id = IDGenerator.generate(time, reg, mach, seq)
        val expected = BigInteger.valueOf(84802015307412L)

        assertThat(id).isEqualTo(expected)
    }

    @Test
    fun testThatIDsAreSorted() {
        val reg = AWSRegion.US_EAST_1
        val ip = "10.0.128.255"
        val epoch = Instant.parse("2016-01-01T00:00:00Z")
        val generator = IDGenerator.getInstance(reg, epoch, ip)
        val ids = (1..50).map { encode(generator.nextId(), 13) }
        val sorted = ids.sorted()
        assertThat(ids).isEqualTo(sorted)
    }

    @Test
    fun testIsSingleton() {
        val reg = AWSRegion.US_EAST_1
        val ip = "10.0.128.255"
        val epoch = Instant.parse("2016-01-01T00:00:00Z")
        val generator1 = IDGenerator.getInstance(reg, epoch, ip)
        val generator2 = IDGenerator.getInstance(reg, epoch, ip)
        assertThat(generator1).isEqualTo(generator2)
    }

    @Test
    fun testIsSingleton_DifferentInputs() {
        val reg = AWSRegion.US_EAST_1
        val ip = "10.0.128.255"
        val epoch = Instant.parse("2016-01-01T00:00:00Z")
        val generator1 = IDGenerator.getInstance(reg, epoch, ip)
        val generator2 = IDGenerator.getInstance(AWSRegion.US_EAST_2, Instant.parse("2020-01-02T03:04:05Z"), "1.2.3.4")
        assertThat(generator1).isEqualTo(generator2)
    }

    @Test
    fun testBuilder_Singleton() {
        val reg = AWSRegion.US_EAST_1
        val ip = "10.0.128.255"
        val epoch = Instant.parse("2016-01-01T00:00:00Z")
        val generator1 = IDGenerator.Builder().epoch(epoch).ip(ip).region(reg).build()
        val generator2 = IDGenerator.Builder().epoch(epoch).ip(ip).region(reg).build()
        assertThat(generator1).isEqualTo(generator2)
    }

    @Test
    fun testBuilder_Singleton_DifferentInputs() {
        val reg = AWSRegion.US_EAST_1
        val ip = "10.0.128.255"
        val epoch = Instant.parse("2016-01-01T00:00:00Z")
        val generator1 = IDGenerator.Builder().epoch(epoch).ip(ip).region(reg).build()
        val generator2 = IDGenerator.Builder()
                .epoch(Instant.parse("2020-01-02T03:04:05Z"))
                .ip("1.2.3.4")
                .region(AWSRegion.US_EAST_2)
                .build()
        assertThat(generator1).isEqualTo(generator2)
    }
}
