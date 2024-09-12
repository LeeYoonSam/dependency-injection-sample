import com.ys.injectron.container.DIContainer
import com.ys.injectron.manually.*
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

class CarIntegrationTest {
    @Test
    fun `car starts with gasoline engine`() {
        val container = DIContainer()
        container.register(Engine::class.java, GasolineEngine())

        val car = Car(container.resolve(Engine::class.java))
        // 여기서는 실제로 출력을 검증하기 어려우므로, 예외가 발생하지 않는지만 확인합니다.
        assertDoesNotThrow { car.start() }
    }

    @Test
    fun `car starts with diesel engine`() {
        val container = DIContainer()
        container.register(Engine::class.java, DieselEngine())

        val car = Car(container.resolve(Engine::class.java))
        assertDoesNotThrow { car.start() }
    }

    @Test
    fun `car starts with electric engine`() {
        val container = DIContainer()
        container.register(Engine::class.java, ElectricEngine())

        val car = Car(container.resolve(Engine::class.java))
        assertDoesNotThrow { car.start() }
    }
}