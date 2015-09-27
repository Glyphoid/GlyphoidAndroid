package me.scai.plato.AndroidWebCient;

import com.google.api.client.http.GenericUrl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import me.scai.handwriting.CStroke;
import me.scai.handwriting.CWrittenToken;
import me.scai.handwriting.CWrittenTokenSet;
import me.scai.parsetree.HandwritingEngine;
import me.scai.parsetree.HandwritingEngineException;
import me.scai.parsetree.TokenSetParserOutput;
import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.parsetree.evaluation.ValueUnion;
import me.scai.plato.helpers.CStrokeJsonHelper;
import me.scai.plato.helpers.CWrittenTokenJsonHelper;
import me.scai.plato.helpers.CWrittenTokenSetJsonHelper;

import me.scai.network.webutils.JsonWebClient;

public class AndroidPlatoWebClient implements HandwritingEngine {
    /* Constants */
    private static final String TIME_ZONE = "UTC";
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final String PLATO_WEB_CLIENT_HTTP_METHOD = "POST";

    /* Member variables */
    private String platoHandwritingEndpoint;
    private String platoTokenRecogEndpoint;
    private String securityTokenStr;

    private JsonObject customClientData;

    /* Engine state variables */
    private String handwritingEngineUuid;

    private int createEngineMaxNumAttempts = 2;

    private GenericUrl handwritingEndpointUrl;
    private GenericUrl tokenRecogEndpointUrl;
    private JsonElement securityToken;



    /* Written token set */
    private CWrittenTokenSet wtSet;
    private List<int []> wtConstStrokeIdx;

    /* Constructor: Create an instance of web engine client */
    public AndroidPlatoWebClient(String platoHandwritingEndpoint,
                                 String platoTokenRecogEndpoint,
                                 JsonObject customClientData,
                                 String securityTokenStr)
            throws HandwritingEngineException {
        this.securityTokenStr = securityTokenStr;

        /* Set time zone info */
        final TimeZone utcTimeZone = TimeZone.getTimeZone(TIME_ZONE);
        dateFormat.setTimeZone(utcTimeZone);

        this.platoHandwritingEndpoint = platoHandwritingEndpoint;
        this.platoTokenRecogEndpoint  = platoTokenRecogEndpoint;
        this.customClientData         = customClientData;

        handwritingEndpointUrl = new GenericUrl(this.platoHandwritingEndpoint);
        tokenRecogEndpointUrl  = new GenericUrl(this.platoTokenRecogEndpoint);

        securityToken = new JsonPrimitive(securityTokenStr);

        /* Obtain client type */
        JsonObject reqData = AndroidPlatoWebClientInfoHelper.getAndroidClientData();
        reqData.add("CustomClientData", customClientData);

        boolean success = false;
        int nAttempts = 0;

        while (!success && nAttempts < createEngineMaxNumAttempts) {
            nAttempts++;
            JsonObject respObj = sendRequestToHandwritingEngineAndGetResponse(PlatoNotesAction.CreateEngine, reqData);

            if (respObj != null) {
                if(respObj.get("errors").getAsJsonArray().size() > 0) {
                    /* Will try again */
                    continue;
                }

                handwritingEngineUuid = respObj.get("engineUuid").getAsString();
                success = true;
            }
        }

        if (!success) {
            throw new HandwritingEngineException("All attempt(s) to obtain a valid engine ID have failed");
        }
    }

    /* Remove the current engine */
    @Override
    public synchronized void removeEngine() throws HandwritingEngineException {
        if (handwritingEngineUuid == null) {
            throw new HandwritingEngineException("Attempt to remove non-existent engine: handwritingEngineUuid = null");
        }

        JsonObject removeEngineParams = new JsonObject();
        removeEngineParams.add("engineUuid", new JsonPrimitive(this.handwritingEngineUuid));

        sendRequestToHandwritingEngineAndGetResponse(PlatoNotesAction.RemoveEngine, removeEngineParams);

        this.handwritingEngineUuid = null;
    }

    /* Add a stroke */
    @Override
    public synchronized void addStroke(CStroke stroke) throws HandwritingEngineException {
        JsonObject strokeObj = CStrokeJsonHelper.CStroke2JsonObject(stroke);

        JsonObject addStrokeParams = new JsonObject();
        addStrokeParams.add("stroke", strokeObj);

        sendRequestToHandwritingEndpointAndUpdateStatus(PlatoNotesAction.AddStroke, addStrokeParams);
    }

    /* Remove the last token */
    @Override
    public synchronized void removeLastToken() throws HandwritingEngineException {
        sendRequestToHandwritingEndpointAndUpdateStatus(PlatoNotesAction.RemoveLastToken, null);
    }

    /* Remove the i-th token */
    public synchronized void removeToken(int tokenIdx) throws HandwritingEngineException {
        JsonObject removeTokenData = new JsonObject();

        removeTokenData.add("idxToken", new JsonPrimitive(tokenIdx));

        sendRequestToHandwritingEndpointAndUpdateStatus(PlatoNotesAction.RemoveToken, removeTokenData);
    }

    /* Merge strokes with specified indices as a single token */
    @Override
    public synchronized void mergeStrokesAsToken(int [] strokeInds) throws HandwritingEngineException {
        JsonObject mergeStrokesParams = new JsonObject();

        JsonArray jsStrokeInds = new JsonArray();
        for (int i = 0; i < strokeInds.length; ++i) {
            jsStrokeInds.add(new JsonPrimitive(strokeInds[i]));
        }
        mergeStrokesParams.add("strokeIndices", jsStrokeInds);

        sendRequestToHandwritingEndpointAndUpdateStatus(PlatoNotesAction.MergeStrokesAsToken,  mergeStrokesParams);
    }

    /* Force-set the recognition winner of a token */
    @Override
    public synchronized void forceSetRecogWinner(int tokenIdx, String tokenName) throws HandwritingEngineException {
        JsonObject setParams = new JsonObject();
        setParams.add("tokenIdx", new JsonPrimitive(tokenIdx));
        setParams.add("tokenRecogWinner", new JsonPrimitive(tokenName));

        sendRequestToHandwritingEndpointAndUpdateStatus(PlatoNotesAction.ForceSetTokenName, setParams);
    }

    /* Clear all strokes */
    @Override
    public synchronized void clearStrokes() throws HandwritingEngineException {
        sendRequestToHandwritingEndpointAndUpdateStatus(PlatoNotesAction.ClearStrokes, null);
    }

    /* Parse token set */
    @Override
    public synchronized TokenSetParserOutput parseTokenSet() throws HandwritingEngineException {
        JsonObject respObj = sendRequestToHandwritingEngineAndGetResponse(PlatoNotesAction.ParseTokenSet, null);

        return obtainTokenSetParserResponse(respObj);
    }

    /* Recognize a token */
    public synchronized JsonObject recognizeToken(CWrittenToken wt) throws HandwritingEngineException {
        JsonObject wtObj = CWrittenTokenJsonHelper.CWrittenToken2JsonObject(wt);

        JsonObject tokenRecogObj = wtObj;
        addSecurityToken(tokenRecogObj);

        return sendRequestAndGetResponseWithRepeats("token recognition", this.tokenRecogEndpointUrl, tokenRecogObj);
    }

    /* Send request to handwriting endpoint and update the status of the written token set */
    private void sendRequestToHandwritingEndpointAndUpdateStatus(PlatoNotesAction action, JsonObject additionalData)
        throws HandwritingEngineException {
        obtainWrittenTokenSetStatus(sendRequestToHandwritingEngineAndGetResponse(action, additionalData));
    }

    /* Update the status of the token written token set based on  */
    private void obtainWrittenTokenSetStatus(JsonObject clientRespObj) throws HandwritingEngineException {
        /* Retrieve the written token set state */
        try {
            wtSet = CWrittenTokenSetJsonHelper.jsonObj2CWrittenTokenSet(
                    clientRespObj.getAsJsonObject("writtenTokenSet")
            );
        } catch (Exception exc) {
            throw new HandwritingEngineException("Failed to parse written token set in response to add-stroke request");
        }

        /* Retrieve the constituent strokes indicies */
        try {
            wtConstStrokeIdx = CWrittenTokenSetJsonHelper.jsonArray2ConstituentStrokeIndices(
                    clientRespObj.getAsJsonArray("constituentStrokes")
            );
        } catch (Exception exc) {
            throw new HandwritingEngineException("Failed to parse constituent stroke indices in response to add-stroke request");
        }
    }

    /* Send request and get response, specifically for the handwriting endpoint */
    private JsonObject sendRequestToHandwritingEngineAndGetResponse(PlatoNotesAction action, JsonObject additionalData)
        throws HandwritingEngineException {
        JsonObject reqData = new JsonObject();

        /* Specify action */
        reqData.add("action", new JsonPrimitive(action.getCommandString()));

        /* Add the security token */
        addSecurityToken(reqData);

        /* Attach handwriting engine UUID */
        if ( action != PlatoNotesAction.CreateEngine ) {
            addEngineId(reqData);
        }

        /* Add timestamp */
        addTimestamp(reqData);

        /* Include additional data */
        if (additionalData != null) {
            for (Map.Entry<String, JsonElement> entry : additionalData.entrySet()) {
                reqData.add(entry.getKey(), entry.getValue());
            }
        }

        return sendRequestAndGetResponseWithRepeats(action.getCommandString(), this.handwritingEndpointUrl, reqData);
    }

    private JsonObject sendRequestAndGetResponseWithRepeats(String actionName, GenericUrl url, JsonObject bodyData)
        throws HandwritingEngineException {

        JsonObject respObj = null;
        try {
            respObj = JsonWebClient.sendRequestAndGetResponseWithRepeats(url, PLATO_WEB_CLIENT_HTTP_METHOD, bodyData);
        } catch (JsonWebClient.StatusException statusExc) {
            throw new HandwritingEngineException("Failed to get successful response from " +
                    actionName + " request, due to " + statusExc.getMessage());
        } catch (JsonWebClient.BodyReadException bodyReadExc) {
            throw new HandwritingEngineException("Exception occurred during reading of the response to " + actionName +
                    " request, due to: " + bodyReadExc.getMessage());
        } catch (JsonWebClient.BodyParseException bodyParseExc) {
            throw new HandwritingEngineException("Exception occurred during parsing of the response to " + actionName +
                    " request, due to: " + bodyParseExc.getMessage());
        } catch (JsonWebClient.AllAttemptsFailedException allFailedExc) {
            throw new HandwritingEngineException("Faile to read web server response to " + actionName +
                    " request, due to: " + allFailedExc.getMessage());
        }

        /* Look for errors */
        if ( respObj != null &&
                respObj.has("errors") &&
                respObj.get("errors").isJsonArray()) {
            JsonArray errors = respObj.get("errors").getAsJsonArray();
            if (errors.size() == 1 && errors.get(0).getAsString().contains("Engine UUID is invalid")) { // TODO: Do not hard code
                throw new InvalidEngineUuidException(errors.get(0).getAsString());
            } else if (errors.size() > 0) {
                throw new HandwritingEngineException("The operation of \""+  actionName +
                        "\" encountered the following error(s): " + errors);
            }
        } else {
            throw new HandwritingEngineException("Received unexpected JSON response during action "+  actionName);
        }

        return respObj;
    }

    private TokenSetParserOutput obtainTokenSetParserResponse(JsonObject respObj)
            throws HandwritingEngineException {
        TokenSetParserOutput parserResp = null;
        try {
            JsonObject parseResult = respObj.get("parseResult").getAsJsonObject();

            parserResp = new TokenSetParserOutput(
                    parseResult.get("stringizerOutput").getAsString(),
                    parseResult.get("evaluatorOutput").getAsString(),
                    parseResult.get("mathTex").getAsString());

        }
        catch (Exception exc) {
            throw new HandwritingEngineException("Parsing of token-set parser failed due to " + exc.getMessage());
        }

        return parserResp;
    }

    /* Add security token to request object */
    private void addSecurityToken(JsonObject obj) {
        obj.add("securityToken", securityToken);
    }

    /* Add engine UUID to object */
    private void addEngineId(JsonObject obj) throws HandwritingEngineException {
        if (this.handwritingEndpointUrl == null) {
            throw new HandwritingEngineException("Null Engine UUID");
        }

        obj.add("engineUuid", new JsonPrimitive(this.handwritingEngineUuid));
    }


    private void addTimestamp(JsonObject obj) {
        obj.add("timestamp", new JsonPrimitive(dateFormat.format(new Date())));
    }

    public synchronized int getNumStrokes() {
        int nStrokes = 0;
        Iterator<int []> wtConstStrokeIdxIter = wtConstStrokeIdx.iterator();

        while (wtConstStrokeIdxIter.hasNext()) {
            nStrokes += wtConstStrokeIdxIter.next().length;
        }

        return nStrokes;
    }

    /* Getters */
    @Override
    public synchronized CWrittenTokenSet getTokenSet() {
        return wtSet;
    }

    @Override
    public synchronized float[] getTokenBounds(int tokenIdx)
        throws HandwritingEngineException {
        if (tokenIdx < 0 || tokenIdx >= wtSet.getNumTokens()) {
            throw new HandwritingEngineException("Invalid token index: " + tokenIdx);
        }

        return wtSet.getTokenBounds(tokenIdx);
    }

    @Override
    public synchronized List<int []> getTokenConstStrokeIndices() {
        return wtConstStrokeIdx;
    }

    @Override
    public synchronized float[] moveToken(int tokenIdx, float[] newBounds)
        throws HandwritingEngineException {
        JsonArray newBoundsJson = new JsonArray();
        for (int i = 0; i < newBounds.length; ++i) {
            newBoundsJson.add(new JsonPrimitive(newBounds[i]));
        }

        JsonObject moveTokenParams = new JsonObject();
        moveTokenParams.add("tokenIdx", new JsonPrimitive(tokenIdx));
        moveTokenParams.add("newBounds", newBoundsJson);

        JsonObject respObj = sendRequestToHandwritingEngineAndGetResponse(PlatoNotesAction.MoveToken, moveTokenParams);

        /* Update status of wtSet */
        obtainWrittenTokenSetStatus(respObj);

        if (respObj.has("oldBounds")) {
            JsonArray oldBoundsJson = respObj.get("oldBounds").getAsJsonArray();
            float[] oldBounds = new float[oldBoundsJson.size()];

            for (int i = 0; i < oldBoundsJson.size(); ++i) {
                oldBounds[i] = oldBoundsJson.get(i).getAsFloat();
            }

            return oldBounds;
        } else {
            return null;
        }
    }

    @Override
    public PlatoVarMap getVarMap()
            throws HandwritingEngineException {
        JsonObject respObj = sendRequestToHandwritingEngineAndGetResponse(PlatoNotesAction.GetVarMap, null);

        JsonObject varMapObj = respObj.get("varMap").getAsJsonObject();

        PlatoVarMap varMap = new PlatoVarMap();
        for (Map.Entry<String, JsonElement> entry : varMapObj.entrySet()) {
            String varName = entry.getKey();
            JsonObject varObj = entry.getValue().getAsJsonObject();

            varMap.addVar(varName, jsonObj2ValueUnion(varObj));
        }

        return varMap;
    }

    @Override
    public ValueUnion getFromVarMap(String varName)
        throws HandwritingEngineException {
        JsonObject varNameObject = new JsonObject();
        varNameObject.add("varName", new JsonPrimitive(varName));

        JsonObject respObj = sendRequestToHandwritingEngineAndGetResponse(PlatoNotesAction.GetFromVarMap, varNameObject);

        JsonObject varMap = respObj.get("varMap").getAsJsonObject();
        JsonObject varObj = varMap.get(varName).getAsJsonObject();

        ValueUnion vu = jsonObj2ValueUnion(varObj);

        return vu;
    }

    @Override
    public synchronized void injectState(JsonObject state) throws HandwritingEngineException {
        JsonObject injectParams = new JsonObject();
        injectParams.add("stateData", state);

        sendRequestToHandwritingEndpointAndUpdateStatus(PlatoNotesAction.InjectState, injectParams);
    }

    @Override
    public List<String> getAllTokenNames() throws HandwritingEngineException {
        JsonObject respObj = sendRequestToHandwritingEngineAndGetResponse(PlatoNotesAction.GetFromVarMap, null);

        JsonArray allTokenNamesArray = respObj.get("allTokenNames").getAsJsonArray();

        List allTokenNames = new ArrayList<String>();

        for (int i = 0; i < allTokenNamesArray.size(); ++i) {
            allTokenNames.add(allTokenNamesArray.get(i).getAsString());
        }

        return allTokenNames;
    }

    public synchronized String getHandwritingEngineUuid() {
        return handwritingEngineUuid;
    }

    /* Setters */
    public synchronized void setHandwritingEngineUuid(String handwritingEngineUuid) {
        this.handwritingEngineUuid = handwritingEngineUuid;
    }

    private ValueUnion jsonObj2ValueUnion(JsonObject varObj) {
        String valueType = varObj.get("valueType").getAsString();
        double value = varObj.get("value").getAsDouble();
        String description = varObj.get("description").getAsString();

        ValueUnion vu = null;
        if (valueType.equals("Double")) {
            vu = new ValueUnion(value, description);
        } else if (valueType.equals("PhysicalQuantity")) {
            vu = new ValueUnion(value, description);
            // TODO: Add unit information
        }

        return vu;
    }

    public void setSecurityTokenStr(String securityTokenStr) {
        this.securityTokenStr = securityTokenStr;
    }
}
