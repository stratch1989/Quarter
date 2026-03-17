package com.example.quarter

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform