package com.ys.injectron.factory

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
annotation class Produces

class DIContainer {
    private val dependencies: MutableMap<DependencyKey, () -> Any> = mutableMapOf()
    private val singletons: MutableMap<DependencyKey, Any> = mutableMapOf()

    inline fun <reified T : Any> register(qualifier: String? = null, noinline creator: (() -> T)? = null) {
        register(T::class, qualifier, creator)
    }

    fun <T : Any> register(klass: KClass<T>, qualifier: String? = null, creator: (() -> T)? = null) {
        val key = DependencyKey(klass, qualifier)
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

    inline fun <reified T : Any> resolve(qualifier: String? = null): T = resolve(T::class, qualifier)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(klass: KClass<T>, qualifier: String? = null): T {
        val key = DependencyKey(klass, qualifier)

        if (klass.hasAnnotation<Singleton>()) {
            return singletons.getOrPut(key) { createInstance(klass, qualifier) } as T
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

    private fun registerFactoryMethods(factoryClass: KClass<*>) {
        val factoryInstance = createInstance(factoryClass, null)
        factoryClass.memberFunctions
            .filter { it.hasAnnotation<Produces>() }
            .forEach { method ->
                val returnType = method.returnType.classifier as? KClass<*>
                    ?: throw IllegalStateException("Cannot determine return type for factory method ${method.name}")
                val qualifier = method.findAnnotation<Qualifier>()?.value
                val key = DependencyKey(returnType, qualifier)
                dependencies[key] = {
                    val params = method.parameters.drop(1).map { param ->
                        val paramQualifier = param.findAnnotation<Qualifier>()?.value
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

    private fun <T : Any> createInstance(klass: KClass<T>, qualifier: String?): T {
        val constructor = klass.primaryConstructor
            ?: throw IllegalArgumentException("No primary constructor found for class ${klass.simpleName}")

        val params = constructor.parameters.map { param ->
            val paramQualifier = param.findAnnotation<Qualifier>()?.value ?: qualifier
            resolve(param.type.classifier as KClass<*>, paramQualifier)
        }.toTypedArray()

        return constructor.call(*params)
    }

    private data class DependencyKey(val klass: KClass<*>, val qualifier: String?)
}

interface Animal {
    fun makeSound(): String
}

@Injectable
class Dog : Animal {
    override fun makeSound(): String = "Woof!"
}

@Injectable
class Cat : Animal {
    override fun makeSound(): String = "Meow!"
}

@Factory
class AnimalFactory {
    @Produces
    fun createDog(): Animal = Dog()

    @Produces
    @Qualifier("lazy")
    fun createLazyCat(): Animal = object : Animal {
        override fun makeSound(): String = "Meow... (yawn)"
    }
}

@Injectable
class Zoo(@Qualifier("lazy") private val lazyCat: Animal, private val dog: Animal) {
    fun makeAnimalSounds() = "${dog.makeSound()} ${lazyCat.makeSound()}"
}

fun main() {
    val container = DIContainer()
    container.register<AnimalFactory>()
    println(container.resolve<Animal>().makeSound())
    println(container.resolve<Animal>("lazy").makeSound())

    val zoo = container.resolve<Zoo>()
    println(zoo.makeAnimalSounds())
}