package me.scai.plato.android.activities;

import java.io.File;
import java.io.IOException;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import me.scai.handwriting.CWrittenTokenSet;
import me.scai.parsetree.HandwritingEngineException;
import me.scai.parsetree.TokenSetParserOutput;
import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.plato.AndroidWebCient.AndroidPlatoWebClient;
import me.scai.plato.AndroidWebCient.PlatoNotesAction;
import me.scai.plato.android.options.PlatoNotesOptions;
import me.scai.plato.android.options.PlatoNotesOptionsAgent;
import me.scai.plato.android.events.BackToMainViewEvent;
import me.scai.plato.android.events.CreateAndroidWebClientFailureEvent;
import me.scai.plato.android.events.GeneratedImageViewEvent;
import me.scai.plato.android.events.PlatoOperationCancellationEvent;
import me.scai.plato.android.helpers.GsonHelper;
import me.scai.plato.android.helpers.NetworkStatusHelper;
import me.scai.plato.android.helpers.TokenNameHelper;
import me.scai.plato.android.responses.MathJaxClientTaskResponse;
import me.scai.plato.android.utils.EventBus;
import me.scai.plato.android.views.FineActionsPanel;
import me.scai.plato.android.tasks.MathJaxWebClientAsyncTask;
import me.scai.plato.android.responses.PlatoClientTaskResponse;
import me.scai.plato.android.R;
import me.scai.plato.android.events.WrittenTokenSetChangedEvent;
import me.scai.plato.android.events.ForceSetTokenNameEvent;
import me.scai.plato.android.views.PlatoBoard;
import me.scai.plato.android.events.TokenRecogCandidatesEvent;
import me.scai.plato.android.views.GeneratedImageView;
import me.scai.plato.android.views.LetterSetView;
import me.scai.plato.android.views.OptionsView;
import me.scai.plato.android.views.ParserView;
import me.scai.plato.android.views.VarMapView;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.app.FragmentManager;
import android.app.FragmentTransaction;

import com.google.gson.JsonObject;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class ThulikaTrainerActivity extends Activity
	implements LetterSetView.OnSelectSavedTokenListener
{
    /* Member variables */
	private static final String COMMA_STRING = "&COMMA;";
	private PlatoBoard trainer;
	
	private final String LETTERSOURCE = "alpha.txt";
	
	/* Set of tokens (also called "letters") */
	String [] letters;
	String [] letterDisplayNames;

    /* App configurations */
    private int createEngineMaxNumAttempts = 3;
    private int createEngineAttemptCount = 0;

	/* Token engine and graphical parser resource locations */
	private static final  String RESOURCES_DIR = "resources";
	private static final String TERMINALS_FILE = "graph_lang/terminals.txt";
	private static final String TERMINALS_FILE_NAME = "terminals.json";
	private static final String PRODCTIONS_FILE = "graph_lang/productions.txt";
	private static final String PRODUCTIONS_FILE_NAME = "productions.txt";
	private static final String RESOURCES_CONFIG_DIR = "config";
	private static final String STROKE_CURATOR_CONFIG_FILE = "stroke_curator_config.json";

    private static final String RESOURCES_TOKEN_ENGINE_DIR = "token_engine";
    private static final String TOKEN_ENGINE_FILE_NAME = "token_engine.sdv.sz0_whr0_ns1.ser";
    private URL tokenEngineFileUrl;

	/* UI members */
    private FineActionsPanel fineActionsPanel;
    private LinearLayout topCommandPanel;
    private LinearLayout bottomCommandPanel;

    private ProgressDialog initializationProgressDialog;
	private ProgressDialog mProgressDialog;
    private ProgressDialog parserProgressDialog;

	private Dialog dlgTokEng;
    private Button mergeStrokesButton;
    private Button unmergeStrokesButton;
	private Button dlgTokEngButtonOK;
	private Button dlgTokEngButtonCancel;

    private Button parseTokenSetButton;
    private Button clearButton;
    private Button clearAllButton;

    private ImageButton optionsButton;
    private ImageButton historyButton;
    private ImageButton varMapImageButton;

    private boolean parserProgressDialogShown;

    /* Sub views */
	private LetterSetView letterSetView;
    private OptionsView optionsView;
    private ParserView parserView;
    private VarMapView varMapView;
    private GeneratedImageView generatedImageView;

    private AtomicReference<ParserProgressDisplayAsyncTask> parserProgressDisplayAsyncTask =
            new AtomicReference<ParserProgressDisplayAsyncTask>();

    private PlatoNotesOptionsAgent optionsAgent;

    private Bus eventBus;

    private AtomicBoolean webActionPending = new AtomicBoolean();

	/* ************** Methods ************** */

    /* UI event listeners */
    private final OnClickListener clearAllListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            trainer.act(PlatoNotesAction.ClearStrokes);
            trainer.resetBoardZoomPan();

            fineActionsPanel.clear();
        }
    };

    private final OnClickListener removeLastTokenListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            trainer.act(PlatoNotesAction.RemoveLastToken);

            fineActionsPanel.clear();
        }
    };

    private final OnClickListener mergeStrokesAsTokenListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Button b = (Button) v;

            if (b == mergeStrokesButton) {
                trainer.act(PlatoNotesAction.MergeStrokesAsToken, "2");
            } else {
                final int buttonIdx = fineActionsPanel.getMergeLastStrokesButtons().indexOf(b);
                if (buttonIdx != -1) {
                    trainer.act(PlatoNotesAction.MergeStrokesAsToken , Integer.toString(2 + buttonIdx));
                } else {
                    throw new IllegalStateException("Unexpected button pressed for merge-strokes-as-token action");
                }
            }

            fineActionsPanel.clear();
        }
    };

    private OnClickListener forceSetTokenNameListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Button button = (Button) v;
            final String tokenName = button.getText().toString(); /* TODO: Translate between internal name and display name */

            /* Swap name */
            CWrittenTokenSet wtSet = trainer.getWrittenTokenSet();
            String currLastTokenName = wtSet.recogWinners.get(wtSet.recogWinners.size() - 1);

            TokenNameHelper tokenNameHelper = TokenNameHelper.createOrGetInstance(letters, letterDisplayNames);
            final String currLastTokenDisplayName = tokenNameHelper.tokenName2DisplayName(currLastTokenName);

            button.setText(currLastTokenDisplayName);

            trainer.act(PlatoNotesAction.ForceSetTokenName, tokenName); /* 2 args: set last token */
        }
    };

    private OnClickListener forceSetTokenNameInputDialogListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            FragmentManager fragMan = getFragmentManager();
            FragmentTransaction fragTrans = fragMan.beginTransaction();

            hideCommandPanels();

            letterSetView.setMode(LetterSetView.LetterSetViewMode.ForceSetTokenName);
            fragTrans.replace(R.id.trainerLayout, letterSetView); 	/* fragment test */
            fragTrans.addToBackStack(null);
            fragTrans.commit();
        }
    };

    private final OnClickListener unmergeLastStrokeListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            trainer.act(PlatoNotesAction.UnmergeLastStroke);

            fineActionsPanel.clear();
        }
    };
	
	/* Create graphical token-set parser */
	private void createTokenSetParser() {
		String dirName = getExternalFilesDir(null).getPath();
		dirName = dirName.endsWith("/") ? dirName : dirName + "/";
		
		String terminalsFN = dirName + TERMINALS_FILE;
		String productionsFN = dirName + PRODCTIONS_FILE;		
		
		URL terminalsConfigUrl = this.getClass().getResource(File.separator + RESOURCES_DIR + 
															 File.separator + RESOURCES_CONFIG_DIR + 
															 File.separator + TERMINALS_FILE_NAME);
		URL productionsConfigUrl = this.getClass().getResource(File.separator + RESOURCES_DIR + 
				                                               File.separator + RESOURCES_CONFIG_DIR + 
				                                               File.separator + PRODUCTIONS_FILE_NAME);		
		
//		trainer.createParser(terminalsFN, productionsFN);
		trainer.createParser(terminalsConfigUrl, productionsConfigUrl);
		
		/* Load stroke curator configuration from JSON config file */
		URL strokeCuratorConfigUrl = this.getClass().getResource(File.separator + RESOURCES_DIR +  
							                                     File.separator + RESOURCES_CONFIG_DIR + 
				                                                 File.separator + STROKE_CURATOR_CONFIG_FILE);	
		trainer.createStrokeCurator(strokeCuratorConfigUrl, trainer.tokEngine);

	}

    private final Button generateButton(final String text, final OnClickListener onClickListener) {
        final float btnTextSize = 10.0f;
        final LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                              LinearLayout.LayoutParams.WRAP_CONTENT);

        Button btn = new Button(getBaseContext());
        btn.setText(text);
        btn.setTextSize(btnTextSize);
        btn.setLayoutParams(layoutParams);
        btn.setOnClickListener(onClickListener);

        return btn;
    }

    private class ParserProgressDisplayWaitingPeriodEndsToken {}

    private class ParserProgressDisplayAsyncTask extends AsyncTask<Void, Void, String> {
        private long waitPeriodMillis = 1000L;

        @Override
        public String doInBackground(Void... args) {
            try {
                Thread.sleep(waitPeriodMillis);
            } catch (InterruptedException exc) {}

            return "";
        }

        @Override
        public void onPostExecute(String result) {
            /* Get current time and calculate elapsed time */
            Bus eventBus = EventBus.getInstance();
            eventBus.post(new ParserProgressDisplayWaitingPeriodEndsToken());
        }

    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("onCreate: Calling super.onCreate");
        super.onCreate(savedInstanceState);
        System.out.println("onCreate: DONE Calling super.onCreate");

        /* Create or get the options */
        optionsAgent = new PlatoNotesOptionsAgent(getExternalFilesDir(null).getPath());
        try {
            optionsAgent.createOrGetOptionsFromFile();
        } catch (IOException ioExc) {}

        /* Remove title bar */
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //setContentView(R.layout.main);
        setContentView(R.layout.trainer);

        System.out.println("onCreate: Calling EventBus.getInstance()");
        eventBus = EventBus.getInstance();

        /* Get handles to UI controls */
        topCommandPanel = (LinearLayout) findViewById(R.id.topCommandPanel);
        bottomCommandPanel = (LinearLayout) findViewById(R.id.cPanel);

        fineActionsPanel  = new FineActionsPanel(this, trainer);

        optionsButton = (ImageButton) findViewById(R.id.optionsButton);
        historyButton = (ImageButton) findViewById(R.id.historyButton);
        varMapImageButton = (ImageButton) findViewById(R.id.varMapImageButton);
        
        trainer = (PlatoBoard) findViewById(R.id.trainer1);
                
		letters = getResources().getStringArray(R.array.letters);
		letterDisplayNames = getResources().getStringArray(R.array.letters_display_names);
		trainer.createWrittenTokenSetDrawer();

        TokenNameHelper.createOrGetInstance(letters, letterDisplayNames);

		/* Merge button */
		mergeStrokesButton = (Button) findViewById(R.id.mergeStrokesButton);
		// TODO: Activate state
		mergeStrokesButton.setOnClickListener(mergeStrokesAsTokenListener);
        mergeStrokesButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                fineActionsPanel.toggleMergeFineActions(unmergeLastStrokeListener,
                        mergeStrokesAsTokenListener);

                return true;
            }

        });

        /* Unmerge button */
        unmergeStrokesButton = (Button) findViewById(R.id.unmergeStrokesButton);
        unmergeStrokesButton.setOnClickListener(unmergeLastStrokeListener);
		
		/* Button: clear token set */
		clearButton = (Button) findViewById(R.id.clearButton);
		clearButton.setOnClickListener(removeLastTokenListener);

        clearAllButton = (Button) findViewById(R.id.clearAllButton);
        clearAllButton.setOnClickListener(clearAllListener);

        clearButton.setOnLongClickListener(new View.OnLongClickListener() {
           @Override
           public boolean onLongClick(View view) {
               fineActionsPanel.toggleClearFineActions(removeLastTokenListener, clearAllListener);

               return true;
           }
        });

        /* ImageButton: Plato ImageButton */
        /* TODO: Put the callback in the right place */
        /* TODO: Place the right callback here */
        optionsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionsView();
            }
        });

        historyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showParserView();
            }
        });

        varMapImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showVarMapView();
            }
        });
		
		/* Button: parse token set */
		parseTokenSetButton = (Button) findViewById(R.id.parseTokenSetButton);
		parseTokenSetButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
                disableUIControls();
                setParserProgressDialogShown(true);
                parserProgressDialog = new ProgressDialog(trainer.getContext());
                parserProgressDialog.setMessage(getString(R.string.parser_operation_in_progress));
                parserProgressDialog.setCancelable(false);
                parserProgressDialog.setCanceledOnTouchOutside(false);
                parserProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                        (DialogInterface.OnClickListener) null);
                parserProgressDialog.show();

                Button cancelButton = parserProgressDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//                        eventBus.post(new PlatoOperationCancellationEvent());
                        parserProgressDialog.setMessage(getString(R.string.operation_cancelling));

                        System.out.println("Cancel button clicked. Calling cancelParserAsyncTask()");
                        trainer.cancelParserAsyncTask();
                    }
                });


//                "",
//                        "Parsing...",
//                        true,
//                        true

                trainer.invalidate();

                trainer.act(PlatoNotesAction.ParseTokenSet);
			}
		});

        System.out.println("onCreate: Calling ProgressDialog.show");
        initializationProgressDialog = ProgressDialog.show(trainer.getContext(),
                                                           "",
                                                           getString(R.string.reading_handwriting_recog_engine),
                                                           true,
                                                           false);

        System.out.println("onCreate: Calling trainer.invalidate()");
        trainer.invalidate();

        /* *** */

        System.out.println("onCreate: Creating LetterSetView instance");
        /* Create letter set view */
        letterSetView = new LetterSetView(letters, letterDisplayNames);

        System.out.println("onCreate: Creating GeneratedImageView instance");
        /* Create generated-image view */
        generatedImageView = new GeneratedImageView(this);

        System.out.println("onCreate: Creating ParserView instance");

        /* Create options view */
        optionsView = new OptionsView(this, optionsAgent);

        /* Create parser view */
        parserView = new ParserView(this, generatedImageView);

        System.out.println("onCreate: Creating VarMapView instance");
        /* Create var map view */
        varMapView = new VarMapView(getBaseContext());

		/* Create web client */

        System.out.println("Calling NetworkStatusHelper.isNetworkAvailable");
        if (NetworkStatusHelper.isNetworkAvailable(this)) {
            createEngineAttemptCount = 0;

            initializationProgressDialog.setMessage(getString(R.string.obtaining_plato_web_session));
            trainer.invalidate();

            /* Network appears to be available, try securing Plato web engine */
            (new CreateAndroidPlatoWebClientAsync()).execute();
        } else {
            initializationProgressDialog.dismiss();
            setPlatoStatusIcon(false); // TODO: This line may be causing Fatal signal 11 (SIGSEGV) at 0x3f800004 (code=1), thread 8473 (FinalizerDaemon)

            Toast.makeText(getBaseContext(),
                    getString(R.string.no_internet_connection) +
                    getString(R.string.use_local_compute_resource),
                    Toast.LENGTH_LONG).show();
        }

//		trainer.setWebClientStatusDisplay((TextView) findViewById(R.id.textParserOutput));
		trainer.setBaseContext(getBaseContext());

        try {
            (new ReadTokenEngineAsync()).execute().get();
        } catch (InterruptedException intExc) {

        } catch (ExecutionException execExc) {

        }

        /* UI initialize */
        uiStateInitialize();


    }

    @Override
    public void onStart() {
        super.onStart();

        try {
            System.out.println("onStart: Calling eventBus.register");
            eventBus.register(this);
            System.out.println("onStart: DONE Calling eventBus.register");
        } catch (Exception exc) {

        }


        eventBus.post(optionsAgent.getOptions());

        trainer.invalidate();

//        try {
//            tokenEngineFileUrl = new File(getExternalFilesDir(null).getPath() + File.separator + "engine" +
//                    TOKEN_ENGINE_FILE_NAME).toURI().toURL();
//        } catch (MalformedURLException exc)


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        System.out.println("onSaveInstanceState: Calling putString");
        outState.putString(getString(R.string.plato_board_state_key),
                           GsonHelper.toJson(trainer.getStateSerialization()));
        System.out.println("onSaveInstanceState: DONE Calling putString");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        System.out.println("onRestoreInstanceState: Calling injectSerializedState");

        trainer.injectSerializedState(GsonHelper.parseJson(
                savedInstanceState.getString(getString(R.string.plato_board_state_key))).getAsJsonObject());

        System.out.println("onRestoreInstanceState: DONE Calling injectSerializedState");

        eventBus.post(new WrittenTokenSetChangedEvent());
    }

    @Override
    public void onStop() {

        /* Unregister event bus */
        try {
            System.out.println("onStop: Calling eventBus.unregister");
            eventBus.unregister(this);
            System.out.println("onStop: DONE calling eventBus.unregister");
        } catch (Exception exc) {}

        /* Mandatory call to super */
        super.onStop();
    }


    @Override
    public void onResume() {
        System.out.println("Calling super.onResume()");
        super.onResume();
        System.out.println("DONE Calling super.onResume()");

//        try {
//            eventBus.register(this);
//        } catch (Exception exc) {
//
//        }
    }

    @Override
    public void onPause() {
        super.onPause();

//        try {
//            eventBus.unregister(this);
//        } catch (Exception exc) {
//
//        }
    }

    @Override
    public void onDestroy() {
        /* Remove web client */
        if ( isFinishing() ) {
            trainer.cancelWebClient();
        }

        super.onDestroy();
    }


    private void showSaveTokenFragment() {
        /* The fragment approach */
        FragmentManager fragMan = getFragmentManager();
        FragmentTransaction fragTrans = fragMan.beginTransaction();

        hideCommandPanels();

        letterSetView.setMode(LetterSetView.LetterSetViewMode.SaveToken);
        fragTrans.replace(R.id.trainerLayout, letterSetView); 	/* fragment test */
        fragTrans.addToBackStack(null);
        fragTrans.commit();
    }

    private void showOptionsView(){
        final String OPTIONS_VIEW_NAME = "OptionsView";

        /* The fragment approach */
        FragmentManager fragMan = getFragmentManager();

        // TODO: Code de-duplication with "showParserView"
        boolean toShow = false;
        int stackHeight = fragMan.getBackStackEntryCount();
        if (stackHeight == 0) {
            toShow = true;
        } else {
                    /* If the fragment is already being shown, do not push more instances of it to the stack */
            toShow = !fragMan.getBackStackEntryAt(stackHeight - 1).getName().equals(OPTIONS_VIEW_NAME);
        }

        if (toShow) {
            FragmentTransaction fragTrans = fragMan.beginTransaction();

            hideCommandPanels();

            fragTrans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_right);
            fragTrans.replace(R.id.trainerLayout, optionsView);
            fragTrans.addToBackStack(OPTIONS_VIEW_NAME);
            fragTrans.commit();
        }
    }

    private void showParserView() {
        final String PARSER_VIEW_NAME = "ParserView";

        /* The fragment approach */
        FragmentManager fragMan = getFragmentManager();

        boolean toShow = false;
        int stackHeight = fragMan.getBackStackEntryCount();
        if (stackHeight == 0) {
            toShow = true;
        } else {
                    /* If the fragment is already being shown, do not push more instances of it to the stack */
            toShow = !fragMan.getBackStackEntryAt(stackHeight - 1).getName().equals(PARSER_VIEW_NAME);
        }

        if (toShow) {
            FragmentTransaction fragTrans = fragMan.beginTransaction();

            hideCommandPanels();

            fragTrans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_right);
            fragTrans.replace(R.id.trainerLayout, parserView);
            fragTrans.addToBackStack(PARSER_VIEW_NAME);
            fragTrans.commit();
        }
    }

    private void showVarMapView() {
        trainer.act(PlatoNotesAction.GetVarMap);
    }

    private void uiStateInitialize() {
        parseTokenSetButton.setVisibility(View.GONE);
        clearButton.setVisibility(View.GONE);
        clearAllButton.setVisibility(View.GONE);
        mergeStrokesButton.setVisibility(View.GONE);
        unmergeStrokesButton.setVisibility(View.GONE);

        fineActionsPanel.clear();
        trainer.invalidate();
    }

    private void disableUIControls() {
        parseTokenSetButton.setEnabled(false);
        clearButton.setEnabled(false);
        clearAllButton.setEnabled(false);
        mergeStrokesButton.setEnabled(false);
        unmergeStrokesButton.setEnabled(false);

        optionsButton.setEnabled(false);
        historyButton.setEnabled(false);
        varMapImageButton.setEnabled(false);
    }

    private void enableUIControls() {
        parseTokenSetButton.setEnabled(true);
        clearButton.setEnabled(true);
        clearAllButton.setEnabled(true);
        mergeStrokesButton.setEnabled(true);
        unmergeStrokesButton.setEnabled(true);

        optionsButton.setEnabled(true);
        historyButton.setEnabled(true);
        varMapImageButton.setEnabled(true);
    }

    /* AsyncTask for reading token engine */
    private class ReadTokenEngineAsync extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            System.out.println("onCreate: Getting value of tokenEngineFileUrl");
            tokenEngineFileUrl = this.getClass().getResource(File.separator + RESOURCES_DIR +
                    File.separator + RESOURCES_TOKEN_ENGINE_DIR +
                    File.separator + TOKEN_ENGINE_FILE_NAME);

            System.out.println("onCreate: Reading token engine");
            trainer.readTokenEngine(tokenEngineFileUrl);
            System.out.println("onCreate: Calling setCanvasTokenNames");
            trainer.setCanvasTokenNames();
            System.out.println("onCreate: DONE Reading token engine");

            /* Prepare local token-set parser */
            createTokenSetParser();

            return "";
        }
    }

    /* Async task for creating the Plato web client */
    private class CreateAndroidPlatoWebClientAsync extends AsyncTask<JsonObject, Void, AndroidPlatoWebClient> {
        @Override
        protected AndroidPlatoWebClient doInBackground(JsonObject... params) {
            AndroidPlatoWebClient webClient = null;
			try {
				trainer.createPlatoWebClient();
                webClient = trainer.getWebClient();
			} catch (HandwritingEngineException exc) {}

            return webClient;
        }

        @Override
        protected void onPostExecute(AndroidPlatoWebClient webClient) {
            Bus eventBus = EventBus.getInstance();

            if (webClient != null) {
                eventBus.post(webClient);
            } else {
                eventBus.post(new CreateAndroidWebClientFailureEvent());
            }
		}

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onStart();
//    	trainer.init();
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.trainmenu, menu);
        return true;
    }
    
    private class trainTokenEngineTask extends AsyncTask<PlatoBoard, Integer, Long> {
    	PlatoBoard trainer = null;
    	
    	protected void onPreExecute() {
    		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	}
    	
    	protected Long doInBackground(PlatoBoard... ts) {
    		mProgressDialog.setProgress(0);    		
    		
    		if ( trainer == null ) {
    			trainer = ts[0];
    		}
    		
    		/* TODO: Produce warning that training TokenRecogEngine is not
    		 * supported on mobile device; or remove this option altogether. 
    		 */
//    		trainer.trainTokenEngine(mProgressDialog);
    		
    		return 0L;
    	}
    	
    	protected void onProgressUpdate(Integer... progress) {
    		//mProgressDialog.setProgress(5000);
    	}
    	
    	protected void onPostExecute(Long result) {
    		mProgressDialog.dismiss();
    		  		
    		Toast msg = Toast.makeText(getBaseContext(),"Trained token engine has been saved.", Toast.LENGTH_SHORT);
			msg.show();
			
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    		    		
    		//recognizeB.setActivated(trainer.isReadyToRecognize());
    	}
    }
    
    /* Parameterized callback function for context menu OK button */
    class ContextMenuOKButtonListener implements DialogInterface.OnClickListener {
    	private String paramName = null;
    	private String dialogText = null;
    	private View dataView = null;
    	
    	/* Constructor */
    	public ContextMenuOKButtonListener(String t_paramName, View t_dataView) {
    		paramName = t_paramName;
    		dataView = t_dataView;
    	}
    	
		@Override
	    public void onClick(DialogInterface dialog, int which) {
			if ( paramName.equals("trainThreshErr") ) {
				TextView editTextInput = (TextView) dataView;
				dialogText = editTextInput.getText().toString();
				Double new_trainThreshErr = Double.parseDouble(dialogText);			
				if ( new_trainThreshErr > 0.0 && new_trainThreshErr < 1.0 ) {
					trainer.setTrainThreshErr(new_trainThreshErr);
					Toast msg = Toast.makeText(getBaseContext(),
							"Set threshold error rate for token engine to " + new_trainThreshErr, Toast.LENGTH_SHORT);
					msg.show();
				}
				else {
					Toast msg = Toast.makeText(getBaseContext(),
							"Invalid input value for threhsold error rate: " + new_trainThreshErr, Toast.LENGTH_SHORT);
					msg.show();
				}
			}
			else if ( paramName.equals("trainMaxIter") ) {
				TextView editTextInput = (TextView) dataView;
				dialogText = editTextInput.getText().toString();
				Integer new_trainMaxIter = Integer.parseInt(dialogText);
    			if ( new_trainMaxIter > 0 ) {
	        		trainer.setTrainMaxIter(new_trainMaxIter);
	        		Toast msg = Toast.makeText(getBaseContext(),
							"Set maximum iteration for token engine to " + new_trainMaxIter, Toast.LENGTH_SHORT);
					msg.show();
	        	}
	        	else {
	        		Toast msg = Toast.makeText(getBaseContext(),
							"Invalid input value for maximum iteration: " + new_trainMaxIter, Toast.LENGTH_SHORT);
					msg.show();
	        	}
			}
	    }
	}
    
    /* Callback function for context menu OK button */
    class ContextMenuCancelButtonListener implements DialogInterface.OnClickListener {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        dialog.cancel();
	    }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Handle item selection */
        switch (item.getItemId()) {
            case R.id.deleteLastTokenFile:
                trainer.deleteLastLetterFile();
                return true;

        	case R.id.showSummary:
        		trainer.showImFileSummary();
        		return true;

        	case R.id.saveToken:
                showSaveTokenFragment();
        		return true;

	        case R.id.saveTokenSet:
	        	try {
	        		boolean bBrief = true;
	        		trainer.saveTokenSet(bBrief, true);
	        	}
	        	catch (IOException e) {
	        		Toast msg = Toast.makeText(getBaseContext(), "Failed to save the token set", Toast.LENGTH_LONG);
	        		msg.show();
	        	}
	        	finally {
	        		
	        	}

	        	return true;

	        case R.id.tokenEngineSettings:
	        	dlgTokEng = new Dialog(this);
	        	dlgTokEng.setContentView(R.layout.token_engine_settings_dialog);
	        	dlgTokEng.setTitle("Token engine settings");
	        		        	
	        	dlgTokEngButtonOK = 
	        			(Button) dlgTokEng.findViewById(R.id.tokenEngineSettingsButtonOK);
	        	dlgTokEngButtonCancel =
	        			(Button) dlgTokEng.findViewById(R.id.tokenEngineSettingsButtonCancel);
	        	
	        	final EditText editTokEngMaxIter =
	        			(EditText) dlgTokEng.findViewById(R.id.editTextTokenEngineTrainingMaxIter);
	        	editTokEngMaxIter.setText("" + trainer.getTrainMaxIter());
	        	
	        	final EditText editTokEngThreshErr =
	        			(EditText) dlgTokEng.findViewById(R.id.editTextTokenEngineTrainingThreshErr);
	        	editTokEngThreshErr.setText("" + trainer.getTrainThreshErr());
	        		        	
	        	final CheckBox cbIncludeSize = (CheckBox) dlgTokEng.findViewById(R.id.cbIncludeSize);
    			final CheckBox cbIncludeWHRatio = (CheckBox) dlgTokEng.findViewById(R.id.cbIncludeWHRatio);
    			final CheckBox cbIncludeNumStrokes = (CheckBox) dlgTokEng.findViewById(R.id.cbIncludeNumStrokes);
    			
    			cbIncludeSize.setChecked(trainer.getIncludeTokenSize());
    			cbIncludeWHRatio.setChecked(trainer.getIncludeTokenWHRatio());
    			cbIncludeNumStrokes.setChecked(trainer.getIncludeTokenNumStrokes());
	        	
	        	dlgTokEngButtonOK.setOnClickListener(new OnClickListener() {
	        		@Override
	        		public void onClick(View argView) {
	        			/* Feature option */
	        			trainer.setIncludeTokenSize( cbIncludeSize.isChecked() );
	        			trainer.setIncludeTokenWHRatio(cbIncludeWHRatio.isChecked());
	        			trainer.setIncludeTokenNumStrokes( cbIncludeNumStrokes.isChecked() );
	        			
	        			/* Max iteration */	        			
	    				String dialogText = editTokEngMaxIter.getText().toString();
	    				int new_trainMaxIter = Integer.parseInt(dialogText);
	    				if ( new_trainMaxIter != trainer.getTrainMaxIter() ) {
	    					Toast msg = null;
		    				if ( new_trainMaxIter > 0 ) {
		    					trainer.setTrainMaxIter(new_trainMaxIter);
		    					msg = Toast.makeText(getBaseContext(),
		    							"Set maximum iteration for token engine to " + new_trainMaxIter, Toast.LENGTH_SHORT);		    					
		    				}
		    				else {
		    					msg = Toast.makeText(getBaseContext(),
		    							"Invalid input value for maximum iteration: " + new_trainMaxIter, Toast.LENGTH_SHORT);		    					
		    				}
		    				msg.show();
	    				}
	    				
	    				/* Threshold error */        			
	    				dialogText = editTokEngThreshErr.getText().toString();
	    				Double new_trainThreshErr = Double.parseDouble(dialogText);
	    				if ( new_trainThreshErr != trainer.getTrainThreshErr() ) {
	    					Toast msg = null;
	 	    				if ( new_trainThreshErr > 0.0 && new_trainThreshErr < 1.0 ) {
		    					trainer.setTrainThreshErr(new_trainThreshErr);
		    					msg = Toast.makeText(getBaseContext(),
		    							"Set threshold error rate for token engine to " + new_trainThreshErr, Toast.LENGTH_SHORT);
		    				}
		    				else {
		    					msg = Toast.makeText(getBaseContext(),
		    							"Invalid input value for threhsold error rate: " + new_trainThreshErr, Toast.LENGTH_SHORT);		    					
		    				}
	 	    				msg.show();
	    				}
	    				
	        			dlgTokEng.dismiss();
	        			
	        			/* Reload token engine */
	        			trainer.readTokenEngine(tokenEngineFileUrl);
	        		} /* public void onClick(View argView) */
	        	});
	        	
	        	dlgTokEngButtonCancel.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View argView) {
                        dlgTokEng.dismiss();
                    }
                });
	        	dlgTokEng.show();      	
	        
	        	return true;

	        case R.id.trainTokenEngine:
	        	mProgressDialog = new ProgressDialog(this);
	        	mProgressDialog.setCancelable(false);
	        	mProgressDialog.setMessage("Training token engine...");
	        	mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        	mProgressDialog.show();
	     
	        	/* Keep the screen on for the duration of the training */	        	
	        	(new trainTokenEngineTask()).execute(trainer);
	        	
	        	return true;

	        case R.id.reFormatImFiles:
	        	trainer.reFormatImFiles();
	        	
	        	return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private synchronized void setParserProgressDialogShown(final boolean parserProgressDialogShown) {
        this.parserProgressDialogShown = parserProgressDialogShown;
    }

    private synchronized boolean getParserProgressDialogShown() {
        return this.parserProgressDialogShown;
    }

	@Override
	public void onSelectSavedToken(String tokenName) {
		trainer.saveToken(tokenName);

        unhideCommentPanels();

		FragmentManager fragMan = getFragmentManager();
		fragMan.popBackStack();
	}


    /* Respond to events from getVarMap or getFromVarMap */
    @Subscribe
    public void onVarMapResult(PlatoVarMap varMap) {
        final String VAR_MAP_VIEW_NAME = "VarMapView";

        varMapView.setVarMap(varMap);

        System.out.println("varMap = " + varMap);
        // TODO

        FragmentManager fragMan = getFragmentManager();
        FragmentTransaction fragTrans = fragMan.beginTransaction();

        hideCommandPanels();

        fragTrans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_right);
        fragTrans.replace(R.id.trainerLayout, varMapView);
        fragTrans.addToBackStack(VAR_MAP_VIEW_NAME);
        fragTrans.commit();
    }

    /* Respond to candidates event from token recognition (e.g., during add-stroke) */
    @Subscribe
    public void onTokenRecognCandidatesEvent(TokenRecogCandidatesEvent event) {
        final String[] candidates = event.getCandidates();

        if (candidates == null) {
            return;
        }

        fineActionsPanel.setTokenNameCandidates(event.getCandidates(),
                                                forceSetTokenNameListener,
                                                forceSetTokenNameInputDialogListener);
    }

    /* Respond to force set token name input dialog event */
    @Subscribe
    public void onForceSetTokenNameEvent(ForceSetTokenNameEvent event) {
        final String tokenName = event.getTokenName();

        trainer.act(PlatoNotesAction.ForceSetTokenName, tokenName); /* 2 args: set last token */

        unhideCommentPanels();

        FragmentManager fragMan = getFragmentManager();
        fragMan.popBackStack();
    }

    @Subscribe
    public void onParserProgressDisplayWaitPeriodEnds(ParserProgressDisplayWaitingPeriodEndsToken obj) {
        setParserProgressDialogShown(true);
        parserProgressDialog = ProgressDialog.show(ThulikaTrainerActivity.this,
                                                   "",
                                                   getString(R.string.parser_operation_in_progress),
                                                   true,
                                                   false);

    }

    @Subscribe
    public void onBackToMainViewEvent(BackToMainViewEvent event) {
        unhideCommentPanels();
    }

    @Subscribe
    public void onWebClientTaskResponseEvent(PlatoClientTaskResponse resp) {
        if (resp.isEngineInvalid()) {
            webActionPending.set(true);

            // It is possible that the engine has expired or someone has purged it. Try to create
            // a new engine
            CreateAndroidPlatoWebClientAsync restoreEngineAsync
                    = new CreateAndroidPlatoWebClientAsync();

            // This will execute asynchronously
            trainer.orderWebClientStateInjection();
            restoreEngineAsync.execute();

            Toast.makeText(getBaseContext(),
                    getString(R.string.attempting_to_restore_web_session),
                    Toast.LENGTH_SHORT).show();

        } else {
            webActionPending.set(false);

            enableUIControls();

            if (parserProgressDisplayAsyncTask.get() != null) {
                parserProgressDisplayAsyncTask.getAndSet(null).cancel(true);
            }

            if (getParserProgressDialogShown()) {
                parserProgressDialog.dismiss();
                setParserProgressDialogShown(false);
            }

            CWrittenTokenSet writtenTokenSet = resp.getWrittenTokenSet();
            List<int[]> wtConstStrokeIx = resp.getWrittenTokenConstStrokeIdx();

            if (writtenTokenSet != null && wtConstStrokeIx != null) {
                trainer.setWrittenTokenSet(writtenTokenSet, wtConstStrokeIx);
                eventBus.post(new WrittenTokenSetChangedEvent());
            }

            trainer.invalidate();
            int toastLength = Toast.LENGTH_SHORT;

            if (resp.getOriginalAction() == PlatoNotesAction.ParseTokenSet) {
                System.out.println("Received " + PlatoNotesAction.ParseTokenSet + " result PlatoClientTaskResponse");

                TokenSetParserOutput parserOutput = null;
                if (resp.isCancelled()) {
                    System.out.println("parse-token-set result indicates Cancellation");
                    Toast.makeText(getBaseContext(), getString(R.string.operation_cancelled), toastLength).show();
                    parserView.setTokenSetParserOutput(new TokenSetParserOutput(resp.getErrorMessage()));
                } else if (resp.getErrorMessage() != null &&
                        resp.getErrorMessage().contains(getString(R.string.parser_timed_out))) {
                    Toast.makeText(getBaseContext(), getString(R.string.parser_timed_out), toastLength).show();
                    parserView.setTokenSetParserOutput(new TokenSetParserOutput(resp.getErrorMessage()));
                } else {
                    parserOutput = resp.getParserOutput();

                    if (parserOutput == null) {
                        parserOutput = new TokenSetParserOutput(getString(R.string.stringization_failed),
                                getString(R.string.evaluation_failed),
                                getString(R.string.mathtexification_failed));
                        parserOutput.setErrorMsg(getString(R.string.parsing_failed));
                    } else if (parserOutput.getStringizerOutput().toLowerCase().contains("failed") &&
                            parserOutput.getEvaluatorOutput().toLowerCase().contains("failed") &&
                            parserOutput.getMathTex().toLowerCase().contains("failed")) {
                        // TODO: Remove the ad-hocness
                        parserOutput.setErrorMsg(getString(R.string.parsing_failed));
                    }

                /* Set UI data in parserView */
                    parserView.setTokenSetParserOutput(parserOutput);

                    System.out.println("Calling showParserView");
                    showParserView();
                }

                trainer.stopParserKiller();

            }

        }



    }

    @Subscribe
    public void onWrittenTokenSetChanged(WrittenTokenSetChangedEvent event) {
        organizeBottomCommandButtons();
        trainer.invalidate();
    }

    @Subscribe
    public void onGeneratedImageView(GeneratedImageViewEvent event) {
        hideCommandPanels();
    }

    @Subscribe
    public void onMathJaxClientTaskResponse(MathJaxClientTaskResponse response) {
        final String format = response.getFormat();

        if (format.equals(MathJaxWebClientAsyncTask.getFormatImagePng())) {
            generatedImageView.postMathJaxClientTaskResponse(response);
        } else if (format.equals(MathJaxWebClientAsyncTask.getFormatStringMathML())) {
            parserView.postMathJaxClientTaskResponse(response);
        } else {
            throw new IllegalStateException("Unrecognized format in MathJax task response: " + format);
        }
    }

    @Subscribe
    public void onCreateAndroidWebClientCompletionEvent(AndroidPlatoWebClient webClient) {
        if ( !webActionPending.get() ) {
            setPlatoStatusIcon(true);
        } else {
            trainer.actAgain();

            Toast.makeText(getBaseContext(),
                    getString(R.string.successfully_restored_web_session),
                    Toast.LENGTH_SHORT).show();
        }

        if (initializationProgressDialog != null) {
            initializationProgressDialog.dismiss();
            initializationProgressDialog = null;
        }
    }

    @Subscribe
    public void onCreateAndroidWebClientFailureEvent(CreateAndroidWebClientFailureEvent event) {
        if (createEngineAttemptCount++ < createEngineMaxNumAttempts) {
            /* Make another attempt */
            (new CreateAndroidPlatoWebClientAsync()).execute();
        } else {
            /* Give up */
            Toast.makeText(getBaseContext(),
                           getString(R.string.failed_to_obtain_plato_web_session) +
                           getString(R.string.use_local_compute_resource),
                           Toast.LENGTH_LONG).show();

            setPlatoStatusIcon(false);

            initializationProgressDialog.dismiss();
        }
    }

    @Subscribe
    public void onPlatoOperationCancellationEvent(PlatoOperationCancellationEvent event) {
        if (parserProgressDialog != null) {
            parserProgressDialog.setMessage(getString(R.string.operation_cancelling));
        }
    }

    @Subscribe
    public void onPlatoNotesOptions(PlatoNotesOptions options) {
        trainer.setDisplayOverlaidSymbols(options.isDisplayOverlaidSymbols());
    }

    private void setPlatoStatusIcon(boolean isWeb) {
        int statusDrawableId;
        if (isWeb) {
            statusDrawableId = R.drawable.glyphoid_internet_icon_40x40;
        } else {
            statusDrawableId = R.drawable.glyphoid_icon_40x40;
        }

        Resources resources = getResources();

        Bitmap platoStatusIcon = BitmapFactory.decodeResource(resources, statusDrawableId);
        optionsButton.setImageBitmap(platoStatusIcon);

        historyButton.setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.sundial_icon_40x40));
        varMapImageButton.setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.variable_icon_40x40));

    }

    private synchronized void hideCommandPanels() {
        topCommandPanel.setVisibility(View.GONE);
        bottomCommandPanel.setVisibility(View.GONE);
        fineActionsPanel.hide();
    }


    private synchronized void unhideCommentPanels() {
        topCommandPanel.setVisibility(View.VISIBLE);
        bottomCommandPanel.setVisibility(View.VISIBLE);
        fineActionsPanel.unhide();
    }


    /* Show / hide bottom-panel command buttons according to the status of the CWrittenTokenSet */
    private synchronized void organizeBottomCommandButtons() {
        CWrittenTokenSet wtSet = trainer.getWrittenTokenSet();

        if (wtSet.getNumTokens() == 0) {
            parseTokenSetButton.setVisibility(View.GONE);
            clearButton.setVisibility(View.GONE);
            clearAllButton.setVisibility(View.GONE);
            mergeStrokesButton.setVisibility(View.GONE);
            unmergeStrokesButton.setVisibility(View.GONE);
        } else {
            parseTokenSetButton.setVisibility(View.VISIBLE);
            clearButton.setVisibility(View.VISIBLE);
            mergeStrokesButton.setVisibility(View.VISIBLE);

            clearAllButton.setVisibility(wtSet.getNumTokens() > 1 ? View.VISIBLE : View.GONE);
//            unmergeStrokesButton.setVisibility(
//                    (wtSet.getNumStrokes() > 1 && wtSet.getNumTokens() < wtSet.getNumStrokes()) ?
//                        View.VISIBLE : View.GONE);
            unmergeStrokesButton.setVisibility(trainer.isLastStrokeMerged() ? View.VISIBLE : View.GONE);
        }

    }



}