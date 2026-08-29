# NoteShare - Same-Network Note Sharing App

A full-stack note-sharing app that runs on your local Wi-Fi.

## Structure
- `backend/`  Spring Boot REST API + MySQL (XAMPP)
- `frontend/`  Static browser client for the NoteShare API

## How it works
Devices on the same Wi-Fi connect to your Mac's IP where Spring Boot runs,
which reads/writes MySQL. Notes can be shared between users from the browser client.

## Quick start
1. Start MySQL in XAMPP.
2. Start the backend: `cd backend && mvn spring-boot:run`
3. Start the frontend in another terminal: `cd frontend && python3 -m http.server 5173`
4. Open http://localhost:5173 in a browser.

The frontend auto-detects the backend from whatever host served the page (so
opening it via your Mac's LAN IP on your phone just works). To override this,
set `localStorage.noteshare-api` in the browser before loading the app.

## What's new
- Passwords are hashed with BCrypt; login tokens are stored in the database
  with a 24-hour expiry (a backend restart no longer logs everyone out).
- CORS is restricted to localhost + common private network ranges instead of
  allowing any origin.
- `GET /api/users` now requires authentication.
- Notes can be unshared, and you can see who a note is currently shared with.
- The notes list distinguishes "My notes" from "Shared with me", and supports
  search and sorting.
- A banner appears if the frontend can't reach the backend.
- Unit tests cover `AuthService`, `NoteService`, and `AccountService`.
- Accounts can be permanently deleted (with password confirmation) from the
  top bar — this removes your notes, your shares, and every login session.
- Visual redesign: a warm paper/pine palette instead of the earlier
  cream-and-coral look, a serif display face (Fraunces) for headlines, and a
  consistent per-person avatar "pin" system used in the sidebar, note owner
  badges, and share chips so you can recognize who's who at a glance.
- Added a friend system: send/accept/decline friend requests, remove a
  friend, and see who's on the network. Notes can now only be shared with
  friends (not anyone who happens to be registered), so the two features are
  actually connected.
- The app is now split into three navigable layers - **Notes**, **Feed**, and
  **Friends** - via a top-level tab nav, instead of one flat page with
  everything mixed together.
- Added a Feed: a combined view of your own notes, all of your friends'
  notes (public or private), and public notes from anyone else on the
  network. Notes can be marked Public or Private when creating/editing them
  (Private by default) - this only affects whether strangers can see a note
  in their feed; friends can always see all of each other's notes there,
  separately from the explicit note-sharing system.
- Added an account-level **Private account** toggle (topbar), on top of the
  per-note flag - modeled on a private social account: with your account
  set to Private, your notes are only ever visible to friends in the feed,
  even ones marked Public. Friends always see everything either way; only
  strangers are affected by this setting.

See [backend/README.md](backend/README.md) for full API details.
