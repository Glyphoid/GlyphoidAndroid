package me.scai.plato.AndroidWebCient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import me.scai.parsetree.HandwritingEngineException;

public class MainTestAndroidWebClient {
    private static final String platoHandwritingEndpoint = "http://127.0.0.1:8080/glyphoid/handwriting-bypass";
    private static final String platoTokenRecogEndpoint = "http://127.0.0.1:8080/glyphoid/token-recog-bypass";

    private static final JsonParser jsonParser = new JsonParser();

    private static JsonObject genMockCustomClientData() {
        JsonObject customClientData = new JsonObject();

        customClientData.add("ScreenWidth", new JsonPrimitive(600));
        customClientData.add("ScreenHeight", new JsonPrimitive(1000));

        return customClientData;
    }

    public static void main(String[] args) {
        AndroidPlatoWebClient platoClient = null;
        JsonObject customClientData = genMockCustomClientData();
        try {
            platoClient = new AndroidPlatoWebClient(platoHandwritingEndpoint,
                                                    platoTokenRecogEndpoint,
                                                    customClientData,
                                                    ""); // Manually fill this string in for testing
        } catch (HandwritingEngineException exc) {
            throw new RuntimeException(exc.getMessage());
        }

        System.out.println("Obtained new handwriting engine from \"" +
                platoHandwritingEndpoint + "\": \"" +
                platoClient.getHandwritingEngineUuid() + "\"");
    }
}
