package me.scai.plato.android.views;

import android.graphics.Path;
import android.graphics.Point;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by scai on 5/24/2015.
 */
public class ExtendedPath extends Path {
    private List<Float> xs = new LinkedList<Float>();
    private List<Float> ys = new LinkedList<Float>();

    @Override
    public void moveTo(float x, float y) {
        super.moveTo(x, y);

        xs.add(x);
        ys.add(y);
    }

    @Override
    public void lineTo(float x, float y) {
        super.lineTo(x, y);

        xs.add(x);
        ys.add(y);
    }

    /* Shift the path */
    public void shift(float dx, float dy) {
        Iterator<Float> xit = xs.iterator();
        Iterator<Float> yit = ys.iterator();

        List<Float> newXs = new LinkedList<Float>();
        List<Float> newYs = new LinkedList<Float>();

        while (xit.hasNext()) {
            newXs.add(xit.next() + dx);
            newYs.add(yit.next() + dy);
        }

        xs = newXs;
        ys = newYs;
    }

    /* Get number of points */
    public int numPoints() {
        return xs.size();
    }

    /* Getters */
    public List<Float> getXs() {
        return xs;
    }

    public List<Float> getYs() {
        return ys;
    }
}
