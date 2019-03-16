# Just identity check

This is the easiest case. 
You want a basic ok/nope authentication.
Or maybe you have a special on-boarding process which works with only a remote id.

### Useable with:
 - email auth
 - user+pass auth
 - google/fb/github
 - ldap
 
### Needed hook implementations:
 - you can skip the hook implementations
 
### Registration process on frontend:
 - See the choosen auth method documentation!

### Registration process on backend:
 - Your registration process only handled by tutelar.
 
### Login process on frontend:
 - See the choosen auth method documentation!
 
### Login process on backend:
 - You don't need to implement anything.
 
### After registration:
 - Your backend should check the JWT in the `Authorization` header
     - You can ensure that the user is authenticated
 - You can check if the `id` in the jwt is in your db or not, if not, you can start a special onboarding process
     - get nick, favorite color etc.
     - this method kills the porpoise of using JWT, if you need this type of functionality dive into the authority-handling use-case
 - You can track the user by the `id` in the JWT
     - You know if the same user tries to upvote something twice
