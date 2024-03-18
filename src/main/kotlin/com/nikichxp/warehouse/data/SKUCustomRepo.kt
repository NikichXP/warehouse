package com.nikichxp.warehouse.data

import com.mongodb.client.result.UpdateResult
import com.nikichxp.warehouse.entity.ListOptions
import com.nikichxp.warehouse.entity.SKU
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId

interface SKUCustomRepo {
    fun customFind(parameters: ListOptions): Flow<SKU>
    suspend fun updateQuantity(id: ObjectId, quantity: Int, userId: String): UpdateResult
}