package app.babylog.nativeapp

internal fun isEditablePregnancyRecord(eventType: String): Boolean {
    return eventType == "ultrasound" ||
        eventType == "pregnancy_checkup" ||
        eventType == "maternal_metric" ||
        eventType == "fetal_movement" ||
        eventType == "contraction" ||
        BabyLogService.isScreeningEventType(eventType) ||
        isEditableBabyRecord(eventType)
}

internal fun isEditableBabyRecord(eventType: String): Boolean {
    return eventType == "feed" ||
        eventType == "sleep" ||
        eventType == "diaper" ||
        eventType == "temperature" ||
        eventType == "medication" ||
        eventType == "growth" ||
        eventType == "child_checkup" ||
        eventType == "breastfeed" ||
        eventType == "bottle" ||
        eventType == "wake" ||
        eventType == "pee" ||
        eventType == "poop"
}
