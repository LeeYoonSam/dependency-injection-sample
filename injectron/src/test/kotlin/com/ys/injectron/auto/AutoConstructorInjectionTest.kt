package com.ys.injectron.auto

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows

class AutoConstructorInjectionTest {
    lateinit var container: DIContainer

    @BeforeEach
    fun setUp() {
        container = DIContainer()
    }

    @Test
    @DisplayName("자동 생성자 주입이 작동해야 함")
    fun autoConstructorInjection() {
        container.register { Database() }

        val userService = container.resolve<UserService>()
        assertNotNull(userService)
        assertTrue(userService.getUser(1).startsWith("User"))
    }

    @Test
    @DisplayName("Injectable 애노테이션이 없는 클래스는 예외를 발생시켜야 함")
    fun exceptionNoAnnotation() {
        class NonInjectableClass

        assertThrows(IllegalArgumentException::class.java) {
            container.resolve<NonInjectableClass>()
        }
    }

    @Test
    @DisplayName("등록되지 않은 의존성은 예외를 발생시켜야 함")
    fun exceptionNotRegisteredDependency () {
        @Injectable
        class ServiceWithUnregisteredDependency(val someUnregisteredDependency: String)

        assertThrows(IllegalArgumentException::class.java) {
            container.resolve<ServiceWithUnregisteredDependency>()
        }
    }

    @Test
    @DisplayName("명시적 등록과 자동 주입이 함께 작동해야 함")
    fun explicitRegisterAndAutoInjectionWorksCorrectly() {
        container.register { Database() }
        container.register { UserRepository(container.resolve()) }

        val userService = container.resolve<UserService>()
        assertNotNull(userService)
        assertTrue(userService.getUser(1).startsWith("User"))
    }
}