package me.ccampo.awsflake.server

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Factory
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.runtime.Micronaut
import me.ccampo.awsflake.core.AWSRegion
import me.ccampo.awsflake.core.IDGenerator
import me.ccampo.awsflake.core.encode
import java.time.Instant
import javax.inject.Singleton

object AWSFlakeApplication {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("me.ccampo.awsflake")
                .mainClass(AWSFlakeApplication.javaClass)
                .start()
    }
}

@Controller("/id")
class AWSFlakeController(private val idGenerator: IDGenerator) {

    @Get(produces = [MediaType.TEXT_PLAIN])
    fun generateId(@QueryValue minLength: Int?): String {
        val id = idGenerator.nextId()
        return encode(id, minLen = minLength)
    }
}

@Factory
internal class IDGeneratorFactory {

    @Singleton
    fun idGenerator(ctx: ApplicationContext): IDGenerator {
        val builder = IDGenerator.Builder()

        ctx.environment.getProperty("awsflake.ip", String::class.java)
                .ifPresent { builder.ip(it) }
        ctx.environment.getProperty("awsflake.epoch", String::class.java)
                .ifPresent { builder.epoch(Instant.parse(it)) }
        ctx.environment.getProperty("awsflake.region", String::class.java)
                .ifPresent { builder.region(AWSRegion.parse(it)) }

        return builder.build()
    }
}
