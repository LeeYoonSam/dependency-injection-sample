package com.ys.injectron.provider

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ProviderDIContainerTest {
    lateinit var container: DIContainer

    @BeforeEach
    fun setUp() {
        container = DIContainer()
    }

    @Test
    @DisplayName("프로바이더 패턴이 올바르게 동작하는지 확인")
    fun `test provider pattern`() {
        container.register<RandomNumberGenerator>()
        container.registerProvider { container.resolve<RandomNumberGenerator>().generate() }

        val numberPrinter = container.resolve<NumberPrinter>()

        val numbers = mutableSetOf<Int>()
        repeat(10) {
            numberPrinter.printNumber()
            numbers.add(container.resolveProvider<Int>().get())
        }

        assertTrue(numbers.size > 1, "프로바이더가 매번 새로운 숫자를 생성해야 합니다.")
    }
}