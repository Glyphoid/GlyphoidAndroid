package me.scai.plato.android.requests;

import me.scai.plato.AndroidWebCient.AndroidPlatoWebClient;
import me.scai.plato.AndroidWebCient.PlatoNotesAction;

public class PlatoWebClientForceSetTokenNameRequest extends PlatoWebClientTaskRequest {
    private int tokenIndex;
    private String tokenName;

    public PlatoWebClientForceSetTokenNameRequest(AndroidPlatoWebClient webClient,
                                                  PlatoNotesAction action,
                                                  final int tokenIndex,
                                                  final String tokenName) {
        super(webClient, action);

        this.tokenIndex = tokenIndex;
        this.tokenName  = tokenName;
    }

    public int getTokenIndex() {
        return tokenIndex;
    }

    public String getTokenName() {
        return tokenName;
    }
}
