# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html

-dontskipnonpubliclibraryclasses

# Optimization is turned off by default. Dex does not like code run
# through the ProGuard optimize and preverify steps (and performs some
# of these optimizations on its own).
-dontoptimize
-dontpreverify
# Note that if you want to enable optimization, you cannot just
# include optimization flags in your own project configuration file;
# instead you will need to point to the
# "proguard-android-optimize.txt" file instead of this one from your
# project.properties file.

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}

# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**

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

-keep class org.apache.commons.logging.**               { *; }
-keep class com.amazonaws.services.sqs.QueueUrlHandler  { *; }
-keep class com.amazonaws.javax.xml.transform.sax.*     { public *; }
-keep class com.amazonaws.javax.xml.stream.**           { *; }
-keep class com.amazonaws.services.**.model.*Exception* { *; }
-keep class org.codehaus.**                             { *; }
-keepattributes Signature,*Annotation*

-dontwarn javax.xml.stream.events.**
-dontwarn org.fasterxml.jackson.**
-dontwarn org.apache.commons.logging.impl.**
-dontwarn org.apache.http.conn.scheme.**