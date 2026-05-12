# Integrations and External Systems

This document describes the planned integrations for the HR PeopleOps platform.

## ZKTeco Attendance

Attendance data will be imported from the ZKTeco platform.

- Host: `10.1.70.2`
- Purpose: read attendance events and synchronize them into `attendance_logs`
- Source type: external platform database or API, depending on available access
- Target module: Attendance

Credentials must not be stored in source code. Put them in a local secret file and keep that file out of Git.

Recommended local template:

```properties
ZKTECO_HOST=10.1.70.2
ZKTECO_DB_NAME=
ZKTECO_USERNAME=
ZKTECO_PASSWORD=
ZKTECO_SYNC_ENABLED=false
```

## Active Directory

The platform will create and synchronize users in Active Directory.

- AD server: `10.1.10.11`
- Purpose: create domain accounts, read employee directory data, assign department-based access
- Admin-only module: Settings / Integrations
- Normal users must not see the Settings menu

Credentials must be stored outside the repository.

Recommended local template:

```properties
AD_HOST=10.1.10.11
AD_URL=ldap://10.1.10.11:389
AD_BASE_DN=DC=DMUK,DC=EDU
AD_BIND_USER=
AD_BIND_PASSWORD=
AD_DEFAULT_DOMAIN=dmuk.edu.kz
AD_SYNC_ENABLED=false
```

## Account Creation Flow

HR/Admin enters the employee full name and department. The system should:

1. Generate username from name.
2. Generate corporate email.
3. Generate a Windows-compatible temporary password.
4. Create the AD user.
5. Assign access based on the employee department.
6. Send credentials to the employee personal email.
7. Create the employee profile in the HR platform.
8. Create the employee folder structure on Synology.

Example:

- Full name: `Куралаев Аскар Акатаевич`
- Generated login: `a.kuralayev`
- Generated email: `a.kuralayev@dmuk.edu.kz`
- Personal email: entered by employee or HR

Password rules must follow the local Windows 11 / AD password policy. The exact policy should be configured by the administrator.

## Self-Service Registration

New employees must be able to open a separate public/self-service page and fill in their own data.

Planned data:

- Full name
- Personal email
- Phone
- Department or invite code
- Documents
- Photo

After submission, the request must wait for admin approval before AD account creation.

## Synology Employee Storage

Employee files will be stored on Synology.

- Synology host: `10.1.30.49`
- Windows mapped drive: `Z:`
- Root path: `Z:\Global\people`

Folder naming uses the generated login.

Example for `Куралаев Аскар`:

```text
Z:\Global\people\a.kuralayev
Z:\Global\people\a.kuralayev\agreement
Z:\Global\people\a.kuralayev\photo
Z:\Global\people\a.kuralayev\documents
Z:\Global\people\a.kuralayev\other
```

The platform must support uploading, viewing, and replacing files in these folders from the employee profile page.

## Roles

Required roles:

- `SUPER_ADMIN`: full system access
- `HR_ADMIN`: employees, departments, positions, documents, attendance
- `INTEGRATION_ADMIN`: AD, ZKTeco, Synology settings and sync jobs
- `MANAGER`: view own department and direct reports
- `EMPLOYEE`: view and edit allowed personal data only

Only admins may see Settings and integration screens.

