package app.babylog.nativeapp.ui.screens

internal object BabyLogRoutes {
    const val Home = "home"
    const val Disclaimer = "disclaimer"
    const val Timeline = "timeline"
    const val Library = "library"
    const val Settings = "settings"
    const val RecordUltrasound = "record/ultrasound"
    const val RecordPregnancyEvent = "record/pregnancy-event"
    const val RecordMaternalMetric = "record/maternal-metric"
    const val RecordBabyCare = "record/baby-care"
    const val SmartEntry = "smartEntry"
    const val SettingsProfile = "settings/profile"
    const val SettingsSync = "settings/sync"
    const val SettingsModel = "settings/model"
    const val SettingsSpeech = "settings/speech"
    const val SettingsDisclaimer = "settings/disclaimer"
    const val LibraryAttachments = "library/attachments"
    const val LibraryTrash = "library/trash"
    const val AttachmentPreview = "attachment/preview"

    fun isTopLevel(route: String?): Boolean {
        return route == Home || route == Timeline || route == Library || route == Settings
    }

    fun isRecord(route: String?): Boolean {
        return route == RecordUltrasound ||
            route == RecordPregnancyEvent ||
            route == RecordMaternalMetric ||
            route == RecordBabyCare
    }

    fun isSettingsSubpage(route: String?): Boolean {
        return route == SettingsProfile ||
            route == SettingsSync ||
            route == SettingsModel ||
            route == SettingsSpeech ||
            route == SettingsDisclaimer
    }

    fun isBrowseSubpage(route: String?): Boolean {
        return route == LibraryAttachments ||
            route == LibraryTrash ||
            route == AttachmentPreview
    }
}
