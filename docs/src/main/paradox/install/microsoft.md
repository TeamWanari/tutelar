# Microsoft integration

With this integration you can add `Login with Microsoft` button to your application.
We integrated [the authorization code flow](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow).

### Pre-config
Before you start, you must [register/create your application](https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationsListBlade) in the Azure portal.
The callback url is the trickiest part.
For local tests you can use `https://lvh.me:9443/microsoft/callback` or `https://localtest.me:9443/microsoft/callback` (these domains pointed to 127.0.0.1 which will be your local machine).
For production, you want to use your own (tutelar) domain (with the `/microsoft/callback` path).

@@@ note

Note the https and 9443 port. 
Microsoft only accepts http and https domains, https is the prefered. 
It needs a full callback url!

@@@

After you created your new OAuth2 app on the Azure portal, you will need the application (client) ID and clientSecret.

### Configuration
You should set the `oauth2.microsoft.clientId`, `oauth2.microsoft.clientSecret` and `oauth2.microsoft.scopes` variables.
The first two comes from the pre-config step. The last one is `user.read` by default, but you can use anything from the [official list](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-permissions-and-consent).
(If you use multiple scopes, you need to separate them with `,` like `a,b,c`.)

Also you should set the `rootUrl` correctly (it is needed for the callbacks). And the `callback.success` and `callback.failure` to handle redirects correctly.

Furthermore, if you'd like to be able to use the token endpoint for retrieving the user's access tokens, you should set the
`oauth2.microsoft.auth` to the desired authentication method and set the corresponding configurations as well.

#### Example
Your frontend is on `https://example.com`, your tutelar service is on `https://auth.example.com`.

The rootUrl should be `https://auth.example.com`. The success url is something like `https://example.com/authentication?token=<<TOKEN>>` 
and the failure like `https://example.com/authenticationError/<<ERROR>>` 
    
### Hooks data
The hooks will contain the user [profile](https://docs.microsoft.com/en-us/graph/api/user-get?view=graph-rest-1.0&tabs=http) from `https://graph.microsoft.com/v1.0/me`.

### Getting more data from the API
You can get the user's access token for Microsoft account by calling the `https://lvh.me:9443/microsoft/token?userId=<<USER_ID>>`
endpoint where the `<<USER_ID>>` query param is the id found in the JWT provided by the user.

### Frontend for registration
Not needed to differentiate your registration and login frontend.

### Frontend for login
The easiest way to create a proper button is using an [already created lib](https://lipis.github.io/bootstrap-social/) for it.

If you want to go with minimal HTML:
```html
<a id="microsoft-button" class="btn btn-block btn-social btn-microsoft">
  <i class="fa fa-microsoft"></i> Sign in with Microsoft
</a>
```

You should add a `href="https://auth.example.com/microsoft/login"` to the `<a>` tag too.
 
Or if you want to go with the [official](https://docs.microsoft.com/en-us/azure/active-directory/develop/howto-add-branding-in-azure-ad-apps) just use that.

### Mobile implementation

@@@ warning

The correct mobile implementation is not yet designed.

@@@
