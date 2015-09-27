package me.scai.plato.android.helpers;


import java.util.HashMap;
import java.util.Map;

public class TokenNameHelper {
    /* Member variables */
    private Map<String, String> tokenName2DisplayNameMap;

    private static TokenNameHelper instance;

    /* Constructor */
    private TokenNameHelper(String[] tokenNames, String[] tokenDisplayNames) {
        tokenName2DisplayNameMap = new HashMap<String, String>();
        for (int i = 0; i < tokenNames.length; ++i) {
            tokenName2DisplayNameMap.put(tokenNames[i], tokenDisplayNames[i]);
        }
    }

    /* Singleton access */
    public static TokenNameHelper createOrGetInstance(String[] tokenNames, String[] tokenDisplayNames) {
        if (instance == null) {
            instance = new TokenNameHelper(tokenNames, tokenDisplayNames);
        }

        return instance;
    }

    public static TokenNameHelper getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Not instance has been generated yet");
        }

        return instance;
    }

    public static String tokenName2DisplayName(String tokenName) {
        if (instance != null) {
            if (!instance.tokenName2DisplayNameMap.containsKey(tokenName)) {
                return tokenName;
            } else {
                return instance.tokenName2DisplayNameMap.get(tokenName);
            }
        } else {
            return tokenName;
        }


    }
}
