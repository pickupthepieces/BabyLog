package app.babylog.nativeapp.ui.screens

internal object BabyLogRoutes {
    const val Home = "home"
    const val Timeline = "timeline"
    const val Library = "library"
    const val Settings = "settings"

    fun isTopLevel(route: String): Boolean {
        return route == Home || route == Timeline || route == Library || route == Settings
    }
}
