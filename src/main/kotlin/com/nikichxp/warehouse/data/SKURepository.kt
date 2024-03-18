package com.nikichxp.warehouse.data

import com.nikichxp.warehouse.entity.SKU
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SKURepository : CoroutineCrudRepository<SKU, ObjectId> {
    @Suppress("SpringDataRepositoryMethodParametersInspection", "SpringDataRepositoryMethodReturnTypeInspection")
    suspend fun findByIdAndUserIds(id: ObjectId, userId: String): SKU?
}