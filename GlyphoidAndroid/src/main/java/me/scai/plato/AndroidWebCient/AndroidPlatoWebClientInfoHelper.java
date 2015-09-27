package me.scai.plato.AndroidWebCient;


import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class AndroidPlatoWebClientInfoHelper {
    public static JsonObject getAndroidClientData() {
        JsonObject data = new JsonObject();

        data.add("ClientTypeMajor", new JsonPrimitive("MobileAndroid"));
        if (android.os.Build.VERSION.RELEASE != null) {
            data.add("ClientPlatformVersion", new JsonPrimitive(android.os.Build.VERSION.RELEASE));
        } else {
            data.add("ClientPlatformVersion", new JsonPrimitive("null"));
        }
        data.add("ClientAppVersion", new JsonPrimitive("0.1")); // TODO: Do not hard code

        return data;
    }
}
