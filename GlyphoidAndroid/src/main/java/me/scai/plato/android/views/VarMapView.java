package me.scai.plato.android.views;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.parsetree.evaluation.ValueUnion;
import me.scai.plato.android.R;
import me.scai.plato.android.helpers.FragmentHelper;
import me.scai.plato.android.helpers.TokenNameHelper;
import me.scai.plato.android.listeners.DismissClickListener;

public class VarMapView extends Fragment {
    /* Member variables */
    private Context ctx;
    private ClipboardManager clipManager;

    private PlatoVarMap varMap;

    private ObservableScrollView obsScrollView;

    /* UI members */
    private LinearLayout varMapLayout;
    private LinearLayout [] horizLayouts;
    private Button dismiss;

    /* UI listeners */

    /* Methods */
    public VarMapView(Context ctx) {
        super();

        /* Obtain clip service */
        this.ctx = ctx;
        clipManager = (ClipboardManager) this.ctx.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    /* Overridden methods */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        obsScrollView = (ObservableScrollView) inflater.inflate(R.layout.var_map_fragment, container, false);

        varMapLayout = (LinearLayout) obsScrollView.findViewById(R.id.varMapLayout);
        dismiss      = (Button) obsScrollView.findViewById(R.id.dismissVarMap);

        dismiss.setOnClickListener(new DismissClickListener(getFragmentManager()));

        showVarMap();

        return obsScrollView;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        FragmentHelper.postBackToMainViewEvent();

        super.onStop();
    }

    public void setVarMap(final PlatoVarMap varMap) {
        this.varMap = varMap;
    }

    public void showVarMap() {
        varMapLayout.removeAllViews(); // Clean up before adding views

        horizLayouts = new LinearLayout[varMap.numVars()];

        LinearLayout.LayoutParams nameColParams   = new LinearLayout.LayoutParams(60, LinearLayout.LayoutParams.WRAP_CONTENT);
//        LinearLayout.LayoutParams valColParams    = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams buttonColParams = new LinearLayout.LayoutParams(60, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonColParams.setMargins(2, 2, 2, 2);

        int rowNum = 0;
        for (String varName : varMap.getVarNamesSorted()) {
            ValueUnion val = varMap.getVarValue(varName);

            double dv = Double.NaN;
            try {
                dv = val.getDouble();
            } catch (Throwable t) {
            }

            horizLayouts[rowNum] = new LinearLayout(ctx);
            horizLayouts[rowNum].setOrientation(LinearLayout.HORIZONTAL);

            /* Button */
//            Button deleteVar = new Button(ctx);
//            deleteVar.setText("X");
//            deleteVar.setLayoutParams(buttonColParams);
//            deleteVar.setBackground(ctx.getResources().getDrawable(R.drawable.button_bg_general));

//            horizLayouts[rowNum].addView(deleteVar);

            /* Variable name */
            TextView varNameView = new TextView(ctx);
            varNameView.setText(getTokenDisplayName(varName));
            varNameView.setLayoutParams(nameColParams);

            horizLayouts[rowNum].addView(varNameView);

            /* Variable value */
            TextView varValueView = new TextView(ctx);
            varValueView.setText(Double.toString(dv));
//            varNameView.setLayoutParams(valColParams);

            horizLayouts[rowNum].addView(varValueView);


            varMapLayout.addView(horizLayouts[rowNum]);

            rowNum++;
        }
    }

    private String getTokenDisplayName(String tokenName) {
        return TokenNameHelper.tokenName2DisplayName(tokenName);
    }

}
