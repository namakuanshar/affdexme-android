# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/AlanCasalas/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

#prevent proguard from warning us about not including the GooglePlay dependency
-dontwarn **

-keepattributes Exceptions,InnerClasses,Signature,Deprecated, SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

#keep all classes (otherwise Proguard may remove classes that use reflection, injection, Gson, etc...)
-keep class sun.**
-keepclassmembers class sun.** {*;}
-keep class android.**
-keepclassmembers class android.** {*;}
-keep class dagger.**
-keepclassmembers class dagger.** {*;}
-keep class javax.**
-keepclassmembers class javax.** {*;}


#keep certain class members (otherwise Proguard would strip the members of these classes)
-keep class com.**
-keepclassmembers class !com.affectiva.affdexme.MainActivity,!com.affectiva.android.affdex.sdk.detector.Detector {*;}
-keepclassmembers class com.affectiva.android.affdex.sdk.detector.Detector {
    public void setDetect**;
}

# Dagger
-dontwarn dagger.internal.codegen.**
-keepclassmembers,allowobfuscation class * {
	@javax.inject.* *;
	@dagger.* *;
	<init>();
}
-keep class dagger.* { *; }
-keep class javax.inject.* { *; }
-keep class * extends dagger.internal.Binding
-keep class * extends dagger.internal.ModuleAdapter
-keep class * extends dagger.internal.StaticInjection