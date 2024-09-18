package com.ys.injectron.basedinterface

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DIContainerTest {
    private lateinit var container: DIContainer

    @BeforeEach
    fun setUp() {
        container = DIContainer()
    }

    /**
     * 인터페이스 기반 의존성을 등록하고 해결할 수 있는지
     */
    @Test
    fun `register and resolve interface-based dependency`() {
        container.register<Logger> { ConsoleLogger() }

        val logger = container.resolve<Logger>()

        assertTrue(logger is ConsoleLogger)
    }

    /**
     * 등록되지 않은 의존성을 해결하려 할 때 예외가 발생하는지
     */
    @Test
    fun `resolve unregistered dependency throws exception`() {
        assertThrows(Exception::class.java) {
            container.resolve<Logger>()
        }
    }

    /**
     * 다른 구현체로 의존성을 교체할 수 있는지
     */
    @Test
    fun `register and resolve different implementations`() {
        container.register<Logger> { ConsoleLogger() }
        val consoleLogger = container.resolve<Logger>()
        assertTrue(consoleLogger is ConsoleLogger)

        container.register<Logger> { FileLogger() }
        val fileLogger = container.resolve<Logger>()
        assertTrue(fileLogger is FileLogger)
    }

    /**
     * 중첩된 의존성(UserService가 Logger에 의존)을 해결할 수 있는지
     */
    @Test
    fun `resolve nested dependencies`() {
        container.register<Logger> { ConsoleLogger() }
        container.register<UserService> { UserService(container.resolve()) }

        val userService = container.resolve<UserService>()
        assertNotNull(userService)

        // We can't directly access the logger property as it's private, but we can test the behavior
        userService.performAction() // This should not throw an exception
    }
}