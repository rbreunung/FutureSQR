# Authentication

This is a first sketch of the authentication sequence.

```plantuml
participant server
participant client

client -> server: GET CSRF token
server --> client: JSON with token and session cookie

client -> server: POST credentials with CSRF and cookie
server <-- client: JSON with user details, new session cookie and new CSRF
```
