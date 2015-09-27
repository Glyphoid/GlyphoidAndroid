package me.scai.plato.android.listeners;

import android.app.FragmentManager;
import android.view.View;

import me.scai.plato.android.helpers.FragmentHelper;

public class DismissClickListener implements View.OnClickListener {
    FragmentManager fragMan;

    public DismissClickListener(FragmentManager fragMan) {
        this.fragMan = fragMan;
    }

    @Override
    public void onClick(View v) {
        FragmentHelper.postBackToMainViewEvent();

        fragMan.popBackStack();
    }

}
