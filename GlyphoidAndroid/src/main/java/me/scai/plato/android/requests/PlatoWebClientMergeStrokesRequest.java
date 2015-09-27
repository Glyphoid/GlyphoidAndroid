package me.scai.plato.android.requests;

import me.scai.plato.AndroidWebCient.AndroidPlatoWebClient;
import me.scai.plato.AndroidWebCient.PlatoNotesAction;

public class PlatoWebClientMergeStrokesRequest extends PlatoWebClientTaskRequest {
    private int[] strokeIndices;

    public PlatoWebClientMergeStrokesRequest(AndroidPlatoWebClient webClient,
                                             PlatoNotesAction action,
                                             int[] strokeIndices) {
        super(webClient, action);

        this.strokeIndices = strokeIndices;
    }

    public int[] getStrokeIndices() {
        return strokeIndices;
    }
}
