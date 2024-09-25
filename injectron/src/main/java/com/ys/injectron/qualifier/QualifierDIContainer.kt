package com.ys.injectron.qualifier

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Injectable

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class LazyInject

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Singleton

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Qualifier(val value: String = "")

// Lazy wrapper class
class Lazy<T : Any>(private val initializer: () -> T) {
    private var instance: T? = null

    fun get(): T {
        if (instance == null) {
            instance = initializer()
        }

        return instance!!
    }
}

class DIContainer {
    private val dependencies: MutableMap<Pair<KClass<*>, String?>, () -> Any> = mutableMapOf()
    private val singletons: MutableMap<Pair<KClass<*>, String?>, Any> = mutableMapOf()

    inline fun <reified T : Any> register(qualifier: String? = null, noinline creator: () -> T) {
        register(T::class, qualifier, creator)
    }

    fun <T : Any> register(klass: KClass<T>, qualifier: String? = null, creator: () -> T) {
        dependencies[klass to qualifier] = creator
    }

    inline fun <reified T : Any> resolve(qualifier: String? = null): T {
        return resolve(T::class, qualifier)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(klass: KClass<T>, qualifier: String? = null): T {
        val key = klass to qualifier
        dependencies[key]?.let { creator ->
            return creator() as T
        }

        // 인터페이스나 추상 클래스인 경우 예외 처리
        if (klass.java.isInterface || klass.isAbstract) {
            throw IllegalArgumentException("No concrete implementation registered for ${klass.simpleName} with qualifier $qualifier")
        }

        if (klass.hasAnnotation<Singleton>()) {
            return singletons.getOrPut(key) { createInstance(klass, qualifier) } as T
        }

        return createInstance(klass, qualifier)
    }

    private fun <T : Any> createInstance(klass: KClass<T>, qualifier: String?): T {
        if (!klass.hasAnnotation<Injectable>()) {
            throw IllegalArgumentException("Class ${klass.simpleName} is not annotated with @Injectable")
        }

        val constructor = klass.constructors.firstOrNull()
            ?: throw IllegalArgumentException("No constructor found for ${klass.simpleName}")

        val params = constructor.parameters.map { param ->
            val paramQualifier = param.annotations.filterIsInstance<Qualifier>().firstOrNull()?.value
            resolve(param.type.classifier as KClass<*>, paramQualifier ?: qualifier)
        }.toTypedArray()

        val instance = constructor.call(*params)

        klass.memberProperties.forEach { property ->
            when {
                property.findAnnotation<Inject>() != null -> {
                    val propQualifier = property.annotations.filterIsInstance<Qualifier>().firstOrNull()?.value ?: qualifier
                    injectProperty(property, instance, propQualifier)
                }
                property.findAnnotation<LazyInject>() != null -> {
                    val propQualifier = property.annotations.filterIsInstance<Qualifier>().firstOrNull()?.value ?: qualifier
                    injectLazyProperty(property, instance, propQualifier)
                }
            }
        }

        return instance
    }

    private fun injectProperty(property: KProperty1<*, *>, instance: Any, qualifier: String?) {
        property.isAccessible = true
        if (property is KMutableProperty<*>) {
            val value = resolve(property.returnType.classifier as KClass<*>, qualifier)
            try {
                property.setter.call(instance, value)
            } catch (e: Exception) {
                throw IllegalStateException("Cannot inject property ${property.name}", e)
            }
        } else {
            throw IllegalStateException("Cannot inject non-mutable property ${property.name}")
        }
    }

    private fun injectLazyProperty(property: KProperty1<*, *>, instance: Any, qualifier: String?) {
        property.isAccessible = true
        if (property is KMutableProperty<*>) {
            val lazyValue = Lazy { resolve(property.returnType.arguments[0].type!!.classifier as KClass<*>, qualifier) }
            try {
                property.setter.call(instance, lazyValue)
            } catch (e: Exception) {
                throw IllegalStateException("Cannot inject lazy property ${property.name}", e)
            }
        } else {
            throw IllegalStateException("Cannot inject non-mutable lazy property ${property.name}")
        }
    }
}

interface MessageService {
    fun getMessage(): String
}

@Injectable
@Qualifier("email")
class EmailService : MessageService {
    override fun getMessage(): String = "Email message"
}

@Injectable
@Qualifier("sms")
class SmsService : MessageService {
    override fun getMessage(): String = "SMS message"
}

@Injectable
class NotificationService(
    @Qualifier("email") private val emailService: EmailService,
    @Qualifier("sms") private val smsService: SmsService,
) {
    fun sendNotification(useEmail: Boolean): String {
        return if (useEmail) emailService.getMessage() else smsService.getMessage()
    }
}

fun main() {
    val container = DIContainer()

    val notificationService = container.resolve<NotificationService>()

    println("Email notification: ${notificationService.sendNotification(true)}")
    println("SMS notification: ${notificationService.sendNotification(false)}")

    container.register<MessageService>("email") { EmailService() }
    container.register<MessageService>("sms") { SmsService() }

    val emailService = container.resolve<MessageService>("email")
    val smsService = container.resolve<MessageService>("sms")

    println("Direct email service: ${emailService.getMessage()}")
    println("Direct SMS service: ${smsService.getMessage()}")
}