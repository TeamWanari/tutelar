# LDAP integration

With this integration you can add a user/pass form which will authenticate from LDAP/AD.

### Pre-config
Before you start, you need a lot of info about your LDAP server.
 - Need the url of the server ex.: `ldap://localhost:389`
 - Need a readonly user with namespace ex.: `cn=readonly,dc=example,dc=com`
 - Need a password of that user
 - Need a search base domain ex.: `ou=users,dc=example,dc=com`
 - Need a search attribute ex.: `cn`
 - Need a return attribute list ex.: `cn,sn,givenName`
 - Need an array type return attribute list ex.: `memberof`
 
In this example the tutelar service will log in to the LDAP server with the read-only user.
When a user try to authenticate with the user/pass combination, at first we try to find the user in the basedomain with the given search attribute.
(If the given username is `test` we try to find the `cn=test` user in the `ou=users,dc=example,dc=com` domain.)
We ask the LDAP to validate if the found user has the same password as we get.
We get the listed `cn,sn,givenName,memberof` attributes, and insert them to the login hook.


### Configuration
You should set the gathered variables to these:
```
ldap.url
ldap.readonlyUserWithNamespace
ldap.readonlyUserPassword
ldap.userSearchBaseDomain
ldap.userSearchAttribute
ldap.userSearchReturnAttributes
ldap.userSearchReturnArrayAttributes
```
   
### Hooks data
The hooks will contain the search return attributes (both the single and plural ones).

### Getting more data from the API
You have the option to get back all the data you need. 
So we don't have api to get more info from a given user.

### Frontend for registration
Not needed to differenciate your registration and login frontend. 
(We don't really have registration phase, the registration when the users get into to the given LDAP/AD)

### Frontend for login
You should create a simple username/password form. 
On submit you should send a request to the tutelar service, and based on the response you get, you should handle the successful/unsuccessful login.
(For concrete examples see the example implementations. For concrete documentation see the swagger document.)

### Mobile implementation
Same as the frontend.
