package com.zhufucdev.practiso

interface Aead {
    fun encrypt(plainText: ByteArray): ByteArray
    fun decrypt(cipherText: ByteArray): ByteArray
}