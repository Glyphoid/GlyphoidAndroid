package me.scai.plato.android.helpers;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public class ClientInfoHelper {
    public static JsonObject getCustomClientData(final Activity activity) {
        JsonObject data = new JsonObject();

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        data.add("ScreenWidth", new JsonPrimitive(screenWidth));
        data.add("ScreenHeight", new JsonPrimitive(screenHeight));

        JsonObject locale = new JsonObject();
        try {
            locale.add("Default", new JsonPrimitive(Locale.getDefault().toString()));
            locale.add("DefaultDisplayLanguage", new JsonPrimitive(Locale.getDefault().getDisplayLanguage()));
            locale.add("DefaultDisplayCountry", new JsonPrimitive(Locale.getDefault().getDisplayCountry()));
            locale.add("DefaultDisplayName", new JsonPrimitive(Locale.getDefault().getDisplayName()));
        } catch (Exception exc) {}

        data.add("locale", locale);

        return data;
    }
}
