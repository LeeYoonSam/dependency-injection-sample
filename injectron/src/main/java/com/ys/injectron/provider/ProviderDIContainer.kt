package com.ys.injectron.provider

import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Injectable

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class LazyInject

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Singleton

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Qualifier(val value: String = "")

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Factory

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Produce

interface Provider<T> {
    fun get(): T
}

class DIContainer {
    private val dependencies: MutableMap<DependencyKey, () -> Any> = mutableMapOf()
    private val singleton: MutableMap<DependencyKey, Any> = mutableMapOf()

    inline fun <reified T : Any> register(qualifier: String? = null, noinline creator: (() -> Any)? = null) {
        register(T::class, qualifier, creator)
    }

    fun <T : Any> register(klass: KClass<T>, qualifier: String?, creator: (() -> Any)?) {
        val key = DependencyKey(klass, qualifier ?: "")
        if (creator != null) {
            dependencies[key] = creator
        } else if (!klass.isAbstract) {
            if (klass.hasAnnotation<Injectable>() || klass.hasAnnotation<Factory>()) {
                dependencies[key] = { createInstance(klass, qualifier) }
            } else {
                throw IllegalArgumentException("Class ${klass.simpleName} is not annotated with @Injectable or @Factory and no creator function provided")
            }
        }

        if (klass.hasAnnotation<Factory>()) {
            registerFactoryMethods(klass)
        }
    }

    inline fun <reified T : Any> resolve(qualifier: String? = null): T {
        return resolve(T::class, qualifier)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(klass: KClass<T>, qualifier: String? = null): T {
        val key = DependencyKey(klass, qualifier ?: "")

        if (klass.hasAnnotation<Singleton>()) {
            return singleton.getOrPut(key) { createInstance(klass, qualifier) } as T
        }

        val creator = dependencies[key] ?: findCompatibleDependency(klass, qualifier)
        ?: if (!klass.isAbstract && (klass.hasAnnotation<Injectable>() || klass.hasAnnotation<Factory>())) {
            { createInstance(klass, qualifier) }
        } else {
            throw IllegalArgumentException("No dependency found for ${klass.simpleName} with qualifier $qualifier")
        }

        val instance = creator() as T
        return instance
    }

    private fun <T : Any> registerFactoryMethods(factoryClass: KClass<T>) {
        val factoryInstance = createInstance(factoryClass)
        factoryClass.memberFunctions
            .filter { it.hasAnnotation<Produce>() }
            .forEach { method ->
                val returnType = method.returnType.classifier as? KClass<*>
                    ?: throw IllegalStateException("Cannot determine return type for factory method ${method.name}")
                val qualifier = method.findAnnotation<Qualifier>()?.value
                val key = DependencyKey(returnType, qualifier)
                dependencies[key] = {
                    val params = method.parameters.drop(1).map { param ->
                        val paramQualifier = param.findAnnotation<Qualifier>()?.value ?: qualifier
                        resolve(param.type.classifier as KClass<*>, paramQualifier)
                    }.toTypedArray()
                    method.call(factoryInstance, *params)!!
                }
            }
    }

    private fun <T : Any> findCompatibleDependency(klass: KClass<T>, qualifier: String?): (() -> Any)? {
        return dependencies.entries
            .firstOrNull { (key, _) ->
                key.klass.isSubclassOf(klass) && (qualifier == null || key.qualifier == qualifier)
            }
            ?.value
    }

    private fun <T : Any> createInstance(klass: KClass<T>, qualifier: String? = null): T {
        val constructor = klass.primaryConstructor
            ?: throw IllegalArgumentException("No primary constructor found for class ${klass.simpleName}")

        val params = constructor.parameters.map { param ->
            val paramQualifier = param.findAnnotation<Qualifier>()?.value ?: qualifier
            resolve(param.type.classifier as KClass<*>, paramQualifier)
        }.toTypedArray()

        return constructor.call(*params)
    }

    fun <T : Any> registerProvider(qualifier: String? = null, provider: () -> T) {
        val key = DependencyKey(Provider::class, qualifier)
        dependencies[key] = { object : Provider<T> {
            override fun get(): T = provider()
        }}
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> resolveProvider(qualifier: String? = null): Provider<T> {
        return resolve(Provider::class, qualifier) as Provider<T>
    }

    data class DependencyKey(val klass: KClass<*>, val qualifier: String?)
}

@Injectable
class RandomNumberGenerator {
    fun generate(): Int = Random.nextInt(100)
}

@Injectable
class NumberPrinter(private val numberProvider: Provider<Int>) {
    fun printNumber() {
        println("Generated number: ${numberProvider.get()}")
    }
}

@Injectable
class StatisticsCollector(private val numberProvider: Provider<Int>) {
    private val numbers = mutableListOf<Int>()

    fun collectNumber() {
        numbers.add(numberProvider.get())
    }

    fun getAverage(): Double {
        return if (numbers.isEmpty()) {
            0.0
        } else {
            numbers.average()
        }
    }
}

fun main() {
    val container = DIContainer()
    container.register<RandomNumberGenerator>()
    container.registerProvider { container.resolve<RandomNumberGenerator>().generate() }

    val numberPrinter = container.resolve<NumberPrinter>()
    println(numberPrinter.printNumber())
}