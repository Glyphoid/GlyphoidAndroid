package me.scai.plato.android.events;


public final class ForceSetTokenNameEvent {
    /* Member variables */
    private String tokenName;

    /* Constructor */
    public ForceSetTokenNameEvent(final String tokenName) {
        this.tokenName = tokenName;
    }

    /* Getters */
    public String getTokenName() {
        return tokenName;
    }
}
