package me.scai.plato.android.views;

/**
 * Created by scai on 5/23/2015.
 */
public final class PinchMovement {
    public enum Type {
        PinchPan,
        PinchZoom,
        PinchComposite /* Currently not used */
    }

    private static final float fixedZoomRatio = 0.1f;

    private Type type;
    private float panX;
    private float panY;
    private float zoomRatio; /* 0: no zoom; >0 zoom out */

    /* Constructors */
    private PinchMovement() {}

    public PinchMovement(final float[][] prev, final float [][] curr) {
        /* Displacement of the first point */
        float xDsp0 = curr[0][0] - prev[0][0];
        float yDsp0 = curr[0][1] - prev[0][1];

        /* Displacement of the second point */
        float xDsp1 = curr[1][0] - prev[1][0];
        float yDsp1 = curr[1][1] - prev[1][1];

        if (xDsp0 * xDsp1 > 0f || yDsp0 * yDsp1 > 0f) {
            /* Pan */
            type = Type.PinchPan;

            panX = (xDsp0 + xDsp1) * 0.5f;
            panY = (yDsp0 + yDsp1) * 0.5f;
        } else {
            /* Zoom */
            type = Type.PinchZoom;

            /* Previous distance */
            float dstPrev = (prev[0][0] - prev[1][0]) * (prev[0][0] - prev[1][0]) +
                            (prev[0][1] - prev[1][1]) * (prev[0][1] - prev[1][1]);
            /* Current distance */
            float dstCurr = (curr[0][0] - curr[1][0]) * (curr[0][0] - curr[1][0]) +
                            (curr[0][1] - curr[1][1]) * (curr[0][1] - curr[1][1]);

            if (dstCurr > dstPrev) { /* Zoom out */
                zoomRatio = fixedZoomRatio;
            } else if (dstCurr < dstPrev) {
                zoomRatio = -1f * fixedZoomRatio;
            }
        }
    }

    /* Getters */
    public Type getType() {
        return type;
    }

    public float getPanX() {
        return panX;
    }

    public float getPanY() {
        return panY;
    }

    public float getZoomRatio() {
        return zoomRatio;
    }

    /* TODO: Override toString, hashCode and equals */
}
