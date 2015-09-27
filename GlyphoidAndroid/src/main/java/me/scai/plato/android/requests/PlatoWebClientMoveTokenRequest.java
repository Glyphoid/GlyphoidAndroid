package me.scai.plato.android.requests;

import me.scai.plato.AndroidWebCient.AndroidPlatoWebClient;
import me.scai.plato.AndroidWebCient.PlatoNotesAction;

/**
 * Created by scai on 6/12/2015.
 */
public class PlatoWebClientMoveTokenRequest extends PlatoWebClientTaskRequest {
    private int idxTokenMoved;
    private float[] newBounds;

    public PlatoWebClientMoveTokenRequest(AndroidPlatoWebClient webClient,
                                          PlatoNotesAction action,
                                          int idxToken,
                                          float[] newBounds) {
        super(webClient, action);

        this.idxTokenMoved = idxToken;
        this.newBounds = newBounds;
    }

    public int getIdxTokenMoved() {
        return idxTokenMoved;
    }

    public float[] getNewBounds() {
        return newBounds;
    }
}
