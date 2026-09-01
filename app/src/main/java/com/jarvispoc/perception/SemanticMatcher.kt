package com.jarvispoc.perception

interface SemanticMatcher {
    suspend fun match(query: String, nodes: List<Any>): Any?
}
