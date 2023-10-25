package dev.gitlive.firebase

import kotlin.reflect.KClass

//TODO @InternalApi
interface FirebaseService {

    fun release() {}
}

//TODO @InternalApi
inline fun <reified T : FirebaseService> FirebaseApp.require() : T = require(T::class)

//TODO @InternalApi
inline fun <reified T : FirebaseService> FirebaseApp.getOrPut(
    noinline factory : (app : FirebaseApp) -> T
) : T = getOrPut(T::class, factory)

actual class FirebaseApp(
    private val _name: String?,
    actual val options: FirebaseOptions
)  {

    private val services = mutableMapOf<String, FirebaseService>()

    private fun <T : FirebaseService> keyFor(serviceClass : KClass<T>)=
        requireNotNull(serviceClass.simpleName) {
            "Service must not be an inline class"
        }

    //TODO @InternalApi
    @Suppress("UNCHECKED_CAST")
    fun <T : FirebaseService> require(clazz : KClass<T>) : T {
        return requireNotNull(services[keyFor(clazz)]) as T
    }

    //TODO @InternalApi
    @Suppress("UNCHECKED_CAST")
    fun <T : FirebaseService> getOrPut(
        clazz : KClass<T>,
        factory : (app : FirebaseApp) -> T
    ) : T {

        val key = keyFor(clazz)

        services[key]?.let {
            return it as T
        }

        return factory(this).also { services[key] = it }
    }

    var platform : FirebasePlatform = DefaultFirebasePlatform()

    actual val name: String
        get() = _name ?: "default"

    actual fun delete() {
        instances.remove(name)
        val s = services.values
        services.clear()
        s.forEach(FirebaseService::release)
    }

    internal companion object {
        internal val instances : MutableMap<String?, FirebaseApp> = mutableMapOf()
    }
}

fun FirebaseApp.key(key : String) = "$key-$name"

actual val Firebase.app: FirebaseApp
    get() = requireNotNull(FirebaseApp.instances[null]) {
        "Firebase app is not configured"
    }

//actual fun Firebase.apps(context: Any?): List<FirebaseApp> {
//    return FirebaseApp.instances.values.toList()
//}
//actual fun Firebase.apps(context: Any?): List<FirebaseApp> {
//    throw IllegalStateException("Rest client must be initialized with options")
//}

fun Firebase.initialize(options: FirebaseOptions): FirebaseApp {

    FirebaseApp.instances[null]?.delete()

    val app = FirebaseApp(null, options)
    FirebaseApp.instances[null] = app
    return app
}

actual fun Firebase.apps(context: Any?): List<FirebaseApp> =
    FirebaseApp.instances.values.toList()

actual fun Firebase.app(name: String): FirebaseApp {
    throw IllegalStateException("Rest client must be initialized with options")
}

actual fun Firebase.initialize(context: Any?): FirebaseApp? = initialize()

actual fun Firebase.initialize(context: Any?, options: FirebaseOptions): FirebaseApp =
    initialize(options)

actual fun Firebase.initialize(context: Any?, options: FirebaseOptions, name: String): FirebaseApp {

    FirebaseApp.instances[name]?.delete()

    val app = FirebaseApp(name, options)
    FirebaseApp.instances[name] = app
    return app
}

actual open class FirebaseException : Exception()

actual class FirebaseNetworkException : FirebaseException()

actual open class FirebaseTooManyRequestsException : FirebaseException()

actual open class FirebaseApiNotAvailableException : FirebaseException()
