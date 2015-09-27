package me.scai.plato.android.requests;

import me.scai.handwriting.CStroke;
import me.scai.plato.AndroidWebCient.AndroidPlatoWebClient;
import me.scai.plato.AndroidWebCient.PlatoNotesAction;

public class PlatoWebClientTaskRequest {
    private final AndroidPlatoWebClient webClient;
    private final PlatoNotesAction action;
    private CStroke stroke;

    public PlatoWebClientTaskRequest(AndroidPlatoWebClient webClient, PlatoNotesAction action) {
        this.webClient = webClient;
        this.action = action;
    }

    public PlatoWebClientTaskRequest(AndroidPlatoWebClient webClient, PlatoNotesAction action, CStroke stroke) {
        this.webClient = webClient;
        this.action = action;
        this.stroke = stroke;
    }

    public AndroidPlatoWebClient getWebClient() {
        return webClient;
    }

    public PlatoNotesAction getAction() {
        return action;
    }

    public CStroke getStroke() {
        return stroke;
    }


}
