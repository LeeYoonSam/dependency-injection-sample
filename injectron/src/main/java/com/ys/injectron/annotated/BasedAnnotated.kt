package com.ys.injectron.annotated

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * 어노테이션:
 *
 * @Injectable: 의존성 주입이 가능한 클래스를 표시합니다.
 * @Inject: 필드 주입이 필요한 프로퍼티를 표시합니다.
 * @Singleton: 싱글톤으로 관리될 클래스를 표시합니다.
 *
 *
 * DIContainer 클래스:
 *
 * dependencies: 일반 의존성을 저장하는 맵입니다.
 * singletons: 싱글톤 인스턴스를 저장하는 맵입니다.
 * register: 의존성을 수동으로 등록하는 메서드입니다.
 * resolve: 의존성을 해결하는 메서드입니다. @Singleton 어노테이션이 있으면 싱글톤으로 처리합니다.
 * createInstance: 실제 인스턴스를 생성하고 의존성을 주입하는 메서드입니다. 생성자 주입과 필드 주입을 모두 지원합니다.
 *
 *
 * 예제 사용:
 *
 * UserRepository는 @Singleton으로 표시되어 항상 같은 인스턴스가 사용됩니다.
 * UserService는 생성자를 통해 UserRepository를 주입받고, @Inject 어노테이션을 통해 Logger를 필드 주입받습니다.
 *
 *
 *
 * 이 구현의 장점은 다음과 같습니다:
 *
 * 어노테이션을 통해 의존성 관계를 명확하게 표현할 수 있습니다.
 * 생성자 주입과 필드 주입을 모두 지원하여 유연성을 제공합니다.
 * @Singleton 어노테이션을 통해 쉽게 싱글톤을 관리할 수 있습니다.
 * 코드의 가독성이 향상되고, 의존성 관리가 더욱 직관적이 됩니다.
 */

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Injectable

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Singleton

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
            return singletons.getOrPut(klass) { createInstance(klass) } as T
        }

        return createInstance(klass)
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
            if (property.findAnnotation<Inject>() != null) {
                property.isAccessible = true
                if (property is KMutableProperty<*>) {
                    val value = resolve(property.returnType.classifier as KClass<*>)
                    try {
                        property.setter.call(instance, value)
                    } catch (e: Exception) {
                        throw IllegalStateException("Cannot inject property ${property.name} in ${klass.simpleName}", e)
                    }
                } else {
                    throw IllegalStateException("Cannot inject non-mutable property ${property.name} in ${klass.simpleName}")
                }
            }
        }

        return instance
    }
}

// Example usage
@Injectable
@Singleton
class UserRepository {
    fun getUser(id: Int): String = "User $id"
}

@Injectable
class UserService(private val userRepository: UserRepository) {
    @Inject
    lateinit var logger: Logger

    fun getUserInfo(id: Int): String {
        logger.log("Getting user info for id: $id")
        return userRepository.getUser(id)
    }
}

@Injectable
class Logger {
    fun log(message: String) {
        println("LOG: $message")
    }
}

fun main() {
    val container = DIContainer()
    container.register { Logger() }

    val userService = container.resolve<UserService>()
    println(userService.getUserInfo(1))
}