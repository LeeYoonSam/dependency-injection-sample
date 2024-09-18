package com.ys.injectron.basedinterface.complex.extended

import com.ys.injectron.basedinterface.DIContainer

data class User(val id: Int, val name: String, val email: String)
data class Product(val id: Int, val name: String, val price: Double, var stock: Int)
data class  Order(val id: Int, val userId: Int, val products: List<OrderItem>, val totalPrice: Double)
data class OrderItem(val productId: Int, val quantity: Int)

interface Logger {
    fun log(message: String)
}

interface UserDao {
    fun getUser(id: Int): User
}

interface OrderDao {
    fun createOrder(order: Order)
    fun getOrder(id: Int): Order
}

interface EmailService {
    fun sendEmail(to: String, subject: String, body: String)
}

interface ProductDao {
    fun getProduct(id: Int): Product
    fun updateStock(id: Int, quantity: Int)
}

interface PaymentService {
    fun processPayment(userId: Int, amount: Double): Boolean
}

interface DiscountService {
    fun applyDiscount(userId: Int, originalPrice: Double): Double
}

class ConsoleLogger : Logger {
    override fun log(message: String) {
        println("LOG: $message")
    }
}

class SimpleUserDao : UserDao {
    private val users = mutableMapOf(
        1 to User(1, "John Doe", "john@example.com"),
        2 to User(2, "Jane Smith", "jane@example.com")
    )

    override fun getUser(id: Int): User {
        return users[id] ?: throw Exception("User not found")
    }
}

class SimpleOrderDao : OrderDao {
    private val orders = mutableMapOf<Int, Order>()
    private var nextId = 1

    override fun createOrder(order: Order) {
        val newOrder = order.copy(id = nextId++)
        orders[newOrder.id] = newOrder
    }

    override fun getOrder(id: Int): Order {
        return orders[id] ?: throw Exception("Order not found")
    }
}

class SimpleProductDao : ProductDao {
    private val products = mutableMapOf(
        1 to Product(1, "Laptop", 1000.0, 10),
        2 to Product(2, "Smartphone", 500.0, 20)
    )

    override fun getProduct(id: Int): Product {
        return products[id] ?: throw Exception("Product not found")
    }

    override fun updateStock(id: Int, quantity: Int) {
        products[id]?.let { it.stock -= quantity } ?: throw Exception("Product not found")
    }
}

class SimpleEmailService : EmailService {
    override fun sendEmail(to: String, subject: String, body: String) {
        println("Email sent to $to: Subject: $subject, Body: $body")
    }
}

class SimplePaymentService : PaymentService {
    override fun processPayment(userId: Int, amount: Double): Boolean {
        println("Processing payment for user $userId: $$amount")
        return true
    }
}

class SimpleDiscountService : DiscountService {
    override fun applyDiscount(userId: Int, originalPrice: Double): Double {
        return originalPrice * 0.9
    }
}

// Main Service
class ShoppingService(
    private val userDao: UserDao,
    private val orderDao: OrderDao,
    private val productDao: ProductDao,
    private val emailService: EmailService,
    private val paymentService: PaymentService,
    private val discountService: DiscountService,
    private val logger: Logger
) {
    fun placeOrder(userId: Int, items: List<OrderItem>): Order {
        logger.log("Placing order for user $userId")

        val user = userDao.getUser(userId)
        var totalPrice = 0.0

        // Check stock and calculate total price
        items.forEach { item ->
            val product = productDao.getProduct(item.productId)
            if (product.stock < item.quantity) {
                throw Exception("Insufficient stock for product ${product.name}")
            }
            totalPrice += product.price * item.quantity
        }

        // Apply discount
        val discountedPrice = discountService.applyDiscount(userId, totalPrice)
        logger.log("Origianl price: $totalPrice, Discounted price: $discountedPrice")

        // Process payment
        if (!paymentService.processPayment(userId, discountedPrice)) {
            throw Exception("Payment failed")
        }

        // Create order
        val order = Order(0, userId, items, discountedPrice)
        orderDao.createOrder(order)

        // Update stock
        items.forEach { item ->
            productDao.updateStock(item.productId, item.quantity)
        }

        // Send confirmation email
        emailService.sendEmail(
            to = user.email,
            subject = "Order Confirmation",
            body = "Your order has been placed. Total: $$discountedPrice"
        )

        logger.log("Order placed successfully for user $userId")
        return order
    }
}

fun main() {
    val container = DIContainer()

    // Register dependencies
    container.register<Logger> { ConsoleLogger() }
    container.register<UserDao> { SimpleUserDao() }
    container.register<OrderDao> { SimpleOrderDao() }
    container.register<ProductDao> { SimpleProductDao() }
    container.register<EmailService> { SimpleEmailService() }
    container.register<PaymentService> { SimplePaymentService() }
    container.register<DiscountService> { SimpleDiscountService() }
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

    // Use the service
    val shoppingService = container.resolve<ShoppingService>()
    val orderItems = listOf(OrderItem(1, 2), OrderItem(2, 1))
    try {
        val order = shoppingService.placeOrder(1, orderItems)
        println("Order placed: $order")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}