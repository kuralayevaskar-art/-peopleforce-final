# Database Schema

## Overview
The platform uses PostgreSQL 16 with UUID (v4) as primary keys. All entities inherit from `BaseEntity` providing `id`, `created_at`, and `updated_at` fields.

## Tables
- `companies`: Main company information and settings.
- `roles`: RBAC roles (SUPER_ADMIN, HR_ADMIN, etc.).
- `users`: Authentication accounts.
- `user_roles`: Many-to-many join table for users and roles.
- `employees`: Core employee profiles.
- `departments`: Organizational units.
- `positions`: Job roles within departments.
- `document_types`: Configuration for document requirements.
- `files_metadata`: Tracking for uploaded files.
- `employee_documents`: Links employees to their documents.
- `employee_requests`: Leave, sick leave, and other requests.
- `attendance_logs`: Daily attendance records.
- `integration_settings`: Non-secret settings for AD, ZKTeco, Synology, and mail.
- `employee_registration_requests`: Self-service requests waiting for admin approval.
- `ad_account_provisioning_logs`: AD user creation attempts and results.
- `external_attendance_sync_runs`: ZKTeco sync runs and errors.
- `department_access_rules`: Department-to-AD-group access mapping.

## Enums
- `CompanyStatus`: ACTIVE, BLOCKED, ARCHIVED
- `UserStatus`: ACTIVE, BLOCKED, DISABLED
- `EmployeeStatus`: ACTIVE, ON_PROBATION, ON_LEAVE, BLOCKED, DISMISSED, ARCHIVED
- `EmploymentType`: FULL_TIME, PART_TIME, CONTRACTOR, INTERN
- `DirectoryStatus`: ACTIVE, ARCHIVED
- `DocumentStatus`: ACTIVE, EXPIRED, DELETED
- `RequestType`: LEAVE, SICK_LEAVE, REMOTE_WORK, BUSINESS_TRIP, DOCUMENT, OTHER
- `RequestStatus`: DRAFT, SUBMITTED, APPROVED_BY_MANAGER, APPROVED_BY_HR, REJECTED, CANCELLED, COMPLETED
- `AttendanceStatus`: PRESENT, LATE, ABSENT, REMOTE, ON_LEAVE, SICK_LEAVE, BUSINESS_TRIP, DAY_OFF
- `AttendanceSource`: MANUAL, IMPORT, DEVICE, API

## Planned Integration Data

### Active Directory

Do not store AD bind password in database unless encrypted secret storage is implemented. Store only non-secret settings such as host, base DN, filters, and enabled flags.

Planned fields:

- `ad_host`: `10.1.10.11`
- `ad_url`: `ldap://10.1.10.11:389`
- `ad_base_dn`
- `default_domain`: `dmuk.edu.kz`
- `sync_enabled`

### ZKTeco

Do not store ZKTeco password in normal tables. Store connection metadata and sync state.

Planned fields:

- `zkteco_host`: `10.1.70.2`
- `sync_enabled`
- `last_sync_at`
- `last_success_at`
- `last_error`

### Synology

Store file metadata in PostgreSQL and actual files on Synology.

Planned root:

```text
Z:\Global\people
```

Employee folder example:

```text
Z:\Global\people\a.kuralayev\agreement
Z:\Global\people\a.kuralayev\photo
Z:\Global\people\a.kuralayev\documents
Z:\Global\people\a.kuralayev\other
```

### Department Access Rules

Each department can map to one or more AD groups. During AD user creation the system assigns the user to groups based on these rules.

## Seed Data
The database is initialized with:
- Default roles.
- A "Demo Company".
- Demo users with password `Admin123!`.
- Initial organizational structure (HR, IT, Sales, Finance).
