package com.ys.injectron.lazy

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Injectable

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Singleton

// Lazy wrapper class
class Lazy<T : Any>(private val initializer: () -> T) {
    private var instance: T? = null

    fun get(): T {
        if (instance == null) {
            instance = initializer()
        }
        return instance!!
    }
}

// Lazy injection annotation
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class InjectLazy

class DIContainer {
    private val dependencies: MutableMap<KClass<*>, () -> Any> = mutableMapOf()
    private val singletons: MutableMap<KClass<*>, Any> = mutableMapOf()

    inline fun <reified T : Any> register(noinline creator: () -> T) {
        register(T::class, creator)
    }

    fun <T : Any> register(klass: KClass<T>, creator: () -> T) {
        dependencies[klass] = creator
    }

    inline fun <reified T : Any> resolve(): T = resolve(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(klass: KClass<T>): T {
        // dependencies 에서 의존성이 있는지 먼저 확인
        dependencies[klass]?.let { creator ->
            return creator() as T
        }

        // Singleton 체크
        if (klass.hasAnnotation<Singleton>()) {
            return singletons.getOrPut(klass) {
                createInstance(klass)
            } as T
        }

        return createInstance(klass)
    }

    private fun <T : Any> resolveLazy(klass: KClass<T>): Lazy<T> {
        return Lazy { resolve(klass) }
    }

    private fun <T : Any> createInstance(klass: KClass<T>): T {
        if (!klass.hasAnnotation<Injectable>()) {
            throw IllegalArgumentException("Class ${klass.simpleName} is not annotated with @Injectable")
        }

        val constructor = klass.constructors.firstOrNull()
            ?: throw IllegalArgumentException("No constructor found for ${klass.simpleName}")

        val params = constructor.parameters.map { param ->
            resolve(param.type.classifier as KClass<*>)
        }.toTypedArray()

        val instance = constructor.call(*params)

        // Property injection
        klass.memberProperties.forEach { property ->
            when {
                property.findAnnotation<Inject>() != null -> {
                    injectProperty(property, instance)
                }
                property.findAnnotation<InjectLazy>() != null -> {
                    injectLazyProperty(property, instance)
                }
            }
        }

        return instance
    }

    private fun injectProperty(property: KProperty1<*, *>, instance: Any) {
        property.isAccessible = true
        if (property is KMutableProperty<*>) {
            val value = resolve(property.returnType.classifier as KClass<*>)
            try {
                property.setter.call(instance, value)
            } catch (e: Exception) {
                throw IllegalStateException("Cannot inject property ${property.name}", e)
            }
        } else {
            throw IllegalStateException("Cannot inject non-mutable property ${property.name}")
        }
    }

    private fun injectLazyProperty(property: KProperty1<*, *>, instance: Any) {
        property.isAccessible = true
        if (property is KMutableProperty<*>) {
            val lazyValue = resolveLazy(property.returnType.arguments[0].type!!.classifier as KClass<*>)
            try {
                property.setter.call(instance, lazyValue)
            } catch (e: Exception) {
                throw IllegalStateException("Cannot inject lazy property ${property.name}", e)
            }
        } else {
            throw IllegalStateException("Cannot inject non-mutable lazy property ${property.name}")
        }
    }
}

// 테스트를 위한 항목
@Injectable
class LazyDependency {
    init {
        println("LazyDependency initialized")
    }

    fun doSomething() {
        println("LazyDependency doing something")
    }
}

@Injectable
class ServiceWithLazyDependency {
    @InjectLazy
    lateinit var lazyDependency: Lazy<LazyDependency>

    fun useDependency() {
        println("About to use lazy dependency")
        lazyDependency.get().doSomething()
    }
}

fun main() {
    val container = DIContainer()

    val service = container.resolve<ServiceWithLazyDependency>()
    println("Service resolved")

    service.useDependency()
}