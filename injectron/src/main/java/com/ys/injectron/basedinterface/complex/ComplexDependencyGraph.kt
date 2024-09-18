package com.ys.injectron.basedinterface.complex

import com.ys.injectron.basedinterface.DIContainer

// Interfaces
interface Logger {
    fun log(message: String)
}

interface UserDao {
    fun getUser(id: Int): String
}

interface OrderDao {
    fun createOrder(userId: Int, product: String)
}

interface EmailService {
    fun sendEmail(to: String, message: String)
}

interface UserService {
    fun getUser(id: Int): String
    fun createOrder(userId: Int, product: String)
}

interface OrderService {
    fun placeOrder(userId: Int, product: String)
}

// Implementations
class ConsoleLogger : Logger {
    override fun log(message: String) = println("LOG: $message")
}

class SimpleUserDao : UserDao {
    override fun getUser(id: Int) = "User$id"
}

class SimpleOrderDao : OrderDao {
    override fun createOrder(userId: Int, product: String) {
        println("Order created for User$userId: $product")
    }
}

class SimpleEmailService : EmailService {
    override fun sendEmail(to: String, message: String) {
        println("Email sent to $to: $message")
    }
}

class UserServiceImpl(
    private val userDao: UserDao,
    private val logger: Logger
) : UserService {
    override fun getUser(id: Int): String {
        logger.log("Getting user $id")
        return userDao.getUser(id)
    }

    override fun createOrder(userId: Int, product: String) {
        logger.log("Creating order for user $userId")
        // 실제로는 여기서 OrderService를 호출해야 하지만, 순환 의존성을 피하기 위해 생략합니다.
    }
}

class OrderServiceImpl(
    private val orderDao: OrderDao,
    private val userService: UserService,
    private val emailService: EmailService,
    private val logger: Logger
) : OrderService {
    override fun placeOrder(userId: Int, product: String) {
        logger.log("Placing order for user $userId")
        val user = userService.getUser(userId)
        orderDao.createOrder(userId, product)
        emailService.sendEmail(user, "Your order for $product has been placed.")
    }
}

// Usage
fun main() {
    val container = DIContainer()

    // Register dependencies
    container.register<Logger> { ConsoleLogger() }
    container.register<UserDao> { SimpleUserDao() }
    container.register<OrderDao> { SimpleOrderDao() }
    container.register<EmailService> { SimpleEmailService() }
    container.register<UserService> { UserServiceImpl(container.resolve(), container.resolve()) }
    container.register<OrderService> { OrderServiceImpl(container.resolve(), container.resolve(), container.resolve(), container.resolve()) }

    // Use the services
    val orderService = container.resolve<OrderService>()
    orderService.placeOrder(1, "Laptop")
}