package me.scai.plato.android.views;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import me.scai.plato.android.helpers.TokenNameHelper;
import me.scai.plato.android.R;

public class FineActionsPanel {
    /* Enum types */
    private enum FineActionsType {
        None,
        Candidates,
        Merge,
        Clear
    }

    /* CONSTANTS */
    private static final int numMergeLastStrokesButtons = 2;

    /* Member variables */
    LinearLayout fineActionsPanel;

    Activity activity;
    PlatoBoard platoBoard;

    private String[] tokenNameCandidates;

    private List<Button> tokenNameCandidateButtons;
    private FineActionsType displayedFineActionsType;

    // Data for merge
    private Button unmergeLastStrokeButton;
    private List<Button> mergeLastStrokesButtons;

    // Data for clear
    private Button removeLastTokenButton;
    private Button clearAllButton;

    // Data for force setting token name
    private View.OnClickListener forceSetTokenNameListener;
    private View.OnClickListener forceSetTokenNameInputDialogListener;
    private Button forceSetTokenNameInputDialogButton;

    /* Constructors */
    public FineActionsPanel(Activity activity, PlatoBoard platoBoard) {
        this.activity = activity;
        this.platoBoard = platoBoard;

        fineActionsPanel = (LinearLayout) activity.findViewById(R.id.FineActionsPanel);

        initDisplayedFineActionsType();
    }

    public void toggleMergeFineActions(final View.OnClickListener unmergeLastStrokeListener,
                                       final View.OnClickListener mergeStrokesAsTokenListener) {
        if (displayedFineActionsType == FineActionsType.None ||
            displayedFineActionsType == FineActionsType.Candidates) {
            fineActionsPanel.removeAllViews();

            mergeLastStrokesButtons = new ArrayList<Button>();

            /* New button: unmerge last stroke */
            unmergeLastStrokeButton = generateButton(activity.getString(R.string.ui_unmerge_button_caption), unmergeLastStrokeListener);
            fineActionsPanel.addView(unmergeLastStrokeButton);

            /* New buttons: merge last n strokes */
            for (int i = 0; i < numMergeLastStrokesButtons; ++i) {
                Button newMergeStrokesButton =
                        generateButton(String.format("%d " + activity.getString(R.string.ui_strokes), (i + 2)),
                                       mergeStrokesAsTokenListener);
                mergeLastStrokesButtons.add(newMergeStrokesButton);
                fineActionsPanel.addView(newMergeStrokesButton);
            }

            fineActionsPanel.setVisibility(View.VISIBLE);

            displayedFineActionsType = FineActionsType.Merge;
        } else {
            fineActionsPanel.removeView(unmergeLastStrokeButton);
            unmergeLastStrokeButton = null;

            for (int i = 0; i < mergeLastStrokesButtons.size(); ++i) {
                fineActionsPanel.removeView(mergeLastStrokesButtons.get(i));
            }
            mergeLastStrokesButtons = null;

            displayedFineActionsType = FineActionsType.None;

            if (tokenNameCandidateButtons == null) {
                fineActionsPanel.setVisibility(View.GONE);
            } else {
                showTokenNameCandidates();
            }
        }
    }

    private void initDisplayedFineActionsType() {
        displayedFineActionsType = FineActionsType.None;
    }

    public void toggleClearFineActions(final View.OnClickListener removeLastTokenListener,
                                       final View.OnClickListener clearAllListener) {
        if (displayedFineActionsType == FineActionsType.None ||
            displayedFineActionsType == FineActionsType.Candidates) {
            fineActionsPanel.removeAllViews();

            /* New button: Remove last stroke button */
            // TODO

             /* New button: Remove last token */
            removeLastTokenButton = generateButton(activity.getString(R.string.ui_last_token_button_caption), removeLastTokenListener);
            fineActionsPanel.addView(removeLastTokenButton);

            /* New button: Clear All button */
            clearAllButton = generateButton(activity.getString(R.string.ui_all_button_caption), clearAllListener);
            fineActionsPanel.addView(clearAllButton);

            fineActionsPanel.setVisibility(View.VISIBLE);

            displayedFineActionsType = FineActionsType.Clear;
        } else {
            fineActionsPanel.removeView(removeLastTokenButton);
            removeLastTokenButton = null;

            fineActionsPanel.removeView(clearAllButton);
            clearAllButton = null;

            displayedFineActionsType = FineActionsType.None;

            if (tokenNameCandidateButtons == null) {
                fineActionsPanel.setVisibility(View.GONE);
            } else {
                showTokenNameCandidates();
            }

        }
    }

    public void setTokenNameCandidates(final String[] candidates,
                                       final View.OnClickListener forceSetTokenNameListener,
                                       final View.OnClickListener forceSetTokenNameInputDialogListener) {
        tokenNameCandidates = candidates;

        this.forceSetTokenNameListener = forceSetTokenNameListener;
        this.forceSetTokenNameInputDialogListener = forceSetTokenNameInputDialogListener;

        showTokenNameCandidates();
    }

    private void showTokenNameCandidates() {
        fineActionsPanel.removeAllViews();

        tokenNameCandidateButtons = new ArrayList<Button>();

        for (int i = 1; i < tokenNameCandidates.length; ++i) {
            final String candTokenName = tokenNameCandidates[i];

            String candTokenDisplayName = candTokenName;
            try {
                TokenNameHelper tokenNameHelper = TokenNameHelper.getInstance();
                candTokenDisplayName = tokenNameHelper.tokenName2DisplayName(candTokenName);
            } catch (IllegalStateException exc) {}

            Button candButton = generateButton(candTokenDisplayName,
                                               forceSetTokenNameListener,
                                               75,
                                               Color.rgb(0, 0, 255),
                                               Color.rgb(255, 255, 0));

            tokenNameCandidateButtons.add(candButton);
            fineActionsPanel.addView(candButton);
        }

        forceSetTokenNameInputDialogButton = generateButton(activity.getString(R.string.ui_ellipsis),
                                                            forceSetTokenNameInputDialogListener,
                                                            75,
                                                            Color.rgb(0, 0, 255),
                                                            Color.rgb(255, 255, 0));
        fineActionsPanel.addView(forceSetTokenNameInputDialogButton);

        fineActionsPanel.setVisibility(View.VISIBLE);

        displayedFineActionsType = FineActionsType.Candidates;
    }

    public void clear() {
        if (fineActionsPanel.getVisibility() == View.VISIBLE){
            fineActionsPanel.removeAllViews();
            fineActionsPanel.setVisibility(View.GONE);

        }
    }

    public void hide() {
        fineActionsPanel.setVisibility(View.GONE);
    }

    public void unhide() {
        fineActionsPanel.setVisibility(View.VISIBLE);
    }

    private Button generateButton(final String text, final View.OnClickListener onClickListener) {
        return generateButton(text, onClickListener, 100,
                              Color.rgb(128, 128, 128), Color.rgb(255, 255, 255));
    }

    private Button generateButton(final String text,
                                  final View.OnClickListener onClickListener,
                                  final int buttonWidth,
                                  final int bgClr,
                                  final int textClr) {
        final float btnTextSize = 10.0f;
        final LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        Button btn = new Button(activity.getBaseContext());

        btn.setBackground(activity.getResources().getDrawable(R.drawable.button_bg_token_options)); // TODO

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(1, 1, 0, 1);

        btn.setText(text);
        btn.setTextSize(btnTextSize);
//        btn.setLayoutParams(layoutParams);
        btn.setOnClickListener(onClickListener);

        btn.setMinimumWidth(buttonWidth);
//        btn.setFadingEdgeLength(5);
//        btn.setPadding(5, 5, 3, 3);
        btn.setPadding(0, 0, 0, 0);

        btn.setWidth(buttonWidth);

//        btn.setBackgroundColor(bgClr);
        btn.setTextColor(textClr);

        btn.setLayoutParams(params);

        return btn;
    }

    /* Getters */
    public List<Button> getMergeLastStrokesButtons() {
        return mergeLastStrokesButtons;
    }
}
