package dev.gitlive.firebase


interface FirebasePlatform {

    fun store(key: String, value: String)

    fun retrieve(key: String): String?

    fun clear(key: String)

    fun log(msg: String)
}

class DefaultFirebasePlatform : FirebasePlatform {

    private val keyValueStorage = mutableMapOf<String, String>()

    override fun store(key: String, value: String) {
        keyValueStorage[key] = value
    }

    override fun retrieve(key: String): String? {
        return keyValueStorage[key]
    }

    override fun clear(key: String) {
        keyValueStorage.remove(key)
    }

    override fun log(msg: String) {
        println(msg)
    }
}

