package com.nikichxp.warehouse.service

import com.nikichxp.warehouse.entity.ElementNotFoundException
import com.nikichxp.warehouse.entity.ListOptions
import com.nikichxp.warehouse.data.SKUDao
import com.nikichxp.warehouse.entity.NewSKURequest
import com.nikichxp.warehouse.entity.SKU
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

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