# Route điều hướng kiểu an toàn dùng kotlinx.serialization.
-keepclassmembers @kotlinx.serialization.Serializable class com.studymate.app.ui.navigation.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.studymate.app.ui.navigation.**$$serializer { *; }
