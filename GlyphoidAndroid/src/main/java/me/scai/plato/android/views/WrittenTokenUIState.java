package me.scai.plato.android.views;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by scai on 5/25/2015.
 */
public class WrittenTokenUIState {
    /* Enums */
    public static enum State {
        NotSelected,
        Preselection,
        Selected
    }

    /* Constants */
    private static final int SELECTION_THRESHOLD = 30;
    private static final float SELECTION_MAX_DIST = 10f;
    private static final long SELECTION_TIME_THRESHOLD_MS = 1000L;

    /* Member variables */
    private State state = State.NotSelected;
    private int preselectionCounter;

    private long downTime; /* Time when ACTION_DOWN occurred */

    /* Preselection touch coordinates (in screen coordinates) */
    private List<Float> preselectXs = new LinkedList<Float>();
    private List<Float> preselectYs = new LinkedList<Float>();

    /* Methods */
    public void incrementPreselectionCounter(float touchX, float touchY) {
        if (this.state == State.NotSelected) {
            this.state = State.Preselection;
            this.preselectionCounter = 1;

            preselectXs.add(touchX);
            preselectYs.add(touchY);

            downTime = new Date().getTime();

        } else if (this.state == State.Preselection) {
            if (++this.preselectionCounter == SELECTION_THRESHOLD) {
                if (selectionDistTest()) {
                    this.state = State.Selected;

                    preselectXs.add(touchX);
                    preselectYs.add(touchY);
                } else {
                    reset();
                }

            }
        }
    }

    public boolean selectionTimeTest(long upTime) {
        boolean isSelection = upTime - this.downTime > SELECTION_TIME_THRESHOLD_MS;

        if (isSelection) {
            this.state = State.Selected;
        }

        return isSelection;
    }

    /* @returns   true:  if is selection event (distance smaller than max)
    *             false: if not selection */
    private boolean selectionDistTest() {
        final long currTime = new Date().getTime();

        Iterator<Float> xIter = preselectXs.iterator();
        Iterator<Float> yIter = preselectYs.iterator();

        float min_x = Float.POSITIVE_INFINITY;
        float max_x = Float.NEGATIVE_INFINITY;
        float min_y = Float.POSITIVE_INFINITY;
        float max_y = Float.NEGATIVE_INFINITY;

        while (xIter.hasNext()) {
            float x = xIter.next();
            float y = yIter.next();

            if (x < min_x) {
                min_x = x;
            }
            if (x > max_x) {
                max_x = x;
            }

            if (y < min_y) {
                min_y = y;
            }
            if (y > max_y) {
                max_y = y;
            }
        }

        return ( currTime - downTime > SELECTION_TIME_THRESHOLD_MS )
               && ( ((max_x - min_x) * (max_x - min_x) + (max_y - min_y) * (max_y - min_y)
                < SELECTION_MAX_DIST * SELECTION_MAX_DIST) );
    }

    public void reset() {
        this.state = State.NotSelected;
        preselectionCounter = 0;
        preselectXs.clear();
        preselectYs.clear();
    }

    /* Getters */
    public State getState() {
        return state;
    }



}
