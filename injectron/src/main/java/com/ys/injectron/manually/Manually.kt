package com.ys.injectron.manually

/**
 * 이 예제는 DI의 기본 개념을 보여줍니다.
 *
 * Engine 인터페이스를 정의하고, 이를 구현하는 GasolineEngine과 DieselEngine, ElectricEngine 클래스를 만들었습니다.
 * Car 클래스는 생성자를 통해 Engine을 주입받습니다. 이것이 바로 생성자 주입의 예입니다.
 * main 함수에서 서로 다른 엔진 타입으로 Car 객체를 생성합니다. 이는 수동으로 DI를 수행하는 방식입니다.
 *
 * 이 예제를 실행하면, 가솔린 차와 전기차가 각각 시동을 거는 것을 볼 수 있습니다.
 *
 * 키워드: 생성자 주입
 */
interface Engine {
    fun start()
}

class GasolineEngine: Engine {
    override fun start() {
        println("가솔린 엔진 시동")
    }
}

class DieselEngine : Engine {
    override fun start() {
        println("디젤 엔진 시동")
    }
}

class ElectricEngine : Engine {
    override fun start() {
        println("전기 엔진 시동")
    }
}

class Car(private val engine: Engine) {
    fun start() {
        engine.start()
        println("차량 출발 준비 완료")
    }
}

fun main() {
    val gasolineCar = Car(GasolineEngine())
    gasolineCar.start()

    val dieselCar = Car(DieselEngine())
    dieselCar.start()

    val electricCar = Car(ElectricEngine())
    electricCar.start()
}