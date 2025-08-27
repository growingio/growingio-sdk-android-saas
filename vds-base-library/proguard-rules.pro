-keepparameternames

-keep class com.growingio.android.sdk.collection.GrowingIO {
    public *;
    static <fields>;
}

-keep class com.growingio.android.sdk.collection.ActivityLifecycleCallbacksRegistrar {
    *;
}

-keep class com.growingio.android.sdk.utils.LogUtil {
    public *;
}

-keep class com.growingio.android.sdk.instrumentation.**

-keep class com.growingio.android.sdk.collection.Configuration {
    public *;
}

-kee  class com.growingio.android.replacegrowingsdk.module.**
-keep class com.growingio.android.sdk.utils.**
-keep class com.growingio.android.sdk.collection.**
-dontwarn com.growingio.android.sdk.collection.**

-keep class com.growingio.android.sdk.collection.GConfig {
    public static <fields>;
    public static boolean isInstrumented();
    public static boolean isMultiProcessEnabled();
    public static java.lang.String getUrlScheme();
    public static java.lang.String getProjectId();
}

-keep class com.growingio.android.sdk.autoburry.VdsJsBridgeManager{
    public *;
}

-keep class com.growingio.android.sdk.utils.ClassExistHelper{
    public *;
}

-dontwarn  com.growingio.android.sdk.collection.SessionManager
-keep class com.growingio.android.sdk.collection.SessionManager{
    static void leaveLastActivity();
    static boolean enterNewPage();
}

-keep class com.growingio.android.sdk.autoburry.VdsAgent {
    public *;
}

-keep class com.growingio.android.sdk.collection.VdsJsHelper$VdsBridge {
    public *;
}
-keep class com.growingio.android.sdk.collection.VdsJsHelper {
    public *;
}

-keep class com.growingio.android.sdk.circle.HybridEventEditDialog$HybridCircleContent {
    public *;
}

-keep class com.growingio.android.sdk.models.EventSID {
    *;
}

