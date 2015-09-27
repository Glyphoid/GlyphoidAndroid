package me.scai.plato.android.utils;

import com.squareup.otto.Bus;

/**
 * Created by scai on 5/21/2015.
 */
public class EventBus {
    private static final Bus BUS = new Bus();

    public static Bus getInstance() {
        return BUS;
    }
}
