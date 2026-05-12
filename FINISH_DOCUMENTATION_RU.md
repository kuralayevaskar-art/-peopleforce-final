# Финальная документация проекта peopleforce HR + AD + ZKTeco

## 1. Что это за папка

Финальный проект лежит здесь:

```text
C:\people2\finesh
```

Это единый проект. Старые папки `peaple` и `zkt` больше не нужны для работы финальной версии.

Главное правило интеграции:

- сотрудники и отделы приходят только из Active Directory;
- ZKTeco не создает сотрудников и отделы;
- ZKTeco только добавляет посещаемость к сотруднику, если этот сотрудник уже есть в AD-списке;
- документы сохраняются на диск `Z:\Global\people`.

## 2. Структура папок

```text
C:\people2\finesh
  backend\     Spring Boot API
  frontend\    Angular сайт
  docs\        технические заметки
  screen\      скриншоты дизайна
```

Важные файлы:

```text
backend\src\main\resources\application.yml
backend\src\main\resources\application-secrets.properties
backend\src\main\resources\application-secrets.properties.example
frontend\src\app\app.component.html
frontend\src\app\app.component.scss
frontend\src\app\app.component.ts
```

## 3. Где дизайн

Основной дизайн сайта находится во frontend:

```text
frontend\src\app\app.component.html   разметка экранов и кнопок
frontend\src\app\app.component.scss   стили, карточки, таблицы, меню
frontend\src\app\app.component.ts     логика экранов, загрузка AD/ZKT/documents
```

Скриншоты дизайна лежат здесь:

```text
screen\
```

## 4. Backend

Backend запускается на:

```text
http://127.0.0.1:8080
```

API имеет общий путь:

```text
/api/v1
```

Главные backend-модули:

```text
backend\src\main\java\com\orca\hrplatform\integration\ad
```

Работа с Active Directory.

```text
backend\src\main\java\com\orca\hrplatform\attendance
```

Работа с ZKTeco и посещаемостью.

```text
backend\src\main\java\com\orca\hrplatform\document
```

Сохранение и чтение файлов сотрудников с диска `Z:`.

## 5. Frontend

Frontend запускается на:

```text
http://127.0.0.1:4300
```

Основные страницы:

- `Сотрудники` - список сотрудников из AD.
- `Отделы` - отделы из AD.
- `Посещаемость` - события ZKTeco, отфильтрованные только по AD-сотрудникам.
- `Документы` - документы сотрудников.
- `Настройки` - AD, ZKTeco, Synology/Z-диск.

## 6. Кнопки и что они делают

### Верхняя панель

`Login`  
Вход пользователя.

`Logout`  
Выход пользователя.

`+ Создать пользователя`  
Открывает/использует блок создания AD-пользователя на странице настроек.

### Страница `Настройки`

`Test connection`  
Проверяет подключение к Active Directory / LDAP.

`Save settings`  
Сохраняет настройки AD в runtime backend.

`Load employees from AD`  
Загружает сотрудников из AD и сохраняет их в HR-базу. Если HR-компания еще не создана, backend сам создает компанию `DMUK`.

`Сгенерировать логин и пароль`  
Генерирует предварительный логин/пароль для нового пользователя.

`Создать на сайте`  
Добавляет пользователя в локальный список сайта. Реальное создание в AD нужно подключать отдельно, если потребуется.

### Карточка сотрудника

`Upload photo`  
Загружает фото сотрудника локально в интерфейс.

`Documents -> Add`  
Загружает файл сотрудника в:

```text
Z:\Global\people\<login>\documents\
```

Например:

```text
Z:\Global\people\a.podlesnyy\documents\file.pdf
```

`View`  
Открывает документ из папки сотрудника.

`Attendance -> Refresh`  
Обновляет историю посещений выбранного сотрудника.

## 7. IP-адреса и подключения

### Active Directory / LDAP

```text
10.1.10.11:389
ldap://10.1.10.11:389
```

Настройки находятся:

```text
backend\src\main\resources\application-secrets.properties
```

Параметры:

```properties
AD_URL=ldap://10.1.10.11:389
AD_BASE_DN=DC=DMUK,DC=EDU
AD_BIND_DN=zkt@dmuk.edu.kz
AD_BIND_PASSWORD=...
```

### ZKTeco PostgreSQL

```text
10.1.70.2:5442
database: biosecurity-boot
```

Параметры:

```properties
ZKTECO_HOST=10.1.70.2
ZKTECO_PORT=5442
ZKTECO_DB_NAME=biosecurity-boot
ZKTECO_USERNAME=...
ZKTECO_PASSWORD=...
ZKTECO_SYNC_ENABLED=true
```

### Synology / Z-диск

Файлы сотрудников сохраняются сюда:

```text
Z:\Global\people
```

Параметр:

```properties
SYNOLOGY_ROOT_PATH=Z:\\Global\\people
```

Важно: backend должен запускаться под тем Windows-пользователем, у которого виден диск `Z:`.

## 8. Как менять IP и пароли

Открыть файл:

```text
C:\people2\finesh\backend\src\main\resources\application-secrets.properties
```

Изменить нужные строки:

```properties
AD_URL=ldap://10.1.10.11:389
AD_BIND_DN=zkt@dmuk.edu.kz
AD_BIND_PASSWORD=ваш_пароль

ZKTECO_HOST=10.1.70.2
ZKTECO_PORT=5442
ZKTECO_DB_NAME=biosecurity-boot
ZKTECO_USERNAME=ваш_логин
ZKTECO_PASSWORD=ваш_пароль

SYNOLOGY_ROOT_PATH=Z:\\Global\\people
```

После изменения перезапустить backend.

## 9. Как запустить

### Backend

```powershell
cd C:\people2\finesh\backend
mvnw.cmd spring-boot:run
```

Проверка:

```text
http://127.0.0.1:8080/api/v1/health
```

### Frontend

Первый раз установить зависимости:

```powershell
cd C:\people2\finesh\frontend
npm install
```

Запуск:

```powershell
npm start -- --port 4300 --host 127.0.0.1
```

Открыть:

```text
http://127.0.0.1:4300
```

## 10. Как работает синхронизация

1. Открыть сайт.
2. Перейти в `Настройки`.
3. Проверить AD через `Test connection`.
4. Нажать `Load employees from AD`.
5. Сотрудники появятся в `Сотрудники`.
6. Отделы появятся в `Отделы`.
7. ZKTeco-посещаемость будет показываться только для сотрудников, которые совпали с AD.

## 11. Где смотреть API

AD:

```text
POST /api/v1/admin/integrations/ad/test
POST /api/v1/admin/integrations/ad/users/sync
```

ZKTeco:

```text
GET /api/v1/attendance/inside
GET /api/v1/attendance/history?pin=...&date=...
GET /api/v1/attendance/photo?path=...
```

Документы:

```text
POST /api/v1/employees/{employeeLogin}/files
GET  /api/v1/employees/{employeeLogin}/files?category=documents
GET  /api/v1/employees/{employeeLogin}/files/download?category=documents&fileName=...
```

## 12. Частые проблемы

### Нет сотрудников и отделов

Причина: не нажата загрузка из AD или AD sync упал.

Что сделать:

1. `Настройки`.
2. `Test connection`.
3. `Load employees from AD`.

### AD connection successful, но сотрудники не появились

Проверить пароль bind user и Search base DN.

Также backend должен иметь доступ к HR-базе.

### Документ сохраняется, но не виден

Проверить путь:

```text
Z:\Global\people\<login>\documents
```

`<login>` берется из email до `@`.  
Например `a.podlesnyy@dmu.ac.uk` -> `a.podlesnyy`.

### Документ виден у другого сотрудника

Preview очищается при переходе между сотрудниками. Если браузер держит старую версию, нажать:

```text
Ctrl + F5
```

### Backend не видит Z-диск

Проверить, что backend запущен под тем же Windows-пользователем, где подключен диск `Z:`.

### ZKTeco показывает лишних людей

В финальной логике ZKTeco не создает сотрудников. Он только сопоставляет посещаемость с AD-сотрудниками по:

- `zktecoPin`;
- `adUsername`;
- login из email;
- ФИО.

## 13. Что не удалено специально

В финальной папке оставлен backend API `/attendance/departments`, но frontend больше не использует ZKTeco как источник отделов. Источник кадров и отделов - только AD.

