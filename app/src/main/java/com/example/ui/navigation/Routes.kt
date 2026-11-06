package com.example.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val FINANCE = "finance"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{kind}?id={id}"
    val tabs = listOf(HOME, FINANCE, FAVORITES, SETTINGS)
}
