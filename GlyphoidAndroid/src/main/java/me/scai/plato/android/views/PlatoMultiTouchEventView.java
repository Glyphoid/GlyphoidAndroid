/*
 * Author : Sinu John
 * www.sinujohn.wordpress.com
 */

package me.scai.plato.android.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import me.scai.handwriting.CStroke;
import me.scai.handwriting.CWrittenToken;
import me.scai.handwriting.CWrittenTokenSet;
import me.scai.parsetree.MathHelper;
import me.scai.parsetree.TokenSetParserOutput;

/* Class for plotting data bars in a canvas */
class CanvasBarPlotter {
	/* Parameters: settings */
	private double axesRLeft = 0.1;
	private double axesRTop = 0.6;
	private double axesRWidth = 0.8;
	private double axesRHeight = 0.2;
	
	private double verticalRMargin = 0.02;
	private double horizontalRMargin = 0.02;
	private double horizontalRSpacing = 0.01;
	
	private int [] axesColor = {0, 255, 0};
	private int [] barColor = {0, 255, 0};	
	private int [] textColor = {255, 255, 255};
		
	private int alpha = 127;
	
	/* Parameters: variables */
	private int cWidth;
	private int cHeight;
	
	private double axesLeft = 0.0;
	private double axesRight = 0.0;
	private double axesTop = 0.0;
	private double axesBottom = 0.0;
	
	private double axesWidth = 0.0;
	private double axesHeight = 0.0;
	
	private double barBottom = 0.0;
	private double horizontalMargin = 0.0;
	private double horizontalSpacing = 0.0;
	private double barWidth = 0.0;
	private double maxBarHeight = 0.0;
	
	Rect rectAxes = null;
	Paint paintAxes = null;
	Paint paintBar = null;
	Paint paintTxt = null;

	/* Methods */
	/* Constructors */
	public CanvasBarPlotter(Canvas canvas) {	
		cWidth = canvas.getWidth();
		cHeight = canvas.getHeight();
		
		axesLeft = cWidth * axesRLeft;
		axesRight = cWidth * (axesRLeft + axesRWidth);
		axesTop = cHeight * axesRTop;
		axesBottom = cHeight * (axesRTop + axesRHeight);
		
		axesWidth = axesRight - axesLeft;
		axesHeight = axesBottom - axesTop;
	
		paintAxes = new Paint();
		paintAxes.setStyle(Paint.Style.STROKE);
		paintAxes.setStrokeWidth(1);
		paintAxes.setARGB(alpha, axesColor[0], axesColor[1], axesColor[2]);
		
		rectAxes = new Rect((int)axesLeft, (int)axesTop, (int)axesRight, (int)axesBottom);
		
		/* Bar-related */
		barBottom = axesBottom - axesHeight * verticalRMargin;
		horizontalMargin = cWidth * horizontalRMargin;
		horizontalSpacing = cWidth * horizontalRSpacing;
		
		maxBarHeight = axesHeight * (1 - 2 * verticalRMargin);
		
		paintBar = new Paint();
		paintBar.setARGB(alpha, barColor[0], barColor[1], barColor[2]);
		
		/* Text-related */
		paintTxt = new Paint();
		paintTxt.setARGB(alpha, textColor[0], textColor[1], textColor[2]);
	}
	
	private void drawAxes(Canvas canvas) {
		if ( rectAxes != null && paintAxes != null )
		canvas.drawRect(rectAxes, paintAxes);
	}
	
	private void drawBars(double [] ys, String [] strs, Canvas canvas) {	
		if ( ys.length == 0 || (strs != null && ys.length != strs.length) )
			return;
		
		barWidth = (axesWidth - 2 * horizontalMargin - (ys.length - 1) * horizontalSpacing) / ys.length;
		
		double currX = axesLeft + horizontalMargin;
		for (int i = 0; i < ys.length; ++i) {
			double barHeight = maxBarHeight * ys[i];
			Rect rectBar = new Rect((int)currX, (int)(barBottom - barHeight), 
									(int)(currX + barWidth), (int)barBottom);
			
			canvas.drawRect(rectBar, paintBar);
			
			if ( strs != null ) {
                canvas.drawText(strs[i], (float) currX, (float) barBottom, paintTxt);
            }
			
			currX += (barWidth + horizontalSpacing);
		}
		
	}
	
	public void draw(double [] ys, String [] strs, Canvas canvas) {
		if ( rectAxes == null || paintAxes == null || paintBar == null ) {
            return;
        }
		
		drawAxes(canvas);
		drawBars(ys, strs, canvas);
	}
		
}


public class PlatoMultiTouchEventView extends View {
	private static final boolean bDebug = false;
	
	private Paint paintStrokeCurve = new Paint();
	private Paint paintStrokePoint = new Paint();
	
	//private Path path = new Path();
	protected LinkedList<ExtendedPath> paths = new LinkedList<ExtendedPath>();
	protected LinkedList<Integer> nPoints = new LinkedList<Integer>();
	protected List<Point> firstPoints = new LinkedList<Point>();
	
	private double [] probVals = null;
	
	protected CanvasBarPlotter canvasBarPlotter = null;

	protected CWrittenTokenSetDrawer wtSetDrawer;
	protected CWrittenToken writtenToken = new CWrittenToken();

	protected CStroke lastStroke;

	private TokenSetParserOutput webParserOutput;

//	protected LinkedList<Integer> strokeStat = new LinkedList<Integer>(); // -1: unincorporated; >=0 index to the owning token
	protected CWrittenTokenSet wtSet = new CWrittenTokenSet();			/* TODO: set to null initially */
    protected List<int []> wtConstStrokeIdx;  // TODO: Wire like wtSet

    protected List<WrittenTokenUIState> wtUIStates = new ArrayList<WrittenTokenUIState>();

	/* Recognition results: full probability vectors (NN output) */		
//	protected List<double []> wtRecogPs = null;		/* TODO: set to null initially */
	/* TODO: hash map or list of indices to the constituent strokes */
	//protected LinkedList<int []> wtOwnedStrokeIdx = new LinkedList<int []>();
	
	protected String [] tokenNames = null;

    /* Member variables for keeping track of twoPointer pinch movements */
    private PlatoMultiTouchUIState uiCurrentStatus = PlatoMultiTouchUIState.Write;
    private boolean hasBeenPinch;
    private boolean hasBeenSelection;
    private boolean hasBeenTokenMove;
    private int[] pinchPointerIds;
    private final float[][] pinchPointsPrev = new float[2][2]; /* First index: two pointers */
    private final float[][] pinchPointsCurr = new float[2][2];

    private float panSensitivity = 0.001f;
    private float zoomSensitivity = 0.4f;

    /* Variables for token move */
    private float[] tokenMoveOrigin;
    private float[] tokenMoveLastPos;
    protected int idxTokenMoved = -1;
    private int[] movedTokenConstStrokeIdx; /* Indices of the strokes making up the token being moved */
    private float[] tokenMoveInitialBounds;

    private boolean isLastStrokeMerged = false;
	
	/* ************************ Methods ************************ */
	/* Constructor */
	public PlatoMultiTouchEventView(Context context, AttributeSet attrs) {
		super(context, attrs);

		paintStrokeCurve.setAntiAlias(true);
		paintStrokeCurve.setStrokeWidth(3f);	/* TODO: Remove magic number */
		paintStrokeCurve.setColor(Color.WHITE);
		paintStrokeCurve.setStyle(Paint.Style.STROKE);
		paintStrokeCurve.setStrokeJoin(Paint.Join.ROUND);
		
		paintStrokePoint.setAntiAlias(true);
		paintStrokePoint.setStrokeWidth(5f);	/* TODO: Remove magic number */
		paintStrokePoint.setColor(Color.WHITE);
		paintStrokePoint.setStyle(Paint.Style.STROKE);
		paintStrokePoint.setStrokeJoin(Paint.Join.ROUND);

		wtSetDrawer = new CWrittenTokenSetDrawer();

        /* Initialize multitouch state */
        internalResetPinchMoveState();
	}
	
	/* Get the number of strokes */
//	public final int nStrokes() {		// WORK: Adding StrokeCurator
//		return strokes.size();
//	}
	
	/* Get the number of incorporated strokes */
//	public final int nIncoporatedStrokes() {	// WORK: Adding StrokeCurator
//		return ( strokeStat.size() - Collections.frequency(strokeStat, -1) );
//	}
	
	/* Get the number of unincorporated strokes */
//	public final int nUnincorporatedStrokes() {		// WORK
//		return Collections.frequency(strokeStat, -1);
//	}
	
	protected void setTokenNames(String [] t_tokenNames) {
		tokenNames  = t_tokenNames;
		
		wtSet.setTokenNames(t_tokenNames);
	}
	
	protected void setProbabilityValues(double [] ps) {
		if ( (probVals == null || probVals.length != ps.length) && (ps != null && ps.length > 0) ) {
			probVals = new double[ps.length];
		}

		for (int i = 0; i < ps.length; ++i) {
            probVals[i] = ps[i];
        }
	}	
	
	protected void clearProbabilityValues() {
		probVals = null;
	}

    private Path worldPath2ScreenPath(final ExtendedPath path) {
        Path screenPath = new Path();

//        int nPts = path.numPoints();
        Iterator<Float> xsIter = path.getXs().iterator();
        Iterator<Float> ysIter = path.getYs().iterator();

        int nPts = 0;
        while (xsIter.hasNext()) {
            float[] xy = new float[2];
            xy[0] = xsIter.next();
            xy[1] = ysIter.next();

            float[] screenXY = wtSetDrawer.worldPos2ScreenPos(xy);

            if (nPts++ == 0) {
                screenPath.moveTo(screenXY[0], screenXY[1]);
            } else {
                screenPath.lineTo(screenXY[0], screenXY[1]);
            }
        }

        return screenPath;

//        final Matrix xfm = new Matrix();
//
//        final float scale = 1f / wtSetDrawer.getScale();
//        xfm.postScale(scale, scale);
//
//        xfm.postTranslate(-1f * wtSetDrawer.getDx(), -1f * wtSetDrawer.getDy());
//
//        path.transform(xfm, screenPath);
//
//        return screenPath;
    }

    private void drawStrokes(Canvas canvas) {
        for (int i = 0; i < paths.size(); ++i) {
            if (nPoints.get(i) <= 2) {	 /* TODO: Remove magic number */
                Point tFirstPoint = firstPoints.get(i);

                float[] pointXY = new float[2];
                pointXY[0] = tFirstPoint.x;
                pointXY[1] = tFirstPoint.y;
                float[] worldXY = wtSetDrawer.worldPos2ScreenPos(pointXY);

                canvas.drawPoint(worldXY[0], worldXY[1], paintStrokePoint);
            } else {
                ExtendedPath tPath = paths.get(i);
                Path tWorldPath = worldPath2ScreenPath(tPath);

                canvas.drawPath(tWorldPath, paintStrokeCurve);
            }
        }
    }

	@Override
	protected void onDraw(Canvas canvas) {
		/* Draw the strokes */
        drawStrokes(canvas);
		
		/* Test code: draw axes and bars */
		if ( canvasBarPlotter == null ) { /* Initialize */
			canvasBarPlotter = new CanvasBarPlotter(canvas);
		}

		if ( probVals != null && probVals.length > 0 ) {
			canvasBarPlotter.draw(probVals, tokenNames, canvas);
		}

        /* Draw the token recognition results */
		if ( !wtSet.empty() ) {
			wtSetDrawer.draw(canvas, wtSet, wtUIStates);
		}
	}


    protected void injectPathsState() {

    }

    private void removeTempPathInfo() {
        paths.removeLast();
        nPoints.removeLast();
        firstPoints.remove(firstPoints.size() - 1);
    }

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float eventX = event.getX();
		float eventY = event.getY();

        final float[] eventScreenXY = new float[] {eventX, eventY};
        float[] eventWorldXY  = wtSetDrawer.screenPos2WorldPos(eventScreenXY);
        float eventWorldX = eventWorldXY[0];
        float eventWorldY = eventWorldXY[1];

        final int idxEncompToken = this.indexOfEncompassingToken(eventWorldX, eventWorldY);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
            /* Determine if the tap event happens within the bounds of an existing token */

            if (idxEncompToken != -1) {
                if (this.wtUIStates.get(idxEncompToken).getState() == WrittenTokenUIState.State.Selected) {
                    /* Already selected: Entering TokenMove mode */
                    tokenMoveOrigin = new float[] {eventX, eventY};
                    tokenMoveLastPos = new float[] {eventX, eventY};
                    idxTokenMoved = idxEncompToken;
                    movedTokenConstStrokeIdx = wtConstStrokeIdx.get(idxEncompToken);
                    tokenMoveInitialBounds = this.wtSet.getTokenBounds(idxEncompToken);

                    this.uiCurrentStatus = PlatoMultiTouchUIState.MoveToken;
                    this.hasBeenTokenMove = true;
                } else {
                    /* Not yet selected: increment preselection counter */
                    this.wtUIStates.get(idxEncompToken).incrementPreselectionCounter(eventX, eventY);
                }
            }

            if (this.uiCurrentStatus != PlatoMultiTouchUIState.MoveToken) {
                paths.add(new ExtendedPath());

                paths.getLast().moveTo(eventWorldX, eventWorldY);
                writtenToken.newStroke(eventWorldX, eventWorldY);

                nPoints.add(1);
                firstPoints.add(new Point((int) eventWorldX, (int) eventWorldY));

                /* Add a new, unincorporated stroke */
                lastStroke = new CStroke(eventWorldX, eventWorldY);

                if (bDebug) {
                    System.out.println("ACTION_DOWN: eventX = " + eventX + "; eventY = " + eventY);
                    System.out.println("             eventWorldX = " + eventWorldX + "; eventY = " + eventWorldY);
                }
            }
			
			break;
		case MotionEvent.ACTION_MOVE:
            if (uiCurrentStatus == PlatoMultiTouchUIState.Pinch && event.getPointerCount() == 2) {
                /* Pinch movement */
                getPinchPoints(pinchPointsCurr, event, pinchPointerIds);

                /* TODO: Calculate the type of the pinch movement: pan vs zoom */
                PinchMovement pinchMvt = new PinchMovement(pinchPointsPrev, pinchPointsCurr);

                /* Invoke drawer FOV update */
                if (pinchMvt.getType() == PinchMovement.Type.PinchPan) {
                    float fovCompositeSize = this.wtSetDrawer.getFovCompositeSize();

                    this.wtSetDrawer.fovPan(-panSensitivity * fovCompositeSize * pinchMvt.getPanX(),
                                            -panSensitivity * fovCompositeSize * pinchMvt.getPanY());
                } else if (pinchMvt.getType() == PinchMovement.Type.PinchZoom) {
                    this.wtSetDrawer.fovZoom(-zoomSensitivity * pinchMvt.getZoomRatio());
                } else {
                    throw new RuntimeException("Unsupported PinchMovement type: " + pinchMvt.getType());
                }

                /* Prepare for the next event */
                rememberPrevPinchPoints();
            } else if (uiCurrentStatus == PlatoMultiTouchUIState.MoveToken) {
                /* Total shifts in this token move event */
                final float xShift = eventX - tokenMoveOrigin[0];
                final float yShift = eventY - tokenMoveOrigin[1];

                /* Incremental shifts */
                final float xShiftIncr = eventX - tokenMoveLastPos[0];
                final float yShiftIncr = eventY - tokenMoveLastPos[1];

                final float[] newBounds = new float[] {tokenMoveInitialBounds[0] + xShift,
                                                       tokenMoveInitialBounds[1] + yShift,
                                                       tokenMoveInitialBounds[2] + xShift,
                                                       tokenMoveInitialBounds[3] + yShift};
                this.wtSet.setTokenBounds(idxTokenMoved, newBounds);

                // TODO: The path(s) that belong to this token also need to be moved (use method: offset(dx, dy))
                for (int i = 0; i < movedTokenConstStrokeIdx.length; ++i) {
                    int strokeIdx = movedTokenConstStrokeIdx[i];
                    paths.get(strokeIdx).shift(xShiftIncr, yShiftIncr);
                }

                tokenMoveLastPos[0] = eventX;
                tokenMoveLastPos[1] = eventY;
            } else if (!hasBeenPinch && !hasBeenSelection) {
                /* Plain write mode */
                if (idxEncompToken != -1) {
                    if (this.wtUIStates.get(idxEncompToken).getState() == WrittenTokenUIState.State.Preselection) {
                        this.wtUIStates.get(idxEncompToken).incrementPreselectionCounter(eventX, eventY);
                    }

                    if (this.wtUIStates.get(idxEncompToken).getState() == WrittenTokenUIState.State.Selected) {
                        hasBeenSelection = true;

                        /* Remove temporary paths */
                        removeTempPathInfo();
                    }

                }

                if (!hasBeenSelection) {
                    /* Single-pointer movement: Handwriting */
                    eventScreenXY[0] = eventX;
                    eventScreenXY[1] = eventY;
                    eventWorldXY = wtSetDrawer.screenPos2WorldPos(eventScreenXY);
                    eventWorldX = eventWorldXY[0];
                    eventWorldY = eventWorldXY[1];

                    nPoints.set(nPoints.size() - 1, nPoints.getLast() + 1);
                    paths.getLast().lineTo(eventWorldX, eventWorldY);
                    writtenToken.addPoint(eventWorldX, eventWorldY);
                    lastStroke.addPoint(eventWorldX, eventWorldY);

                    if (bDebug) {
                        System.out.println("ACTION_DOWN: eventX = " + eventX + "; eventY = " + eventY);
                        System.out.println("             eventWorldX = " + eventWorldX + "; eventY = " + eventWorldY);
                    }
                }
            }
			
			break;
		case MotionEvent.ACTION_UP:
            /* Examine any selection */
            if (idxEncompToken != -1 &&
                this.wtUIStates.get(idxEncompToken).getState() == WrittenTokenUIState.State.Preselection &&
                this.wtUIStates.get(idxEncompToken).selectionTimeTest(new Date().getTime())) {

                hasBeenSelection = true;

                removeTempPathInfo();
            }

            internalResetPinchMoveState();
			break;

        case MotionEvent.ACTION_POINTER_DOWN:
            if ( uiCurrentStatus != PlatoMultiTouchUIState.Pinch && event.getPointerCount() == 2 ) {
                uiCurrentStatus = PlatoMultiTouchUIState.Pinch;
                hasBeenPinch = true;

                /* Remove temporary path info */
                removeTempPathInfo();

                pinchPointerIds = getPinchPoints(pinchPointsPrev, event, pinchPointerIds);
            } else {
                internalResetPinchMoveState();
            }


            break;
        case MotionEvent.ACTION_POINTER_UP:
            internalResetPinchMoveState();
            break;
		default:
			return false;
		}

		/* Schedules a repaint */
		invalidate();
		return true;
	}
	

	public int[] getImagePixels() {
		Bitmap bitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(),
                Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		this.draw(canvas);
		int[] pixels = new int[bitmap.getWidth()*bitmap.getHeight()];
		bitmap.getPixels(pixels, 0, this.getWidth(), 0, 0, this.getWidth(), this.getHeight());
		return pixels;
		/*for(int i=0;i<this.getHeight(); i++) {
			String str="";
			for(int j=0; j<this.getWidth(); j++) {
				str = str+pixels[(i*this.getWidth())+j];
			}
			Log.i(TAG, str);
			
		}*/
		// compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(new
		// File("D:/tt.jpg")));
	}
	
	public void clear() {
		paths.clear();
		nPoints.clear();
		firstPoints.clear();
		
		writtenToken.clear();
		
		lastStroke = null;
		
		postInvalidate();
	}
	
	public boolean isEmpty() {
		//return path.isEmpty();
		return paths.isEmpty();
	}
	
	/* Get string */	
	public String getWrittenTokenSetString() {
		String str = "WrittenTokenSet (n=" + wtSet.nTokens() + ")";
        List<String >wtRecogWinners = wtSet.recogWinners;
		
		if ( wtSet.nTokens() > 0 )
			str += ":\n";
		
		for (int i = 0; i < wtSet.nTokens(); ++i) {
			str += "\"" + wtRecogWinners.get(i) + "\": [";
			float [] bnds = wtSet.tokens.get(i).getBounds();
			str +=  + bnds[0] + ", " + bnds[1] + ", " + bnds[2] + ", " + bnds[3] + "]\n";
		}
		
		return str;
	}

	public synchronized TokenSetParserOutput getWebParserOutput() {
		return webParserOutput;
	}

	public synchronized void setWebParserOutput(TokenSetParserOutput webParserOutput) {
		this.webParserOutput = webParserOutput;
	}

    public synchronized void setWrittenTokenSet(CWrittenTokenSet writtenTokenSet,
                                                List<int []> wtConstStrokeIdx) {
        this.wtSet = writtenTokenSet;
        this.wtConstStrokeIdx = wtConstStrokeIdx;

        /* Initialize UI states for the tokens */
        this.wtUIStates = new ArrayList<WrittenTokenUIState>();
        for (int i = 0; i < wtSet.getNumTokens(); ++i) {
            wtUIStates.add(new WrittenTokenUIState());
        }

        /* Determine if the last stroke was merged */
        isLastStrokeMerged = false;
        int ilt = writtenTokenSet.getNumStrokes() - 1; /* Index to the last stroke */
        for (int[] strokeIndices : wtConstStrokeIdx) {
            if (strokeIndices.length > 1 && MathHelper.countOccurrences(strokeIndices, ilt) > 0) {
                isLastStrokeMerged = true;
                break;
            }
        }
    }

    /* Examine if the current position (in world coordinates) falls within the
     * boundary of any existing tokens.
     *
     * @returns   Index of first token of which the boundary encompasses the coordinates
     *            TODO: Cases of overlapping tokens
     *            -1 of none of the tokens encompasses the coordinates
     */
    private int indexOfEncompassingToken(final float x, final float y) {
        int idx = -1;

        int nTokens = wtSet.getNumTokens();
        for (int i = 0; i < nTokens; ++i) {
            float[] bnds = wtSet.getTokenBounds(i);
            if (bnds[0] < x && bnds[2] > x && bnds[1] < y && bnds[3] > y) {
                return i;
            }
        }

        return idx;
    }

    private final void internalResetPinchMoveState() {
        uiCurrentStatus = PlatoMultiTouchUIState.Write;
        pinchPointerIds = null;

        /* hasBeenPinch should be externally reset */
    }

    public final void externalResetUIState() {
        internalResetPinchMoveState();

        hasBeenPinch     = false;
        hasBeenSelection = false;
        hasBeenTokenMove = false;

        /* Reset moveToken state */
        idxTokenMoved = -1;
    }

    /* @returns  Pinch pointer ids */
    public int[] getPinchPoints(float[][] pinchPoints,  /* Side-effect modification */
                                final MotionEvent event,
                                final int[] pinchPointerIds) {
        if (pinchPointerIds == null) {
            int[] rPinchPointerIds = new int[2];

            /* Capture the IDs of the two pointers */
            rPinchPointerIds[0] = event.getPointerId(0);
            rPinchPointerIds[1] = event.getPointerId(1);

                /* Capture the current position of the two pointers */
            for (int i = 0; i < 2; ++i) {
                pinchPoints[i][0] = event.getX(i);
                pinchPoints[i][1] = event.getY(i);
            }

            return rPinchPointerIds;
        } else {
            /* Search for the indices of the pointers */
            int ptrId0 = -1;
            int ptrId1 = -1;

            for (int i = 0; i < event.getPointerCount(); ++i) {
                if (event.getPointerId(i) == pinchPointerIds[0]) {
                    ptrId0 = i;
                } else if (event.getPointerId(i) == pinchPointerIds[1]) {
                    ptrId1 = i;
                }
            }

            if (ptrId0 == -1 || ptrId1 == -1) {
                throw new RuntimeException("Cannot find expected multi-touch pointers");
            }

            pinchPoints[0][0] = event.getX(ptrId0);
            pinchPoints[0][1] = event.getY(ptrId0);
            pinchPoints[1][0] = event.getX(ptrId1);
            pinchPoints[1][1] = event.getY(ptrId1);

            return pinchPointerIds;
        }
    }

    private void rememberPrevPinchPoints() {
        for (int k0 = 0; k0 < pinchPointsCurr.length; ++k0) {
            for (int k1 = 0; k1 < pinchPointsCurr[k0].length; ++k1) {
                pinchPointsPrev[k0][k1] = pinchPointsCurr[k0][k1];
            }
        }
    }

    public boolean getHasBeenPinch() {
        return hasBeenPinch;
    }

    public boolean getHasBeenSelection() {
        return hasBeenSelection;
    }

    public boolean getHasBeenTokenMove() {
        return hasBeenTokenMove;
    }

    public float getPanSensitivity() {
        return panSensitivity;
    }

    public float getZoomSensitivity() {
        return zoomSensitivity;
    }

    public void setPanSensitivity(float panSensitivity) {
        this.panSensitivity = panSensitivity;
    }

    public void setZoomSensitivity(float zoomSensitivity) {
        this.zoomSensitivity = zoomSensitivity;
    }

    public boolean isLastStrokeMerged() {
        return isLastStrokeMerged;
    }
}