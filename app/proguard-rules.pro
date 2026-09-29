# Binder/AIDL entry points are loaded across a Shizuku user-service boundary.
-keep class com.trigger.automation.engine.IInputEventService { *; }
-keep interface com.trigger.automation.engine.IInputEventService { *; }
-keep class com.trigger.automation.engine.IInputEventService$Stub { *; }
-keep class com.trigger.automation.engine.IInputEventService$Stub$Proxy { *; }
-keep class com.trigger.automation.engine.ShizukuUserService { public <init>(...); *; }
-keep class com.trigger.automation.engine.ShizukuServiceConnection { *; }

# Reflection-only framework API names are resolved in the shell process at runtime.
-keepnames class android.hardware.input.IInputManager
-keepnames class android.hardware.input.IInputManager$Stub
-keepnames class android.os.ServiceManager
-dontwarn android.hardware.input.IInputManager

# Kotlin serialization uses generated serializers and runtime annotations.
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault
-keep class com.trigger.feature.macroeditor.model.** { *; }
-keep class com.trigger.feature.scheduling.data.** { *; }
-keep class **$$serializer { *; }
-keepclassmembers class ** { kotlinx.serialization.KSerializer serializer(...); }

# Room generated implementations are discovered by generated code; retain schema model members.
-keep class com.trigger.core.database.TriggerDatabase { *; }
-keep class com.trigger.core.database.MacroEntity { *; }
-keep class com.trigger.core.database.MacroStepEntity { *; }
-dontwarn javax.annotation.**
