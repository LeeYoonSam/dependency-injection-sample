package com.ys.injectron.scope

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SingletonScopeTest {
    private lateinit var container: DIContainer

    @BeforeEach
    fun setUp() {
        container = DIContainer()
    }

    @Test
    @DisplayName("기본 등록 및 해결 기능이 정상 작동해야 함")
    fun registerAndResolve() {
        container.register<UserDao> { SimpleUserDao() }
        val userDao = container.resolve<UserDao>()
        assertTrue(userDao is SimpleUserDao)
    }

    @Test
    @DisplayName("싱글톤 스코프는 항상 같은 인스턴스를 반환해야 함")
    fun singletonScopeReturnsSameInstance() {
        container.registerSingleton<Logger> { ConsoleLogger() }
        val logger1 = container.resolve<Logger>()
        val logger2 = container.resolve<Logger>()
        assertEquals(logger1, logger2)
    }

    @Test
    @DisplayName("비싱글톤 스코프는 매번 새로운 인스턴스를 생성해야 함")
    fun nonSingletonScopeCreatesNewInstances() {
        container.register<UserDao> { SimpleUserDao() }
        val userDao1 = container.resolve<UserDao>()
        val userDao2 = container.resolve<UserDao>()
        assertNotEquals(userDao1, userDao2)
    }

    @Test
    @DisplayName("의존성 주입이 올바르게 작동해야 함")
    fun dependencyInjectionWorks() {
        container.registerSingleton<Logger> { ConsoleLogger() }
        container.register<UserDao> { SimpleUserDao() }
        container.register<UserService> { UserServiceImpl(container.resolve(), container.resolve()) }

        val userService = container.resolve<UserService>()
        assertTrue(userService is UserServiceImpl)
    }

    @Test
    @DisplayName("등록되지 않은 의존성 해결 시 예외가 발생해야 함")
    fun throwsExceptionForUnregisteredDependency() {
        assertThrows(Exception::class.java) {
            container.resolve<UserService>()
        }
    }
}