package me.scai.plato.android.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class GsonHelper {
    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    public static String toJson(JsonElement elem) {
        return gson.toJson(elem);
    }

    public static JsonElement parseJson(String json) {
        return jsonParser.parse(json);
    }
}
