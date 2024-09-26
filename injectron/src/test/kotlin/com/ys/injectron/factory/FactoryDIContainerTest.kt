package com.ys.injectron.factory

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FactoryDIContainerTest {
    lateinit var container: DIContainer

    @BeforeEach
    fun setup() {
        container = DIContainer()
    }

    @Test
    @DisplayName("기본적인 의존성 주입이 제대로 작동하는지 확인")
    fun `test basic dependency injection`() {
        container.register<AnimalFactory>()

        val zoo = container.resolve<Zoo>()
        assertNotNull(zoo)
        assertEquals("Woof! Meow... (yawn)", zoo.makeAnimalSounds())
    }

    @Test
    @DisplayName("팩토리가 올바른 인스턴스를 생성하는지 확인")
    fun `test factory creates correct instances`() {
        container.register<AnimalFactory>()

        val dog = container.resolve<Animal>()
        val lazyCat = container.resolve<Animal>("lazy")

        assertTrue(dog is Dog)
        assertEquals("Woof!", dog.makeSound())

        assertTrue(lazyCat !is Cat) // It should be an anonymous object
        assertEquals("Meow... (yawn)", lazyCat.makeSound())
    }

    @Test
    @DisplayName("싱글톤 스코프가 제대로 동작하는지 확인")
    fun `test singleton scope`() {
        container.register<AnimalFactory>()

        @Singleton
        @Injectable
        class SingletonService

        val instance1 = container.resolve<SingletonService>()
        val instance2 = container.resolve<SingletonService>()

        assertSame(instance1, instance2)
    }

    @Test
    @DisplayName("Qualifier가 올바르게 작동하는지 확인")
    fun `test qualifier`() {
        container.register<AnimalFactory>()
        container.register<Animal>("normal") { Cat() }
        container.resolve<Animal>("lazy")
        val normalCat = container.resolve<Animal>("normal")
        val lazyCat = container.resolve<Animal>("lazy")

        assertNotEquals(normalCat.makeSound(), lazyCat.makeSound())
    }

    @Test
    @DisplayName("복잡한 의존성 그래프에서도 DI 컨테이너가 제대로 동작하는지 확인")
    fun `test complex dependency graph`() {
        container.register<AnimalFactory>()

        @Injectable
        class AnimalShelter(private val zoo: Zoo, @Qualifier("normal") private val normalCat: Animal) {
            fun describeAnimals() = "In the shelter: ${normalCat.makeSound()}, In the zoo: ${zoo.makeAnimalSounds()}"
        }

        container.register<Animal>("normal") { Cat() }

        val shelter = container.resolve<AnimalShelter>()
        assertEquals("In the shelter: Meow!, In the zoo: Woof! Meow... (yawn)", shelter.describeAnimals())
    }
}