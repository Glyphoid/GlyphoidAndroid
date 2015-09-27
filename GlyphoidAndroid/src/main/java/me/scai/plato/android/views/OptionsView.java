package me.scai.plato.android.views;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.squareup.otto.Bus;

import java.io.IOException;

import me.scai.plato.android.options.PlatoNotesOptions;
import me.scai.plato.android.options.PlatoNotesOptionsAgent;
import me.scai.plato.android.utils.EventBus;
import me.scai.plato.android.R;
import me.scai.plato.android.helpers.FragmentHelper;

public class OptionsView extends Fragment {
    /* Member variables */
    private Context ctx;

    private PlatoNotesOptionsAgent optionsAgent;

    private ObservableScrollView obsScrollView;

    private CheckBox useLocalDeviceOnly;
    private CheckBox displayOverlaidSymbols;
    private EditText parserTimeoutSeconds;
    private Button restoreDefaults;
    private Button dismiss;

    /* Constructors */
    public OptionsView(Context ctx, PlatoNotesOptionsAgent optionsAgent) {
        super();

        this.ctx = ctx;
        this.optionsAgent = optionsAgent;
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
        obsScrollView = (ObservableScrollView) inflater.inflate(R.layout.options_fragment, container, false);

        useLocalDeviceOnly = (CheckBox) obsScrollView.findViewById(R.id.optDisplayOverlaySymbols);
        displayOverlaidSymbols = (CheckBox) obsScrollView.findViewById(R.id.optDisplayOverlaySymbols);
        parserTimeoutSeconds = (EditText) obsScrollView.findViewById(R.id.parserTimeoutSeconds);

        useLocalDeviceOnly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                optionsAgent.getOptions().setUseLocalDeviceOnly(useLocalDeviceOnly.isChecked());
            }
        });

        displayOverlaidSymbols.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                optionsAgent.getOptions().setDisplayOverlaidSymbols(displayOverlaidSymbols.isChecked());
            }
        });

        restoreDefaults = (Button) obsScrollView.findViewById(R.id.defaultOptions);

        restoreDefaults.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    optionsAgent.restoreDefaultOptionsToFile();
                } catch (IOException exc) {
                    // TODO
                }

                updateUiState();
            }
        });

        dismiss = (Button) obsScrollView.findViewById(R.id.dismissOptions);

        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateData()) {
                    FragmentManager fragMan = getFragmentManager();
                    fragMan.popBackStack();
                }
            }
        });

        return obsScrollView;
    }

    /* Validate data in the view
     *
     * @returns     true iff validated
     */
    private boolean validateData() {
        /* Validate parser timeout */
        String text = parserTimeoutSeconds.getText().toString();

        long parserTimeoutMillis = -1L;
        boolean isValueValid = true;
        try {
            parserTimeoutMillis = (long) ((float) (Float.parseFloat(text) * 1000.f));
        } catch (NumberFormatException exc) {
            isValueValid = false;
        }

        if (isValueValid && parserTimeoutMillis <= 0) {
            isValueValid = false;
        }

        if (!isValueValid) {
            AlertDialog alertDialog = new AlertDialog.Builder(ctx)
                    .setTitle(R.string.opt_caption_parser_timeout_no_units)
                    .setMessage(R.string.invalid_numeric_value)
                    .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                            }
                        }
                    ).show();

            parserTimeoutSeconds.setFocusableInTouchMode(true);
            parserTimeoutSeconds.requestFocus();

            return false;
        } else {
            optionsAgent.getOptions().setParserTimeoutMillis(parserTimeoutMillis);

            return true;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        updateUiState();
    }

    @Override
    public void onStop() {
        Bus bus = EventBus.getInstance();
        bus.post(optionsAgent.getOptions());

        try {
            optionsAgent.writeOptionsToFile();
        } catch (IOException ioExc) {

        }

        FragmentHelper.postBackToMainViewEvent();

        super.onStop();
    }

    private void updateUiState() {
        PlatoNotesOptions options = optionsAgent.getOptions();

        useLocalDeviceOnly.setChecked(options.isUseLocalDeviceOnly());
        displayOverlaidSymbols.setChecked(options.isDisplayOverlaidSymbols());
        parserTimeoutSeconds.setText(Float.toString((float) options.getParserTimeoutMillis() / 1000.0f));
    }
}
