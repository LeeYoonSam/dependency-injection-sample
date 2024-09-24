package com.ys.injectron.lazy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class LazyInjectionTest {
    lateinit var container: DIContainer

    @BeforeEach
    fun setUp() {
        container = DIContainer()
    }

    @Test
    @DisplayName("LazyDependency 초기화 검증")
    fun lazyInjection() {
        var lazyDependencyInitialized = false
        container.register<LazyDependency> {
            LazyDependency().also { lazyDependencyInitialized = true }
        }

        val service = container.resolve<ServiceWithLazyDependency>()
        assertFalse(lazyDependencyInitialized, "LazyDependency should not be initialized yet")

        service.useDependency()
        assertTrue(lazyDependencyInitialized, "LazyDependency should be initialized after use")
    }
}