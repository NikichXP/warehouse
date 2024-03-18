package com.nikichxp.warehouse.entity

data class ExceptionInfo(
    val message: String,
    val errorType: String
) {
    constructor(exception: Throwable) : this(exception.message ?: "No info provided", exception.javaClass.simpleName)
}