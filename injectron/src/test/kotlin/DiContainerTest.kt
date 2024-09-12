import com.ys.injectron.container.DIContainer
import com.ys.injectron.manually.Car
import com.ys.injectron.manually.ElectricEngine
import com.ys.injectron.manually.Engine
import com.ys.injectron.manually.GasolineEngine
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class DIContainerTest {
    private lateinit var container: DIContainer

    @BeforeEach
    fun setup() {
        container = DIContainer()
    }

    @Test
    fun `register and resolve dependency`() {
        val engine = GasolineEngine()
        container.register(Engine::class.java, engine)

        val resolvedEngine = container.resolve(Engine::class.java)
        assertSame(engine, resolvedEngine)
    }

    @Test
    fun `resolve unregistered dependency throws exception`() {
        assertThrows(Exception::class.java) {
            container.resolve(Engine::class.java)
        }
    }

    @Test
    fun `register and resolve different implementations`() {
        val gasolineEngine = GasolineEngine()
        container.register(Engine::class.java, gasolineEngine)

        val resolvedGasolineEngine = container.resolve(Engine::class.java)
        assertTrue(resolvedGasolineEngine is GasolineEngine)

        val electricEngine = ElectricEngine()
        container.register(Engine::class.java, electricEngine)

        val resolvedElectricEngine = container.resolve(Engine::class.java)
        assertTrue(resolvedElectricEngine is ElectricEngine)
    }

    @Test
    fun `car uses injected engine`() {
        val mockEngine = object : Engine {
            var started = false
            override fun start() {
                started = true
            }
        }

        container.register(Engine::class.java, mockEngine)
        val car = Car(container.resolve(Engine::class.java))

        car.start()
        assertTrue(mockEngine.started)
    }
}