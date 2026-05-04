# Deployment Runtime Guide

This document covers the minimum runtime setup for the deployed server.

## 1. Environment File

The systemd service reads:

```text
/etc/plate-main.env
```

Use this template:

- [plate-main.env.example](C:/workspace/plate-main/deploy/plate-main.env.example)

Required values:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APPLE_CLIENT_ID`
- `GOOGLE_CLIENT_ID`

Minimum example:

```bash
DB_URL=jdbc:postgresql://your-db-host:5432/your-db-name
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password
JWT_SECRET=replace-with-32-bytes-or-more-secret
APPLE_CLIENT_ID=your-apple-client-id
GOOGLE_CLIENT_ID=your-google-client-id
```

## 2. Common 502 Cause

If the client gets `502`, the most likely cause is:

1. deployment succeeded
2. systemd restarted the app
3. Spring failed during boot
4. reverse proxy could not reach backend `:8090`

Typical causes:
- missing `JWT_SECRET`
- missing `DB_PASSWORD`
- missing `APPLE_CLIENT_ID`
- missing `GOOGLE_CLIENT_ID`
- DB connection failure

## 3. Server Checks

Run these on the server:

```bash
sudo systemctl status plate-main -l --no-pager
sudo journalctl -u plate-main -n 200 --no-pager
sudo cat /etc/plate-main.env
curl -i http://127.0.0.1:8090/api/health
```

## 4. Deployment Validation

CodeDeploy validation now checks:

1. `systemctl is-active plate-main`
2. `http://127.0.0.1:8090/api/health`

If health check fails, the script prints:
- `systemctl status plate-main`
- recent `journalctl` logs

## 5. Recommended Apply Steps

1. Copy example file and fill values
2. Save it as `/etc/plate-main.env`
3. Restart service

```bash
sudo cp /opt/plate-main/deploy/plate-main.env.example /etc/plate-main.env
sudo vi /etc/plate-main.env
sudo systemctl restart plate-main
sudo systemctl status plate-main -l --no-pager
curl -i http://127.0.0.1:8090/api/health
```
