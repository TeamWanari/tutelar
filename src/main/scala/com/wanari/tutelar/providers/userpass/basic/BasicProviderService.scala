package com.wanari.tutelar.providers.userpass.basic

import com.wanari.tutelar.providers.userpass.UserPassService

trait BasicProviderService[F[_]] extends UserPassService[F]
