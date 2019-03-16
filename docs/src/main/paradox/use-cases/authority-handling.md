# Authority handling

You want to keep track if the user can do things on your site or not (without hitting the db).


### Useable with:
 - email auth
 - user+pass auth
 - google/fb/github
 - ldap
 
### Needed hook implementations:
 - you can skip the registration hook implementation
 - you need to implement the login hook
 
### Registration process on frontend:
 - See the choosen auth method documentation!

### Registration process on backend:
 - Your registration process only handled by tutelar.
 
### Login process on frontend:
 - See the choosen auth method documentation!
 
### Login process on backend:
 - You need to implement a login callback
 - The callback should return the data you want to get back in the JWT
   - for example if you want to get back the name of the user, you should provide the name
   - if you want to add authorities you can add them as a string or a list of strings or anything suitable for you
 
### After registration:
 - Your backend should check the JWT in the `Authorization` header
     - You can ensure that the user is authenticated
     - You will get back the data you provided in the login-hook
 - You can build custom on-boarding if you add flags to the jwt with the hook
     - for example: `regOk: false` We should do the onboarding process
     - if `name` attribute is missing we should start the onboarding process
 - If you modify values in your db, which is in the JWT too, you need to request a new JWT from the tutelar service and tell the new token to your frontend
 - Because of the data in the JWT you don't need to query the user from the db in every request
