package com.nikichxp.warehouse

import com.mongodb.client.result.UpdateResult
import java.util.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.update
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
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
                val showEmpty = it.queryParam("showEmpty").map { it.toBoolean() }
                val options = ListOptions(
                    showEmpty = showEmpty
                )
                ServerResponse.ok().bodyAndAwait(storageService.listSKUs(options))
            }
            GET("/{id}") {
                val id = ObjectId(it.pathVariable("id"))
                ServerResponse.ok().bodyValueAndAwait(storageService.getById(id))
            }
            DELETE("/{id}") {
                val id = ObjectId(it.pathVariable("id"))
                ServerResponse.ok().bodyValueAndAwait(storageService.deleteById(id))
            }
            PUT("/{id}") {
                val id = ObjectId(it.pathVariable("id"))
                val quantity: Int = it.queryParam("quantity").map { i -> i.toIntOrNull() }.orElseThrow()!!
                storageService.updateQuantity(id, quantity)
                ServerResponse.ok().bodyValueAndAwait("Success")
            }
            POST("/") {
                ServerResponse.ok().bodyValueAndAwait(storageService.addSKU(it.awaitBody()))
            }
        }
        onError<Exception> { err, _ ->
            when (err) {
                is ElementNotFoundException -> status(404).bodyValueAndAwait(ExceptionInfo(err))
                else -> status(503).bodyValueAndAwait(ExceptionInfo(err))
            }
        }
    }
}

data class ListOptions(
    val showEmpty: Optional<Boolean> = Optional.empty()
) {
    fun toQuery(): Query {
        val query = Query()
        if (!this.showEmpty.get()) {
            query.addCriteria(Criteria.where(SKU::quantity.name).gt(0))
        }
        return query
    }
}

data class ExceptionInfo(
    val message: String,
    val errorType: String
) {
    constructor(exception: Throwable) : this(exception.message ?: "No info provided", exception.javaClass.simpleName)
}

class ElementNotFoundException(query: Any) : Exception("Element not found by query: $query")

@Service
class StorageService(
    private val dao: SKUDao
) {

    suspend fun addSKU(request: NewSKURequest): SKU {
        return dao.save(SKU(name = request.name, quantity = request.quantity))
    }

    suspend fun getById(id: ObjectId): SKU {
        return dao.findById(id) ?: throw ElementNotFoundException(id)
    }

    fun listSKUs(parameters: ListOptions): Flow<SKU> {
        return dao.customFind(parameters)
    }

    suspend fun deleteById(id: ObjectId): Boolean {
        val item = dao.findById(id) ?: throw ElementNotFoundException(id)
        return if (item.quantity > 0) {
            false
        } else {
            dao.delete(item)
            true
        }
    }

    suspend fun updateQuantity(id: ObjectId, quantity: Int) {
        val result = dao.updateQuantity(id, quantity)
        val success = result.modifiedCount > 0
        if (!success) {
            throw ElementNotFoundException(id)
        }
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

interface SKUCustomRepo {
    fun customFind(parameters: ListOptions): Flow<SKU>
    suspend fun updateQuantity(id: ObjectId, quantity: Int): UpdateResult
}

@Repository
interface SKUDao : SKURepository, SKUCustomRepo

@Repository
class SKUCustomRepoImpl(
    private val mongoTemplate: ReactiveMongoTemplate
) : SKUCustomRepo {
    override fun customFind(parameters: ListOptions): Flow<SKU> {
        val query = parameters.toQuery()
        return mongoTemplate.find<SKU>(query).asFlow()
    }

    override suspend fun updateQuantity(id: ObjectId, quantity: Int): UpdateResult {
        return mongoTemplate.update<SKU>().matching(Criteria.where(SKU::id.name).`is`(id))
            .apply(Update.update(SKU::quantity.name, quantity))
            .first()
            .awaitSingle()
    }
}
