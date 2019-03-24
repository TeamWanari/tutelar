### build/run/test locally

`make up`: start the dependencies:

| Application   | PORT  |
| ------------- | -----:|
| ReverseProxy  |  9443 |
| Database      |  5432 |
| LDAP          |   389 |
| EmailService  |  9010 |
| SMTP mock UI  |  8025 |

`sbt test it:test`: run all test

`sbt run`: start tutelar (port 9000)

[OPEN](https://lvh.me:9443/index.html) `https://lvh.me:9443/index.html`


### for docs
```
make rundocs
```
