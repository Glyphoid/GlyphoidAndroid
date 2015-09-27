package me.scai.plato.android.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import me.scai.parsetree.TokenSetParserOutput;
import me.scai.plato.MathJaxWebClient.MathJaxWebClient;
import me.scai.plato.android.responses.MathJaxClientTaskResponse;
import me.scai.plato.android.tasks.MathJaxWebClientAsyncTask;
import me.scai.plato.android.requests.MathJaxWebClientTaskRequest;
import me.scai.plato.android.R;
import me.scai.plato.android.helpers.FragmentHelper;
import me.scai.plato.android.helpers.NetworkStatusHelper;
import me.scai.plato.android.listeners.DismissClickListener;


public class ParserView extends Fragment {
    private enum Mode {
        MathTex,
        MathML
    }

    /* Member variables */
    private Context ctx;
    private ClipboardManager clipManager;

    private Mode currentMode;

    private ObservableScrollView obsScrollView;

    private TokenSetParserOutput tokenSetParserOutput;

    private EditText etStringizerResult;
    private EditText etEvaluatorResult;
    private EditText etMathMarkup;

    private TextView mathMarkupLabel;

    private Button copyStringizerResult;
    private Button copyEvaluatorResult;
    private Button copyMathTex;
    private Button generateMathML;
    private Button generateImage;
    private Button dismiss;

    private ProgressDialog mathMLProgressDialog;

    private GeneratedImageView generatedImageView;

    private String mathTex;

    /* UI listeners */
    private final OnClickListener copyClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ClipData clip = null;
            String copiedDataType = null;
            switch (v.getId()) {
                case R.id.copyStringizerResult:
                    clip = ClipData.newPlainText("PlatoStringizerOutput", etStringizerResult.getText());
                    copiedDataType = ctx.getString(R.string.ui_parser_parsing_result).toLowerCase();

                    break;
                case R.id.copyEvaluatorResult:
                    clip = ClipData.newPlainText("PlatoEvaluatorOutput", etEvaluatorResult.getText());
                    copiedDataType = ctx.getString(R.string.ui_parser_math_ml).toLowerCase();

                    break;
                case R.id.copyMathTex:
                    clip = ClipData.newPlainText("PlatoMathTex", etMathMarkup.getText());
                    if (currentMode == Mode.MathTex) {
                        copiedDataType = ctx.getString(R.string.ui_parser_math_tex);
                    } else {
                        copiedDataType = ctx.getString(R.string.ui_parser_math_ml);
                    }

                    break;
                default:
                    break;
            }

            if (clip != null) {
                clipManager.setPrimaryClip(clip);

                Toast.makeText(ctx,
                        String.format(ctx.getString(R.string.copied_to_clipboard_template, copiedDataType)),
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final OnClickListener generateMathMLListener = new OnClickListener() {
        @Override
        public void onClick(View view) {

            if (currentMode == Mode.MathTex) {
                /* Current state is Math Tex: To generate Math ML */
                if (NetworkStatusHelper.isNetworkAvailable(ctx)) {
                    // Internet is available
                    String mathJaxEndpointUrl = ctx.getString(R.string.mathjax_endpoint);
                    MathJaxWebClient mathJaxWebClient = new MathJaxWebClient(mathJaxEndpointUrl);

                    MathJaxWebClientTaskRequest request = new MathJaxWebClientTaskRequest(mathJaxWebClient,
                            etMathMarkup.getText().toString(),
                            MathJaxWebClientAsyncTask.getFormatStringMathML());

                    new MathJaxWebClientAsyncTask().execute(request);

                    mathMLProgressDialog = ProgressDialog.show(ctx,
                            "",
                            ctx.getString(R.string.waiting_for_mathjax_server_response),
                            true,
                            false); // TODO: Make cancellable

                } else {
                    showNoInternetAlertDialog();
                }
            } else {
                /* Current state is MathML, to roll back to Math Tex */
                mathMarkupLabel.setText(ctx.getString(R.string.ui_parser_math_tex));
                generateMathML.setText(ctx.getString(R.string.ui_parser_math_ml));
                etMathMarkup.setText(mathTex);

                currentMode = Mode.MathTex;
            }
        }
    };

    public void postMathJaxClientTaskResponse(MathJaxClientTaskResponse response) {
        if (mathMLProgressDialog != null) {
            mathMLProgressDialog.dismiss();
        }

        if (response.getFormat().equals(MathJaxWebClientAsyncTask.getFormatStringMathML())) {
            etMathMarkup.setText(response.getConversionResult());

            mathMarkupLabel.setText(ctx.getString(R.string.ui_parser_math_ml));
            generateMathML.setText(ctx.getString(R.string.ui_parser_math_tex));
            currentMode = Mode.MathML;
        }
    }

    private final OnClickListener generateImageListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (NetworkStatusHelper.isNetworkAvailable(ctx)) {
                // Internet is available
                showGeneratedImageView();
            } else {
                // Internet is not available
                showNoInternetAlertDialog();
            }

        }
    };

    private void showNoInternetAlertDialog() {

        AlertDialog.Builder noInternetAlert = new AlertDialog.Builder(ctx);
        noInternetAlert.setTitle(ctx.getString(R.string.no_internet_connection));
        noInternetAlert.setMessage(ctx.getString(R.string.conversion_requires_internet));

        noInternetAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        noInternetAlert.show();
    }

    /* Methods */
    /* Constructor */
    public ParserView(Context ctx, GeneratedImageView generatedImageView) {
        super();

        /* Obtain clip service */
        this.ctx = ctx;
        clipManager = (ClipboardManager) this.ctx.getSystemService(Context.CLIPBOARD_SERVICE);

        this.generatedImageView = generatedImageView;
    }

    private void showGeneratedImageView() {
        final String GENERATED_IMAGE_VIEW_NAME = "GeneratedImageView";

        generatedImageView.setMathTex(etMathMarkup.getText().toString());

        /* The fragment approach */
        FragmentManager fragMan = getFragmentManager();

        boolean toShow = true;
//        int stackHeight = fragMan.getBackStackEntryCount();
//        if (stackHeight == 0) {
//            toShow = true;
//        } else {
//            /* If the fragment is already being shown, do not push more instances of it to the stack */
//            toShow = !fragMan.getBackStackEntryAt(stackHeight - 1).getName().equals(GENERATED_IMAGE_VIEW_NAME);
//        }

        if (toShow) {
            FragmentTransaction fragTrans = fragMan.beginTransaction();

            fragTrans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_right);
            fragTrans.replace(R.id.trainerLayout, generatedImageView);
            fragTrans.addToBackStack(GENERATED_IMAGE_VIEW_NAME);
            fragTrans.commit();
        }
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
        obsScrollView = (ObservableScrollView) inflater.inflate(R.layout.parser_fragment, container, false);

        mathMarkupLabel      = (TextView) obsScrollView.findViewById(R.id.mathMarkupLabel);

        etStringizerResult   = (EditText) obsScrollView.findViewById(R.id.stringizerResult);
        etEvaluatorResult    = (EditText) obsScrollView.findViewById(R.id.evaluatorResult);
        etMathMarkup         = (EditText) obsScrollView.findViewById(R.id.mathMarkup);

        copyStringizerResult = (Button) obsScrollView.findViewById(R.id.copyStringizerResult);
        copyEvaluatorResult  = (Button) obsScrollView.findViewById(R.id.copyEvaluatorResult);
        copyMathTex          = (Button) obsScrollView.findViewById(R.id.copyMathTex);

        dismiss              = (Button) obsScrollView.findViewById(R.id.dismissParser);
        generateMathML       = (Button) obsScrollView.findViewById(R.id.generateMathML);
        generateImage        = (Button) obsScrollView.findViewById(R.id.generateSVG);

        /* Set click listeners to copy buttons */
        copyStringizerResult.setOnClickListener(copyClickListener);
        copyEvaluatorResult.setOnClickListener(copyClickListener);
        copyMathTex.setOnClickListener(copyClickListener);

        dismiss.setOnClickListener(new DismissClickListener(getFragmentManager()));
        generateMathML.setOnClickListener(generateMathMLListener);
        generateImage.setOnClickListener(generateImageListener);

        obsScrollView.setScrollViewListener(new ParserViewScollListener());

        return obsScrollView;
    }

    private void updateGenerateImageEnabled() {
        final boolean toEnable = (tokenSetParserOutput != null) && (tokenSetParserOutput.getErrorMsg() == null || tokenSetParserOutput.getErrorMsg().isEmpty());

        if (generateMathML != null) {
            generateMathML.setEnabled(toEnable);
        }

        if (generateImage != null) {
            generateImage.setEnabled(toEnable);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (tokenSetParserOutput != null) {
            if (tokenSetParserOutput.getErrorMsg() != null &&
                tokenSetParserOutput.getErrorMsg().length() != 0) {
                setTextColor(Color.rgb(255, 128, 0));

                etStringizerResult.setText(tokenSetParserOutput.getErrorMsg());
                etEvaluatorResult.setText("");
                etMathMarkup.setText("");
            } else {
                setTextColor(Color.rgb(255, 255, 255));

                setStringizerResult();
                setEvaluatorResult();
                setMathTex();
            }
        }

        updateGenerateImageEnabled();
    }

    @Override
    public void onStop() {
        FragmentHelper.postBackToMainViewEvent();

        super.onStop();
    }

    private void setTextColor(int clr) {
        etStringizerResult.setTextColor(clr);
        etEvaluatorResult.setTextColor(clr);
        etMathMarkup.setTextColor(clr);
    }

    /* Unique methods */
    /* Set token set parser output */
    public void setTokenSetParserOutput(TokenSetParserOutput tokenSetParserOutput) {
        this.tokenSetParserOutput = tokenSetParserOutput;
    }

    /* Set text to stringizer result */
    private void setStringizerResult() {
        etStringizerResult.setText(tokenSetParserOutput.getStringizerOutput());
    }

    /* Set text to evaluator result */
    private void setEvaluatorResult() {
        etEvaluatorResult.setText(tokenSetParserOutput.getEvaluatorOutput());
    }

    /* Set text to Math Tex */
    private void setMathTex() {
        mathTex = tokenSetParserOutput.getMathTex();
        etMathMarkup.setText(mathTex);

        currentMode = Mode.MathTex;
    }

    class ParserViewScollListener implements ScrollViewListener {
        @Override
        public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {

        }
    }
}
