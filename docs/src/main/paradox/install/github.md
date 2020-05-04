# Github integration

With this integration you can add `Login with Github` button to your application.
We integrated [the original flow](https://developer.github.com/apps/building-oauth-apps/authorizing-oauth-apps/).

### Pre-config
Before you start, you need to [register/create your application](https://developer.github.com/apps/building-oauth-apps/creating-an-oauth-app/) in your github settings.
The only nontrivial part is the "callback url".
For local tests you can use `https://lvh.me:9443/` or `https://localtest.me:9443` (these domains pointed to 127.0.0.1 which will be your local machine).
For production, you want to use your own (tutelar) domain.

@@@ note

Note the https and 9443 port. 
When this documentation born, only the Github integration worked with simple http. 
You can try to use simple http for local tests but it is an anti-pattern everywhere. 
Try to make your local tests-env to work over https too!

@@@

After you created your new OAuth app on github, you will need the clientId and clientSecret from their site.

### Configuration
You should set the `oauth2.github.clientId`, `oauth2.github.clientSecret` and `oauth2.github.scopes` variables.
The first two cames from the pre-config step. The last one is `read:user` by default, but you can use anything from the [official list](https://developer.github.com/apps/building-oauth-apps/understanding-scopes-for-oauth-apps/#available-scopes).
(If you use multiple scopes, you need to separate them with `,` like `a,b,c`.)

Also you should set the `rootUrl` correctly (it is needed for the callbacks). And the `callback.success` and `callback.failure` to handle redirects correctly.

Furthermore, if you'd like to be able to use the token endpoint for retrieving the user's access tokens, you should set the
`oauth2.github.auth` to the desired authentication method and set the corresponding configurations as well.

#### Example
Your frontend is on `https://example.com`, your tutelar service is on `https://auth.example.com`.

The rootUrl should be `https://auth.example.com`. The success url is something like `https://example.com/authentication?token=<<TOKEN>>` 
and the failure like `https://example.com/authenticationError/<<ERROR>>` 
    
### Hooks data
The hooks will contain the user [profile](https://developer.github.com/v3/users/) from `https://api.github.com/user`.

### Getting more data from the API
You can get the user's access token for Github account by calling the `https://lvh.me:9443/github/token?userId=<<USER_ID>>`
endpoint where the `<<USER_ID>>` query param is the id found in the JWT provided by the user.

### Frontend for registration
Not needed to differenciate your registration and login frontend.

### Frontend for login
The easiest way to create a proper button is using an [already created lib](https://lipis.github.io/bootstrap-social/) for it.

If you want to go with minimal HTML:
```html
<a id="github-button" class="btn btn-block btn-social btn-github">
  <i class="fa fa-github"></i> Sign in with Github
</a>
```

You should add a `href="https://auth.example.com/github/login"` to the `<a>` tag too.
 
### Mobile implementation

@@@ warning

The correct mobile implementation is not yet designed. (Maybe not even possible/needed.)

@@@
