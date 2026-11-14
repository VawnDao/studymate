# DTO dùng kotlinx.serialization: giữ serializer sinh tự động.
-keepclassmembers @kotlinx.serialization.Serializable class com.studymate.data.remote.dto.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.studymate.data.remote.dto.**$$serializer { *; }
