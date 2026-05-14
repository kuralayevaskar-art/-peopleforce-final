# Oscar HR PeopleOps: установка на Windows Server 2022

Эта папка подготовлена для сервера с доменом `test.dmuk.kz`.

Рекомендуемая ОС: **Windows Server 2022**.

## 1. Что нужно установить на сервер

На новом сервере нужны:

- Windows Server 2022;
- Java 21, лучше Eclipse Temurin JDK/JRE 21;
- PostgreSQL 16;
- nginx for Windows;
- RSAT Active Directory tools;
- доступ по сети к AD `10.1.10.11`;
- доступ по сети к ZKTeco `10.1.70.2`;
- DNS запись `test.dmuk.kz -> IP нового сервера`.

## 2. Куда копировать файлы

На новом сервере создайте папку:

```powershell
C:\OscarHR
```

Скопируйте содержимое этой папки `win2022` внутрь `C:\OscarHR`.

В итоге должно быть так:

```text
C:\OscarHR\backend\app.jar
C:\OscarHR\frontend\index.html
C:\OscarHR\config\application-secrets.properties.example
C:\OscarHR\nginx\nginx.conf
C:\OscarHR\scripts\start-backend.ps1
```

## 3. Подготовить сервер

Откройте PowerShell **от имени администратора**:

```powershell
Set-ExecutionPolicy RemoteSigned -Scope LocalMachine
cd C:\OscarHR
.\scripts\01-prepare-server.ps1
```

Скрипт:

- создаст нужные папки;
- откроет firewall порты `80` и `8080`;
- установит RSAT Active Directory tools;
- добавит `10.1.10.11` в TrustedHosts для M365 sync.

Проверьте AD tools:

```powershell
Get-Command New-ADUser
```

Если команда есть, AD-модуль установлен.

## 4. Установить PostgreSQL 16

При установке PostgreSQL запомните пароль пользователя `postgres`.

Потом создайте базу и пользователя:

```powershell
psql -U postgres
```

Внутри `psql`:

```sql
CREATE DATABASE hr_peopleops;
CREATE USER hr_user WITH PASSWORD 'ВАШ_СИЛЬНЫЙ_ПАРОЛЬ';
GRANT ALL PRIVILEGES ON DATABASE hr_peopleops TO hr_user;
\c hr_peopleops
GRANT ALL ON SCHEMA public TO hr_user;
ALTER SCHEMA public OWNER TO hr_user;
\q
```

## 5. Заполнить настройки

Скопируйте шаблон:

```powershell
Copy-Item C:\OscarHR\config\application-secrets.properties.example C:\OscarHR\config\application-secrets.properties
notepad C:\OscarHR\config\application-secrets.properties
```

Заполните реальные значения:

- `DB_PASSWORD` - пароль `hr_user`;
- `AD_BIND_PASSWORD` - пароль сервисного AD-аккаунта;
- `M365_SYNC_USERNAME` и `M365_SYNC_PASSWORD`;
- `ZKTECO_PASSWORD`;
- `MAIL_PASSWORD`.

Важно:

```properties
PROVISIONING_DRY_RUN=false
POWERSHELL_EXECUTABLE=powershell.exe
```

Так система будет реально создавать пользователей в AD.

## 6. Установить nginx

Скачайте nginx for Windows:

```text
https://nginx.org/en/download.html
```

Распакуйте так, чтобы было:

```text
C:\nginx\nginx.exe
C:\nginx\conf\mime.types
```

Наш конфиг уже лежит тут:

```text
C:\OscarHR\nginx\nginx.conf
```

Он делает так:

- `http://test.dmuk.kz` открывает frontend;
- `/api/v1/...` отправляет запросы в backend `127.0.0.1:8080`.

Важно для мобильной камеры Face ID:

- live-камера в браузере работает только на `HTTPS` или на `localhost`;
- для телефона нужно открыть сайт как `https://test.dmuk.kz`;
- если открыть просто `http://test.dmuk.kz`, телефон сможет сделать фото через системную камеру, но live-овал с проверкой может не запуститься.

Для production поставьте SSL-сертификат на `test.dmuk.kz`. Самый простой вариант - поставить перед этим nginx внешний reverse proxy с HTTPS или использовать Windows IIS/Win-ACME для сертификата Let's Encrypt.

## 7. Запустить вручную первый раз

PowerShell от имени администратора:

```powershell
cd C:\OscarHR
.\scripts\start-backend.ps1
Start-Sleep -Seconds 10
.\scripts\start-nginx.ps1
.\scripts\health-check.ps1
```

Откройте:

```text
http://test.dmuk.kz
```

Проверка backend:

```text
http://test.dmuk.kz/api/v1/health
```

## 8. Сделать автозапуск после перезагрузки

```powershell
cd C:\OscarHR
.\scripts\install-startup-tasks.ps1
```

Будут созданы задачи:

- `OscarHR Backend`;
- `OscarHR Nginx`.

## 9. Проверить M365 sync

```powershell
cd C:\OscarHR
.\scripts\test-m365-sync.ps1
```

Если все правильно, должен быть результат `Success`.

## 10. Как перезапустить систему

```powershell
cd C:\OscarHR
.\scripts\restart-all.ps1
```

## 11. Где смотреть логи

Backend:

```text
C:\OscarHR\logs\hr-platform-backend.log
C:\OscarHR\logs\backend-stdout.log
C:\OscarHR\logs\backend-stderr.log
```

nginx:

```text
C:\nginx\logs\access.log
C:\nginx\logs\error.log
```

## 12. Как обновить версию

1. Остановить:

```powershell
cd C:\OscarHR
.\scripts\stop-backend.ps1
.\scripts\stop-nginx.ps1
```

2. Заменить:

```text
C:\OscarHR\backend\app.jar
C:\OscarHR\frontend\*
```

3. Запустить:

```powershell
.\scripts\restart-all.ps1
```

## 13. Частые ошибки

### Port 8080 already in use

Backend уже запущен. Проверить:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

### New-ADUser не найден

Установите RSAT:

```powershell
Add-WindowsCapability -Online -Name Rsat.ActiveDirectory.DS-LDS.Tools~~~~0.0.1.0
```

### Фото ZKT не видно

Проверьте доступ с сервера:

```powershell
Test-Path "\\10.1.70.2\C$\Program Files\ZKBioCVSecurity\service\zkbiosecurity\BioSecurityFile"
```

### M365 sync не работает

Проверьте:

```powershell
Set-Item WSMan:\localhost\Client\TrustedHosts -Value "10.1.10.11" -Force
```

И потом:

```powershell
.\scripts\test-m365-sync.ps1
```
