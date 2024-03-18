package com.nikichxp.warehouse.entity

class ElementNotFoundException(query: Any) : Exception("Element not found by query: $query")