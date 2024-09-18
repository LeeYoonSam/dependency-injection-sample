package com.ys.injectron.basedinterface

class DIContainer {
    val dependencies: MutableMap<Class<*>, () -> Any> = mutableMapOf()

    inline fun <reified T: Any> register(noinline creator: () -> T) {
        dependencies[T::class.java] = creator
    }

    inline fun <reified T: Any> resolve(): T {
        return dependencies[T::class.java]?.invoke() as? T
            ?: throw Exception("No dependency found for ${T::class.java}")
    }
}

interface Logger {
    fun log(message: String)
}

class ConsoleLogger : Logger {
    override fun log(message: String) {
        println("Console: $message")
    }
}

class FileLogger : Logger {
    override fun log(message: String) {
        println("File: $message")
    }
}

// Usage example
class UserService(private val logger: Logger) {
    fun performAction() {
        logger.log("User action performed")
    }
}

fun main() {
    val container = DIContainer()

    // Register dependencies
    container.register<Logger> { ConsoleLogger() }
    container.register<UserService> { UserService(container.resolve()) }

    // Resolve and use
    val userService = container.resolve<UserService>()
    userService.performAction()
}
