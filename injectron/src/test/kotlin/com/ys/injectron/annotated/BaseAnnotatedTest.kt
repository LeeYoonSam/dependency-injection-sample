package com.ys.injectron.annotated

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 어노테이션 기반 의존성 주입을 구현함으로써 얻을 수 있는 이점은 다음과 같습니다:
 *
 * 코드의 가독성 향상: 의존성 관계가 명확하게 표현됩니다.
 * 유연성: 생성자 주입과 필드 주입을 모두 지원합니다.
 * 간편한 싱글톤 관리: @Singleton 어노테이션으로 쉽게 싱글톤을 지정할 수 있습니다.
 * 선언적 프로그래밍: 의존성 관계를 선언적으로 정의할 수 있습니다.
 * 테스트 용이성: 의존성을 쉽게 모의(mock) 객체로 대체할 수 있어 단위 테스트가 용이합니다.
 */

class BaseAnnotatedTest {
    private lateinit var container: DIContainer

    @BeforeEach
    fun setUp() {
        container = DIContainer()
    }

    @Test
    @DisplayName("생성자를 통한 의존성 주입이 올바르게 작동하는지 확인")
    fun testConstructorInjection() {
        val userService = container.resolve<UserService>()
        assertNotNull(userService)
        assertTrue(userService.getUserInfo(1).startsWith("User"))
    }

    @Test
    @DisplayName("@Inject 어노테이션을 사용한 필드 주입이 올바르게 작동하는지 확인")
    fun testFieldInjection() {
        container.register<Logger> { Logger() }
        val userService = container.resolve<UserService>()
        assertNotNull(userService.logger)
    }

    @Test
    @DisplayName("@Singleton 어노테이션이 붙은 클래스가 실제로 싱글톤으로 동작하는지 확인")
    fun testSingleton() {
        val repo1 = container.resolve<UserRepository>()
        val repo2 = container.resolve<UserRepository>()

        assertSame(repo1, repo2)
    }

    @Test
    @DisplayName("@Singleton 어노테이션이 없는 클래스는 매번 새로운 인스턴스를 생성하는지 확인")
    fun testNonSingleton() {
        val service1 = container.resolve<UserService>()
        val service2 = container.resolve<UserService>()

        assertNotSame(service1, service2)
    }

    @Test
    @DisplayName("@Injectable 어노테이션이 없는 클래스를 해결하려 할 때 적절한 예외가 발생하는지 확인")
    fun testExceptionForNonInjectableClass() {
        assertThrows(IllegalArgumentException::class.java) {
            container.resolve<NonInjectableClass>()
        }
    }

    @Test
    @DisplayName("register 메서드를 통한 수동 등록이 올바르게 작동하는지 확인")
    fun testManualRegistration() {
        val sayHello = "Hello, World!"

        container.register<String> { sayHello }
        assertEquals(sayHello, container.resolve<String>())
    }

    @Test
    @DisplayName("복잡한 의존성 그래프에서도 DIContainer가 올바르게 작동하는지 확인")
    fun testComplexDependencyGraph() {
        val service = container.resolve<ComplexService>()
        assertNotNull(service)
        assertNotNull(service.repository)
        assertNotNull(service.logger)
        assertNotNull(service.helper)
        assertNotNull(service.helper.logger)
    }
}

class NonInjectableClass

@Injectable
class ComplexService(val repository: UserRepository) {
    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var helper: Helper
}

@Injectable
class Helper {
    @Inject
    lateinit var logger: Logger
}
