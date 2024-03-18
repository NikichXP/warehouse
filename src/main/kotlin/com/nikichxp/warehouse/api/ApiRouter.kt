package com.nikichxp.warehouse.api

import com.nikichxp.warehouse.entity.ElementNotFoundException
import com.nikichxp.warehouse.entity.ExceptionInfo
import com.nikichxp.warehouse.entity.ListOptions
import com.nikichxp.warehouse.entity.SKUDTO
import com.nikichxp.warehouse.entity.StorageTagsService
import com.nikichxp.warehouse.service.StorageService
import com.nikichxp.warehouse.util.getUser
import kotlinx.coroutines.flow.map
import org.bson.types.ObjectId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.ConversionService
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class ApiRouter(
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

