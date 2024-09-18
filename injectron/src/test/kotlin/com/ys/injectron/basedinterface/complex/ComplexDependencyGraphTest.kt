package com.ys.injectron.basedinterface.complex

import com.ys.injectron.basedinterface.DIContainer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ComplexDependencyGraphTest {

    private lateinit var container: DIContainer

    @BeforeEach
    fun setup() {
        container = DIContainer()
    }

    /**
     * 복잡한 의존성 그래프가 제대로 해결되는지 확인합니다.
     */
    @Test
    fun `resolve complex dependency graph`() {
        // Register dependencies
        container.register<Logger> { ConsoleLogger() }
        container.register<UserDao> { SimpleUserDao() }
        container.register<OrderDao> { SimpleOrderDao() }
        container.register<EmailService> { SimpleEmailService() }
        container.register<UserService> { UserServiceImpl(container.resolve(), container.resolve()) }
        container.register<OrderService> { OrderServiceImpl(container.resolve(), container.resolve(), container.resolve(), container.resolve()) }

        // Resolve OrderService and check if it's the correct type
        val orderService = container.resolve<OrderService>()
        assertTrue(orderService is OrderServiceImpl)
    }

    /**
     * OrderService가 모든 의존성을 올바르게 사용하는지 검증합니다.
     */
    @Test
    fun `order service uses all dependencies correctly`() {
        // Create mock objects
        val mockLogger = mockk<Logger>(relaxed = true)
        val mockUserDao = mockk<UserDao>()
        val mockOrderDao = mockk<OrderDao>(relaxed = true)
        val mockEmailService = mockk<EmailService>(relaxed = true)

        // Set up behavior for mockUserDao
        every { mockUserDao.getUser(any()) } returns "TestUser"

        // Register dependencies with mocks
        container.register<Logger> { mockLogger }
        container.register<UserDao> { mockUserDao }
        container.register<OrderDao> { mockOrderDao }
        container.register<EmailService> { mockEmailService }
        container.register<UserService> { UserServiceImpl(container.resolve(), container.resolve()) }
        container.register<OrderService> { OrderServiceImpl(container.resolve(), container.resolve(), container.resolve(), container.resolve()) }

        // Resolve and use OrderService
        val orderService = container.resolve<OrderService>()
        orderService.placeOrder(1, "TestProduct")

        // Verify that all dependencies were used correctly
        verify {
            mockLogger.log(any())
            mockUserDao.getUser(1)
            mockOrderDao.createOrder(1, "TestProduct")
            mockEmailService.sendEmail("TestUser", any())
        }
    }

    /**
     * UserService가 자신의 의존성을 올바르게 사용하는지 확인합니다.
     */
    @Test
    fun `user service uses its dependencies correctly`() {
        // Create mock objects
        val mockLogger = mockk<Logger>(relaxed = true)
        val mockUserDao = mockk<UserDao>()

        // Set up behavior for mockUserDao
        every { mockUserDao.getUser(any()) } returns "TestUser"

        // Register dependencies with mocks
        container.register<Logger> { mockLogger }
        container.register<UserDao> { mockUserDao }
        container.register<UserService> { UserServiceImpl(container.resolve(), container.resolve()) }

        // Resolve and use UserService
        val userService = container.resolve<UserService>()
        val user = userService.getUser(1)

        // Verify that all dependencies were used correctly
        assertEquals("TestUser", user)
        verify {
            mockLogger.log(any())
            mockUserDao.getUser(1)
        }
    }
}
