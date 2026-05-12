# Local Secret File Template

Create a real secret file locally and do not commit it to Git.

Recommended path:

```text
backend/src/main/resources/application-secrets.properties
```

Add this file to `.gitignore`.

## Template

```properties
# Active Directory
AD_HOST=10.1.10.11
AD_URL=ldap://10.1.10.11:389
AD_BASE_DN=DC=DMUK,DC=EDU
AD_BIND_USER=
AD_BIND_PASSWORD=
AD_DEFAULT_DOMAIN=dmuk.edu.kz

# ZKTeco
ZKTECO_HOST=10.1.70.2
ZKTECO_DB_NAME=
ZKTECO_USERNAME=
ZKTECO_PASSWORD=

# Synology
SYNOLOGY_HOST=10.1.30.49
SYNOLOGY_ROOT_PATH=Z:\\Global\\people
SYNOLOGY_USERNAME=
SYNOLOGY_PASSWORD=

# Mail
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@dmuk.edu.kz
```

## Important

Never store real passwords in:

- `README.md`
- `docs/*.md`
- frontend files
- committed backend configuration
- Git history

