package me.scai.plato.android.views;

import java.util.Iterator;
import java.util.List;

import me.scai.handwriting.CWrittenTokenSet;
import me.scai.plato.android.helpers.TokenNameHelper;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.Typeface;

/* Class for plotting recognized token set (CWrittenTokenSet) */
public class CWrittenTokenSetDrawer {
    /* Member variables */
	private int boxAlpha = 127;
	private int [] boxColor = {255, 255, 0};
	private int txtAlpha = 127;
	private int [] txtColor = {255, 255, 0};
	private float txtSize = 25;
    private int formattedTxtAlpha = 127;
    private int [] formattedTxtColor = {255, 255, 0};

    private int selBoxAlpha = 127;
    private int [] selBoxColor = {127, 127, 127};

	private Paint paintBox;
    private Paint paintBoxSel;
	private Paint paintTxtLabel;
    private Paint paintTxtFormatted;
    private Paint paintTxtFormattedManual;

    private boolean displayOverlaidSymbols = false;

//	private Map<String, String> letterDisplayNames;

    /* Variables for zoom/pan state */
    private float dx = 0f; /* Translation */
    private float dy = 0f;
    private float scale = 1f;  /* Scaling */

    private float screenW; /* Screen width and height */
    private float screenH;

    /* Field of view (FOV) parameters */
    private boolean screenSizePreviouslySet = false;
    private final float[] fov = new float[4]; /* x_min, y_min, x_max, y_max */
	
	/* Constructor */
//	public CWrittenTokenSetDrawer(String [] tLetters, String [] tLetterDisplayNames) {
//        this();

//		letterDisplayNames = new HashMap<String, String>();
//		for (int i = 0; i < tLetters.length; ++i) {
//			letterDisplayNames.put(tLetters[i], tLetterDisplayNames[i]);
//		}
//	}
	
	public CWrittenTokenSetDrawer() {
//		letterDisplayNames = null;
	}

    /**
     * Zoom in or out
     * @params    new zoom ratio:
     *              >0f --> zoom out
     *             <0f --> zoom in
     * @return    new FOV array */
    public float[] fovZoom(final float zoomRatio) {
        /* Calculate the center of the FOV. Zoom with respect to it */
        final float ctrX = (fov[0] + fov[2]) * 0.5f;
        final float ctrY = (fov[1] + fov[3]) * 0.5f;

        float halfW = (fov[2] - fov[0]) * 0.5f;
        float halfH = (fov[3] - fov[1]) * 0.5f;

        final float r = 1f + zoomRatio;
        halfW *= r;
        halfH *= r;

        /* Update FOV */
        fov[0] = ctrX - halfW;
        fov[1] = ctrY - halfH;
        fov[2] = ctrX + halfW;
        fov[3] = ctrY + halfH;

        /* Update the state variables */
        scale *= r;

        /* TODO: Handle overflow */
        return fov;
    }

    /**
     * Pan left/right up/down
     * Move the field of view by the amount specified with panX and panY.
     * Note that in the of touch-screen pinch movement, [panX, panY] should be the
     * negative of the pointer movement vector.
     *
     * @params    panX and panY
     * @return    new FOV array */
    public float[] fovPan(final float panX, final float panY) {
        /* Update FOV */
        fov[0] += panX;
        fov[1] += panY;
        fov[2] += panX;
        fov[3] += panY;

        dx += panX;
        dy += panY;

        /* TODO: Handle overflow */
        return fov;

    }

    public void reset() {
        fov[0] = 0f;
        fov[1] = 0f;
        fov[2] = screenW;
        fov[3] = screenH;

        dx = 0f;
        dy = 0f;
        scale = 1f;
    }

	/* Property setters */
	public void setBoxARGB(int a, int r, int g, int b) {
		boxAlpha = a;
		boxColor[0] = r;
		boxColor[1] = g;
		boxColor[2] = b;
	}
	
	public void setTxtARGB(int a, int r, int g, int b) {
		txtAlpha = a;
		txtColor[0] = r;
		txtColor[1] = g;
		txtColor[2] = b;
	}
	
	private String getTokenDisplayName(String tokenName) {
        return TokenNameHelper.tokenName2DisplayName(tokenName);
	}
	
	/* Main function */
	public void draw(Canvas canvas,
			         CWrittenTokenSet wtSet,
                     List<WrittenTokenUIState> wtUIStates) {
        List<String> wtRecogWinners = wtSet.recogWinners;

		if ( paintBox == null ) {
			paintBox = new Paint();
			paintBox.setARGB(boxAlpha, boxColor[0], boxColor[1], boxColor[2]);
			paintBox.setStyle(Style.STROKE);
		}

        if ( paintBoxSel == null ) {
            paintBoxSel = new Paint();
            paintBoxSel.setARGB(selBoxAlpha, selBoxColor[0], selBoxColor[1], selBoxColor[2]);
            paintBoxSel.setStyle(Style.FILL_AND_STROKE);
        }
		
		if ( paintTxtLabel == null ) {
			paintTxtLabel = new Paint();
			paintTxtLabel.setARGB(txtAlpha, boxColor[0], boxColor[1], boxColor[2]);
			paintTxtLabel.setStyle(Style.STROKE);
			
			paintTxtLabel.setTextSize(txtSize);
		}

        if ( paintTxtFormatted == null ) {
            Typeface tf = Typeface.create("lucida", Typeface.ITALIC); /* TODO: Remove magic value */

            paintTxtFormatted = new Paint();
            paintTxtFormatted.setAntiAlias(true);
            paintTxtFormatted.setARGB(formattedTxtAlpha,
                                      formattedTxtColor[0],
                                      formattedTxtColor[1],
                                      formattedTxtColor[2]);
            paintTxtFormatted.setStyle(Style.FILL_AND_STROKE);
            paintTxtFormatted.setTypeface(tf);
        }

        if ( paintTxtFormattedManual == null ) {
            paintTxtFormattedManual = new Paint();
            paintTxtFormatted.setAntiAlias(true);
            paintTxtFormattedManual.setARGB(formattedTxtAlpha,
                                            formattedTxtColor[0],
                                            formattedTxtColor[1],
                                            formattedTxtColor[2]);
            paintTxtFormatted.setStrokeWidth(5f); /* TODO: Remove magic number */
        }
		
		if ( wtSet == null ) {
            return;
        }

        Iterator<WrittenTokenUIState> wtUIStateIter = wtUIStates.iterator();
		for (int i = 0; i < wtSet.nTokens(); ++i) {
            WrittenTokenUIState wtUIState = wtUIStateIter.next();

            float[] bnds = wtSet.tokens.get(i).getBounds();
			float[] mappedBnds = worldBnds2ScreenBnds(bnds);

            /* Bounding box */
            Rect rectBox = new Rect((int) mappedBnds[0], (int) mappedBnds[1],
                                    (int) mappedBnds[2], (int) mappedBnds[3]);
            canvas.drawRect(rectBox, paintBox);
            if (wtUIState.getState() == WrittenTokenUIState.State.Selected) {
                canvas.drawRect(rectBox, paintBoxSel);
            }

            /* Text label */
			canvas.drawText(getTokenDisplayName(wtRecogWinners.get(i)),
					        mappedBnds[0], mappedBnds[1] - (mappedBnds[3] - mappedBnds[1]) * 0.01F,
                            paintTxtLabel);

            if (displayOverlaidSymbols) {
            /* Formatted text */
                String tokenDispName = getTokenDisplayName(wtRecogWinners.get(i));

                if (tokenDispName.equals("-")) {
                    float startX = mappedBnds[0];
                    float startY = 0.5f * (mappedBnds[1] + mappedBnds[3]);
                    float endX = mappedBnds[2];
                    float endY = startY;

                    canvas.drawLine(startX, startY, endX, endY, paintTxtFormatted);
                } else if (tokenDispName.equals("sqrt")) {
                    final float topBarLeftShiftFactor = 0.16f;
                    final float centerLineLeftShiftFactor = 0.08f;
                    final float leftLineDownShiftFactor = 0.70f;

                    float topBarStartX = mappedBnds[0] +
                            topBarLeftShiftFactor * (mappedBnds[2] - mappedBnds[0]);
                    float topBarEndX = mappedBnds[2];
                    float topBarStartY = mappedBnds[1];
                    float topBarEndY = mappedBnds[1];

                    float centerLineStartX = mappedBnds[0] +
                            centerLineLeftShiftFactor * (mappedBnds[2] - mappedBnds[0]);
                    float centerLineEndX = topBarStartX;
                    float centerLineStartY = mappedBnds[3];
                    float centerLineEndY = mappedBnds[1];

                    float leftLineStartX = mappedBnds[0];
                    float leftLineEndX = centerLineStartX;
                    float leftLineStartY = mappedBnds[1] + leftLineDownShiftFactor * (mappedBnds[3] - mappedBnds[1]);
                    float leftLineEndY = mappedBnds[3];

                    canvas.drawLine(leftLineStartX, leftLineStartY, leftLineEndX, leftLineEndY,
                            paintTxtFormatted);
                    canvas.drawLine(centerLineStartX, centerLineStartY, centerLineEndX, centerLineEndY,
                            paintTxtFormatted);
                    canvas.drawLine(topBarStartX, topBarStartY, topBarEndX, topBarEndY,
                            paintTxtFormatted);

                } else if (tokenDispName.equals("1")) {
                    float txtSize = (mappedBnds[2] - mappedBnds[0] > mappedBnds[3] - mappedBnds[1]) ?
                            (mappedBnds[2] - mappedBnds[0]) :
                            (mappedBnds[3] - mappedBnds[1]);
                    txtSize *= 1.35f; /* TODO: Remove magic number */
                    paintTxtFormatted.setTextSize(txtSize);

                    canvas.drawText(getTokenDisplayName(wtRecogWinners.get(i)),
                            mappedBnds[0] - 0.30f * txtSize, mappedBnds[3],
                            paintTxtFormatted);
                } else if (tokenDispName.equals("=")) {
                    float txtSize = (mappedBnds[2] - mappedBnds[0] > mappedBnds[3] - mappedBnds[1]) ?
                            (mappedBnds[2] - mappedBnds[0]) :
                            (mappedBnds[3] - mappedBnds[1]);
                    txtSize *= 0.90f; /* TODO: Remove magic number */

                    final float h = mappedBnds[3] - mappedBnds[1];
                    canvas.drawText(getTokenDisplayName(wtRecogWinners.get(i)),
                            mappedBnds[0], mappedBnds[3] + 0.8f * h,
                            paintTxtFormatted);
                } else {
                    float txtSize = (mappedBnds[2] - mappedBnds[0] > mappedBnds[3] - mappedBnds[1]) ?
                            (mappedBnds[2] - mappedBnds[0]) :
                            (mappedBnds[3] - mappedBnds[1]);
                    txtSize *= 1.30f; /* TODO: Remove magic number */
                    paintTxtFormatted.setTextSize(txtSize);

                    canvas.drawText(getTokenDisplayName(wtRecogWinners.get(i)),
                            mappedBnds[0], mappedBnds[3],
                            paintTxtFormatted);
                }
            }
		}
	}

    /**
     * @param:   xy: length-2 array
     * @return: mapped position */
    public float[] worldPos2ScreenPos(final float[] xy) {
        assert(xy.length == 2);

        final float x = xy[0];
        final float y = xy[1];

        final float rx = (x - fov[0]) / (fov[2] - fov[0]);
        final float ry = (y - fov[1]) / (fov[3] - fov[1]);

        final float[] mappedXY = new float[2];
        mappedXY[0] = rx * screenW;
        mappedXY[1] = ry * screenH;

        return mappedXY;
    }

    public float[] screenPos2WorldPos(final float[] xy) {
        assert (xy.length == 2);

        float[] worldXY = new float[2];

        final float rx = xy[0] / screenW;
        final float ry = xy[1] / screenH;

        worldXY[0] = fov[0] + rx * (fov[2] - fov[0]);
        worldXY[1] = fov[1] + ry * (fov[3] - fov[1]);

        return worldXY;
    }

    private float[] worldBnds2ScreenBnds(final float[] bnds) {
        assert(bnds.length == 4);

        final float[] corner0 = new float[2];
        corner0[0] = bnds[0]; /* x_min */
        corner0[1] = bnds[1]; /* y_min */

        final float[] corner1 = new float[2];
        corner1[0] = bnds[2]; /* x_max */
        corner1[1] = bnds[3]; /* y_max */

        final float[] mappedCorner0 = worldPos2ScreenPos(corner0);
        final float[] mappedCorner1 = worldPos2ScreenPos(corner1);

        float[] mappedBnds = new float[4];
        mappedBnds[0] = mappedCorner0[0]; /* x_min */
        mappedBnds[1] = mappedCorner0[1]; /* y_min */
        mappedBnds[2] = mappedCorner1[0]; /* x_max */
        mappedBnds[3] = mappedCorner1[1]; /* y_max */

        return mappedBnds;
    }

    public void setScreenWH(float screenWidth, float screenHeight) {
        if (screenWidth <= 0f || screenHeight <= 0f) {
            return; // We do not want to adjust the fov and center in response to events of full collapse of the canvas
        }

        if ( !screenSizePreviouslySet ) {
            this.screenW = screenWidth;
            this.screenH = screenHeight;

            /* Set initial pan/zoom state according to screen size */
            fov[0] = 0f; /* x_min */
            fov[1] = 0f; /* y_min */
            fov[2] = screenW; /* x_max */
            fov[3] = screenH; /* y_max */

            dx = 0f;
            dy = 0f;
            scale = 1f;

            screenSizePreviouslySet = true;
        } else {
            /* React to screen size change */
            float rw = screenWidth / this.screenW;
            float rh = screenHeight / this.screenH;
            float rwh = (float) Math.sqrt(rw * rh);

            float cx = (fov[0] + fov[2]) * 0.5f;
            float cy = (fov[1] + fov[3]) * 0.5f;
            float hw = (fov[2] - fov[0]) * 0.5f;
            float hh = (fov[3] - fov[1]) * 0.5f;

            fov[0] = cx - hw / rwh;
            fov[1] = cy - hh / rwh;
            fov[2] = cx + hw / rwh;
            fov[3] = cy + hh / rwh;

            scale *= rwh;

            this.screenW = screenWidth;
        }
    }

    public float getScale() {
        return scale;
    }

    public float getDx() {
        return dx;
    }

    public float getDy() {
        return dy;
    }

    public void setDisplayOverlaidSymbols(boolean displayOverlaidSymbols) {
        this.displayOverlaidSymbols = displayOverlaidSymbols;
    }

    public float getFovWidth() {
        return fov[2] - fov[0];
    }

    public float getFovHeight() {
        return fov[3] - fov[1];
    }

    public float getFovCompositeSize() {
        return 0.5f * (getFovWidth() + getFovHeight());
    }
}