package me.scai.plato.android.tasks;

import android.os.AsyncTask;

import com.squareup.otto.Bus;

import java.util.Date;
import java.util.concurrent.Semaphore;

import me.scai.parsetree.HandwritingEngineException;
import me.scai.plato.AndroidWebCient.AndroidPlatoWebClient;
import me.scai.plato.AndroidWebCient.InvalidEngineUuidException;
import me.scai.plato.AndroidWebCient.PlatoNotesAction;
import me.scai.plato.android.utils.EventBus;
import me.scai.plato.android.responses.PlatoClientTaskResponse;
import me.scai.plato.android.requests.PlatoWebClientForceSetTokenNameRequest;
import me.scai.plato.android.requests.PlatoWebClientInjectStateRequest;
import me.scai.plato.android.requests.PlatoWebClientMergeStrokesRequest;
import me.scai.plato.android.requests.PlatoWebClientMoveTokenRequest;
import me.scai.plato.android.requests.PlatoWebClientTaskRequest;

public class AndroidWebClientAsyncTask extends AsyncTask<PlatoWebClientTaskRequest, Void, PlatoClientTaskResponse> {
    private static final Semaphore semaphore = new Semaphore(1);
    private PlatoNotesAction action;

    @Override
    public PlatoClientTaskResponse doInBackground(PlatoWebClientTaskRequest... args) {

        try {
            semaphore.acquire();
        } catch (InterruptedException exc) {
            // TODO
        }

        /* Get start time */
        long startTimeMillis = new Date().getTime();

        PlatoWebClientTaskRequest req = args[0];
        AndroidPlatoWebClient webClient = req.getWebClient();
        action = req.getAction();

        PlatoClientTaskResponse resp = new PlatoClientTaskResponse();
        resp.setOriginalAction(action);

        if (webClient != null) {
            try {
                if (action == PlatoNotesAction.AddStroke) {
                    webClient.addStroke(req.getStroke());
                } else if (action == PlatoNotesAction.RemoveLastToken) {
                    webClient.removeLastToken();
                } else if (action == PlatoNotesAction.ClearStrokes) {
                    webClient.clearStrokes();
                } else if (action == PlatoNotesAction.MergeStrokesAsToken) {
                    PlatoWebClientMergeStrokesRequest mergeStrokesRequest = (PlatoWebClientMergeStrokesRequest) req;
                    int[] strokeIndices = mergeStrokesRequest.getStrokeIndices();

                    if (webClient.getNumStrokes() >= strokeIndices.length) {
                        webClient.mergeStrokesAsToken(strokeIndices);
                    }
                } else if (action == PlatoNotesAction.ForceSetTokenName) {
                    PlatoWebClientForceSetTokenNameRequest forceSetTokenNameRequest =
                            (PlatoWebClientForceSetTokenNameRequest) req;
                    final int tokenIdx     = forceSetTokenNameRequest.getTokenIndex();
                    final String tokenName = forceSetTokenNameRequest.getTokenName();

                    if (webClient.getTokenSet().getNumTokens() > tokenIdx) {
                        webClient.forceSetRecogWinner(tokenIdx, tokenName);
                    }
                } else if (action == PlatoNotesAction.MoveToken) {
                    PlatoWebClientMoveTokenRequest moveTokenRequest = (PlatoWebClientMoveTokenRequest) req;
                    int idxTokenMoved = moveTokenRequest.getIdxTokenMoved();
                    float[] newBounds = moveTokenRequest.getNewBounds();

                    webClient.moveToken(idxTokenMoved, newBounds);
                } else if (action == PlatoNotesAction.ParseTokenSet) {
                    resp.setParserOutput(webClient.parseTokenSet());
                } else if (action == PlatoNotesAction.GetVarMap) {
                    resp.setVarMap(webClient.getVarMap());
                } else if (action == PlatoNotesAction.RemoveEngine) {
                    webClient.removeEngine();
                } else if (action == PlatoNotesAction.InjectState) {
                    PlatoWebClientInjectStateRequest injectStateReq = (PlatoWebClientInjectStateRequest) req;

                    webClient.injectState(injectStateReq.getStateData());
                }
            } catch (InvalidEngineUuidException exc) {
                // Maybe the engine has expired / been purged
                resp.setEngineInvalid(true);
            } catch (HandwritingEngineException exc) {
                resp.setErrorMessage(exc.getMessage());
            }
        }

        resp.setWrittenTokenSet(webClient.getTokenSet());
        resp.setWrittenTokenConstStrokeIdx(webClient.getTokenConstStrokeIndices());

        long elapsedTimeMillis = new Date().getTime() - startTimeMillis;
        resp.setElapsedTimeMillis(elapsedTimeMillis);

        semaphore.release();

        return resp;
    }

    @Override
    public void onPostExecute(PlatoClientTaskResponse resp) {
        final Bus eventBus = EventBus.getInstance();

        if (resp.getVarMap() != null) {
            eventBus.post(resp.getVarMap());
        }

        /* Get current time and calculate elapsed time */
        if ( resp.isEngineInvalid() ||   // Engine expiration or purging events always need to be posted, to allow restoration
             !action.isPrimarilyLocal() ) {
            /* For these operations, the local stroke curator handles the job */
            eventBus.post(resp);
        }
    }

    @Override
    public void onCancelled() {
        final Bus eventBus = EventBus.getInstance();

        PlatoClientTaskResponse resp = new PlatoClientTaskResponse();
        resp.setOriginalAction(action);
        resp.setCancelled(true);

        eventBus.post(resp);
    }
}
