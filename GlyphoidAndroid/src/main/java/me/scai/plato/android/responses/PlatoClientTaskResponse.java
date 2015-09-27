package me.scai.plato.android.responses;

import java.util.List;
import java.util.Map;

import me.scai.handwriting.CWrittenTokenSet;
import me.scai.parsetree.TokenSetParserOutput;
import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.parsetree.evaluation.ValueUnion;
import me.scai.plato.AndroidWebCient.PlatoNotesAction;

public class PlatoClientTaskResponse {
    private PlatoNotesAction originalAction;
    private CWrittenTokenSet writtenTokenSet;
    private List<int []> writtenTokenConstStrokeIdx;
    private TokenSetParserOutput parserOutput;
    private long elapsedTimeMillis;
    private String errorMessage;
    private PlatoVarMap varMap;

    private boolean engineInvalid;

    private boolean isCancelled;

    /* Getters */
    public PlatoNotesAction getOriginalAction() {
        return originalAction;
    }

    public CWrittenTokenSet getWrittenTokenSet() {
        return writtenTokenSet;
    }

    public TokenSetParserOutput getParserOutput() {
        return parserOutput;
    }

    public long getElapsedTimeMillis() {
        return elapsedTimeMillis;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<int[]> getWrittenTokenConstStrokeIdx() {
        return writtenTokenConstStrokeIdx;
    }

    public PlatoVarMap getVarMap() {
        return varMap;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public boolean isEngineInvalid() {
        return engineInvalid;
    }

    /* Setters */
    public void setOriginalAction(PlatoNotesAction originalAction) {
        this.originalAction = originalAction;
    }

    public void setWrittenTokenSet(CWrittenTokenSet writtenTokenSet) {
        this.writtenTokenSet = writtenTokenSet;
    }

    public void setParserOutput(TokenSetParserOutput parserOutput) {
        this.parserOutput = parserOutput;
    }

    public void setElapsedTimeMillis(long elapsedTimeMillis) {
        this.elapsedTimeMillis = elapsedTimeMillis;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setWrittenTokenConstStrokeIdx(List<int[]> writtenTokenConstStrokeIdx) {
        this.writtenTokenConstStrokeIdx = writtenTokenConstStrokeIdx;
    }

    public void setVarMap(PlatoVarMap varMap) {
        this.varMap = varMap;
    }

    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public void setEngineInvalid(boolean engineInvalid) {
        this.engineInvalid = engineInvalid;
    }


}
