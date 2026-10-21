package com.example.hw_2

import kotlin.random.Random

object PasswordGenerator {
    fun generatePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val length = 10

        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}