package com.nikichxp.warehouse

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Component


//@Component
class Test(
    private val reactiveMongoTemplate: ReactiveMongoTemplate
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(DelicateCoroutinesApi::class)
    override fun run(vararg args: String?) {
        val prefix = "d-${System.currentTimeMillis()}-"
        val entities = (1..1_000_000).map { TestEntity(prefix + it) }
        runBlocking {
            logger.info("---- !!! Job started !!! ----")
            var last = 0
            launch(newSingleThreadContext("ctr")) {
                while (true) {
                    delay(1_000)
                    logger.info("Current item: $last")
                }
            }
            withContext(newFixedThreadPoolContext(12, "example")) {
                val jobs = entities.map { reactiveMongoTemplate.insert(it) }
                val futures = jobs//.map { launch { it.awaitSingle() } }
                futures.withIndex().map { (i, x) ->
                    x.awaitSingle()
                    last = i
                }
            }
            logger.info("---- !!! Job ended !!! ----")
        }
    }

}

data class TestEntity(var content: String) {
    @Id
    lateinit var id: ObjectId
}

