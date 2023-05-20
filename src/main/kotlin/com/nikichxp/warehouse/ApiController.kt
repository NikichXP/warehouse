package com.nikichxp.warehouse

import java.io.FileNotFoundException
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException.NotFound
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class ApiController(
    private val storageService: StorageService
) {

    @Bean
    fun router() = coRouter {
        path("storage").nest {
            GET("/list") {
                ServerResponse.ok().bodyAndAwait(storageService.getAllSKUs())
            }
            GET("/{id}") {
                val id = ObjectId(it.pathVariable("id"))
                ServerResponse.ok().bodyValueAndAwait(storageService.getById(id))
            }
            PUT("/{id}") {
                val id = ObjectId(it.pathVariable("id"))
                val quantity: Int = it.queryParam("quantity").map { i -> i.toIntOrNull() }.orElseThrow()!!
                ServerResponse.ok().bodyValueAndAwait(storageService.updateQuantity(id, quantity))
            }
            POST("/") {
                ServerResponse.ok().bodyValueAndAwait(storageService.addSKU(it.awaitBody()))
            }
            onError<FileNotFoundException>() { err, _ ->
                status(404).bodyValueAndAwait(mapOf("errorMessage" to err.message))
            }
        }
    }
}

@Service
class StorageService(
    private val repository: SKURepository
) {

    suspend fun addSKU(request: NewSKURequest): SKU {
        return repository.save(SKU(name = request.name, quantity = request.quantity))
    }

    suspend fun getById(id: ObjectId): SKU {
        return repository.findById(id) ?: throw FileNotFoundException()
    }

    fun getAllSKUs(): Flow<SKU> {
        return repository.findAll()
    }

    suspend fun updateQuantity(id: ObjectId, quantity: Int): SKU {
        return repository.findById(id)?.let {
            it.quantity = quantity
            repository.save(it)
        } ?: throw FileNotFoundException()
    }
}

data class NewSKURequest(val name: String, val quantity: Int)

data class SKU(
    var name: String,
    var quantity: Int = 0,
    var tags: Set<String> = setOf()
) {

    @Id
    @Field(targetType = FieldType.OBJECT_ID)
    lateinit var id: String

}

interface SKURepository : CoroutineCrudRepository<SKU, ObjectId>