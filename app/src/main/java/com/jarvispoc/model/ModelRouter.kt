package com.jarvispoc.model

enum class ModelType { LOCAL, CLOUD }

class ModelRouter(private val localModel: LanguageModel) {
    fun getModel(type: ModelType = ModelType.LOCAL): LanguageModel {
        return localModel // Fallback to local for now
    }
}
