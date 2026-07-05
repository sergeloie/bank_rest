# Bank Cards

REST API для управления банковскими картами. Позволяет администраторам управлять пользователями и картами, а обычным пользователям — просматривать карты, выполнять переводы и запрашивать блокировку.

## Технологии

- **Java 25** / **Spring Boot 3.5**
- **Spring Security** + JWT (аутентификация, авторизация по ролям ADMIN/USER)
- **Spring Data JPA** + Hibernate (работа с БД)
- **PostgreSQL** (прод) / **H2** (разработка)
- **Liquibase** (миграции схемы БД)
- **MapStruct** (маппинг entity ↔ DTO)
- **Lombok** (генерация бойлерплейта)
- **Springdoc OpenAPI** (Swagger-документация)

## Архитектура

```
Controller → Service → Repository → Database
```

| Слой | Описание |
|------|----------|
| Controller | REST-эндпоинты, валидация входных данных |
| Service | Бизнес-логика, транзакции, проверки |
| Repository | Запросы к БД через Spring Data JPA |
| Entity | JPA-модели, соответствующие таблицам |
| DTO | Объекты передачи данных (API-контракт) |
| Security | JWT-аутентификация, RBAC |

## API Endpoints

### Аутентификация (публичные)
| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/auth/login` | Вход, возврат JWT-токенов |
| POST | `/api/auth/refresh` | Обновление токена |

### Администратор (ROLE_ADMIN)
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/admin/users` | Список пользователей |
| GET | `/api/admin/users/{id}` | Пользователь по ID |
| POST | `/api/admin/users` | Создать пользователя |
| PUT | `/api/admin/users/{id}` | Обновить пользователя |
| DELETE | `/api/admin/users/{id}` | Удалить пользователя |
| GET | `/api/admin/cards` | Список карт (фильтр по user/status) |
| GET | `/api/admin/cards/{id}` | Карта по ID |
| POST | `/api/admin/cards` | Создать карту |
| PATCH | `/api/admin/cards/{id}/block` | Заблокировать карту |
| PATCH | `/api/admin/cards/{id}/activate` | Активировать карту |
| DELETE | `/api/admin/cards/{id}` | Удалить карту |
| GET | `/api/admin/block-requests/pending` | Ожидающие запросы на блокировку |
| PATCH | `/api/admin/block-requests/{id}/approve` | Одобрить запрос |
| PATCH | `/api/admin/block-requests/{id}/reject` | Отклонить запрос |
| GET | `/api/admin/transfers` | История переводов |

### Пользователь (ROLE_USER)
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/cards/person/{personId}` | Мои карты |
| GET | `/api/cards/{id}` | Карта по ID |
| PUT | `/api/cards/{id}/block-request` | Запрос на блокировку |
| POST | `/api/transfers` | Перевод между своими картами |

### Мониторинг
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/actuator/health` | Проверка состояния сервиса и БД |

## Запуск через Docker Compose

### Предварительные требования

- [Docker](https://docs.docker.com/get-docker/) 20.10+
- [Docker Compose](https://docs.docker.com/compose/install/) v2+

### 1. Сборка образа

```bash
./gradlew bootJar
docker compose build
```

### 2. Задание переменных окружения

Перед запуском необходимо задать 4 обязательных переменных:

```bash
export BANK_ADMIN_PASSWORD=$2a$10$ bcrypt_хеш_пароля
export CARD_ENCRYPTION_SECRET=32_символьный_ключ_AES
export CARD_HASH_SECRET=32_символьный_ключ_HMAC
export BANK_JWT_SECRET=32_символьный_ключ_JWT
```

#### Формат и требования к секретам

Все ключи передаются как **обычные UTF-8 строки** (не hex, не Base64). Код конвертирует их в байты через `getBytes(StandardCharsets.UTF_8)`.

| Переменная | Алгоритм | Формат | Длина ключа | Пример |
|-----------|----------|--------|-------------|--------|
| `BANK_ADMIN_PASSWORD` | BCrypt | Хеш пароля | — | `$2a$10$rS.40zL8k2QE8X9zG8Yz7OeMKH5vZ8J3X6WqYbK1vN9mH2dT5iG6a` |
| `CARD_ENCRYPTION_SECRET` | AES-256-GCM | UTF-8 строка | **строго 32 символа** | `My32ByteAesEncryptionKey!!!!` |
| `CARD_HASH_SECRET` | HMAC-SHA256 | UTF-8 строка | **мин. 32 символа** | `My32ByteHmacSha256SecretKey!` |
| `BANK_JWT_SECRET` | HMAC-SHA256 (JJWT) | UTF-8 строка | **мин. 32 символа** | `My32ByteJwtSigningSecretKey!!` |

> **Важно:**
> - `BANK_ADMIN_PASSWORD` — BCrypt-хеш, а не открытый текст. Liquibase подставляет значение напрямую в SQL.
>   Генерация: `htpasswd -bnBC 10 "" 'ваш_пароль' | tr -d ':\n'`
> - `CARD_ENCRYPTION_SECRET` — **строго 32 символа**. AES-256 требует ключ ровно 32 байта, иначе `Cipher.init()` выбросит `InvalidKeyException`.
> - `CARD_HASH_SECRET` и `BANK_JWT_SECRET` — **минимум 32 символа**. JJWT проверяет это при старте.

### 3. Запуск

```bash
docker compose up -d
```

### 4. Проверка работоспособности

```bash
# Health check
curl http://localhost:2265/actuator/health

# Expected response:
# {"status":"UP","components":{"db":{"status":"UP","details":{"database":"PostgreSQL","validationQuery":"isValid()"}},"diskSpace":{"status":"UP"}}}
```

### 5. Первичная настройка

Администратор создаётся автоматически через Liquibase при первом запуске. Пароль берётся из переменной `BANK_ADMIN_PASSWORD` (должна содержать BCrypt-хеш).

```bash
# Вход под администратором (пароль — открытый текст, corresponding хеш уже в БД)
curl -X POST http://localhost:2265/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"name":"admin","password":"открытый_текст_пароля"}'
```

### Остановка

```bash
docker compose down          # Остановить контейнеры
docker compose down -v       # Остановить + удалить данные БД
```

## Порты

| Сервис | Внутренний | Хост |
|--------|-----------|------|
| Bank Cards API | 8080 | **2265** |
| PostgreSQL | 5432 | 5432 |

## Health Check

```
GET http://localhost:2265/actuator/health
```

Ответ `{"status":"UP"}` подтверждает, что приложение и БД работают корректно.

## Swagger UI

Документация API доступна по адресу:

```
http://localhost:2265/swagger-ui.html
```

## Дополнительные переменные окружения

| Переменная | Описание | По умолчанию |
|-----------|----------|-------------|
| `POSTGRES_USER` | Логин PostgreSQL | `postgres` |
| `POSTGRES_PASSWORD` | Пароль PostgreSQL | `postgres` |
| `SPRING_PROFILES_ACTIVE` | Профиль Spring | `prod` (в docker-compose) |
