package me.scai.plato.android.requests;

import com.google.gson.JsonObject;

import me.scai.plato.AndroidWebCient.AndroidPlatoWebClient;
import me.scai.plato.AndroidWebCient.PlatoNotesAction;

public class PlatoWebClientInjectStateRequest extends PlatoWebClientTaskRequest {
    private JsonObject stateData;

    public PlatoWebClientInjectStateRequest(AndroidPlatoWebClient webClient,
                                            final JsonObject stateData) {
        super(webClient, PlatoNotesAction.InjectState);

        this.stateData = stateData;
    }

    public JsonObject getStateData() {
        return stateData;
    }
}
