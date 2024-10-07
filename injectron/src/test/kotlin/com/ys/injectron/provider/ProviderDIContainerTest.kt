package com.ys.injectron.provider

import org.junit.jupiter.api.Assertions.assertTrue
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
    @DisplayName("프로바이더를 통한 동적 값 생성 테스트")
    fun `test dynamic value generation through provider`() {
        // 기본 의존성 등록
        container.register<RandomNumberGenerator>()

        // 프로바이더 등록
        container.registerProvider {
            container.resolve<RandomNumberGenerator>().generate()
        }

        // NumberPrinter 의존성 해결
        val numberPrinter = container.resolve<NumberPrinter>()

        // 여러 번 호출하여 다른 숫자가 생성되는지 확인
        val numbers = mutableSetOf<Int>()
        repeat(5) {
            val provider = container.resolveProvider<Int>()
            numbers.add(provider.get())
        }

        assertTrue(numbers.size > 1) {
            "Provider should generate different numbers"
        }
    }

    @Test
    @DisplayName("프로바이더를 사용하는 여러 클래스의 독립성 테스트")
    fun `test independence of classes using the same provider`() {
        // 기본 의존성 등록
        container.register<RandomNumberGenerator>()
        container.registerProvider {
            container.resolve<RandomNumberGenerator>().generate()
        }

        // 두 개의 다른 클래스 인스턴스 생성
        val printer = container.resolve<NumberPrinter>()
        val collector = container.resolve<StatisticsCollector>()

        // 각각의 클래스가 독립적으로 프로바이더를 사용하는지 확인
        repeat(5) {
            collector.collectNumber()
            printer.printNumber()
        }

        assertTrue(collector.getAverage() in 0.0..100.0) {
            "Average should be between 0 and 100"
        }
    }
}