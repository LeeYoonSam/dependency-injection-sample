package com.ys.injectron.container

import com.ys.injectron.manually.Car
import com.ys.injectron.manually.DieselEngine
import com.ys.injectron.manually.Engine
import com.ys.injectron.manually.GasolineEngine

/**
 * DI Container를 사용함으로써, 개발자는 비즈니스 로직에 더 집중할 수 있고, 애플리케이션의 구조를 더 유연하고 테스트하기 쉽게 만들 수 있습니다.
 * 특히 큰 규모의 애플리케이션에서 DI Container는 코드의 구조화와 관리를 크게 개선할 수 있습니다.
 *
 * 1. 객체 생성 및 수명 주기 관리:
 *    - 애플리케이션에서 필요한 객체들을 생성하고 관리합니다.
 *    - 싱글톤, 프로토타입 등 다양한 스코프의 객체를 관리할 수 있습니다.
 *
 * 2. 의존성 해결:
 *    - 객체가 필요로 하는 의존성을 자동으로 주입합니다.
 *    - 이를 통해 개발자는 객체 간의 관계를 직접 관리하지 않아도 됩니다.
 *
 * 3. 설정의 중앙화:
 *    - 애플리케이션의 설정을 한 곳에서 관리할 수 있게 합니다.
 *    - 이는 코드의 유지보수성을 높이고 변경을 쉽게 만듭니다.
 *
 * 4. 테스트 용이성 증가:
 *    - 의존성을 쉽게 모의 객체(mock)로 대체할 수 있어 단위 테스트가 용이해집니다.
 *
 * 5. 결합도 감소:
 *    - 객체들 사이의 직접적인 의존성을 줄여 결합도를 낮춥니다.
 *    - 이는 코드의 유연성과 재사용성을 높입니다.
 *
 * 6. 관심사의 분리:
 *    - 객체의 생성과 사용을 분리하여 단일 책임 원칙을 지키는 데 도움을 줍니다.
 */

/**
 * DIContainer 클래스를 추가했습니다. 이 클래스는 의존성을 등록하고 해결하는 역할을 합니다.
 * register 메소드를 사용하여 의존성을 등록합니다.
 * resolve 메소드를 사용하여 등록된 의존성을 가져옵니다.
 * main 함수에서 DIContainer를 사용하여 의존성을 관리하고 Car 객체를 생성합니다.
 *
 * 이 예제는 DI 컨테이너의 기본 개념을 보여줍니다. 실제 Dagger나 Hilt는 이보다 훨씬 복잡하고 강력하지만, 기본 원리는 유사합니다.
 */
class DIContainer {
    private val dependencies = mutableMapOf<Class<*>, Any>()

    fun <T: Any> register(clazz: Class<T>, instance: T) {
        dependencies[clazz] = instance
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> resolve(clazz: Class<T>): T {
        return dependencies[clazz] as? T
            ?: throw Exception("No dependency found for ${clazz.name}")
    }
}

fun main() {
    val container = DIContainer()

    // 의존성 등록
    container.register(Engine::class.java, GasolineEngine())

    // 의존성 해결 및 객체 생성
    val engine = container.resolve(Engine::class.java)
    val car = Car(engine)

    car.start()

    // 의존성 변경
    container.register(Engine::class.java, DieselEngine())

    val newEngine = container.resolve(Engine::class.java)
    val newCar = Car(newEngine)

    newCar.start()
}