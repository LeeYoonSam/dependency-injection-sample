# Dependency Injection with GPT

## 목차
1. [Dependency Injection(DI)이란?](#1-dependency-injectiondi란)
2. [기본 DI 구현](#2-기본-di-구현)
3. [간단한 DI 컨테이너 만들기](#3-간단한-di-컨테이너-만들기)
4. [DI 컨테이너 기능 확장](#4-di-컨테이너-기능-확장)

## 1. Dependency Injection(DI)란?

Dependency Injection은 객체 지향 프로그래밍에서 객체 간의 의존성을 외부에서 주입하는 디자인 패턴입니다.DI를 사용하면 다음과 같은 이점이 있습니다:

- 코드의 재사용성 향상
- 테스트 용이성 증가
- 결합도 감소 및 유연성 증가

## 2. 기본 DI 구현

1. 인터페이스 정의하기
2. 구현 클래스 만들기
3. 의존성을 주입받는 클래스 만들기
4. 수동으로 의존성 주입하기

## [3. 간단한 DI 컨테이너 만들기](./injectron/src/main/java/com/ys/injectron/container/DIContainer.kt)

1. 의존성을 저장할 컨테이너 클래스 정의
2. 의존성 등록 메서드 구현
3. 의존성 해결 메서드 구현
4. DI 컨테이너를 사용하여 객체 생성 및 조립

### DI Container 역할
DI Container를 사용함으로써, 개발자는 비즈니스 로직에 더 집중할 수 있고, 애플리케이션의 구조를 더 유연하고 테스트하기 쉽게 만들 수 있습니다. 
특히 큰 규모의 애플리케이션에서 DI Container는 코드의 구조화와 관리를 크게 개선할 수 있습니다.

1. 객체 생성 및 수명 주기 관리:
   - 애플리케이션에서 필요한 객체들을 생성하고 관리합니다. 
   - 싱글톤, 프로토타입 등 다양한 스코프의 객체를 관리할 수 있습니다.

2. 의존성 해결:
   - 객체가 필요로 하는 의존성을 자동으로 주입합니다. 
   - 이를 통해 개발자는 객체 간의 관계를 직접 관리하지 않아도 됩니다.

3. 설정의 중앙화:
   - 애플리케이션의 설정을 한 곳에서 관리할 수 있게 합니다. 
   - 이는 코드의 유지보수성을 높이고 변경을 쉽게 만듭니다.
   
4. 테스트 용이성 증가:
   - 의존성을 쉽게 모의 객체(mock)로 대체할 수 있어 단위 테스트가 용이해집니다.

5. 결합도 감소:
   - 객체들 사이의 직접적인 의존성을 줄여 결합도를 낮춥니다.
   - 이는 코드의 유연성과 재사용성을 높입니다.

6. 관심사의 분리:
   - 객체의 생성과 사용을 분리하여 단일 책임 원칙을 지키는 데 도움을 줍니다.

## 4. DI 컨테이너 기능 확장

1. 인터페이스 기반 의존성 등록 구현
2. 복잡한 의존성 그래프 만들기
3. 생성자 주입 자동화
4. 싱글톤 스코프 구현
5. 모듈 개념 도입
6. 어노테이션 기반 의존성 주입 구현

### [1. 인터페이스 기반 의존성 등록 구현](./injectron/src/main/java/com/ys/injectron/basedinterface/BasedInterface.kt)
인터페이스 기반 의존성 등록의 주요 이점은 다음과 같습니다:

1. 유연성 증가: 구현체를 쉽게 교체할 수 있습니다.
2. 테스트 용이성: 목(mock) 객체를 사용한 테스트가 쉬워집니다.
3. 결합도 감소: 구체적인 구현에 의존하지 않아 결합도가 낮아집니다.
 
인터페이스 기반 의존성 등록 방식은 DI의 핵심 개념을 잘 보여줍니다. 
이를 통해 코드의 결합도를 낮추고, 테스트 용이성을 높이며, 더 유연한 설계를 가능하게 합니다.

### [2. 복잡한 의존성 그래프를 만들기](./injectron/src/main/java/com/ys/injectron/basedinterface/complex/ComplexDependencyGraph.kt)
여러 계층의 서비스와 데이터 접근 객체(DAO)를 포함하는 시나리오를 만들어보겠습니다. 
이 예제에서는 사용자 관리와 주문 처리 시스템의 일부를 구현해볼 것입니다.

이 복잡한 의존성 그래프 예제는 실제 애플리케이션과 유사한 구조를 보여줍니다. 
여기서 볼 수 있는 주요 포인트는 다음과 같습니다:

1. 계층 구조: DAO, Service, 그리고 부가 서비스(EmailService, Logger)가 각각의 계층을 형성합니다.
2. 인터페이스 기반 설계: 모든 컴포넌트가 인터페이스를 통해 정의되어 있어, 구현체를 쉽게 교체할 수 있습니다.
3. 의존성 주입: 각 서비스는 생성자를 통해 의존성을 주입받습니다.
4. 중첩된 의존성: OrderService는 UserService에 의존하고, UserService는 다시 UserDao와 Logger에 의존합니다.

이러한 구조는 다음과 같은 이점을 제공합니다:

1. 테스트 용이성: 각 컴포넌트를 독립적으로 테스트할 수 있습니다.
2. 유연성: 구현체를 쉽게 교체할 수 있어 요구사항 변경에 대응하기 쉽습니다.
3. 관심사의 분리: 각 컴포넌트가 자신의 책임에만 집중할 수 있습니다.

### [더 복잡한 비즈니스 로직 적용](./injectron/src/main/java/com/ys/injectron/basedinterface/complex/extended/ShoppingService.kt)
이번에는 온라인 쇼핑몰의 주문 처리 시스템을 확장하여 재고 관리, 결제 처리, 할인 적용 등의 기능을 추가해보겠습니다.

이 확장된 비즈니스 로직과 테스트 코드는 다음과 같은 추가적인 기능과 복잡성을 보여줍니다:

1. 재고 관리: 주문 처리 전 재고를 확인하고, 주문 후 재고를 업데이트합니다.
2. 결제 처리: 실제 결제를 시뮬레이션하는 PaymentService를 도입했습니다.
3. 할인 적용: DiscountService를 통해 주문에 할인을 적용합니다.
4. 이메일 알림: 주문 확인 이메일을 보내는 기능을 추가했습니다.
5. 예외 처리: 재고 부족, 결제 실패 등의 예외 상황을 처리합니다.


### [3. 생성자 주입 자동화](./injectron/src/main/java/com/ys/injectron/auto/AutoConstructorInjection.kt)
생성자 주입 자동화는 의존성 주입을 더 편리하고 효율적으로 만드는 중요한 기능

이 코드는 생성자 주입 자동화를 구현한 DI 컨테이너입니다. 주요 특징과 작동 방식은 다음과 같습니다:

1. @Injectable 애노테이션:
   - 자동 주입이 가능한 클래스를 표시합니다. 
   - 런타임에 검사할 수 있도록 RUNTIME 유지 정책을 사용합니다.
2. findInjectableConstructor 메서드:
   - 클래스의 기본 생성자를 찾습니다. 
   - @Injectable 애노테이션이 있는지 확인합니다. 
   - 생성자의 파라미터를 분석하고, 각 파라미터에 대한 의존성을 재귀적으로 해결합니다.
3. resolve 메서드 개선:
   - 명시적으로 등록된 의존성이 없는 경우, findInjectableConstructor를 호출하여 자동 주입을 시도합니다.
4. 사용 예제:
   - Database는 명시적으로 등록됩니다.
   - UserRepository와 UserService는 자동으로 생성자 주입됩니다.

**이 접근 방식의 장점:**
1. 편의성: 대부분의 의존성을 자동으로 해결할 수 있어 코드가 간결해집니다.
2. 유연성: 필요한 경우 명시적 등록과 자동 주입을 혼합하여 사용할 수 있습니다.
3. 명확성: @Injectable 애노테이션을 통해 주입 가능한 클래스를 명확히 표시합니다.

**주의할 점:**
1. 순환 의존성: 이 구현은 순환 의존성을 감지하지 않으므로, 순환 의존성이 있는 경우 스택 오버플로우가 발생할 수 있습니다.
2. 성능: 리플렉션을 사용하므로 약간의 성능 저하가 있을 수 있습니다.
3. 생성자 제한: 현재 구현은 기본 생성자만을 지원합니다. 여러 생성자가 있는 경우 추가적인 로직이 필요할 수 있습니다.

이 구현을 통해 대부분의 경우 명시적인 의존성 등록 없이도 객체 그래프를 자동으로 구성할 수 있습니다. 그러나 특별한 초기화가 필요하거나 인터페이스의 구현을 선택해야 하는 경우에는 여전히 명시적 등록이 필요합니다.


### [4. 싱글톤 스코프 구현](./injectron/src/main/java/com/ys/injectron/basedinterface/complex/ComplexDependencyGraph.kt)
싱글톤 스코프가 필요한 이유:
1. 메모리 사용 최적화: 객체를 한 번만 생성하여 메모리를 절약합니다.
2. 전역 상태 관리: 애플리케이션 전체에서 동일한 인스턴스를 사용할 수 있습니다.
3. 성능 향상: 객체 생성 비용을 줄여 성능을 개선합니다.

장점:
1. 리소스 효율성: 객체를 재사용하여 시스템 리소스를 절약합니다.
2. 일관성: 항상 동일한 인스턴스를 사용하여 상태 일관성을 유지합니다.
3. 스레드 안전: Lock을 사용하여 멀티스레드 환경에서 안전합니다.

단점:
1. 테스트 어려움: 전역 상태로 인해 단위 테스트가 복잡해질 수 있습니다.
2. 결합도 증가: 싱글톤에 의존하는 코드는 결합도가 높아질 수 있습니다.
3. 유연성 감소: 항상 같은 인스턴스를 사용하므로 다양한 구현을 사용하기 어려울 수 있습니다.

이 구현의 주요 특징과 변경사항은 다음과 같습니다:

1. `DIContainer` 클래스에 싱글톤 스코프를 추가했습니다.
2. `registerSingleton` 메소드를 추가하여 싱글톤으로 등록할 수 있게 했습니다.
3. `resolve` 메소드에서 싱글톤 인스턴스를 처리하도록 수정했습니다.
4. `KClass`를 사용하여 타입 안전성을 개선했습니다.

이 구현의 장단점:

장점:
1. 유연성: 싱글톤과 일반 인스턴스를 모두 지원합니다.
2. 타입 안전성: `KClass`와 제네릭을 사용하여 타입 안전성을 개선했습니다.
3. 사용 편의성: 인라인 함수와 리플렉션을 활용하여 사용하기 쉽게 만들었습니다.
4. 코드 재사용: 기존 코드 구조를 유지하면서 기능을 확장했습니다.

단점:
1. 복잡성 증가: 싱글톤 지원으로 인해 코드가 약간 복잡해졌습니다.
2. 런타임 오류 가능성: 의존성 누락 시 런타임에 오류가 발생할 수 있습니다.
3. 순환 의존성 처리: 현재 구현은 순환 의존성을 자동으로 처리하지 않습니다.
4. 테스트 복잡성: 싱글톤 사용으로 인해 일부 테스트 시나리오가 복잡해질 수 있습니다.

이 구현은 싱글톤 스코프를 통해 성능을 최적화하고 있습니다. 필요한 경우 객체를 재사용하여 메모리 사용을 줄이고, 객체 생성 비용을 감소시킵니다.
테스트 코드를 통해 이 구현이 의도한 대로 작동하는지, 싱글톤과 일반 인스턴스가 올바르게 관리되는지 확인할 수 있습니다.

**일반적으로 싱글톤과 비싱글톤을 선택하는 기준**
싱글톤으로 적합한 경우:
- 상태가 없는(stateless) 서비스: 내부 상태를 가지지 않고 주로 기능만 제공하는 서비스. 예: 로깅 서비스, 유틸리티 클래스 
- 공유 리소스 관리: 데이터베이스 연결 풀, 스레드 풀 등 비용이 큰 리소스를 관리하는 객체. 
- 설정 정보: 애플리케이션 전체에서 공유되는 설정 정보를 관리하는 객체. 
- 캐시: 애플리케이션 전체에서 공유되는 캐시 객체. 
- 팩토리 객체: 다른 객체를 생성하는 팩토리 클래스.

비싱글톤(프로토타입)으로 적합한 경우:
- 상태를 가진(stateful) 객체: 각 인스턴스마다 고유한 상태를 유지해야 하는 객체. 예: 사용자 세션, 장바구니 
- 스레드 안전성이 필요한 경우: 멀티스레드 환경에서 각 스레드가 독립적인 인스턴스를 사용해야 할 때. 
- 성능 고려: 객체 생성 비용이 적고, 메모리 사용량이 크지 않은 경우. 
- 테스트 용이성: 각 테스트 케이스마다 독립적인 인스턴스가 필요한 경우.

### [5. 모듈 개념 도입](./injectron/src/main/java/com/ys/injectron/module/ModuleInstall.kt)
1. @Injectable 어노테이션:
   - 자동 주입이 가능한 클래스를 표시합니다.
2. Module 인터페이스:
   - 모듈 개념을 구현하기 위한 인터페이스입니다.
3. DIContainer 클래스:
   - dependencies: 모든 의존성을 저장하는 맵입니다. 
   - singletons: 싱글톤 인스턴스를 저장하는 맵입니다.
   - register: 일반 의존성을 등록합니다.
   - registerSingleton: 싱글톤 의존성을 등록합니다.
   - installModules: 모듈을 설치합니다.
   - resolve: 의존성을 해결합니다. 등록된 의존성이 없으면 자동 생성자 주입을 시도합니다.
   - findInjectableConstructor: @Injectable 어노테이션이 붙은 클래스의 생성자를 찾아 의존성을 자동으로 주입합니다.
4. 예제 사용:
   - UserRepository와 UserService는 @Injectable 어노테이션이 붙어있어 자동 주입이 가능합니다.
   - UserModule은 UserRepository를 싱글톤으로, UserService를 일반 의존성으로 등록합니다.
   - main 함수에서는 컨테이너를 생성하고, 모듈을 설치한 후 UserService를 해결하여 사용합니다.

이 구현은 다음과 같은 장점을 가집니다:
- 싱글톤과 일반 의존성을 모두 지원합니다. 
- 자동 생성자 주입을 지원하여 보일러플레이트 코드를 줄입니다. 
- 모듈 시스템을 통해 관련 의존성을 그룹화할 수 있습니다. 
- 타입 안전성을 제공합니다.

이 DIContainer는 소규모에서 중간 규모의 프로젝트에 적합하며, Dagger나 Hilt와 같은 프레임워크의 기본 개념을 이해하는 데 도움이 될 것입니다.

### 6. 어노테이션 기반 의존성 주입 구현