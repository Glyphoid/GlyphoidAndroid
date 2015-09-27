package me.scai.plato.AndroidWebCient;

public enum PlatoNotesAction {

    CreateEngine("create-engine", false),
    AddStroke("add-stroke", true),
    RemoveLastToken("remove-last-token", true),
    RemoveToken("remove-token", true),
    MergeStrokesAsToken("merge-strokes-as-token", true),
    UnmergeLastStroke("unmerge-last-stroke", true),
    ForceSetTokenName("force-set-token-name", true),
    ClearStrokes("clear", true),
    ParseTokenSet("parse-token-set", false),
    MoveToken("move-token", false),         // TODO: Verify that this is correctly implemented
    GetVarMap("get-var-map", false),
    GetFromVarMap("get-from-var-map", false),
    InjectState("inject-state", false),
    GetAllTokenNames("getAllTokenNames", false),
    RemoveEngine("remove-engine", false);

    private String commandString;     // Strnig for HTTP requests
    private boolean primarilyLocal;   // A flag indicating whether the computation is done primarily locally and only mirrored on the remote server

    PlatoNotesAction(String commandString, boolean primarilyLocal) {
        this.commandString  = commandString;
        this.primarilyLocal = primarilyLocal;
    }

    public String getCommandString() {
        return commandString;
    }

    public boolean isPrimarilyLocal() {
        return primarilyLocal;
    }
}
