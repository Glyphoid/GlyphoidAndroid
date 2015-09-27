package me.scai.plato.android.options;

public class PlatoNotesOptions {
    /* Member variables */
    private boolean useLocalDeviceOnly;
    private boolean displayOverlaidSymbols;
    private long parserTimeoutMillis;

    /* Constructor */
    public PlatoNotesOptions(boolean useLocalDeviceOnly, boolean displayOverlaidSymbols, long parserTimeoutMillis) {
        this.useLocalDeviceOnly = useLocalDeviceOnly;
        this.displayOverlaidSymbols = displayOverlaidSymbols;
        this.parserTimeoutMillis = parserTimeoutMillis;
    }

    /* Default constructor */
    public PlatoNotesOptions() {
        useLocalDeviceOnly = false;
        displayOverlaidSymbols          = false;
        parserTimeoutMillis             = 30L * 1000L;
    }

    /* Getters */
    public boolean isUseLocalDeviceOnly() {
        return useLocalDeviceOnly;
    }

    public boolean isDisplayOverlaidSymbols() {
        return displayOverlaidSymbols;
    }

    public long getParserTimeoutMillis() {
        return parserTimeoutMillis;
    }

    /* Setters */

    public void setUseLocalDeviceOnly(boolean useLocalDeviceOnly) {
        this.useLocalDeviceOnly = useLocalDeviceOnly;
    }

    public void setDisplayOverlaidSymbols(boolean displayOverlaidSymbols) {
        this.displayOverlaidSymbols = displayOverlaidSymbols;
    }

    public void setParserTimeoutMillis(long parserTimeoutMillis) {
        this.parserTimeoutMillis = parserTimeoutMillis;
    }
}
