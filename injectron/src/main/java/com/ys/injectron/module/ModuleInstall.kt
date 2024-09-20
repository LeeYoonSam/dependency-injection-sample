package com.ys.injectron.module

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * 1. @Injectable 어노테이션:
 *    - 자동 주입이 가능한 클래스를 표시합니다.
 * 2. Module 인터페이스:
 *    - 모듈 개념을 구현하기 위한 인터페이스입니다.
 * 3. DIContainer 클래스:
 *    - dependencies: 모든 의존성을 저장하는 맵입니다.
 *    - singletons: 싱글톤 인스턴스를 저장하는 맵입니다.
 *    - register: 일반 의존성을 등록합니다.
 *    - registerSingleton: 싱글톤 의존성을 등록합니다.
 *    - installModules: 모듈을 설치합니다.
 *    - resolve: 의존성을 해결합니다. 등록된 의존성이 없으면 자동 생성자 주입을 시도합니다.
 *    - findInjectableConstructor: @Injectable 어노테이션이 붙은 클래스의 생성자를 찾아 의존성을 자동으로 주입합니다.
 * 4. 예제 사용:
 *    - UserRepository와 UserService는 @Injectable 어노테이션이 붙어있어 자동 주입이 가능합니다.
 *    - UserModule은 UserRepository를 싱글톤으로, UserService를 일반 의존성으로 등록합니다.
 *    - main 함수에서는 컨테이너를 생성하고, 모듈을 설치한 후 UserService를 해결하여 사용합니다.
 */

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Injectable

interface Module {
    fun register(container: DIContainer)
}

class DIContainer {
    private val dependencies: MutableMap<KClass<*>, () -> Any> = mutableMapOf()
    private val singletons: MutableMap<KClass<*>, Any> = mutableMapOf()

    inline fun <reified T : Any> register(noinline creator: () -> T) {
        register(T::class, creator)
    }

    fun <T : Any> register(klass: KClass<T>, creator: () -> T) {
        dependencies[klass] = creator
    }

    inline fun <reified T : Any> registerSingleton(noinline creator: () -> T) {
        registerSingleton(T::class, creator)
    }

    fun <T : Any> registerSingleton(klass: KClass<T>, creator: () -> T) {
        dependencies[klass] = {
            singletons.getOrPut(klass) { creator() }
        }
    }

    fun installModules(vararg modules: Module) {
        modules.forEach { it.register(this) }
    }

    inline fun <reified T : Any> resolve(): T = resolve(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(klass: KClass<T>): T {
        val creator = dependencies[klass] ?: findInjectableConstructor(klass)
        return creator.invoke() as T
    }

    private fun <T: Any> findInjectableConstructor(klass: KClass<T>): () -> T {
        val constructor = klass.primaryConstructor
            ?: throw IllegalArgumentException("Class $klass does not have a primary constructor")

        if (klass.findAnnotation<Injectable>() == null) {
            throw IllegalArgumentException("Class ${klass.simpleName} is not annotated with @Injectable")
        }

        return {
            val params = constructor.parameters.map { param ->
                resolve(param.type.classifier as KClass<*>)
            }.toTypedArray()

            constructor.call(*params)
        }
    }
}

@Injectable
class UserRepository {
    fun getUser(id: Int): String = "User $id"
}

@Injectable
class UserService(private val userRepository: UserRepository) {
    fun getUserInfo(id: Int): String = userRepository.getUser(id)
}

class UserModule : Module {
    override fun register(container: DIContainer) {
        container.registerSingleton { UserRepository() }
        container.register { UserService(container.resolve()) }
    }
}

fun main() {
    val container = DIContainer()
    container.installModules(UserModule())

    val userService = container.resolve<UserService>()
    println(userService.getUserInfo(1))
}