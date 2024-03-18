package com.nikichxp.warehouse.entity

data class NewSKURequest(val name: String, val quantity: Int, val tags: Set<String> = setOf())