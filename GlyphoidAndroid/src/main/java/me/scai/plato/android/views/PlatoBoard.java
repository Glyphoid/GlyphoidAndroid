package me.scai.plato.android.views;

import java.io.BufferedInputStream;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import me.scai.handwriting.CWrittenTokenSet;
import me.scai.handwriting.CWrittenTokenSetNoStroke;
import me.scai.parsetree.HandwritingEngineException;
import me.scai.parsetree.Node;
import me.scai.parsetree.ParseTreeMathTexifier;
import me.scai.parsetree.TokenSetParserException;
import me.scai.parsetree.TokenSetParserOutput;
import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.plato.AndroidWebCient.AndroidPlatoWebClient;
import me.scai.plato.AndroidWebCient.PlatoNotesAction;
import me.scai.plato.android.tasks.AndroidWebClientAsyncTask;
import me.scai.plato.android.utils.EventBus;
import me.scai.plato.android.responses.PlatoClientTaskResponse;
import me.scai.plato.android.requests.PlatoWebClientForceSetTokenNameRequest;
import me.scai.plato.android.requests.PlatoWebClientInjectStateRequest;
import me.scai.plato.android.requests.PlatoWebClientMergeStrokesRequest;
import me.scai.plato.android.requests.PlatoWebClientMoveTokenRequest;
import me.scai.plato.android.requests.PlatoWebClientTaskRequest;
import me.scai.plato.android.R;
import me.scai.plato.android.events.WrittenTokenSetChangedEvent;
import me.scai.plato.android.helpers.ClientInfoHelper;
import me.scai.plato.android.helpers.FileSystemHelper;
import me.scai.plato.android.helpers.NetworkStatusHelper;

import android.app.Activity;
import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.otto.Bus;

import org.apache.commons.lang3.exception.ExceptionUtils;

import me.scai.handwriting.StrokeCurator;
import me.scai.handwriting.StrokeCuratorConfigurable;
import me.scai.handwriting.TokenRecogEngine;
import me.scai.handwriting.TokenRecogEngineIMG;
import me.scai.handwriting.TokenRecogEngineSDV;
import me.scai.handwriting.CWrittenToken;
import me.scai.parsetree.GraphicalProductionSet;
import me.scai.parsetree.TerminalSet;
import me.scai.parsetree.TokenSetParser;
import me.scai.parsetree.ParseTreeStringizer;
import me.scai.parsetree.evaluation.ParseTreeEvaluator;

import me.scai.plato.android.events.TokenRecogCandidatesEvent;

public class PlatoBoard extends PlatoMultiTouchEventView {
    /* Constants */
	private static final boolean bDebug = true;
    private static final Gson gson = new Gson();

    private static final String SERIALIZATION_STROKE_CURATOR_STATE_KEY = "strokeCuratorState";
    private static final String SERIALIZATION_PATHS_KEY                = "paths";
    private static final String SERIALIZATION_NPOINTS_KEY              = "nPoints";
    private static final String SERIALIZATION_FIRST_POINTS_KEY         = "firstPoints";

    private static final String WT_FILE_EXT = ".wt";
    private static final String IM_FILE_EXT = ".im";

	/* For SDV-type token engine */
	private int npPerStroke = 16;
	private int maxNumStrokes = 4;

	/* For IMG-type token engine */
	private static final int WT_W = 16;
	private static final int WT_H = 16;

	// private static int DOWNSAMPLE_HEIGHT = 10;
	//  private static int DOWNSAMPLE_WIDTH = 10;
	private static final String LETTER_START = "L_";
	private static final String TOKENSET_START = "TS_";
	private static final String TAG = "OCR";

	/* Sub directory names */
	private static final String LETTERDIR = "letters";
	private static final String ENGINEDIR = "engines";
	private static final String TOKENSETDIR = "tokensets";

	private String externalDir;
	private String letterDir;
	private String engineDir;
	private String tokenSetDir;
	// private Engine engine;

	/* Token engine settings: Default */
	private String tokenEngineType = "SDV"; /* SDV or IMG */

	private int hiddenLayerSize1 = 100;
	private int hiddenLayerSize2 = 0;
	private int trainMaxIter = 100;
	private double trainThreshErr = 0.01;

	/* Feature settings */
	private boolean bIncludeTokenSize = false;
	private boolean bIncludeTokenWHRatio = false; /*
												 * TODO: For SDV, the WHRatio
												 * doesn't quite work yet.
												 */
	private boolean bIncludeTokenNumStrokes = true;

	private static final ScheduledExecutorService worker = Executors
			.newSingleThreadScheduledExecutor();

	public TokenRecogEngine tokEngine = new TokenRecogEngineSDV(
			hiddenLayerSize1, hiddenLayerSize2, trainMaxIter, trainThreshErr);
	private String[] letters = getResources().getStringArray(R.array.letters);

	/* Parameters and variables for timing */
	protected double recogIdleTime_ms = 500.0; /* Unit: ms */

	protected boolean flagWaitTillRecog = false;
	protected boolean flagRecog = false;

	private static final ScheduledExecutorService flagSettingWorker = Executors
			.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> strokeRecogFuture;

	/* Variables and parameters for graphical token-set parser */
	private TokenSetParser tokenSetParser;
	private ParseTreeStringizer stringizer;
	private ParseTreeEvaluator evaluator;
    private ParseTreeMathTexifier mathTexifier;

	private StrokeCurator strokeCurator; // = new StrokeCurator();
	
	private CWrittenToken lastSavedWrittenToken; /* For storing the last saved written token, to allow correction */
	private String lastSavedImageFileBase;

	/* Plato web client */
	private AndroidPlatoWebClient webClient;
	private String platoHandwritingEndpoint;
	private String platoTokenRecogEndpoint;

//    private TextView webClientStatusDisplay;
    private Context baseContext;

    private static final Map<String, Long> parserTimeoutMillis = new HashMap<String, Long>();

    private AsyncTask asyncTask;
    private boolean parsingCancelledDueToTimeOut = false;

    private Thread parserKiller;

	private PlatoNotesAction cachedAction;
	private String[] cachedActionParams;

    private AtomicBoolean requiresWebClientStateInjection = new AtomicBoolean(false);

	/* ************************ Inner classes ************************ */
	protected class SetFlagRecogTask implements Runnable {
		public void run() {
			flagRecog = true;
			flagWaitTillRecog = false;

			// System.out.println("Set flagRecog to " + flagRecog); // DEBUG

			/* Recognize the token formed by the unincorporated strokes */
			// recognizeNewStrokes(); // WORK
		}
	}

	/* **************************** Methods **************************** */
    public synchronized JsonElement getStateSerialization() {
        JsonObject state = new JsonObject();

        state.add(SERIALIZATION_PATHS_KEY,  gson.toJsonTree(paths));
        state.add(SERIALIZATION_NPOINTS_KEY, gson.toJsonTree(nPoints));
        state.add(SERIALIZATION_FIRST_POINTS_KEY, gson.toJsonTree(firstPoints));

        state.add(SERIALIZATION_STROKE_CURATOR_STATE_KEY, strokeCurator.getStateSerialization());

        return state;
    }

    public synchronized void injectSerializedState(JsonObject jsonObj) {
        /* paths */
        JsonArray jsonPaths = jsonObj.get(SERIALIZATION_PATHS_KEY).getAsJsonArray();
        paths = new LinkedList<ExtendedPath>();
        for (int i = 0; i < jsonPaths.size(); ++i) {
            paths.add(gson.fromJson(jsonPaths.get(i), ExtendedPath.class));
        }

        /* nPoints */
        JsonArray jsonNPoints = jsonObj.get(SERIALIZATION_NPOINTS_KEY).getAsJsonArray();
        nPoints = new LinkedList<Integer>();
        for (int i = 0; i < jsonNPoints.size(); ++i) {
            nPoints.add(jsonNPoints.get(i).getAsInt());
        }

        /* firstPoints */
        JsonArray jsonFirstPoints = jsonObj.get(SERIALIZATION_FIRST_POINTS_KEY).getAsJsonArray();
        firstPoints = new LinkedList<Point>();
        for (int i = 0; i < jsonFirstPoints.size(); ++i) {
            firstPoints.add(gson.fromJson(jsonFirstPoints.get(i), Point.class));
        }

        strokeCurator.injectSerializedState(jsonObj.get(SERIALIZATION_STROKE_CURATOR_STATE_KEY).getAsJsonObject());

        updateWrittenTokenSet();

        orderWebClientStateInjection();

        if (isWebClientReady()) {
            if (requiresWebClientStateInjection.getAndSet(false)) {
                final JsonObject prevState = strokeCurator.getStateSerialization().getAsJsonObject();
                PlatoWebClientTaskRequest stateInjectionReq = new PlatoWebClientInjectStateRequest(webClient, prevState);
                new AndroidWebClientAsyncTask().execute(stateInjectionReq);

            }
        }
    }

    public synchronized void orderWebClientStateInjection() {
        requiresWebClientStateInjection.set(true);
    }

	public synchronized void createPlatoWebClient() throws HandwritingEngineException {
        JsonObject prevState = null;
        if (requiresWebClientStateInjection.getAndSet(false)) {
            prevState = strokeCurator.getStateSerialization().getAsJsonObject();
        }

		try {
            Activity activity = (Activity) this.getContext();

			webClient = new AndroidPlatoWebClient(platoHandwritingEndpoint,
			                           		      platoTokenRecogEndpoint,
                                                  ClientInfoHelper.getCustomClientData(activity),
												  activity.getString(R.string.platoServerBypassSecurityToken));

			if (prevState != null) {
				webClient.injectState(prevState);
			}

		} catch (HandwritingEngineException exc) {
			throw exc;
		}


	}

    public synchronized boolean isWebClientReady() {
		if (webClient != null) {
			boolean isNetwork = NetworkStatusHelper.isNetworkAvailable(baseContext);

			if (!isNetwork) {
				webClient = null;
			}

			return isNetwork;
		} else {
			return false;
		}
    }

	public AndroidPlatoWebClient getWebClient() {
		return webClient;
	}

	/* Engine training parameter getters */
	public double getTrainThreshErr() {
		return trainThreshErr;
	}

	/* Engine training maximum number of iterations */
	public int getTrainMaxIter() {
		return trainMaxIter;
	}

	public boolean getIncludeTokenSize() {
		return bIncludeTokenSize;
	}

	public boolean getIncludeTokenWHRatio() {
		return bIncludeTokenWHRatio;
	}

	public boolean getIncludeTokenNumStrokes() {
		return bIncludeTokenNumStrokes;
	}

	/* Engine training parameter setters */
	public void setTrainThreshErr(double t_trainThreshErr) {
		trainThreshErr = t_trainThreshErr;

		/* Refresh engine */
		if (tokenEngineType.equals("SDV")) {
			tokEngine = new TokenRecogEngineSDV(hiddenLayerSize1,
					hiddenLayerSize2, trainMaxIter, trainThreshErr);
		} else if (tokenEngineType.equals("IMG")) {
			tokEngine = new TokenRecogEngineIMG(hiddenLayerSize1,
					hiddenLayerSize2, trainMaxIter, trainThreshErr);
		} else {
			throw new RuntimeException("Unrecognized token engine type: "
					+ tokenEngineType);
		}

		setTokenEngineFeatures();
	}

	public void setTrainMaxIter(int t_trainMaxIter) {
		if (t_trainMaxIter > 0) {
			trainMaxIter = t_trainMaxIter;
		} else {
			System.err.println("ERROR: input trainMaxIter has an non-positive value.");
		}

		/* Refresh engine */
		if (tokenEngineType.equals("SDV")) {
			tokEngine = new TokenRecogEngineSDV(hiddenLayerSize1,
					hiddenLayerSize2, trainMaxIter, trainThreshErr);
		} else if (tokenEngineType.equals("IMG")) {
			tokEngine = new TokenRecogEngineIMG(hiddenLayerSize1,
					hiddenLayerSize2, trainMaxIter, trainThreshErr);
		} else {
			throw new RuntimeException("Unrecognized token engine type: "
					+ tokenEngineType);
		}

		setTokenEngineFeatures();
	}

	public void setIncludeTokenSize(boolean b) {
		bIncludeTokenSize = b;

		setTokenEngineFeatures();
	}

	public void setIncludeTokenWHRatio(boolean b) {
		bIncludeTokenWHRatio = b;

		setTokenEngineFeatures();
	}

	public void setIncludeTokenNumStrokes(boolean b) {
		bIncludeTokenNumStrokes = b;

		setTokenEngineFeatures();
	}

	private void setTokenEngineFeatures() {
		int[] ivs = null;
		boolean[] bvs = null;
		if (tokenEngineType.equals("SDV")) {
			ivs = new int[2];
			ivs[0] = npPerStroke;
			ivs[1] = maxNumStrokes;

			bvs = new boolean[3];
			bvs[0] = bIncludeTokenSize;
			bvs[1] = bIncludeTokenWHRatio;
			bvs[2] = bIncludeTokenNumStrokes;
		} else if (tokenEngineType.equals("IMG")) {
			ivs = new int[2];
			ivs[0] = WT_W;
			ivs[1] = WT_H;

			bvs = new boolean[3];
			bvs[0] = bIncludeTokenSize;
			bvs[1] = bIncludeTokenWHRatio;
			bvs[2] = bIncludeTokenNumStrokes;
		} else {
			throw new RuntimeException("Unrecognized token engine type: "
					+ tokenEngineType);
		}

		tokEngine.setFeatures(ivs, bvs);
	}

	/* Constructor */
	public PlatoBoard(Context context, AttributeSet attrs) {
		super(context, attrs);

        platoHandwritingEndpoint = context.getString(R.string.plato_handwriting_endpoint);
        platoTokenRecogEndpoint  = context.getString(R.string.plato_token_recog_endpoint);

        letterDir = FileSystemHelper.ensureExternalDirectory(getContext(), LETTERDIR);
        engineDir = FileSystemHelper.ensureExternalDirectory(getContext(), ENGINEDIR);

//		File filesDir = getContext().getExternalFilesDir(null);
//
//		if (filesDir == null) {
//			AlertDialog errDialog = new AlertDialog.Builder(context).setMessage(R.string.externalFilesDirMissingErrorMsg)
//				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int whichButton) {
//						dialog.dismiss();
//						System.exit(1); /* TODO: Make it work */
//					}
//				}).create();
//
//			errDialog.show();
//		}
//
//		String filesPath = filesDir.getPath();
//
////		externalDir = getContext().getExternalFilesDir(null).getPath();
//		externalDir = filesPath;
//
//		// externalDir = (externalDir.endsWith("/")? externalDir :
//		// externalDir+"/") + APPDIR;
//		letterDir = externalDir.endsWith("/") ? externalDir : externalDir + "/";
//		letterDir = letterDir + LETTERDIR;
//		engineDir = externalDir.endsWith("/") ? externalDir : externalDir + "/";
//		engineDir = engineDir + ENGINEDIR;
//		File lDir = new File(letterDir);
//		if (!lDir.isDirectory()) {
//			lDir.mkdir();
//		}
//		File eDir = new File(engineDir);
//		if (!eDir.isDirectory()) {
//			eDir.mkdir();
//		}
//		if (!lDir.isDirectory()) {
//			Log.e(TAG, "Directory doesn't exist : " + letterDir);
//		}
//		if (!eDir.isDirectory()) {
//			Log.e(TAG, "Directory doesn't exist : " + engineDir);
//		}

		/* Set engine feature set */
		setTokenEngineFeatures();

        parserTimeoutMillis.put("local", 30L * 1000L); // TODO: Move to config file
        parserTimeoutMillis.put("web", 30L * 1000L);   // TODO: Move to config file
	}

	/* Create graphical language parser */
	public void createParser(URL terminalSetUrl,
                             URL graphicalProductionSetUrl) {
		/* Create terminal set */
		TerminalSet termSet = null;
		try {
			termSet = TerminalSet.createFromJsonAtUrl(terminalSetUrl);
		} catch (Exception e) {
			Toast.makeText(
					getContext(),
					"ERROR: Failed to create TerminalSet from URL: \""
							+ terminalSetUrl + "\"", Toast.LENGTH_LONG).show();
		}

		/* Create graphical production set */
		GraphicalProductionSet gpSet = null;
		try {
//			gpSet = GraphicalProductionSet.createFromFile(graphicalProductionSetFN, termSet);
			gpSet = GraphicalProductionSet.createFromUrl(graphicalProductionSetUrl, termSet);
		} catch (FileNotFoundException fnfe) {
			Toast.makeText(
//					getContext(),
//					"ERROR: Failed to create GraphicalProductionSet from file: "
//							+ graphicalProductionSetFN, Toast.LENGTH_LONG)
//					.show();
					getContext(),
					"ERROR: Failed to create GraphicalProductionSet from URL: \""
							+ graphicalProductionSetUrl + "\"", Toast.LENGTH_LONG)
					.show();
		} catch (IOException e) {
			Toast.makeText(
//					getContext(),
//					"ERROR: Failed to create GraphicalProductionSet from file: "
//							+ graphicalProductionSetFN, Toast.LENGTH_LONG)
//					.show();
					getContext(),
					"ERROR: Failed to create GraphicalProductionSet from URL: \""
							+ graphicalProductionSetUrl + "\"", Toast.LENGTH_LONG)
					.show();
		}

		/* Create token set parser */
		tokenSetParser = new TokenSetParser(termSet, gpSet, 0.90f);
        mathTexifier   = new ParseTreeMathTexifier(gpSet, termSet);

		/* Create stringizer and evaluator */
		stringizer = gpSet.genStringizer();
		evaluator = gpSet.genEvaluator();

		String msgText;
		if (isParserReady()) {
//            msgText = "Graphical token-set parser created from: \n";
        } else {
            msgText = "FAILED to create graphical token-set parser from:\n";

            Toast msg = Toast.makeText(getContext(), msgText + "Terminals file: "
                    + terminalSetUrl + "\n" + "Productions file: "
                    + graphicalProductionSetUrl, Toast.LENGTH_SHORT);
            msg.show();
        }


	}

	public void createStrokeCurator(URL configFN, TokenRecogEngine tokenEngine) {
		strokeCurator = new StrokeCuratorConfigurable(configFN, tokenEngine);
	}

	public boolean isParserReady() {
		return (tokenSetParser != null && stringizer != null && evaluator != null);
	}
	
	public void createWrittenTokenSetDrawer() {
		this.wtSetDrawer = new CWrittenTokenSetDrawer();
	}

	public void setCanvasTokenNames() {
		if (tokEngine.tokenNames != null) {
			String[] aTokenNames = new String[tokEngine.tokenNames.size()];
			tokEngine.tokenNames.toArray(aTokenNames);
			// aTokenNames = (String
			// [])tokEngine.tokenNames.toArray(tokEngine.tokenNames);
			super.setTokenNames(aTokenNames);
		}
	}



	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean b = super.onTouchEvent(event);

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (flagWaitTillRecog) {
				strokeRecogFuture.cancel(false);
				flagRecog = false;
				flagWaitTillRecog = false;
			}

			break;
		case MotionEvent.ACTION_MOVE:
			break;
		case MotionEvent.ACTION_UP:
            if ( !this.getHasBeenPinch() &&
                 !this.getHasBeenSelection() &&
                 !this.getHasBeenTokenMove() ) {
                act(PlatoNotesAction.AddStroke);
            }

            /* Handle token movements */
            if (idxTokenMoved != -1) {
                /* Create string argument for idxTokenMoved */
                final String idxTokenMovedStr = String.format("%d", idxTokenMoved);

                /* Create string argument for newBounds */
                StringBuilder newBoundsStrBuilder = new StringBuilder();
                final float[] newBounds = wtSet.getTokenBounds(idxTokenMoved);
                for (int k = 0; k < newBounds.length; ++k) {
                    newBoundsStrBuilder.append(String.format("%f", newBounds[k]));

                    if (k < newBounds.length - 1) {
                        newBoundsStrBuilder.append(",");
                    }
                }

                act(PlatoNotesAction.MoveToken, idxTokenMovedStr, newBoundsStrBuilder.toString());
            }

            this.externalResetUIState();

			break;
		default:
			break;
		}

		return b;
	}

    private String intArray2String(final int[] ints) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ints.length; ++i) {
            sb.append(Integer.toString(ints[i]));
            if (i < ints.length - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    public synchronized void cancelParserAsyncTask() {
        // TODO: Cancel web server operations
        if (asyncTask != null) {
            AsyncTask.Status status = asyncTask.getStatus();

            if (status == AsyncTask.Status.PENDING || status == AsyncTask.Status.RUNNING) {
                try {
                    System.out.print("Attempting to cancel parser async task");

                    parsingCancelledDueToTimeOut = false;
                    asyncTask.cancel(false);
                } catch (Exception exc) {

                }
            }
        }
    }


	public synchronized void act(PlatoNotesAction action, String... actionParams) {
		act(false, action, actionParams);
	}

	public synchronized void actAgain() {
		act(true, cachedAction, cachedActionParams);
	}

    public synchronized void act(boolean repeat, PlatoNotesAction action, String... actionParams) {

		if (repeat && action.isPrimarilyLocal()) {
			// Do not repeat primarily-local actions, as the result of the action should have
			// already been synced to server side via injectState()
			return;
		}

		// Store current action and actionParameters, just in case this action needs to be repeated in the
		// face or engine expiration or purging.
		cachedAction       = action;
		cachedActionParams = actionParams;

        if (action == PlatoNotesAction.AddStroke) {
            strokeCurator.addStroke(lastStroke);

            /* Display other token candidates */
            /* TODO: Refactor */
            final int nMax = (tokEngine.tokenNames.size() >= 5) ? 5 : tokEngine.tokenNames.size();
            final Bus eventBus = EventBus.getInstance();
            eventBus.post(new TokenRecogCandidatesEvent(strokeCurator.getTokenSet(),
                                                        strokeCurator.getNumTokens() - 1,
                                                        nMax,
                                                        tokEngine.tokenNames));

            // To improve smoothness of the writing experience, the local token curator is always
            // used, even when web session is available. In other words, when a web session is
            // available, add-stroke events will be dispatched to both the local curator and the web
            // session. The same applies to actions remove-last-token, clear, force-set-token-name,
            // merge-strokes-as-token and unmerge-last-stroke.
            updateWrittenTokenSet();

            if (isWebClientReady()) {
                PlatoWebClientTaskRequest webReq = new PlatoWebClientTaskRequest(webClient, action, lastStroke);
                new AndroidWebClientAsyncTask().execute(webReq);
            }

        } else if (action == PlatoNotesAction.RemoveLastToken) {
            int[] pathRemoveIndices = strokeCurator.removeLastToken();

            if (pathRemoveIndices != null) {
                for (int n = pathRemoveIndices.length - 1; n >= 0; --n) {
                    paths.remove(pathRemoveIndices[n]);
                    nPoints.remove(pathRemoveIndices[n]);
                    firstPoints.remove(pathRemoveIndices[n]);
                }
            }

            updateWrittenTokenSet();

            if (isWebClientReady()) {
                asyncInvokeWebClient(action);
            }
        } else if (action == PlatoNotesAction.ForceSetTokenName) {
            final int tokenIdx;
            final String tokenName;
            if (actionParams.length == 2) {
                tokenIdx = Integer.parseInt(actionParams[0]);
                tokenName = actionParams[1];
            } else if (actionParams.length == 1) {
                /* 2 args: set the last token */
                tokenIdx = wtSet.getNumTokens() - 1;
                tokenName = actionParams[0];
            } else {
                throw new IllegalArgumentException("Unexpected length of actionParams: " + actionParams.length);
            }

            strokeCurator.forceSetRecogWinner(tokenIdx, tokenName);

            updateWrittenTokenSet();

            if (isWebClientReady()) {
                asyncInvokeWebClient(action, Integer.toString(tokenIdx), tokenName);
            }
        } else if (action ==PlatoNotesAction.ClearStrokes) {
            super.clear();
            strokeCurator.clear();

            updateWrittenTokenSet();

            // Local-web coordination
            if (isWebClientReady()) {
                asyncInvokeWebClient(action);
            }
        } else if (action == PlatoNotesAction.MergeStrokesAsToken) {
            int numStrokesToMerge = 2;
            if (actionParams.length > 0) {
                numStrokesToMerge = Integer.parseInt(actionParams[0]);
            }

            final int nStrokes = strokeCurator.getNumStrokes();
            if (nStrokes >= numStrokesToMerge) {
                final int[] strokeIndices = new int[numStrokesToMerge];
                for (int i = 0; i < numStrokesToMerge; ++i) {
                    strokeIndices[i] = nStrokes - (numStrokesToMerge - i);
                }

                strokeCurator.mergeStrokesAsToken(strokeIndices);

                updateWrittenTokenSet();

                if (isWebClientReady()) {
                    asyncInvokeWebClient(action, intArray2String(strokeIndices));
                }
            }
        } else if (action == PlatoNotesAction.UnmergeLastStroke) {
            final int nStrokes = strokeCurator.getNumStrokes();

            if (nStrokes > 1) {
                int[] idxStrokesToUnmerge = new int[] {nStrokes - 1};

                strokeCurator.mergeStrokesAsToken(idxStrokesToUnmerge);

                updateWrittenTokenSet();

                if (isWebClientReady()) {
                    asyncInvokeWebClient(PlatoNotesAction.MergeStrokesAsToken, intArray2String(idxStrokesToUnmerge));
                }
            }
        } else if (action == PlatoNotesAction.MoveToken) {
            if (isWebClientReady()) {
                asyncInvokeWebClient(PlatoNotesAction.MoveToken, actionParams[0], actionParams[1]);
            }
        } else if (action == PlatoNotesAction.ParseTokenSet) {
            if (isWebClientReady()) {
                /* Web parser-evaluator */
                asyncInvokeWebClient(action);
            } else {
                /* Local parser-evaluator */
                LocalParserAsyncTask localParserAsyncTask = new LocalParserAsyncTask();
                asyncTask = localParserAsyncTask;

                // Make copy of final reference for inner class callbacks
                final AsyncTask localParserAsyncTaskRef = localParserAsyncTask;

                localParserAsyncTask.execute();

                parserKiller = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int counter = 0;
                        long sleepCycleMillis = 250L;

                        while (counter * sleepCycleMillis < parserTimeoutMillis.get("local")) {
                            try {
                                Thread.sleep(sleepCycleMillis);
                            } catch (InterruptedException exc) {
                            }

                            if (Thread.currentThread().isInterrupted()) {
                                System.out.print("parserKiller is interrupted");
                                return;
                            }

                            counter ++;

                        }

                        if (counter * sleepCycleMillis >= parserTimeoutMillis.get("local")) {
                            parsingCancelledDueToTimeOut = true;

                            System.out.println("Calling cancel() on localParserAsyncTask");
                            localParserAsyncTaskRef.cancel(true);
                        }


                    }
                });

                parserKiller.start();
            }
        } else if (action == PlatoNotesAction.GetVarMap) {
            if (isWebClientReady()) {
				asyncInvokeWebClient(action);
            } else {
                PlatoVarMap varMap = evaluator.getVarMap();

                final Bus eventBus = EventBus.getInstance();
                eventBus.post(varMap);
                // Use wrapper class, otherwise the subscriber method cannot be triggered properly

            }
        } else {
            throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    /* Local parser async task */
    /* TODO: Refactor to reduce duplicate code with Android WebClientAsyncTask */
    /* TODO: Time out */
    /* TODO: Disable all buttons during operation */
    public class LocalParserAsyncTask extends AsyncTask<Void, Void, PlatoClientTaskResponse> {
        private final Bus eventBus = EventBus.getInstance();
        private PlatoClientTaskResponse resp = new PlatoClientTaskResponse();

        /* Constructor */
        public LocalParserAsyncTask() {
            resp.setOriginalAction(PlatoNotesAction.ParseTokenSet);
            resp.setWrittenTokenSet(wtSet);
            resp.setWrittenTokenConstStrokeIdx(wtConstStrokeIdx);
        }

        @Override
        public PlatoClientTaskResponse doInBackground(Void... args) {
            final long startTime = System.currentTimeMillis();
            TokenSetParserOutput parserOutput = parserTokenSet();
            final long elapsedTime = System.currentTimeMillis() - startTime;

            resp.setParserOutput(parserOutput);
            resp.setElapsedTimeMillis(elapsedTime);

            return resp;
        }

        @Override
        public void onPostExecute(PlatoClientTaskResponse resp) {
            /* Get current time and calculate elapsed time */
            eventBus.post(resp);
        }

        @Override
        public void onCancelled() {
            System.out.println("In onCancelled of LocalParserAsyncTask");

            resp.setElapsedTimeMillis(parserTimeoutMillis.get("local"));
            if (parsingCancelledDueToTimeOut) {
                // Parsing cancelled due to timeout
                resp.setErrorMessage("Parser was timed out after " + parserTimeoutMillis.get("local") +
                                     " ms. To allow longer parser operations, you can adjust timeout.");
            } else {
                // Parsing cancelled due to user cancellation
                resp.setErrorMessage("Parsing was cancelled by user.");
                resp.setCancelled(true);
            }

            eventBus.post(resp);

        }
    }

    /* Web-client async tasks */
    private synchronized void asyncInvokeWebClient(PlatoNotesAction action, String... args) {
//        String action = args[0];

        PlatoWebClientTaskRequest req = null;
        if (action == PlatoNotesAction.ForceSetTokenName) {
            final String tokenIdxStr = args[0];
            final int tokenIdx = Integer.parseInt(tokenIdxStr);
            final String tokenName = args[1];

            req = new PlatoWebClientForceSetTokenNameRequest(webClient, action, tokenIdx, tokenName);
        } else if (action == PlatoNotesAction.MergeStrokesAsToken) {
            String strokeIndicesStr = args[0];
            String[] strokeIndicesItems = strokeIndicesStr.split(",");
            int[] strokeIndices = new int[strokeIndicesItems.length];
            for (int i = 0; i < strokeIndices.length; ++i) {
                strokeIndices[i] = Integer.parseInt(strokeIndicesItems[i]);
            }

            req = new PlatoWebClientMergeStrokesRequest(webClient, action, strokeIndices);
        } else if (action == PlatoNotesAction.MoveToken) {
            /* Get token index */
            int idxToken = Integer.parseInt(args[0]);

            /* Get new bounds */
            String newBoundsStr = args[1];
            String[] newBoundsItems = newBoundsStr.split(",");
            float[] newBounds = new float[newBoundsItems.length];
            for (int i = 0; i < newBoundsItems.length; ++i) {
                newBounds[i] = Float.parseFloat(newBoundsItems[i]);
            }

            req = new PlatoWebClientMoveTokenRequest(webClient, action, idxToken, newBounds);
        } else {
            req = new PlatoWebClientTaskRequest(webClient, action);
        }

        AndroidWebClientAsyncTask androidWebClientAsyncTask = new AndroidWebClientAsyncTask();
        asyncTask = androidWebClientAsyncTask;

        androidWebClientAsyncTask.execute(req);
    }

    private void updateWrittenTokenSet() {
        setWrittenTokenSet(strokeCurator.getWrittenTokenSet(),
                           strokeCurator.getWrittenTokenConstStrokeIndices());
        invalidate();



        final Bus eventBus = EventBus.getInstance();
        eventBus.post(new WrittenTokenSetChangedEvent());
    }

    public CWrittenTokenSet getWrittenTokenSet() {
        return wtSet;
    }

	/* Save a written token to file */
	public void saveToken(String letter) {
		CWrittenToken tokenToSave = null;
		String imgFileBase = null;
		boolean resave = false;

		if (writtenToken.isEmpty()) {
			if (lastSavedImageFileBase == null) {
				/* Token is empty and there is not previously saved token */
				if (isEmpty() || letter == null || letter.isEmpty()) {
					Toast msg = Toast.makeText(getContext(),
							                   "Cannot save token because there is none",
							                   Toast.LENGTH_LONG);
					msg.show();
					Log.i(TAG, "No letter");
					return;
				}
			} else {
				/* Token is empty, but there is a previously saved token. Will re-save that token. */
				tokenToSave = lastSavedWrittenToken;
				imgFileBase = lastSavedImageFileBase;
				resave = true;
			}
		} else {
			/* Save a new token */

			/* Put all stroke coordinate data to the [0, 1] interval */
			writtenToken.normalizeAxes();
			tokenToSave = writtenToken;

			/* Get the token file base name */
			String fname = newFileName(letterDir, LETTER_START, IM_FILE_EXT);
			String path = letterDir.endsWith("/") ? letterDir : letterDir + "/";
			imgFileBase = path + fname;

		}

		/* Save the .wt (written token ASCII) file */
		if (bDebug) {
			System.out.println(tokenToSave.toString());
		}

		/* Get the full paths of the actual files to save */
		String wtFileName = imgFileBase + WT_FILE_EXT;
		String imFileName = imgFileBase + IM_FILE_EXT;

		try {
			tokenToSave.writeFile(wtFileName, letter);
		} catch (IOException e) {
			Log.e(TAG, "Error during saving .wt file", e);
		}

		/* Save the .im (image map) file */
		try {
			tokenToSave.writeImgFile(imFileName, letter, WT_W, WT_H);
		} catch (IOException e) {
			Log.e(TAG, "Error during saving .im file", e);
		}

		/* Save the info in case the token needs to be saved again */
		lastSavedImageFileBase = new String(imgFileBase);
		lastSavedWrittenToken = new CWrittenToken(tokenToSave); /* Use copy constructor */

		/* Toast about successful save */
		Toast msg = Toast.makeText(getContext(),
						           String.format((resave ? "Re-saved" : "Saved") + " data to token \"%s\" in file: %s",
								                 letter, imgFileBase),
								   Toast.LENGTH_SHORT);
		msg.show();

		/*
		 * this.entry.downsample(getImagePixels());
		 * this.entry.getSampleData().setLetter(letter.charAt(0));
		 * letterList.add((SampleData) entry.getSampleData().clone());
		 */
		act(PlatoNotesAction.ClearStrokes);
	}

	/* Get the number of valid image files */
	private int[] getNValidImFiles() {
		int[] nvf = new int[letters.length];

		File dir = new File(letterDir);
		final String prefix = LETTER_START;

		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.startsWith(prefix) && name.endsWith(IM_FILE_EXT));
			}
		});

		for (int i = 0; i < files.length; ++i) {
			try {
				FileInputStream fin = new FileInputStream(files[i]);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						fin));
				String line = null;
				String tokenName = null;
				int line_n = 0;
				boolean bValid = true;
				while ((line = in.readLine()) != null) {
					if (line_n == 0) {
						if (line.startsWith("Token name: ")) {
							tokenName = line.replaceFirst("Token name: ", "");
						} else {
							bValid = false;
							break;
						}
					} else if (line_n == 1) {
						if (!line.startsWith("n_w = ")) {
							bValid = false;
							break;
						}
					} else if (line_n == 2) {
						if (!line.startsWith("n_h = ")) {
							bValid = false;
							break;
						}
					} else if (line_n == 3) {
						if (!line.startsWith("w = ")) {
							bValid = false;
							break;
						}
					} else if (line_n == 4) {
						if (!line.startsWith("h = ")) {
							bValid = false;
							break;
						}
					} else if (line_n == 5) {
						break;
					}

					line_n++;
				}
				in.close();

				/* Increment count */
				if (bValid && (tokenName != null)) {
					int k = Arrays.asList(letters).indexOf(tokenName); /*
																		 * TODO:
																		 * Performance
																		 * tweak
																		 * ?
																		 */
					if (k >= 0 && k < nvf.length) {
						nvf[k]++;
					}
				}

			} catch (IOException e) {
				/* TODO */
			}
		}

		return nvf;
	}

	/* Show summary of existing valid data files */
	public void showImFileSummary() {
		int[] nvf = getNValidImFiles();

		String sumStr = new String("Summary of existing valid data files:\n");
		for (int i = 0; i < letters.length; ++i) {
			sumStr += "Letter \"" + letters[i] + "\": " + nvf[i] + " ";
			sumStr += (nvf[i] <= 1) ? "file" : "files";
			sumStr += "\n";
		}

		Toast msg = Toast.makeText(getContext(), sumStr, Toast.LENGTH_LONG);
		msg.show();
	}

	/* Get the name of the new letter or token-set file */
	private String newFileName(String filePath, final String prefix,
			final String extName) {
		File dir = new File(filePath);

		/* Count the number of files in the directory */
//		String [] params = {"/bin/sh", "-c", "ls -f " + filePath + " | wc -l"};
//		int fileCount = -1;
//		try {
//			Process process = Runtime.getRuntime().exec(params);
//			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
//			fileCount = Integer.parseInt(br.readLine().trim());
//		}
//		catch (IOException ioe) {
//			System.out.println(ioe.getMessage());
//			/* TODO */
//		}
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				// return (name.startsWith(prefix) && name.endsWith(IM_FILE_EXT));
				return (name.startsWith(prefix) && name.endsWith(extName));
			}
		});

		String newName = null;
		int n = files.length + 1;
		boolean availableNameFound = false;
		while (!availableNameFound) {
			newName = prefix + n;
			String newFullPath = filePath +
					          (filePath.endsWith(File.separator) ? "" : File.separator) +
					          newName + extName;
			if (files.length > 0) {
				String lastFilePath = files[files.length - 1].getAbsolutePath();
				if (lastFilePath.equals(newFullPath)) {
					n++;
				}
				else {
					availableNameFound = true;
				}
			}
			else {
				availableNameFound = true;
			}
		}

		return newName;

	}

	private File[] getFiles(String filepath, final String prefix) {
		File dir = new File(filepath);
		// final String prefix = LETTER_START;
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix);
			}
		});
		return files;
	}

	public void deleteLastLetterFile() {
		String[] fnames = getLastLetterFiles(letterDir);

        for (int i = 0; i < fnames.length; ++i) {
            String fname = fnames[i];

            if (fname != null) {
                File file = new File(fname);
                if (file.delete()) {
                    Toast msg = Toast.makeText(getContext(),
                            "File \"" + file.getName() + "\" deleted",
                            Toast.LENGTH_SHORT);
                    msg.show();
                } else {
                    Toast msg = Toast.makeText(getContext(),
                            "Failed to delete \"" + file.getName() + "\"",
                            Toast.LENGTH_SHORT);
                    msg.show();
                }
            }
        }
	}

	private String[] getLastLetterFiles(String filepath) {
		File[] files = getFiles(filepath, LETTER_START);
		if (files.length == 0) {
			return null;
		}
		Integer filenos[] = new Integer[files.length];
		int i = 0;
		for (File file : files) {
			String name = file.getName();
			int no;
			try {
				no = Integer.parseInt(name.substring(name.lastIndexOf("_") + 1, name.lastIndexOf(".")));
			} catch (Exception e) {
				no = 0;
			}
			filenos[i++] = no;
		}
		Arrays.sort(filenos);
		String fname = LETTER_START + (filenos[filenos.length - 1]);

        String[] fileNames = new String[2];

		fileNames[0] = (filepath.endsWith("/") ? filepath : filepath + "/") + fname + WT_FILE_EXT;
        fileNames[1] = (filepath.endsWith("/") ? filepath : filepath + "/") + fname + IM_FILE_EXT;

        return fileNames;
	}

	/* Re-format all .im files */
	public void reFormatImFiles() {
		tokEngine.reFormatImFiles(letterDir, WT_W, WT_H);
	}

	/* Train token recognition engine */
	// public void trainTokenEngine(ProgressDialog progDialog) { /* TODO: Remove
	// local training on Android */
	// tokEngine.setTrainingProgDialog(progDialog);
	// tokEngine.train(letterDir);
	//
	// setCanvasTokenNames();
	// // strokeCurator.initialize(tokenNames, tokEngine);
	// strokeCurator.initialize(tokEngine);
	//
	// /* Write token engine to FS */
	// writeTokenEngine(tokEngine);
	// }

	private String getTokenEngineFileName() {
		String path = engineDir.endsWith("/") ? engineDir : engineDir + "/";

		String sz = "sz" + (getIncludeTokenSize() ? "1" : "0");
		String whr = "whr" + (getIncludeTokenWHRatio() ? "1" : "0");
		String ns = "ns" + (getIncludeTokenNumStrokes() ? "1" : "0");

		String tokenEngineFN = "token_engine." + tokenEngineType.toLowerCase()
				+ "." + sz + "_" + whr + "_" + ns + ".ser";
		tokenEngineFN = path + tokenEngineFN;

		return tokenEngineFN;
	}

	/* Write token engine to file system */
	public void writeTokenEngine(TokenRecogEngine te) {
		/* Get the file name */
		String tokenEngineFN = getTokenEngineFileName();

		/* Write the engine to disk, which is serializable */
		ObjectOutputStream objOutStream = null;
		try {
			objOutStream = new ObjectOutputStream(new FileOutputStream(
					tokenEngineFN));
			objOutStream.writeObject(te);
			objOutStream.flush();
		} catch (IOException e) {
			Log.e("WRITE_TOKEN_ENGINE_ERROR",
					"Failed to write token engine to file: " + tokenEngineFN);
		} finally {
			try {
				objOutStream.close();
			} catch (IOException e) {
				Log.e("TOKEN_ENGINE_OUTPUT_STREAM_CLOSE_ERR",
						"Failed to close token engine output stream");
			}
		}
	}

	/* Load trained token engine */
	public void readTokenEngine(final URL tokenEngineFileUrl) {

		ObjectInputStream objInStream = null;
		boolean readSuccessful = false;
		try {

            System.out.println("readTokenEngine(): Instantiating ObjectInputStream from " + tokenEngineFileUrl);
			objInStream = new ObjectInputStream(new BufferedInputStream(tokenEngineFileUrl.openStream()));

            System.out.println("readTokenEngine(): Calling readObject()");
			if (tokenEngineType.equals("SDV")) {
				tokEngine = (TokenRecogEngineSDV) objInStream.readObject();
			} else if (tokenEngineType.equals("IMG")) {
				tokEngine = (TokenRecogEngineIMG) objInStream.readObject();
			} else {
				throw new RuntimeException("Unrecognized token engine type: "
						+ tokenEngineType);
			}
            System.out.println("readTokenEngine(): DONE Calling readObject()");

			readSuccessful = true;
			// objInStream.close();
		} catch (IOException e) {
			Log.e("READ_TOKEN_ENGINE_ERROR",
					"Failed to read token engine from URL: " + tokenEngineFileUrl);
			Toast msg = Toast.makeText(getContext(),
					"IO error occurred during loading of token engine at "
							+ tokenEngineFileUrl, Toast.LENGTH_LONG);
			msg.show();
		} catch (ClassNotFoundException e) {
			Log.e("READ_TOKEN_ENGINE_ERROR",
					"Failed to read token engine from URL: " + tokenEngineFileUrl);
			Toast msg = Toast.makeText(getContext(),
					"Class error occurred during loading of token engine at "
							+ tokenEngineFileUrl, Toast.LENGTH_SHORT);
			msg.show();
		} finally {
			try {
                System.out.println("readTokenEngine(): Calling objInStream.close()");
				objInStream.close();
			} catch (IOException e) {
				Log.e("INPUT_STREAM_CLOSE_ERR",
						"Failed to close token engine input stream");
			}
		}

        System.out.println("readTokenEngine(): Calling setTokenEngineFeatures()");
		setTokenEngineFeatures();
        System.out.println("readTokenEngine(): DONE Calling setTokenEngineFeatures()");

		if (!readSuccessful) {
			Toast msg = Toast.makeText(
					getContext(),
					"ERROR: Failed to Loaded token engine from URL: " + tokenEngineFileUrl
							+ ".\nmaxIter=" + tokEngine.getTrainMaxIter()
							+ ";\nthreshErr="
							+ tokEngine.getTrainThreshErrRate(),
					Toast.LENGTH_LONG);
			msg.show();
		}
	}

	/* Get the list of unique token names */
	public ArrayList<String> getTokenNames() {
		return tokEngine.tokenNames;
	}

	class ClearProbValsTask implements Runnable {
		public void run() {
			clearProbabilityValues();
			postInvalidate();
		}
	}

	/* Check if the token engine is ready to perform recognition */
	public boolean isReadyToRecognize() {
		return tokEngine.isReadyToRecognize();
	}

	class AlertPrompt {
		String letter;

		AlertPrompt(Context context) {
			final EditText input = new EditText(context);
			new AlertDialog.Builder(context)
					.setTitle("Add Letter")
					// .setMessage()
					.setView(input)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									letter = input.getText().toString();
									Log.i(TAG, "onClick Letter" + letter);
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Do nothing.
								}
							}).show();
		}

		public String getLetter() {
			return letter;
		}
	}

	/* Write token set to file */
	public void saveTokenSet(String wtsFileName, boolean bBrief, boolean bPrompt)
			throws IOException {
		try {
			wtSet.writeToFileBrief(wtsFileName, bBrief);
		} catch (IOException e) {
			Toast msg = Toast.makeText(getContext(),
					"IOException occurred during writing to token-set file: "
							+ wtsFileName, Toast.LENGTH_LONG);
			msg.show();
		} catch (IllegalStateException e) {
			Toast msg = Toast.makeText(getContext(),
					"IllegalStateException occurred during writing to token-set file: "
							+ wtsFileName, Toast.LENGTH_LONG);
			msg.show();
		}

		if (bPrompt) {
			Toast msg = Toast.makeText(getContext(),
					"Saved token set to file: " + wtsFileName,
					Toast.LENGTH_LONG);
			msg.show();
		}

		act(PlatoNotesAction.ClearStrokes);
	}

	public void saveTokenSet(boolean bBrief, boolean bPrompt)
			throws IOException {
        tokenSetDir = FileSystemHelper.ensureExternalDirectory(getContext(), TOKENSETDIR);

		/* Directory check */
//		externalDir = getContext().getExternalFilesDir(null).getPath();
//		tokenSetDir = externalDir.endsWith("/") ? externalDir : externalDir
//				+ "/";
//		tokenSetDir += TOKENSETDIR;
//
//		File tsDir = new File(tokenSetDir);
//		if (!tsDir.isDirectory()) {
//			boolean createDirRes = tsDir.mkdir();
//
//			if (!createDirRes)
//				throw new IOException(
//						"Failed to create directory for token sets: "
//								+ tokenSetDir);
//		}

		/* Determine the file name of the new .wts file */
		final String extName = ".wts";
		String fName = newFileName(tokenSetDir, TOKENSET_START, extName);
		String path = tokenSetDir.endsWith("/") ? tokenSetDir : tokenSetDir
				+ "/";
		String wtsFileName = path + fName + extName;

		saveTokenSet(wtsFileName, bBrief, bPrompt);
	}

//    public TextView getWebClientStatusDisplay() {
//        return webClientStatusDisplay;
//    }
//
//    public void setWebClientStatusDisplay(TextView webClientStatusDisplay) {
//        this.webClientStatusDisplay = webClientStatusDisplay;
//    }

    public void setBaseContext(Context baseContext) {
        this.baseContext = baseContext;
    }


    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.wtSetDrawer.setScreenWH((float) w, (float) h);


    }

    public void resetBoardZoomPan() {
        this.wtSetDrawer.reset();
    }

    public TokenSetParserOutput parserTokenSet() {
        if (!isParserReady()) {
            throw new RuntimeException("[ERROR: Parser not ready]");
        } else if (wtSet.empty()) {
            throw new RuntimeException("[WARNING: Empty token set]");
        } else {

            CWrittenTokenSetNoStroke wtSetNS = new CWrittenTokenSetNoStroke(wtSet);
            Node parseRoot = null;
            try {
                parseRoot = tokenSetParser.parse(wtSetNS);
            } catch (TokenSetParserException exc) {

            }
            String stringized = stringizer.stringize(parseRoot);

            String evalResStr = "";
            String mathTex = "";
//            if (parseRoot != null) {
                try {
                    evalResStr = evaluator.eval2String(parseRoot); /* TODO: Not all evaluation output are doubles */
                }
                catch (Exception exc) {
                    String rootCauseMsg = ExceptionUtils.getRootCauseMessage(exc);
                    evalResStr = "[Evaluator exception: " + rootCauseMsg + "]";
                }

                mathTex = mathTexifier.texify(parseRoot);
//            }

            TokenSetParserOutput parserOutput = new TokenSetParserOutput(stringized, evalResStr, mathTex);

            return parserOutput;
        }
    }

    public void stopParserKiller() {
        if (parserKiller != null) {
            parserKiller.interrupt();
        }
    }

    public void cancelWebClient() {
        if (webClient != null) {
            asyncInvokeWebClient(PlatoNotesAction.RemoveEngine);

        }
    }

    public void setDisplayOverlaidSymbols(boolean displayOverlaidSymbols) {
        wtSetDrawer.setDisplayOverlaidSymbols(displayOverlaidSymbols);
    }

}

