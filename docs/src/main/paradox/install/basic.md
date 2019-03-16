# Basic user/pass

With this integration you can add a user/pass authentication.

### Pre-config
You don't need anything.

### Configuration
You don't need anything.
   
### Hooks data
The registration hook will provide the input "data" attribute without modification. 

The login hook will only contain the inner id. (WIP to contain the username too.)

### Getting more data from the API
We have no more data.

### Frontend for registration
You have two options. 

A simple registration form with a username+password field (possibly with client side "is this password strong" test).
You just need to send these informations to the tutelar service, and if the user is unique it will register the user.

A multifield registration form with extra informations. 
With that, you should wrap the other values to a "data" object, and your backend will get them if the registration is happening.

(For concrete examples see the example implementations. For concrete documentation see the swagger document.)

### Frontend for login
You should create a simple username/password form. 
On submit you should send a request to the tutelar service, and based on the response you get, you should handle the successful/unsuccessful login.
(For concrete examples see the example implementations. For concrete documentation see the swagger document.)

### Mobile implementation
Same as the frontend.
