package com.nikichxp.warehouse.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.web.reactive.function.server.ServerRequest


fun getUser(serverRequest: ServerRequest): String {
    return serverRequest.attributes()["user"] as String
}

fun <T> Flow<T>.distinct(): Flow<T> = flow {
    val past = mutableSetOf<T>()
    collect {
        val isNew = past.add(it)
        if (isNew) emit(it)
    }
}
