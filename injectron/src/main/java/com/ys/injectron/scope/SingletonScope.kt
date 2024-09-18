package com.ys.injectron.scope

import kotlin.reflect.KClass

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

// Implementations (기존과 동일)
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

    inline fun <reified T : Any> resolve(): T = resolve(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> resolve(klass: KClass<T>): T {
        val creator = dependencies[klass] ?: throw Exception("No dependency found for ${klass.simpleName}")
        return creator() as T
    }
}

fun main() {
    val container = DIContainer()

    // Register dependencies
    container.registerSingleton<Logger> { ConsoleLogger() }
    container.register<UserDao> { SimpleUserDao() }
    container.register<OrderDao> { SimpleOrderDao() }
    container.register<EmailService> { SimpleEmailService() }
    container.register<UserService> { UserServiceImpl(container.resolve(), container.resolve()) }
    container.register<OrderService> { OrderServiceImpl(container.resolve(), container.resolve(), container.resolve(), container.resolve()) }

    // Use the service
    val orderService = container.resolve<OrderService>()
    orderService.placeOrder(1, "Laptop")

    // Demonstrate singleton behavior
    println("Logger instances are the same: ${container.resolve<Logger>() === container.resolve<Logger>()}")
    println("UserDao instances are different: ${container.resolve<UserDao>() !== container.resolve<UserDao>()}")
}