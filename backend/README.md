# NoteShare Backend (Spring Boot)

REST API for a same-network note-sharing app.

## Requirements
- Java 17+ (project targets Java 25)
- Maven
- XAMPP with MySQL running on port 3306 (default user `root`, empty password)

## Run
1. Start MySQL in XAMPP.
2. From this folder: `mvn spring-boot:run`
3. The API runs at http://localhost:8080

The database `noteshare` is created automatically. On first run, two extra tables
are created alongside `users`/`notes`/`note_shares`: `auth_tokens`, which now
backs login sessions (see "Auth" below).

## Endpoints
- POST /api/register  {username, password}  -> {token}
- POST /api/login     {username, password}  -> {token}
- POST /api/logout    (Authorization: Bearer <token>) -> invalidates the token
- GET  /api/notes     (Authorization: Bearer <token>) [?q=search-term]
- POST /api/notes  {title, content, visibility}  (visibility is optional: "PUBLIC" or
  "PRIVATE", defaults to PRIVATE)
- PUT  /api/notes/{id} {title, content, visibility}  (visibility optional - omit to
  leave it unchanged)
- DELETE /api/notes/{id}
- POST /api/notes/{id}/share {username}  (username must already be your friend)
- GET  /api/notes/{id}/shares            -> usernames the note is shared with (owner only)
- DELETE /api/notes/{id}/share/{username} -> stop sharing with a user (owner only)
- GET  /api/users (Authorization: Bearer <token>)
- DELETE /api/account {password}  -> permanently deletes the account: your notes,
  your shares of others' notes, all your login sessions, and the account itself.
  Requires your current password as confirmation.

### Friends
- GET  /api/friends              -> usernames of your accepted friends
- GET  /api/friends/requests     -> {incoming: [...], outgoing: [...]} pending requests
- POST /api/friends/requests {username}         -> send a friend request (auto-accepts
  if that person already sent you one)
- POST /api/friends/requests/{username}/accept  -> accept an incoming request
- DELETE /api/friends/requests/{username}       -> cancel a request you sent, or
  decline one sent to you
- DELETE /api/friends/{username}                -> remove an existing friend

Notes can only be shared with friends now - `NoteService.share()` checks
`FriendService.areFriends()` before creating a share, so the friend list and
the sharing feature are actually connected rather than parallel features.

### Feed
- GET /api/feed -> your own notes, all of your friends' notes (public or
  private), and every PUBLIC note from anyone else on the network whose
  *account* is also Public - merged and sorted by most-recently-updated.

### Account visibility
- GET /api/account/visibility -> {visibility: "PUBLIC" | "PRIVATE"}
- PUT /api/account/visibility {visibility} -> updates it, returns the new value

This is a second, account-level visibility switch on top of the per-note
one, modeled on how a private social account works: with your account set
to **Private**, your notes are only ever visible to your friends in the
feed - even ones you've individually marked Public. With your account set
to **Public** (the default), a note's own Public/Private flag is what
decides whether strangers can see it. Friends can always see all of your
notes either way, regardless of both settings - that hasn't changed.

Important: the feed grants friends read access to **all** of your notes, not
just ones you've explicitly shared via `POST /api/notes/{id}/share`. Sharing
still exists as a separate, narrower mechanism (adds someone to a note's
share panel specifically), while the feed is the broader "friends can see
what I'm posting" visibility layer, similar to a social feed.

`GET /api/notes` responses now include `owner` (bool) and `ownerUsername`, so the
frontend can distinguish "my notes" from "shared with me".

## Auth
- Passwords are hashed with BCrypt (previously unsalted SHA-256).
- Tokens are stored in the `auth_tokens` table with a 24-hour expiry, instead of
  an in-memory map — a backend restart no longer logs everyone out.
- `POST /api/logout` invalidates the current token server-side.

## CORS
Previously `@CrossOrigin(origins = "*")` on every controller. Now configured
centrally in `WebConfig` to allow `localhost`/`127.0.0.1` plus common private
network ranges (192.168.x.x, 10.x.x.x, 172.16-31.x.x) on any port, since this
app is meant to be reached from other devices on the same Wi-Fi. This is
narrower than "any origin" while still working for its intended use case.

## Same-network access
Find your Mac's IP (`ipconfig getifaddr en0` on macOS) and call
http://<your-ip>:8080 instead of localhost. The frontend now auto-detects the
right API host from whatever hostname served the page, so you shouldn't need
to set `localStorage.noteshare-api` manually anymore (see frontend README).

## Tests
`mvn test` runs unit tests for `AuthService` (hashing, token expiry, duplicate
usernames) and `NoteService` (ownership checks, share/unshare, search).
