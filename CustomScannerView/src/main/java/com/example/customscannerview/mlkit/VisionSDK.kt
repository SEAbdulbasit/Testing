package com.example.customscannerview.mlkit


class VisionSDK private constructor() {

    var environment: Environment? = null
    var apiKey: String? = null

    fun initialise(apiKey: String, environment: Environment) {
        this.apiKey = apiKey
        this.environment = environment
    }


    companion object {
        var single = VisionSDK()

        fun getInstance(): VisionSDK {
            if (single == null) single = VisionSDK()
            return single
        }
    }

}


sealed interface Environment {
    object DEV : Environment
    object QA : Environment
    object STAGING : Environment
    object PRODUCTION : Environment
    object SANDBOX : Environment
}