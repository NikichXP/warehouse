package com.nikichxp.warehouse.entity

import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

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