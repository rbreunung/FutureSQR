# Authentication

## Sequences

This is a first sketch of the authentication sequence.

```plantuml
participant server
participant client

client -> server: GET CSRF token
server --> client: JSON with token and session cookie

client -> server: POST credentials with CSRF and cookie
server <-- client: JSON with user details, new session cookie and new CSRF
```

All POST queries require the CSRF token to be sent as a URL-encoded parameter or as a header attribute. After login a new session cookie and CSFR token are generated

## Implementations

## Authentication Success

Currently the default mechanism is configured by SecurityConfiguration.java. If a different behavior is required, then an AuthenticationSuccessHandler implementation will be required.
