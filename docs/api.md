# API Documentation

## Base URL
`http://localhost:8080/api/v1`

## Response Format

### Success Response
```json
{
  "success": true,
  "data": { ... },
  "message": "Optional success message",
  "timestamp": "2026-05-04T12:00:00"
}
```

### Error Response
```json
{
  "success": false,
  "error": "ERROR_CODE",
  "message": "Human readable message",
  "details": ["Detailed error 1", "Detailed error 2"],
  "timestamp": "2026-05-04T12:00:00"
}
```

## Endpoints

### Health
`GET /health`
Returns the current status of the service.

### Authentication

#### Login
`POST /auth/login`
Authenticates a user and returns access and refresh tokens.

**Request:**
```json
{
  "email": "admin@demo.com",
  "password": "Admin123!"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "ey...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": "...",
      "companyId": "...",
      "email": "admin@demo.com",
      "roles": ["SUPER_ADMIN"]
    }
  }
}
```

#### Refresh Token
`POST /auth/refresh`
Obtains a new access token using a refresh token.

**Request:**
```json
{
  "refreshToken": "..."
}
```

#### Logout
`POST /auth/logout`
Revokes a refresh token.

**Request:**
```json
{
  "refreshToken": "..."
}
```

#### Get Current User
`GET /auth/me`
Returns the currently authenticated user's profile.

**Header:** `Authorization: Bearer <accessToken>`

## Planned Admin and Integration Endpoints

These endpoints describe the intended API surface for the next implementation phases.

### AD Integration

`GET /admin/integrations/ad/settings`

Returns AD settings without exposing passwords.

`PUT /admin/integrations/ad/settings`

Saves AD host, base DN, filters, and sync options. Passwords must be stored through the secret file or secure secret storage.

`POST /admin/integrations/ad/test`

Tests connection to AD server `10.1.10.11`.

`POST /admin/integrations/ad/create-user`

Creates an AD user from an approved employee profile.

Request:

```json
{
  "employeeId": "...",
  "fullName": "Куралаев Аскар Акатаевич",
  "departmentId": "...",
  "personalEmail": "personal@example.com"
}
```

Expected generated values:

```json
{
  "login": "a.kuralayev",
  "corporateEmail": "a.kuralayev@dmuk.edu.kz",
  "temporaryPasswordGenerated": true
}
```

### Self-Service Registration

`POST /self-service/registration`

Creates a pending employee registration request.

`GET /admin/registration-requests`

Admin list of pending requests.

`POST /admin/registration-requests/{id}/approve`

Approves request and starts AD account creation.

### ZKTeco Attendance

`GET /admin/integrations/zkteco/settings`

Returns ZKTeco settings without exposing passwords.

`POST /admin/integrations/zkteco/test`

Tests connection to ZKTeco host `10.1.70.2`.

`POST /admin/integrations/zkteco/sync`

Starts attendance sync.

### Synology Files

`POST /employees/{employeeId}/folders/create`

Creates employee folders under `Z:\Global\people\<login>`.

`POST /employees/{employeeId}/files`

Uploads a file to one of the allowed folders: `agreement`, `photo`, `documents`, `other`.

`GET /employees/{employeeId}/files`

Lists employee files.

`DELETE /employees/{employeeId}/files/{fileId}`

Deletes or archives employee file metadata and removes the file when allowed.
