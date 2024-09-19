package com.ys.injectron.auto

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Injectable

class DIContainer {
    private val dependencies: MutableMap<KClass<*>, () -> Any> = mutableMapOf()

    inline fun <reified T : Any> register(noinline creator: () -> T) {
        register(T::class, creator)
    }

    fun <T : Any> register(klass: KClass<T>, creator: () -> T) {
        dependencies[klass] = creator
    }

    inline fun <reified T : Any> resolve(): T = resolve(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(klass: KClass<T>): T {
        val creator = dependencies[klass] ?: findInjectableConstructor(klass)
        return creator.invoke() as T
    }

    private fun <T : Any> findInjectableConstructor(klass: KClass<T>): () -> T {
        val constructor = klass.primaryConstructor
            ?: throw IllegalArgumentException("Class $klass does not have a constructor")

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

// 사용 예제
@Injectable
class Database {
    fun query(sql: String) = println("Executing query: $sql")
}

@Injectable
class UserRepository(private val database: Database) {
    fun getUser(id: Int) = "User$id"
}

@Injectable
class UserService(private val userRepository: UserRepository) {
    fun getUser(id: Int) = userRepository.getUser(id)
}

fun main() {
    val container = DIContainer()

    // Database 클래스만 명시적으로 등록
    container.register { Database() }

    // UserRepository 와 UserService 는 자동으로 생성자 주입
    val userService = container.resolve<UserService>()
    println(userService.getUser(1))
}