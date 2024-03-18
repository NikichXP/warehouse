package com.nikichxp.warehouse.entity

data class SKUDTO(
    val id: String,
    val name: String,
    val quantity: Int,
    val tags: Set<String>
)