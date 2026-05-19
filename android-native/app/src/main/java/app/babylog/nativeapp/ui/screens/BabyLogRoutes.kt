package app.babylog.nativeapp.ui.screens

internal object BabyLogRoutes {
    const val Home = "home"
    const val Timeline = "timeline"
    const val Library = "library"
    const val Settings = "settings"
    const val RecordUltrasound = "record/ultrasound"
    const val RecordPregnancyEvent = "record/pregnancy-event"
    const val RecordMaternalMetric = "record/maternal-metric"
    const val RecordBabyCare = "record/baby-care"

    fun isTopLevel(route: String?): Boolean {
        return route == Home || route == Timeline || route == Library || route == Settings
    }

    fun isRecord(route: String?): Boolean {
        return route == RecordUltrasound ||
            route == RecordPregnancyEvent ||
            route == RecordMaternalMetric ||
            route == RecordBabyCare
    }
}
