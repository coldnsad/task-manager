# Task Manager

Учебный микросервисный проект на Spring Boot: CRUD задач, JWT-авторизация, Eureka, Config Server, Gateway с Circuit Breaker, PostgreSQL.
UI нет — всё проверяется через HTTP (Swagger или curl).

## Архитектура

```
клиент → gateway :9090 → task-manager-service :8080 → customer-profile-service :8081
           (Eureka lb)       (конфиг ← :8888)            (конфиг ← :8888)
```

| Модуль | Порт | Зачем |
|---|---|---|
| `config-server` | 8888 | Раздаёт конфиги из репозитория [coldnsad/config-repo](https://github.com/coldnsad/config-repo) |
| `eureka-server` | 8761 | Реестр сервисов, дашборд: http://localhost:8761 |
| `gateway` | 9090 | Единая точка входа: маршрут `lb://`, fallback через Circuit Breaker |
| `task-manager-service` | 8080 | Задачи + пользователи (JPA), JWT, Swagger |
| `customer-profile-service` | 8081 | Демо-сервис профилей, вызывается по Feign |

## Требования

- **JDK 24** (в `pom.xml` — `java.version=24`, на JDK 21 сборка упадёт)
- Docker (для PostgreSQL 17)
- Maven не нужен — есть `mvnw` / `mvnw.cmd`

## Запуск

### 1. База данных

```bash
docker compose up -d
```

Postgres слушает **5433** (проброс на 5432 внутри контейнера), БД `task_manager_db`, пользователь `myuser` / `mypassword`.

### 2. Переменные окружения

`task-manager-service` и `customer-profile-service` берут конфиг из Config Server, а там вместо значений — подстановки `${DB_URL}`, `${JWT_TOKEN}` и т.п. Без переменных окружения сервисы не стартуют:

| Переменная | Значение |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/task_manager_db` |
| `DB_USERNAME` | `myuser` |
| `DB_PASSWORD` | `mypassword` |
| `JWT_TOKEN` | любая строка **от 32 символов** (секрет подписи) |
| `JWT_EXPIRATION` | время жизни токена в **миллисекундах**, для теста `3600000` |

`customer-profile-service` нужны только `DB_*`; `gateway` и `eureka-server` — ничего.

PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5433/task_manager_db"
$env:DB_USERNAME="myuser"
$env:DB_PASSWORD="mypassword"
$env:JWT_TOKEN="dev-only-secret-key-please-change-0123456789"
$env:JWT_EXPIRATION="3600000"
```

### 3. Сервисы — по порядку, каждый в своём терминале

```bash
./mvnw -pl config-server spring-boot:run            # ждём "Started ConfigServerApplication"
./mvnw -pl eureka-server spring-boot:run            # http://localhost:8761
./mvnw -pl task-manager-service spring-boot:run     # http://localhost:8080
./mvnw -pl customer-profile-service spring-boot:run # http://localhost:8081
./mvnw -pl gateway spring-boot:run                  # http://localhost:9090
```

Windows: `.\mvnw.cmd -pl config-server spring-boot:run`.
Порядок важен: `task-manager-service` и `customer-profile-service` не стартуют без Config Server (`spring.config.import=configserver:` обязателен), а Gateway ищет сервисы в Eureka.
В IntelliJ достаточно 5 конфигураций Spring Boot + env из шага 2.

### 4. Роли в БД — один раз, после первого старта `task-manager-service`

Таблицы создаёт Hibernate (`ddl-auto=update`), но ролей в них нет:

```bash
docker exec -i task_manager_db psql -U myuser -d task_manager_db \
  -c "INSERT INTO role(name) VALUES ('ROLE_USER'),('ROLE_ADMIN');"
```

Без этого регистрация с `roleName` упадёт, а пользователь без роли получит 403 на задачи.

## Проверка, что всё живо

- http://localhost:8761 — в дашборде 3 приложения: `TASK-MANAGER-SERVICE`, `CUSTOMER-PROFILE-SERVICE`, `API-GATEWAY`
- http://localhost:8080/swagger-ui/index.html — Swagger
- http://localhost:9090/actuator/health — `UP` (там же `/actuator/gateway/routes`, `/actuator/circuitbreakers`)
- http://localhost:8888/task-manager-service/dev — конфиг, который видит сервис

## Тест за 5 минут (curl)

```bash
GW=http://localhost:9090

# 1. регистрация (роль должна уже быть в таблице role)
curl -s -X POST $GW/api/v1/auth/signup -H "Content-Type: application/json" \
  -d '{"userName":"tester","password":"secret","roleName":"ROLE_USER"}'
# → User registered

# 2. логин: это GET с JSON-телом, в ответе — голая строка токена
TOKEN=$(curl -s -X GET $GW/api/v1/auth/signin -H "Content-Type: application/json" \
  -d '{"userName":"tester","password":"secret"}')

# 3. создать задачу
curl -s -X POST $GW/api/v1/tasks -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Первая задача","description":"проверка API","completed":false}'
# → 201 {"id":N,"title":"Первая задача",...,"customer":null}

# 4. список: фильтры + пагинация + сортировка
curl -s -H "Authorization: Bearer $TOKEN" \
  "$GW/api/v1/tasks?completed=false&page=0&size=5&sort=id,desc"

# 5. одна задача по id (несуществующая → 404 {"message":"Task not found with id: ..."})
curl -s -H "Authorization: Bearer $TOKEN" $GW/api/v1/tasks/1

# 6. удаление — только для ROLE_ADMIN, у tester ROLE_USER → 403
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE \
  -H "Authorization: Bearer $TOKEN" $GW/api/v1/tasks/1

# 7. без токена → 403
curl -s -o /dev/null -w "%{http_code}\n" $GW/api/v1/tasks
```

Windows PowerShell: те же команды работают через `curl.exe` (встроенный `curl` — алиас на `Invoke-WebRequest`); присваивания пишутся как `$GW="http://localhost:9090"` и `$TOKEN = curl.exe -s ...`, JSON — в одинарных кавычках, как выше.

## API

Все пути — через Gateway `:9090`; напрямую сервисы тоже отвечают (`:8080`, `:8081`).

| Метод | Путь | Доступ | Примечание |
|---|---|---|---|
| `POST` | `/api/v1/auth/signup` | открыт | `{userName, password, roleName}`; `roleName` = `ROLE_USER` / `ROLE_ADMIN` |
| `GET` | `/api/v1/auth/signin` | открыт | JSON в теле → строка JWT |
| `POST` | `/api/v1/tasks` | USER / ADMIN | `{title, description, completed}` → 201 + `Location` |
| `GET` | `/api/v1/tasks` | USER / ADMIN | `page`, `size`, `sort` + фильтры `title` (точно), `description` (вхождение), `completed` |
| `GET` | `/api/v1/tasks/{id}` | USER / ADMIN | 404, если нет |
| `PUT` | `/api/v1/tasks/{id}` | USER / ADMIN | меняет `title`, `description`, `completed` |
| `DELETE` | `/api/v1/tasks/{id}` | **ADMIN** | 200 с пустым телом |
| `GET` | `/customer-api/customers/{id}` | — | `:8081` напрямую, Gateway этот путь не проксирует |

Ошибки — JSON `{"message": "...", "status": "BAD_REQUEST|NOT_FOUND"}`.

## Что важно знать

- **Половина конфига лежит не здесь.** БД, JWT, Swagger и `ddl-auto` приходят из GitHub-репозитория `coldnsad/config-repo`; в `application.properties` модулей только адреса. Нужен интернет при старте Config Server.
- **`JWT_EXPIRATION` — миллисекунды.** Частое значение `60000` = токен живёт 1 минуту, дальше любой запрос отдаёт 403; для теста ставьте `3600000`.
- **Без токена и с протухшим токеном — 403, а не 401** (в Security нет entry point).
- **Роли называются ровно `ROLE_USER` / `ROLE_ADMIN`**: `hasAnyRole("USER")` ищет authority `ROLE_USER`, поэтому в БД и при регистрации нужен префикс `ROLE_`.
- Регистрация без `roleName` создаёт пользователя без ролей → на задачи только 403.
- **Gateway проксирует только `/api/v1/**`**, остальное — 404.
- Если `task-manager-service` недоступен, Gateway отдаёт 503 из `FallbackController` — `Task Service unavailable. Please try later.` (fallback у `taskManagerCircuitBreaker`).
- `customer-profile-service` — заглушка: таблица `customer` пустая, id при ручной вставке не автогенерируется (`GenerationType.AUTO`), на отсутствующий id — 500. Чтобы увидеть Feign, вставьте `customer` с явным `id` и проставьте `task.customer_id`.
- `ddl-auto=update` — схему Hibernate правит сам, SQL пишется в лог (`show-sql=true`).

## Стек

Spring Boot 3.5.3, Spring Cloud 2025.0.0 (Eureka, Config, Gateway, OpenFeign, Resilience4j), Spring Security + JWT (jjwt 0.11.5), Spring Data JPA + PostgreSQL 17, MapStruct, springdoc-openapi, Java 24.
