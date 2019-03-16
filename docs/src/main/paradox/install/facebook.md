# Facebook integration

With this integration you can add `Login with Facebook` button to your application.
We integrated [the original flow](https://developers.facebook.com/docs/facebook-login/manually-build-a-login-flow/).

### Pre-config
Before you start, you need to [register/create your application](https://auth0.com/docs/connections/social/facebook) in the facebook dev console. 
(You should follow the given documentation for the point when you should set your callback url.)
The callback url is the trickiest part.
For local tests you can use `https://lvh.me:9443/facebook/callback` or `https://localtest.me:9443/facebook/callback` (these domains pointed to 127.0.0.1 which will be your local machine).
For production, you want to use your own (tutelar) domain (with the `/facebook/callback` path).

@@@ note

Note the https and 9443 port. 
Facebook only accepst https domains, and also it need a full callback url!

@@@

After you created your new OAuth app on facebook, you will need the appId and appSecret from their site. (You should continue to follow the given documentation to that part.)

### Configuration
You should set the `oauth2.facebook.clientId`, `oauth2.facebook.clientSecret` and `oauth2.facebook.scopes` variables.
The first two cames from the pre-config step (fb call it appId and appSecret). The last one is `public_profile` by default, but you can use anything from the [official list](https://developers.facebook.com/docs/facebook-login/permissions/).
(If you use multiple scopes, you need to separate them with `,` like `a,b,c`.)

Also you should set the `rootUrl` correctly (it is needed for the callbacks). And the `callback.success` and `callback.failure` to handle redirects correctly.

#### Example
Your frontend is on `https://example.com`, your tutelar service is on `https://auth.example.com`.

The rootUrl should be `https://auth.example.com`. The success url is something like `https://example.com/authentication?token=<<TOKEN>>` 
and the failure like `https://example.com/authenticationError/<<ERROR>>` 
    
### Hooks data
The hooks will contain the user name and id from `https://graph.facebook.com/me`.

@@@ note

Currently we won't get back the other fields.
In the near future probably there will be a better support for this.
(Get back the default data, and the email if it is in the scope.)

@@@

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
<a id="facebook-button" class="btn btn-block btn-social btn-facebook">
  <i class="fa fa-facebook"></i> Sign in with Facebook
</a>
```

You should add a `href="https://auth.example.com/facebook/login"` to the `<a>` tag too.

Or if you want to go with the [official](https://developers.facebook.com/docs/facebook-login/web/login-button/) just use that.

 
