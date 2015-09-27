package me.scai.plato.android.events;

import java.util.List;

import me.scai.handwriting.CWrittenTokenSet;
import me.scai.parsetree.MathHelper;

public class TokenRecogCandidatesEvent {
    private String[] candidates; /* Includes the max-p token */

    /* Constructor */
    public TokenRecogCandidatesEvent(final String[] candidates) {
        this.candidates = candidates;
    }

    public TokenRecogCandidatesEvent(final CWrittenTokenSet wtSet,
                                     final int idxToken,
                                     final int nMax,
                                     final List<String> tokenNames) {
        final double[] lastTokenRecogPs = wtSet.tokens.get(idxToken).getRecogPs();
        int[] maxPIndices = MathHelper.getMaxNIndices(lastTokenRecogPs, nMax);

        candidates = new String[nMax];
        for (int i = 0; i < nMax; ++i) {
            candidates[i] = tokenNames.get(maxPIndices[i]);
        }
    }

    /* Getters */
    public String[] getCandidates() {
        return candidates;
    }
}
