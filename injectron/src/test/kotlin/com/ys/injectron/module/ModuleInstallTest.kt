package com.ys.injectron.module

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ModuleInstallTest {
    private lateinit var container: DIContainer

    @BeforeEach
    fun setUp() {
        container = DIContainer()
    }

    @Test
    @DisplayName("기본적인 의존성 등록과 해결이 제대로 작동")
    fun registerAndResolve() {
        container.register { "Hello" }
        assertEquals("Hello", container.resolve<String>())
    }

    @Test
    @DisplayName("싱글톤 등록이 올바르게 작동")
    fun singletonRegistration() {
        container.registerSingleton { mutableListOf<String>() }

        val list1 = container.resolve<MutableList<String>>()
        val list2 = container.resolve<MutableList<String>>()

        assertSame(list1, list2)
    }

    @Test
    @DisplayName("자동 생성자 주입이 올바르게 작동 - UserService가 UserRepository를 주입받아 생성되는지 검사")
    fun automaticConstructorInjection() {
        container.register { UserRepository() }

        val userService = container.resolve<UserService>()
        assertNotNull(userService)
        assertTrue(userService.getUserInfo(1).startsWith("User"))
    }

    @Test
    @DisplayName("모듈 설치가 올바르게 작동")
    fun moduleInstallation() {
        container.installModules(UserModule())

        val userService = container.resolve<UserService>()
        assertNotNull(userService)
        assertTrue(userService.getUserInfo(1).startsWith("User"))
    }

    @Test
    @DisplayName("등록되지 않은 의존성을 해결하려 할 때 적절한 예외가 발생")
    fun exceptionForUnregisteredDependency() {
        assertThrows<IllegalArgumentException> {
            container.resolve<String>()
        }
    }

    @Test
    @DisplayName("@Injectable 어노테이션이 없는 클래스를 해결하려 할 때 적절한 예외가 발생")
    fun exceptionForNonInjectableClass() {
        assertThrows<IllegalArgumentException> {
            container.resolve<NonInjectableClass>()
        }
    }
}

class NonInjectableClass

/**
 * TestRepository와 TestService를 사용하여 더 복잡한 의존성 관계에서도 DIContainer가 올바르게 작동하는지 확인할 수 있습니다.
 */
@Injectable
class TestRepository

@Injectable
class TestService(private val repository: TestRepository) {
    fun doSomething() = "Service with ${repository::class.simpleName}"
}

class TestModule : Module {
    override fun register(container: DIContainer) {
        container.registerSingleton { TestRepository() }
        container.register { TestService(container.resolve()) }
    }
}