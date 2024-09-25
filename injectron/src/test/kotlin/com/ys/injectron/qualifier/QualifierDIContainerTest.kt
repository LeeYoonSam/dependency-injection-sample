package com.ys.injectron.qualifier

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class QualifierDIContainerTest {
    private lateinit var container: DIContainer

    @BeforeEach
    fun setUp() {
        container = DIContainer()
    }

    @Test
    @DisplayName("Qualifier 를 사용한 의존성 주입이 올바르게 동작하는지 확인")
    fun testQualifierInjection() {
        container.register<MessageService>("email") { EmailService() }
        container.register<MessageService>("sms") { SmsService() }

        val notificationService = container.resolve<NotificationService>()

        assertEquals("Email message", notificationService.sendNotification(true))
        assertEquals("SMS message", notificationService.sendNotification(false))
    }

    @Test
    @DisplayName("Qualifier 를 사용하여 특정 구현체를 직접 해결할 수 있는지 확인")
    fun testQualifierResolution() {
        container.register<MessageService>("email") { EmailService() }
        container.register<MessageService>("sms") { SmsService() }

        val emailService = container.resolve<MessageService>("email")
        val smsService = container.resolve<MessageService>("sms")

        assertTrue(emailService is EmailService)
        assertTrue(smsService is SmsService)
    }

    @Test
    @DisplayName("등록되지 않은 Qualifier 를 사용할때 적절한 예외가 발생하는지 확인")
    fun testQualifierMissing() {
        container.register<MessageService>("email") { EmailService() }

        assertThrows<IllegalArgumentException> {
            container.resolve<MessageService>("sms")
        }
    }

    @Test
    @DisplayName("등록되지 않은 인터페이스를 해결하려고 할때 예외가 발생하는지 확인")
    fun testInterfaceResolutionWithoutRegistration() {
        assertThrows<IllegalArgumentException> {
            container.resolve<MessageService>()
        }
    }
}