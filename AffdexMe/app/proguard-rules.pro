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

#keep all classes (otherwise Proguard may remove classes that use reflection, injection, Gson, etc...)
-keep class sun.**
-keep class com.**
-keep class android.**
-keep class dagger.**
-keep class javax.**

#keep certain class members (otherwise Proguard would strip the members of these classes)
-keepclassmembers class com.affectiva.android.affdex.sdk.detector.License { *; }
-keepclassmembers class com.affectiva.android.affdex.sdk.detector.A* { *; }
-keepclassmembers class * {
    @javax.inject.* *;
    @dagger.* *;
    <init>();
}