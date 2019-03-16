# Google integration

With this integration you can add `Login with Google` button to your application.
We integrated [the original flow](https://developers.google.com/identity/protocols/OpenIDConnect).

### Pre-config
Before you start, you need to [register/create your application](https://support.google.com/googleapi/answer/6158849) in google dev console.
The callback url is the trickiest part.
For local tests you can use `https://lvh.me:9443/google/callback` or `https://localtest.me:9443/google/callback` (these domains pointed to 127.0.0.1 which will be your local machine).
For production, you want to use your own (tutelar) domain (with the `/google/callback` path).

@@@ note

Note the https and 9443 port. 
Google only accepst http and https domains, https is the prefered. 
It need a full callback url!

@@@

After you created your new OAuth2 app on the google dev console, you will need the clientId and clientSecret.

### Configuration
You should set the `oauth2.google.clientId`, `oauth2.google.clientSecret` and `oauth2.google.scopes` variables.
The first two cames from the pre-config step. The last one is `openid,email,profile` by default, but you can use anything from the [official list](https://developers.google.com/identity/protocols/googlescopes).
(If you use multiple scopes, you need to separate them with `,` like `a,b,c`.)

Also you should set the `rootUrl` correctly (it is needed for the callbacks). And the `callback.success` and `callback.failure` to handle redirects correctly.

#### Example
Your frontend is on `https://example.com`, your tutelar service is on `https://auth.example.com`.

The rootUrl should be `https://auth.example.com`. The success url is something like `https://example.com/authentication?token=<<TOKEN>>` 
and the failure like `https://example.com/authenticationError/<<ERROR>>` 
    
### Hooks data
The hooks will contain the user [profile](https://developers.google.com/apis-explorer/#p/oauth2/v2/oauth2.userinfo.get?_h=2&) from `https://www.googleapis.com/oauth2/v2/userinfo`.

### Getting more data from the API

@@@ warning

Currently there is no possible way to get back the auth token from tutelar.
In the near future probably there will be an api for it.

@@@

### Frontend for registration
Not needed to differenciate your registration and login frontend.

### Frontend for login
The easiest way to create a proper button is using an [already created lib](https://lipis.github.io/bootstrap-social/) for it.

If you want to go with minimal HTML:
```html
<a id="google-button" class="btn btn-block btn-social btn-google">
  <i class="fa fa-google"></i> Sign in with Google
</a>
```

You should add a `href="https://auth.example.com/google/login"` to the `<a>` tag too.
 
Or if you want to go with the [official](https://developers.google.com/identity/branding-guidelines) just use that.
