# Code Convention

## ✨ 1. 기본 원칙

- **가독성 최우선**
- **특별한 이유가 없는 경우 IntelliJ IDEA 자동 서식 준수**
- **특별한 이유가 없는 경우 IntelliJ IDEA 경고 제거**

---

## 🏷 2. 네이밍 규칙

### 🔹 클래스 (PascalCase)

- 클래스명은 **파스칼 케이스(PascalCase)** 사용

```java
// ✅ 올바른 예시
public class UserAccount { }
public class OrderService { }
```

### 🔹 변수 및 메서드 (camelCase)

- 변수 및 메서드는 **카멜 케이스(camelCase)** 사용
- `@Value`를 사용한 프로퍼티 값도 **카멜 케이스** 적용

```java
// ✅ 올바른 예시
private String userName;
public void processOrder() { }
```

### 🔹 축약어 (camelCase)

- 축약어도 **카멜 케이스** 적용

```java
// ❌ 잘못된 예시
String HTTP;
String DTO;

// ✅ 올바른 예시
String Http;
String Dto;

```

### 🔹 상수 (UPPER_SNAKE_CASE)

- 상수는 **대문자 스네이크 케이스(UPPER_SNAKE_CASE)** 사용

```java
// ✅ 올바른 예시
public static final int MAX_USER_COUNT = 100;
public static final String API_KEY = "1234";

```

### 🔹 패키지명 (소문자)

- 패키지명은 **전부 소문자** 사용

```java
// ✅ 올바른 예시
com.example.service;
org.myproject.utils;

```

### 🔹 줄임말 사용 금지

- **논의된 경우를 제외하고 줄임말 사용 금지**

```java
// ❌ 잘못된 예시
String msg;
String err;

// ✅ 올바른 예시
String message;
String error;

```

### 🔹 CRUD method 명명 규칙

| 기능                       | Controller 메서드명                      | Service 메서드명                         | Repository 메서드명                                 |
| -------------------------- | ---------------------------------------- | ---------------------------------------- | --------------------------------------------------- |
| **Create** (POST)          | `createUser(UserForm userForm)`          | `createUser(UserForm userForm)`          | `save(User user)` _(JPA 기본)_                      |
| **Read (단일 조회)** (GET) | `getUserById(Long id)`                   | `getUserById(Long id)`                   | `findById(Long id)` _(JPA 기본)_                    |
| **Read (목록 조회)** (GET) | `getUserList()`                          | `getUserList()`                          | `findAll()` _(JPA 기본)_                            |
| **Update** (PUT)           | `updateUser(Long id, UserForm userForm)` | `updateUser(Long id, UserForm userForm)` | `save(User user)` _(JPA 기본, ID 존재 시 업데이트)_ |
| **Delete** (DELETE)        | `deleteUserById(Long id)`                | `deleteUserById(Long id)`                | `deleteById(Long id)` _(JPA 기본)_                  |

- **이외의 것은 비즈니스적으로 명명하기**

✅ **일관성 유지:**

- Controller & Service → `get`, `create`, `update`, `delete`
- Repository → `find`, `save`, `deleteById`

✅ **Controller**

- Controller는 **Request, Response 전용 객체**를 사용하여 클라이언트와의 데이터 교환을 담당합니다.
- **예외 케이스**는 반드시 처리하고, 관련 API 스펙은 `@ApiResponse` 등의 애너테이션으로 문서화합니다.
- **DTO 분리:**
  - Controller에서 사용하는 Request/Response 객체는 보통 `record`로 작성하며, 네이밍 규칙은 `{Domain}{MethodName}Request` 또는 `{Domain}{MethodName}Response` 형태로 합니다.
  - Controller 내에 별도의 Dto 서브패키지를 두어 Service에서 사용하는 DTO와 구분합니다.

✅ **Service**

- **트랜잭션 관리:**
  - 서비스 계층에서 `@Transactional`을 사용하여 트랜잭션을 관리합니다.
- **DTO 변환:**
  - 객체와 DTO 간 변환 로직은 Service 계층에서 수행합니다.
  - 변환 메서드는 보통 static으로 구현하며, 아래와 같은 네이밍 규칙을 따릅니다.
    - **`of`**: 여러 파라미터를 받아 DTO로 조합할 때
    - **`from`**: Entity 객체 하나를 DTO로 변환할 때
    - **`toEntity`**: DTO를 Entity로 변환할 때

✅ **Entity**

- **JPA 변경 감지:**
  - 엔티티 내 필드의 변경은 변경 감지(Dirty Checking)를 활용합니다.
  - 이를 위해 엔티티 내에 별도의 필드 변경 메서드를 작성하며, **Setter 사용은 지양**합니다.
- **객체 생성:**
  - 객체 생성 시 `new` 대신 *`@Builder`*를 적극 사용하여 가독성과 불변성을 높입니다.

✅ **DTO (Data Transfer Object)**

- **DTO 사용:**
  - Controller와 Service에 사용하는 DTO는 용도가 다르므로 분리하여 관리합니다.
  - Controller에서는 클라이언트와의 요청/응답을 위한 Request/Response 객체를 주로 `record`로 작성하며, Service에서 공통적으로 사용하는 데이터 구조는 별도의 DTO 레이어에 생성합니다.
- **네이밍 가이드라인:**
  - Controller용: `{동사}{명사}Request` / `{동사}{명사}Response`
    - 예: `UserJoinRequest`, `UserUpdateResponse`
  - Service용: `{적절한이름}Dto`
- **DTO 변환 메서드:**
  - DTO 내에 변환 메서드를 작성하여 Entity와의 변환을 명확하게 합니다.
  - 일반적으로 많이 사용하는 메서드 이름은 `of`, `from`, `toEntity`입니다.
- **Java Record:**
  - 간결함과 불변성을 위해 Controller의 Request/Response 객체는 `record`를 사용하는 것을 권장합니다.

### 🔹 HTTP REST API 명명 규칙

- prefix 로 api와 버전정보를 갖음 (프로젝트 레벨에서 설정 완료, controller 에서 설정할 필요 없음)
- 동사는 사용하지 않고 http method 를 활용

```java
// ❌ 잘못된 예시
/api/v1/order/create
/api/v1/order/delete

// ✅ 올바른 예시
(post) /api/v1/order
(delete) /api/v1/order

```

- 리소스는 항상 복수형 사용

```bash
// ❌ 잘못된 예시 (단수형 리소스)
GET /api/v1/user
POST /api/v1/order

// ✅ 올바른 예시 (복수형 리소스)
GET /api/v1/users
POST /api/v1/orders

```

- 비즈니스 로직이 들어간 \*\*행위(Action)은 동사 포함 (approve, ban, login 등)

```bash
// ✅ 액션이 필요한 경우
POST /api/v1/auth/login
POST /api/v1/auth/logout
POST /api/v1/orders/123/approve  // 주문 승인
POST /api/v1/users/456/ban  // 유저 차단
```

## 🎨 3. 코드 스타일

### 🔹 중괄호 `{}` 사용

- 단일 라인 조건문에도 **반드시 중괄호 `{}` 사용**

```java
// ❌ 잘못된 예시
if (a == 0) return a;

// ✅ 올바른 예시
if (a == 0) {
    return a;
} DTO 유즈케이스 및 네이밍 규칙
```

## 4. 주석 컨벤션

<aside>
주석은 설명하려는 구문에 맞춰 들여쓰기 합니다.

<br>

```java
// Good
function someFunction() {
  ...

  // statement에 관한 주석
  statements
}
```

</aside>

---

### ☑️ 코드 컨벤션이 필요한 이유

- 팀원끼리 코드를 공유하기 때문에 일관성있는 코드를 작성하면 서로 이해하기 쉽다.
- 나중에 입사 지원 시 프로젝트를 하며 코드 컨벤션을 만들어 진행했다고 하면 협업 면에서 유리하게 작용할 수 있다.

### 참고

[코딩컨벤션](https://ui.toast.com/fe-guide/ko_CODING-CONVENTION)
