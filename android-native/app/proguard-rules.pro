# BabyLogRepository loads the Room-backed collection store by name so JVM smoke
# tests can compile without AndroidX Room on their javac classpath.
-keep class app.babylog.nativeapp.BabyLogRoomCollectionStore {
    public static app.babylog.nativeapp.BabyLogRepositoryCollectionStore$Store create(android.content.Context, app.babylog.nativeapp.BabyLogRepositoryStringStore$Store);
}
