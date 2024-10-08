package com.ys.injectron.provider.async

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration.Companion.seconds

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Injectable

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Singleton

@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Qualifier(val value: String = "")

data class DependencyKey(val klass: KClass<*>, val qualifier: String?)

// Async Provider interfaces
interface AsyncProvider<T> {
    suspend fun get(): T
}

interface Provider<T> {
    fun get(): T
}

// DI Container implementation
class DIContainer {
    // Changed from private to internal for inline function access
    internal val dependencies = mutableMapOf<DependencyKey, () -> Any>()
    internal val singletons = mutableMapOf<DependencyKey, Any>()
    internal val asyncDependencies = mutableMapOf<DependencyKey, suspend () -> Any>()

    internal val containerScope = CoroutineScope(Dispatchers.Default)

    // Non-inline registration methods
    fun <T : Any> registerAsyncProviderNonInline(
        klass: KClass<T>,
        qualifier: String? = null,
        provider: suspend () -> T
    ) {
        val providerKey = DependencyKey(AsyncProvider::class, qualifier)
        dependencies[providerKey] = {
            object : AsyncProvider<T> {
                override suspend fun get(): T = provider()
            }
        }
    }

    // Inline wrapper methods
    inline fun <reified T : Any> registerAsyncProvider(
        qualifier: String? = null,
        noinline provider: suspend () -> T
    ) {
        registerAsyncProviderNonInline(T::class, qualifier, provider)
    }

    fun <T : Any> registerProviderNonInline(
        klass: KClass<T>,
        qualifier: String? = null,
        provider: () -> T
    ) {
        val providerKey = DependencyKey(Provider::class, qualifier)
        dependencies[providerKey] = {
            object : Provider<T> {
                override fun get(): T = provider()
            }
        }
    }

    inline fun <reified T : Any> registerProvider(
        qualifier: String? = null,
        noinline provider: () -> T
    ) {
        registerProviderNonInline(T::class, qualifier, provider)
    }

    // Basic registration methods
    fun <T : Any> registerNonInline(
        klass: KClass<T>,
        qualifier: String? = null,
        creator: (() -> T)? = null
    ) {
        val key = DependencyKey(klass, qualifier)
        if (creator != null) {
            dependencies[key] = creator
        } else if (!klass.isAbstract) {
            if (klass.hasAnnotation<Injectable>()) {
                dependencies[key] = { createInstance(klass, qualifier) }
            } else {
                throw IllegalArgumentException(
                    "Class ${klass.simpleName} is not annotated with @Injectable " +
                            "and no creator function provided"
                )
            }
        }
    }

    inline fun <reified T : Any> register(
        qualifier: String? = null,
        noinline creator: (() -> T)? = null
    ) {
        registerNonInline(T::class, qualifier, creator)
    }

    // Async registration methods
    fun <T : Any> registerAsyncNonInline(
        klass: KClass<T>,
        qualifier: String? = null,
        creator: (suspend () -> T)?
    ) {
        val key = DependencyKey(klass, qualifier)
        if (creator != null) {
            asyncDependencies[key] = creator
        } else if (!klass.isAbstract) {
            if (klass.hasAnnotation<Injectable>()) {
                asyncDependencies[key] = { createAsyncInstance(klass, qualifier) }
            } else {
                throw IllegalArgumentException(
                    "Class ${klass.simpleName} is not annotated with @Injectable " +
                            "and no creator function provided"
                )
            }
        }
    }

    inline fun <reified T : Any> registerAsync(
        qualifier: String? = null,
        noinline creator: (suspend () -> T)?
    ) {
        registerAsyncNonInline(T::class, qualifier, creator)
    }

    // Resolution methods
    fun <T : Any> resolveNonInline(klass: KClass<T>, qualifier: String? = null): T {
        val key = DependencyKey(klass, qualifier)

        if (klass.hasAnnotation<Singleton>()) {
            @Suppress("UNCHECKED_CAST")
            return singletons.getOrPut(key) { createInstance(klass, qualifier) } as T
        }

        val creator = dependencies[key]
            ?: throw IllegalArgumentException(
                "No dependency found for ${klass.simpleName} with qualifier $qualifier"
            )

        @Suppress("UNCHECKED_CAST")
        return creator() as T
    }

    inline fun <reified T : Any> resolve(qualifier: String? = null): T =
        resolveNonInline(T::class, qualifier)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolveAsyncProviderNonInline(
        klass: KClass<T>,
        qualifier: String? = null
    ): AsyncProvider<T> {
        return resolveNonInline(klass::class, qualifier) as AsyncProvider<T>
    }

    inline fun <reified T : Any> resolveAsyncProvider(qualifier: String? = null): AsyncProvider<T> =
        resolveAsyncProviderNonInline(T::class, qualifier)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolveProviderNonInline(
        klass: KClass<T>,
        qualifier: String? = null
    ): Provider<T> {
        return resolveNonInline(Provider::class, qualifier) as Provider<T>
    }

    inline fun <reified T : Any> resolveProvider(qualifier: String? = null): Provider<T> =
        resolveProviderNonInline(T::class, qualifier)

    suspend fun <T : Any> resolveAsyncNonInline(klass: KClass<T>, qualifier: String? = null): T {
        val key = DependencyKey(klass, qualifier)

        if (klass.hasAnnotation<Singleton>()) {
            return withContext(Dispatchers.Default) {
                @Suppress("UNCHECKED_CAST")
                singletons.getOrPut(key) {
                    createAsyncInstance(klass, qualifier)
                } as T
            }
        }

        val creator = asyncDependencies[key]
            ?: throw IllegalArgumentException(
                "No async dependency found for ${klass.simpleName} with qualifier $qualifier"
            )

        @Suppress("UNCHECKED_CAST")
        return creator() as T
    }

    private suspend inline fun <reified T : Any> resolveAsync(qualifier: String? = null): T =
        resolveAsyncNonInline(T::class, qualifier)

    // Instance creation helpers
    private fun <T : Any> createInstance(klass: KClass<T>, qualifier: String?): T {
        val constructor = klass.primaryConstructor
            ?: throw IllegalArgumentException(
                "No primary constructor found for class ${klass.simpleName}"
            )

        val params = constructor.parameters.map { param ->
            val paramQualifier = param.findAnnotation<Qualifier>()?.value ?: qualifier
            val paramType = param.type.classifier as KClass<*>

            when (paramType) {
                AsyncProvider::class -> resolveAsyncProvider<Any>(paramQualifier)
                Provider::class -> resolveProvider<Any>(paramQualifier)
                else -> resolve<Any>(paramQualifier)
            }
        }.toTypedArray()

        return constructor.call(*params)
    }

    private suspend fun <T : Any> createAsyncInstance(klass: KClass<T>, qualifier: String?): T {
        val constructor = klass.primaryConstructor
            ?: throw IllegalArgumentException(
                "No primary constructor found for class ${klass.simpleName}"
            )

        val params = constructor.parameters.map { param ->
            val paramQualifier = param.findAnnotation<Qualifier>()?.value ?: qualifier
            val paramType = param.type.classifier as KClass<*>

            when (paramType) {
                AsyncProvider::class -> resolveAsyncProvider<Any>(paramQualifier)
                Provider::class -> resolveProvider<Any>(paramQualifier)
                else -> resolveAsync<Any>(paramQualifier)
            }
        }.toTypedArray()

        return constructor.call(*params)
    }
}


// Example classes remain the same
@Injectable
class AsyncDatabaseService {
    suspend fun fetchData(): String {
        delay(1.seconds)
        return "Data from database"
    }
}

@Injectable
class CacheService {
    private val cache = mutableMapOf<String, String>()

    suspend fun getData(key: String): String? {
        delay(100)
        return cache[key]
    }

    suspend fun setData(key: String, value: String) {
        delay(100)
        cache[key] = value
    }
}

@Injectable
class DataRepository(
    private val dbProvider: AsyncProvider<String>,
    private val cacheService: CacheService
) {
    suspend fun getData(): String {
        val cachedData = cacheService.getData("key")
        if (cachedData != null) {
            return cachedData
        }

        val dbData = dbProvider.get()
        cacheService.setData("key", dbData)
        return dbData
    }
}

fun main() = runBlocking {
    val container = DIContainer()

    container.register<AsyncDatabaseService>()
    container.register<CacheService>()
    container.registerAsyncProvider<String> {
        container.resolve<AsyncDatabaseService>().fetchData()
    }

    val repository = container.resolve<DataRepository>()
    val data = repository.getData()
    println("Retrieved data: $data")
}