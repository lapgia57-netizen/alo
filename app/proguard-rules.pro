# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Model
-keepclassmembers class com.moneytp.sothuchi.model.** { *; }
