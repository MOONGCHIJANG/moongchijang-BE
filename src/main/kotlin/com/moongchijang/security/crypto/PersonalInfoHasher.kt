package com.moongchijang.security.crypto

interface PersonalInfoHasher {
    fun hash(value: String): String
}
