# Developer Troubleshooting Guide

**Project:** RESTful Housekeeping  
**Audience:** Developers running the Expo frontend and Spring Boot backend locally

This guide covers common development problems and how to check them. For installation and first-time setup, see the main [README](../README.md).

## Quick checks before debugging

1. Confirm the Spring Boot backend is running. For a normal local run, its API port is **50000**.
2. Confirm Expo is running in a separate terminal and the Android emulator is booted.
3. Make sure you are using matching frontend and backend branches or commits when testing a feature that changes API behavior.
4. Read the **first** error message in the Expo terminal and backend terminal. An HTTP status code gives a clue, but it does not prove the root cause.
5. If you changed branches, check `git status` before pulling or installing dependencies.

## 1. Frontend cannot connect to the backend

**Symptoms:** `Network Error`, request timeout, failed login, or API requests that never receive a response.

**Checks:**

- Start the backend from `backend/`:

  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

- Check that the backend is listening on port **50000**. You can also try `http://localhost:50000/swagger-ui/index.html` on your computer.
- Open `frontend/api/apiClient.ts` and verify `API_BASE_URL` matches how you are running the app:

  | Environment | Address |
  | --- | --- |
  | Android emulator | `http://10.0.2.2:50000` |
  | iOS simulator | `http://127.0.0.1:50000` |
  | Physical phone | `http://<computer-LAN-IP>:50000` |

- The physical phone and computer must be on the same accessible network. `localhost` inside the Android emulator refers to the emulator, **not** your computer.
- If using **Docker Compose**, the documented host API port is **8080** instead of 50000. Update the API URL accordingly.

**Do not** change the API URL randomly; identify which backend instance is actually running.

## 2. HTTP 401 or 403 after login or switching branches

**Symptoms:** The app shows `401 Unauthorized` or `403 Forbidden` when loading a screen.

- `401` often means the token is missing, expired, or invalid. The frontend's API client has a `401` handler that can clear the session.
- `403` means the server refused access. Possible causes include role/permission restrictions, a stale test session, or an endpoint mismatch between branches. It is **not always** an expired-token problem.

**Steps:**

1. Check the request URL and status in the Expo terminal.
2. Check the matching backend request/error logs.
3. Log out and log in again, particularly after restarting the local H2 backend.
4. Confirm the account role is allowed to use the requested endpoint.
5. If testing a PR, confirm that the frontend and backend folders use compatible changes from the same combined repository/branch.
6. If the issue continues on current `main`, record the endpoint, role, steps to reproduce, and logs for a Jira ticket.

Never include JWTs, passwords, or full authentication responses in screenshots or tickets.

## 3. Android emulator does not launch or Expo does not open the app

**Symptoms:** Expo waits for an emulator, the emulator freezes, or opening Android times out.

1. Open Android Studio's **Device Manager** and start the emulator manually.
2. Make sure the emulator has finished booting before launching the app.
3. In a terminal inside `frontend/`, start Expo:

   ```powershell
   npm start
   ```

4. Press `a` in the Expo terminal to open Android, or use `npm run android`.
5. If Metro seems to serve stale files, stop Expo and try:

   ```powershell
   npx expo start -c
   ```

6. If necessary, use **Cold Boot** in Android Studio's Device Manager, then try again.

An Expo package update notice is not necessarily the cause of a launch failure. Avoid upgrading dependencies during troubleshooting unless the team approves the change.

## 4. The H2 database loses newly created data

**Symptoms:** Accounts, invitations, properties, or cleanings disappear after restarting the backend.

This is **expected** for local H2. The default profile uses `jdbc:h2:mem:testdb` and `spring.jpa.hibernate.ddl-auto=create-drop`; the database is recreated when the backend restarts. Local sample data is loaded again.

To inspect data while the backend is still running:

1. Open `http://localhost:50000/h2-console`.
2. Use JDBC URL `jdbc:h2:mem:testdb`, username `sa`, and an empty password.
3. Run read-only SQL to verify data. For example:

   ```sql
   SELECT ID, USERNAME, ROLE, ORGANIZATION_ID FROM _USER;
   ```

**Warning:** Avoid editing or deleting database records during normal testing. If you deliberately adjust local test data, document the change and remember it will be lost after a restart.

For persistent data, consult `backend/README.Docker.md` and the MySQL setup. Do not change the application profile just to work around a missing UI update.

## 5. Invitations fail because email cannot be sent

**Symptoms:** Sending an invitation fails with an email/SMTP message; Mailpit has no new message.

The local backend is configured to send test mail to `localhost:1025`.

1. Make sure Mailpit is installed and **running** in a separate terminal.
2. Check that Mailpit is listening for SMTP on port **1025**.
3. Open Mailpit's web interface, normally `http://localhost:8025`.
4. Retry the invitation and inspect the backend logs and Mailpit inbox.

Mailpit captures **test emails** locally; they are not delivered to a real external inbox. If port 1025 is unavailable or another service uses it, investigate the local SMTP configuration before changing code.

## 6. Photos do not upload or load

**Symptoms:** Profile/cleaning photo uploads fail or private cleaning photos do not open.

The backend uses ImageKit. The project README says you can put `IMAGEKIT_PRIVATE_KEY=your-private-key` (without quotes) in `backend/.env`, or set the value in the backend process environment. The Spring Boot configuration reads `${IMAGEKIT_PRIVATE_KEY:}`, so if the `.env` method does not work, check whether that file is actually being loaded in your run environment. Setting the variable in the backend process environment is the reliable fallback.

- Check the backend error response and logs.
- Confirm the key is available to the running backend process. If using `backend/.env`, make sure it is not committed to Git and that your launch method loads it. Do not paste the key into Jira.
- Restart the backend after setting the environment variable.
- Verify that unrelated API requests still work, so you can separate a photo-service problem from a general connection problem.

Never expose the ImageKit private key in a screenshot, README, or support ticket.

## 7. Docker/MySQL behaves differently from local H2

**Symptoms:** The app connects locally but fails when using Docker, or data behaves differently between environments.

- A normal local backend run uses H2 on port **50000**. The Docker Compose setup uses MySQL and exposes the API on port **8080**.
- If the frontend cannot reach Docker, verify `frontend/api/apiClient.ts` points to the correct host/port for your device or emulator.
- The Docker environment uses a persistent MySQL volume. `docker compose down` stops containers without deleting that volume; **`docker compose down --volumes` deletes its stored data**.
- For Docker credentials and Render deployment details, follow `backend/README.Docker.md`. Never post database passwords or service secrets in screenshots or Jira.
- If an API works in H2 but not MySQL, capture the exact backend error and identify which Spring profile is active before changing the database.

## 8. Git or dependency problems after pulling changes

**Symptoms:** Imports fail, modules cannot be found, the app behaves differently from a teammate's branch, or Git refuses to switch branches.

1. Check the active branch and working tree:

   ```bash
   git status
   git branch --show-current
   ```

2. Preserve any uncommitted changes before switching branches. Do not discard teammates' work.
3. Pull from the intended branch only after confirming your working tree is safe.
4. If frontend dependencies changed, from `frontend/` run:

   ```bash
   npm ci
   ```

5. If the app still uses cached Metro output, restart Expo with `npx expo start -c`.
6. If the backend build fails, read the first Maven compiler/test error. Do not assume a successful command in the other terminal means this service also started.

Do not delete lockfiles, update all packages, or run Git reset commands without understanding the impact.

## 9. Chat connection or messages do not appear

The project uses WebSockets/STOMP for messaging.

1. Confirm login and normal REST API requests work first.
2. Check whether the WebSocket connection attempt succeeds in the frontend logs.
3. Check whether the backend is running and whether its WebSocket authentication rejects the connection.
4. Test with two valid accounts in the same expected conversation and check that the message appears after reopening the chat.
5. Record the participating roles, timestamp, exact symptom, and relevant sanitized logs if the issue persists.

---

**Related documentation:** [Project README](../README.md) · [Docker and Render setup](../backend/README.Docker.md)
