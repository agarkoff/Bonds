# API Документация

## Обзор

API приложения разделен на две основные категории:

1. **Административный API** (`/admin/api/*`) - для управления системой и обновления данных
2. **Пользовательский API** (`/api/*`) - для получения данных об облигациях
3. **Подписки** (`/api/subscriptions/*`, `/api/offer-subscriptions/*`) - для управления пользовательскими подписками

---

## Административный API (`/admin/api/`)

**Назначение:** Управление системой, обновление данных, служебные операции.

**Требования:** Административные права доступа.

### Парсинг и обновление данных

#### `POST /admin/api/moex/bonds/parse`
Парсинг облигаций с Московской биржи (MOEX).

**Описание:** Загружает актуальную информацию об облигациях с MOEX, включая основные параметры, даты погашения, купонные выплаты.

**Ответ:**
```json
"MOEX parsing completed successfully"
```

#### `POST /admin/api/tbank/bonds/parse`
Обновление данных об облигациях из T-Bank API.

**Описание:** Загружает дополнительные данные об облигациях (FIGI, тикеры, UIDs).

**Ответ:**
```json
"T-Bank bonds update completed successfully"
```

#### `POST /admin/api/tbank/prices/parse`
Обновление цен облигаций из T-Bank API.

**Описание:** Загружает актуальные рыночные цены облигаций.

**Ответ:**
```json
"T-Bank prices update completed successfully"
```

#### `POST /admin/api/ratings/raexpert/update`
Обновление рейтингов от RaExpert.

**Описание:** Парсит рейтинги эмитентов с сайта RaExpert.

**Ответ:**
```json
"RaExpert ratings update completed successfully"
```

#### `POST /admin/api/ratings/dohod/update`
Обновление рейтингов от Dohod.ru.

**Описание:** Парсит рейтинги эмитентов с сайта Dohod.ru.

**Ответ:**
```json
"Dohod ratings update completed successfully"
```

### Расчеты

#### `POST /admin/api/bonds/calculate`
Расчет доходности всех облигаций.

**Описание:** Выполняет расчет доходности, НКД, прибыли для всех облигаций в базе.

**Ответ:**
```json
"Bonds calculation completed successfully"
```

#### `POST /admin/api/bonds/calculate/{isin}`
Расчет доходности конкретной облигации.

**Параметры:**
- `isin` (path) - ISIN код облигации

**Описание:** Выполняет расчет доходности для указанной облигации.

**Ответ:**
```json
"Bond calculation completed successfully for ISIN: RU000ABC1234"
```

### Комплексные операции

#### `POST /admin/api/update-all`
Полное обновление всех данных.

**Описание:** Последовательно выполняет все операции обновления:
1. Парсинг MOEX
2. Обновление T-Bank данных
3. Обновление T-Bank цен
4. Обновление рейтингов (RaExpert + Dohod)
5. Расчет доходности

**Ответ:**
```json
"Full data update completed successfully"
```

### Авторизация

Админское API должно быть доступно только пользователю с ID 1.
Этот функционал проверки должен быть централизован для всех конечных точек, начинающихся с /api
При запуске с Spring-профилем no-auth авторизация на /api должна быть выключена

---

## Пользовательский API (`/api/`)

**Назначение:** Получение данных об облигациях для пользователей.

**Требования:** Открытый доступ (без авторизации).

### Облигации

#### `GET /api/bonds`
Получение всех облигаций.

**Ответ:** Массив объектов Bond
```json
[
  {
    "id": 123,
    "isin": "RU000ABC1234",
    "ticker": "ABC-25",
    "shortName": "Название облигации",
    "annualYield": 12.5,
    "maturityDate": "2025-12-31",
    "ratingValue": "AA-",
    "price": 98.5,
    ...
  }
]
```

#### `GET /api/bonds/top?limit={limit}`
Получение топа облигаций по доходности.

**Параметры:**
- `limit` (query, optional) - Количество облигаций (по умолчанию 50)

**Ответ:** Массив объектов Bond, отсортированный по убыванию доходности.

#### `GET /api/bonds/filtered`
Получение отфильтрованных облигаций.

**Параметры:**
- `minWeeksToMaturity` (query) - Минимальное количество недель до погашения (по умолчанию 0)
- `maxWeeksToMaturity` (query) - Максимальное количество недель до погашения (по умолчанию 26)
- `showOffer` (query) - Учитывать оферты (по умолчанию false)
- `searchText` (query) - Поиск по тикеру/названию
- `feePercent` (query) - Размер комиссии в процентах (по умолчанию 0.30)
- `minYield` (query) - Минимальная доходность (по умолчанию 0)
- `maxYield` (query) - Максимальная доходность (по умолчанию 50)
- `selectedRatings` (query, array) - Фильтр по рейтингам
- `limit` (query) - Количество результатов (по умолчанию 50)

**Пример запроса:**
```
GET /api/bonds/filtered?minYield=10&maxYield=25&selectedRatings=AA-&selectedRatings=A+&limit=20
```

**Ответ:** Массив отфильтрованных объектов Bond.

#### `GET /api/bonds/{isin}`
Получение облигации по ISIN.

**Параметры:**
- `isin` (path) - ISIN код облигации

**Ответ:** Объект Bond или 404 если не найден.

### Справочная информация

#### `GET /api/ratings`
Получение списка доступных рейтингов.

**Ответ:**
```json
["AAA", "AA+", "AA", "AA-", "A+", "A", "A-", "BBB+", "BBB", "BBB-", ...]
```

#### `GET /api/bonds/stats`
Получение статистики по облигациям.

**Ответ:**
```json
{
  "totalBonds": 1500,
  "bondsWithRating": 800,
  "bondsWithOffers": 150,
  "availableRatings": 15
}
```

---

## API Подписок

### Подписки на рейтинги (`/api/subscriptions/`)

**Назначение:** Управление подписками на уведомления о топе облигаций.

**Требования:** Авторизация через Telegram.

#### `GET /api/subscriptions`
Получение всех подписок текущего пользователя.

**Ответ:** Массив объектов RatingSubscription.

#### `POST /api/subscriptions`
Создание новой подписки.

**Тело запроса:**
```json
{
  "name": "Мои облигации",
  "periodHours": 24,
  "minYield": 10.0,
  "maxYield": 25.0,
  "tickerCount": 20,
  "includeOffer": true,
  "minMaturityWeeks": 4,
  "maxMaturityWeeks": 52,
  "feePercent": 0.30,
  "selectedRatings": ["AA-", "A+", "A"]
}
```

#### `GET /api/subscriptions/{id}`
Получение подписки по ID.

#### `DELETE /api/subscriptions/{id}`
Удаление подписки.

#### `PATCH /api/subscriptions/{id}/toggle`
Включение/выключение подписки.

#### `POST /api/subscriptions/{id}/send`
Принудительная отправка уведомления по подписке.

#### `PATCH /api/subscriptions/{id}`
Обновление названия подписки.

**Тело запроса:**
```json
{
  "name": "Новое название"
}
```

### Подписки на оферты (`/api/offer-subscriptions/`)

**Назначение:** Управление подписками на уведомления об офертах.

**Требования:** Авторизация через Telegram.

#### `GET /api/offer-subscriptions`
Получение всех подписок на оферты текущего пользователя.

#### `DELETE /api/offer-subscriptions/{id}`
Удаление подписки на оферту.

---

## Коды ошибок

- `200` - Успешный запрос
- `400` - Неверные параметры запроса
- `401` - Требуется авторизация
- `403` - Доступ запрещен
- `404` - Ресурс не найден
- `500` - Внутренняя ошибка сервера

## Модель данных

### Bond (Облигация)
```json
{
  "id": 123,
  "isin": "RU000ABC1234",
  "ticker": "ABC-25",
  "shortName": "Название облигации",
  "couponValue": 25.50,
  "maturityDate": "2025-12-31",
  "faceValue": 1000.0,
  "couponFrequency": 2,
  "offerDate": "2024-06-15",
  "price": 98.5,
  "ratingValue": "AA-",
  "ratingCode": 7,
  "annualYield": 12.5,
  "annualYieldOffer": 15.0,
  "nkd": 8.25,
  "costs": 1006.75,
  "profit": 145.0,
  "profitNet": 131.15
}
```

### RatingSubscription (Подписка на рейтинг)
```json
{
  "id": 1,
  "name": "Мои облигации",
  "periodHours": 24,
  "minYield": 10.0,
  "maxYield": 25.0,
  "tickerCount": 20,
  "includeOffer": true,
  "minMaturityWeeks": 4,
  "maxMaturityWeeks": 52,
  "feePercent": 0.30,
  "selectedRatings": ["AA-", "A+"],
  "enabled": true,
  "createdAt": "2024-01-15T10:30:00",
  "lastSentAt": "2024-01-16T10:30:00"
}
```

---

## Примечания

1. **Авторизация**: Административные эндпоинты требуют соответствующих прав доступа. Пользовательские API открыты для всех.

2. **Лимиты**: По умолчанию все запросы ограничены разумными лимитами для предотвращения злоупотреблений.

3. **Кэширование**: Данные об облигациях обновляются ежедневно по расписанию.