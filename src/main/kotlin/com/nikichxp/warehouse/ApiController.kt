package com.nikichxp.warehouse

import com.mongodb.client.result.UpdateResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.converter.Converter
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
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class CorsGlobalConfiguration : WebFluxConfigurer {

    override fun addCorsMappings(corsRegistry: CorsRegistry) {
        corsRegistry
            .addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("*")
            .maxAge(3600)
    }
}

fun getUser(serverRequest: ServerRequest): String {
    return serverRequest.attributes()["user"] as String
}

@Configuration
class ApiController(
    private val storageService: StorageService,
    private val storageTagsService: StorageTagsService,
    private val conversionService: ConversionService
) {

    @Bean
    fun router() = coRouter {
        GET("/ping") {
            ServerResponse.ok().bodyValueAndAwait("pong!~")
        }
        path("storage").nest {
            filter { request, next ->
                request.headers().header("user").firstOrNull()?.let {
                    request.attributes()["user"] = it
                    next(request)
                } ?: ServerResponse.badRequest().bodyValueAndAwait("Missing user header")
            }
            path("/tags").nest {
                GET("/list") {
                    ServerResponse.ok().bodyAndAwait(storageTagsService.listPossibleTags(getUser(it)))
                }
            }
            GET("/list") { req ->
                val showEmpty = req.queryParam("showEmpty").map { it.toBoolean() }.orElse(null)
                val options = ListOptions(
                    showEmpty = showEmpty,
                    userId = getUser(req)
                )
                ServerResponse.ok().bodyAndAwait(
                    storageService
                        .listSKUs(options)
                        .map { conversionService.convert(it, SKUDTO::class.java)!! }
                )
            }
            GET("/{id}") {
                val id = ObjectId(it.pathVariable("id"))
                val userId = getUser(it)
                val responseEntity = storageService.getByIdAndUserId(id, userId)
                ServerResponse.ok().bodyValueAndAwait(responseEntity)
            }
            DELETE("/{id}") {
                val id = ObjectId(it.pathVariable("id"))
                val userId = getUser(it)
                ServerResponse.ok().bodyValueAndAwait(storageService.deleteById(id, userId))
            }
            PUT("/{id}") {
                val id = ObjectId(it.pathVariable("id"))
                val quantity: Int = it.queryParam("quantity").map { i -> i.toIntOrNull() }.orElseThrow()!!
                val userId = getUser(it)
                storageService.updateQuantity(id, quantity, userId)
                ServerResponse.ok().bodyValueAndAwait("Success")
            }
            POST("/") {
                val userId = getUser(it)
                val createdEntity = storageService.addSKU(it.awaitBody(), userId)
                ServerResponse.ok().bodyValueAndAwait(conversionService.convert(createdEntity, SKUDTO::class.java)!!)
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
    val showEmpty: Boolean? = null,
    var userId: String? = null
) {

    fun toQuery(): Query {
        val query = Query()
        userId?.let { query.addCriteria(Criteria.where(SKU::userIds.name).`is`(it)) }
        showEmpty?.let { query.addCriteria(Criteria.where(SKU::quantity.name).gt(0)) }
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

@OptIn(ExperimentalCoroutinesApi::class)
@Service
class StorageTagsService(
    private val dao: SKUDao
) {
    suspend fun listPossibleTags(userId: String): Flow<String> {
        return dao.findAll().map { it.tags.asFlow() }.flattenConcat().distinct()
    }
}

fun <T> Flow<T>.distinct(): Flow<T> = flow {
    val past = mutableSetOf<T>()
    collect {
        val isNew = past.add(it)
        if (isNew) emit(it)
    }
}

@Service
class StorageService(
    private val dao: SKUDao
) {

    suspend fun addSKU(request: NewSKURequest, userId: String): SKU {
        return dao.save(
            SKU(
                name = request.name,
                quantity = request.quantity,
                tags = request.tags
            ).also { it.userIds += userId })
    }

    suspend fun getByIdAndUserId(id: ObjectId, userId: String): SKU {
        return dao.findByIdAndUserIds(id, userId) ?: throw ElementNotFoundException(id)
    }

    fun listSKUs(parameters: ListOptions): Flow<SKU> {
        return dao.customFind(parameters)
    }

    suspend fun deleteById(id: ObjectId, userId: String): Boolean {
        val item = dao.findByIdAndUserIds(id, userId) ?: throw ElementNotFoundException(id)
        return if (item.quantity > 0) {
            false
        } else {
            dao.delete(item)
            true
        }
    }

    suspend fun updateQuantity(id: ObjectId, quantity: Int, userId: String) {
        val result = dao.updateQuantity(id, quantity, userId)
        val success = result.modifiedCount > 0
        if (!success) {
            throw ElementNotFoundException(id)
        }
    }
}

data class NewSKURequest(val name: String, val quantity: Int, val tags: Set<String> = setOf())

data class SKUDTO(
    val id: String,
    val name: String,
    val quantity: Int,
    val tags: Set<String>
)

@Component
class SKUConverter : Converter<SKU, SKUDTO> {

    override fun convert(sku: SKU): SKUDTO {
        return SKUDTO(
            id = sku.id,
            name = sku.name,
            quantity = sku.quantity,
            tags = sku.tags
        )
    }
}

data class SKU(
    var name: String,
    var quantity: Int = 0,
    var tags: Set<String> = setOf()
) {

    @Id
    @Field(targetType = FieldType.OBJECT_ID)
    lateinit var id: String

    var userIds: Set<String> = setOf()

}

interface SKURepository : CoroutineCrudRepository<SKU, ObjectId> {
    @Suppress("SpringDataRepositoryMethodParametersInspection", "SpringDataRepositoryMethodReturnTypeInspection")
    suspend fun findByIdAndUserIds(id: ObjectId, userId: String): SKU?
}

interface SKUCustomRepo {
    fun customFind(parameters: ListOptions): Flow<SKU>
    suspend fun updateQuantity(id: ObjectId, quantity: Int, userId: String): UpdateResult
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

    override suspend fun updateQuantity(id: ObjectId, quantity: Int, userId: String): UpdateResult {
        return mongoTemplate.update<SKU>().matching(
            Criteria.where(SKU::id.name).`is`(id)
                .and(SKU::userIds.name).`is`(userId)
        )
            .apply(Update.update(SKU::quantity.name, quantity))
            .first()
            .awaitSingle()
    }
}
