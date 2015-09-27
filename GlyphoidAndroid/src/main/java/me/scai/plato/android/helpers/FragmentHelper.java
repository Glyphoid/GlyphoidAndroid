package me.scai.plato.android.helpers;

import com.squareup.otto.Bus;

import me.scai.plato.android.events.BackToMainViewEvent;
import me.scai.plato.android.utils.EventBus;

public class FragmentHelper {

    public static void postBackToMainViewEvent() {
        Bus eventBus = EventBus.getInstance();
        eventBus.post(new BackToMainViewEvent());
    }

}
