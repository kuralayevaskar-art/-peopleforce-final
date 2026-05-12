# Roadmap

## Phase 1: Foundation

- Monorepo setup
- Backend initialization
- Frontend initialization
- Docker Compose setup
- Base documentation

## Phase 2: Core HR

- Auth and roles
- Employees CRUD
- Departments and positions
- Employee profile page
- Photo upload and crop/position controls
- Department-based employee views
- Organization chart

## Phase 3: Admin and Access Control

- `SUPER_ADMIN`, `HR_ADMIN`, `INTEGRATION_ADMIN`, `MANAGER`, `EMPLOYEE`
- Hide Settings menu from non-admin users
- Protect integration APIs on backend
- Audit log for admin actions

## Phase 4: Active Directory

- AD connection settings for `10.1.10.11`
- Secret file for AD bind credentials
- Login/email generation from full name
- Temporary Windows/AD password generation
- AD user creation
- Department-based group/access assignment
- Send generated credentials to personal email

## Phase 5: Self-Service Registration

- Public/self-service employee form
- Personal data collection
- Personal email confirmation
- Admin approval queue
- Create AD account after approval

## Phase 6: Synology Documents

- Synology root: `Z:\Global\people`
- Employee folder creation by login
- Folders: `agreement`, `photo`, `documents`, `other`
- Upload and replace documents from employee profile
- File metadata in PostgreSQL

## Phase 7: ZKTeco Attendance

- ZKTeco host: `10.1.70.2`
- Credentials in local secret file
- Attendance sync job
- Attendance import logs and error reporting
- Map ZKTeco users to HR employees

## Phase 8: Production Hardening

- Full backend validation
- Email templates
- Integration retry strategy
- Backup and restore plan
- Deployment documentation
