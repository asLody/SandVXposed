# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/lody/Desktop/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep   class com.amap.api.maps.**{*;}
-keep   class com.autonavi.**{*;}
-keep   class com.amap.api.trace.**{*;}

#定位
-keep class com.amap.api.location.**{*;}
-keep class com.amap.api.fence.**{*;}
-keep class com.autonavi.aps.amapapi.model.**{*;}

#搜索
-keep   class com.amap.api.services.**{*;}

#2D地图
-keep class com.amap.api.maps2d.**{*;}
-keep class com.amap.api.mapcore2d.**{*;}

#导航
-keep class com.amap.api.navi.**{*;}
-keep class com.autonavi.**{*;}

#lazyinject
-ignorewarning
-keepattributes *Annotation*
#保留部分泛型信息，必要!
-keepattributes Signature
#手动启用support keep注解
#http://tools.android.com/tech-docs/support-annotations
-dontskipnonpubliclibraryclassmembers
-keep,allowobfuscation @interface android.support.annotation.Keep

-keep @android.support.annotation.Keep class * {
*;
}

-keepclassmembers class * {
    @android.support.annotation.Keep *;
}
#手动启用Component注解
#http://tools.android.com/tech-docs/support-annotations
-keep,allowobfuscation @interface com.trend.lazyinject.annotation.Component

-keep,allowobfuscation @com.trend.lazyinject.annotation.Component class * {
*;
}

-keepclassmembers,allowobfuscation class * {
    @com.trend.lazyinject.annotation.Provide <methods>;
}

-keepclassmembers class * {
     @com.trend.lazyinject.annotation.Inject <fields>;
}

-keepclassmembers class * {
     @com.trend.lazyinject.annotation.InjectComponent <fields>;
}

-dontwarn javassist.**