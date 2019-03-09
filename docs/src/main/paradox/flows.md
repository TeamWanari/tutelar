# Flows

## Registration

@@@ seqence-diagram
```raw
Client->frontend: credentials, userData
frontend->tutelar: doRegistration( \\n credentials, userData)
tutelar->backend: newRegistration(userData)
backend-->tutelar: additionalData
Note right of backend: optional hook \\n your application can log things \\n can add additional data like authorizations
tutelar-->frontend: success
frontend-->Client: success
```
@@@

## Authentication

@@@ seqence-diagram
```raw
Client->frontend: credentials
frontend->tutelar: doAuthentication(credentials)
Note right of tutelar: accepts or denies
tutelar->backend: newUserLogin(user)
Note right of backend: optional hook \\n your application can log things
tutelar-->frontend: user token
frontend-->Client: successful login
```
@@@

## Authorization

@@@ seqence-diagram
```raw
Client->frontend: do sth
frontend->backend: sth(token)
Note right of backend: with shared secrets \\n the backend can do authorization
backend-->frontend: success
frontend-->Client: success
```
@@@
