# Oscar HR PeopleOps: установка на новый Docker-сервер

Домен для примера: `test.dmuk.kz`.

## 1. Что лежит в папке

- `frontend` - сайт Angular, запускается через nginx.
- `backend` - Spring Boot API, адрес внутри Docker: `backend:8080`, снаружи через `/api/v1`.
- `docker-compose.prod.yml` - финальный запуск.
- `.env.example` - шаблон паролей и адресов.

## 2. Подготовить DNS

В DNS сделайте запись:

```text
test.dmuk.kz  A  IP_АДРЕС_НОВОГО_СЕРВЕРА
```

Проверьте:

```bash
ping test.dmuk.kz
```

## 3. Установить Docker

На Ubuntu/Debian:

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker
```

## 4. Загрузить проект на сервер

Скопируйте папку `Oscar` на сервер, например:

```bash
/opt/Oscar
```

Перейдите в нее:

```bash
cd /opt/Oscar
```

## 5. Создать рабочий файл настроек

```bash
cp .env.example .env
nano .env
```

Заполните реальные пароли:

- `POSTGRES_PASSWORD` - новый пароль базы HR.
- `AD_BIND_PASSWORD` - пароль сервисного AD-аккаунта.
- `M365_SYNC_PASSWORD` - пароль аккаунта, который может запускать синхронизацию M365.
- `ZKTECO_PASSWORD` - пароль PostgreSQL ZKTeco.
- `MAIL_PASSWORD` - пароль SMTP.

## 6. Важное про AD create user

Если Docker-сервер Linux, команда `New-ADUser` внутри Linux-контейнера не работает как на Windows с RSAT.

Есть два рабочих варианта:

1. Развернуть backend на Windows-сервере с RSAT, как сейчас.
2. Оставить Docker для сайта/API, но AD-создание вынести в отдельный Windows PowerShell service.

Для чистого Linux Docker поставьте:

```env
PROVISIONING_DRY_RUN=true
```

Для Windows-сервера с PowerShell/RSAT:

```env
PROVISIONING_DRY_RUN=false
POWERSHELL_EXECUTABLE=powershell.exe
```

## 7. Запустить

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Проверить:

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

## 8. Открыть сайт

Откройте:

```text
http://test.dmuk.kz
```

API будет здесь:

```text
http://test.dmuk.kz/api/v1/health
```

## 9. Если нужен HTTPS

Поставьте внешний reverse proxy или nginx-proxy-manager и выпустите сертификат Let's Encrypt для:

```text
test.dmuk.kz
```

Внешний proxy должен отправлять трафик на контейнер frontend порт `80`.

## 10. Обновление версии

Загрузите новую папку проекта и выполните:

```bash
cd /opt/Oscar
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

## 11. Где смотреть логи

```bash
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f frontend
```

Внутренние backend-логи также лежат в Docker volume `backend_logs`.
