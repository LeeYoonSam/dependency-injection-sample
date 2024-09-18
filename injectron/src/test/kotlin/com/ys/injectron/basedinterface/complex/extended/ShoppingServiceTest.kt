package com.ys.injectron.basedinterface.complex.extended

import com.ys.injectron.basedinterface.DIContainer
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ShoppingServiceTest {
    private lateinit var container: DIContainer
    private lateinit var mockUserDao: UserDao
    private lateinit var mockOrderDao: OrderDao
    private lateinit var mockProductDao: ProductDao
    private lateinit var mockEmailService: EmailService
    private lateinit var mockPaymentService: PaymentService
    private lateinit var mockDiscountService: DiscountService
    private lateinit var mockLogger: Logger

    @BeforeEach
    fun setUp() {
        container = DIContainer()
        mockUserDao = mockk()
        mockOrderDao = mockk()
        mockProductDao = mockk()
        mockEmailService = mockk(relaxed = true)
        mockPaymentService = mockk()
        mockDiscountService = mockk()
        mockLogger = mockk(relaxed = true)

        container.register<UserDao> { mockUserDao }
        container.register<OrderDao> { mockOrderDao }
        container.register<ProductDao> { mockProductDao }
        container.register<EmailService> { mockEmailService }
        container.register<PaymentService> { mockPaymentService }
        container.register<DiscountService> { mockDiscountService }
        container.register<Logger> { mockLogger }
        container.register<ShoppingService> {
            ShoppingService(
                container.resolve(),
                container.resolve(),
                container.resolve(),
                container.resolve(),
                container.resolve(),
                container.resolve(),
                container.resolve()
            )
        }
    }

    /**
     * 정상적인 주문 처리 과정
     */
    @Test
    fun `placeOrder should process order successfully`() {
        // Arrange
        val userId = 1
        val items = listOf(OrderItem(1, 2), OrderItem(2, 1))
        val user = User(userId, "John Doe", "john@example.com")
        val product1 = Product(1, "Laptop", 1000.0, 10)
        val product2 = Product(2, "Smartphone", 500.0, 20)
        val totalPrice = 2500.0
        val discountedPrice = 2250.0

        every { mockUserDao.getUser(userId) } returns user
        every { mockProductDao.getProduct(1) } returns product1
        every { mockProductDao.getProduct(2) } returns product2
        every { mockDiscountService.applyDiscount(userId, totalPrice) } returns discountedPrice
        every { mockPaymentService.processPayment(userId, discountedPrice) } returns true
        every { mockOrderDao.createOrder(any()) } just Runs
        every { mockProductDao.updateStock(any(), any()) } just Runs

        // Act
        val shoppingService = container.resolve<ShoppingService>()
        val order = shoppingService.placeOrder(userId, items)

        // Assert
        assertEquals(userId, order.userId)
        assertEquals(items, order.products)
        assertEquals(discountedPrice, order.totalPrice)

        verify {
            mockUserDao.getUser(userId)
            mockProductDao.getProduct(1)
            mockProductDao.getProduct(2)
            mockDiscountService.applyDiscount(userId, totalPrice)
            mockPaymentService.processPayment(userId, discountedPrice)
            mockOrderDao.createOrder(any())
            mockProductDao.updateStock(1, 2)
            mockProductDao.updateStock(2, 1)
            mockEmailService.sendEmail(user.email, any(), any())
            mockLogger.log(any())
        }
    }

    /**
     * 재고 부족 시 예외 발생
     */
    @Test
    fun `placeOrder should throw exception when insufficient stock`() {
// Arrange
        val userId = 1
        val items = listOf(OrderItem(1, 11)) // Trying to order more than available stock
        val user = User(userId, "John Doe", "john@example.com")
        val product = Product(1, "Laptop", 1000.0, 10)

        every { mockUserDao.getUser(userId) } returns user
        every { mockProductDao.getProduct(1) } returns product

        // Act & Assert
        val shoppingService = container.resolve<ShoppingService>()
        assertThrows(Exception::class.java) {
            shoppingService.placeOrder(userId, items)
        }

        verify {
            mockUserDao.getUser(userId)
            mockProductDao.getProduct(1)
            mockLogger.log(any())
        }
        verify(exactly = 0) {
            mockDiscountService.applyDiscount(any(), any())
            mockPaymentService.processPayment(any(), any())
            mockOrderDao.createOrder(any())
            mockProductDao.updateStock(any(), any())
            mockEmailService.sendEmail(any(), any(), any())
        }
    }

    /**
     * 결제 실패 시 예외 발생
     */
    @Test
    fun `placeOrder should throw exception when payment fails`() {
        // Arrange
        val userId = 1
        val items = listOf(OrderItem(1, 2))
        val user = User(userId, "John Doe", "john@example.com")
        val product = Product(1, "Laptop", 1000.0, 10)
        val totalPrice = 2000.0
        val discountedPrice = 1800.0

        every { mockUserDao.getUser(userId) } returns user
        every { mockProductDao.getProduct(1) } returns product
        every { mockDiscountService.applyDiscount(userId, totalPrice) } returns discountedPrice
        every { mockPaymentService.processPayment(userId, discountedPrice) } returns false

        // Act & Assert
        val shoppingService = container.resolve<ShoppingService>()
        assertThrows(Exception::class.java) {
            shoppingService.placeOrder(userId, items)
        }

        verify {
            mockUserDao.getUser(userId)
            mockProductDao.getProduct(1)
            mockDiscountService.applyDiscount(userId, totalPrice)
            mockPaymentService.processPayment(userId, discountedPrice)
            mockLogger.log(any())
        }
        verify(exactly = 0) {
            mockOrderDao.createOrder(any())
            mockProductDao.updateStock(any(), any())
            mockEmailService.sendEmail(any(), any(), any())
        }
    }
}