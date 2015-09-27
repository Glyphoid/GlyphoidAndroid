package me.scai.plato.android.views;

import me.scai.plato.android.utils.EventBus;
import me.scai.plato.android.R;
import me.scai.plato.android.helpers.FragmentHelper;
import me.scai.plato.android.events.ForceSetTokenNameEvent;

import java.util.Arrays;

import android.app.Activity;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.Button;

import com.google.gson.Gson;
import com.squareup.otto.Bus;

public class LetterSetView extends Fragment {
    /* Enum types */
    public enum LetterSetViewMode {
        None,
        SaveToken,
        ForceSetTokenName
    }

	/*======== Member variables ========*/
	private String [] letters;
	private String [] letterDisplayNames;
	
	private ObservableScrollView obsScrollView;
	private LinearLayout vertLayout;
	private LinearLayout [] horizLayouts;
	
	private int lastScrollY = 0;
	
	private final static Gson gson = new Gson();
	private TokenSettings tokenSettings;

    private LetterSetViewMode mode;
	
	OnSelectSavedTokenListener selectedSavedTokenCallback;

	/*======== Interfaces for communication with main Activity ========*/
	public interface OnSelectSavedTokenListener {
		public void onSelectSavedToken(String tokenName);
	}
	
	/*======== Methods ========*/
	/* Constructor */
	public LetterSetView(String [] tLetters, String [] tLetterDisplayNames) {
		super();
		
		/* Set the letters (tokens) and their display names */
		setLetters(tLetters, tLetterDisplayNames);

        setMode(LetterSetViewMode.None);
	}
	
	/*====== Implementations of interface methods ======*/
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
			selectedSavedTokenCallback = (OnSelectSavedTokenListener) activity;
		} catch ( ClassCastException e ) {
			throw new RuntimeException("Parent activity of LetterSetView does not implement OnSelectSavedTokenListener");
		}
	}
	
	/* Listener for ScrollView scrolling */
	class LetterViewScollListener implements ScrollViewListener {
		ObservableScrollView scrollView;
		public LetterViewScollListener(ObservableScrollView tScrollView) {
			scrollView = tScrollView;
		}
		
		@Override
		public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
			/* Save the scroll status */
			lastScrollY = y; 
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, 
			                 ViewGroup container, 
			                 Bundle savedInstanceState) {
		obsScrollView = (ObservableScrollView) inflater.inflate(R.layout.letter_set_fragment, container, false);

		ScrollViewListener svListener = new LetterViewScollListener(obsScrollView);
		obsScrollView.setScrollViewListener(svListener);
		
		setLetters();
		
		return obsScrollView;
		
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		obsScrollView.post(new Runnable() {
			@Override
			public void run() {
				obsScrollView.scrollTo(obsScrollView.getScrollX(), lastScrollY);
			}
		});
	}

    @Override
    public void onStop() {
        FragmentHelper.postBackToMainViewEvent();

        super.onStop();
    }
	
	/* Methods for receiving information */
	public void setLetters(String [] tLetters, String [] tLetterDisplayNames) {
		final int nButtonsPerRow = 5;
		
		letters = tLetters;
		letterDisplayNames = tLetterDisplayNames;
				
		if ( obsScrollView != null && letters != null ) {
			Context ctx = this.getActivity().getApplicationContext();
			
			vertLayout = (LinearLayout) obsScrollView.findViewById(R.id.vertLayout);
			horizLayouts = new LinearLayout[letters.length / nButtonsPerRow + 1];
			
			int rowNum = 0;
			for (int i = 0; i < letters.length; ++i) {
				rowNum = i / nButtonsPerRow;

				if ( horizLayouts[rowNum] == null ) {
					horizLayouts[rowNum] = new LinearLayout(ctx);
					horizLayouts[rowNum].setOrientation(LinearLayout.HORIZONTAL);
					vertLayout.addView(horizLayouts[rowNum]);
				}
				
				Button b = new Button(ctx);

				b.setText(letterDisplayNames[i]);
				
				//b.setWidth(18);
		    	//b.setHeight(10);
		    	b.setTextSize(9.0f);
		    	
				b.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Button t_button = (Button) v;

						int idx = Arrays.asList(letterDisplayNames).indexOf(t_button.getText().toString());
						String letterName = letters[idx];

                        if (mode.equals(LetterSetViewMode.SaveToken)) {
                            selectedSavedTokenCallback.onSelectSavedToken(letterName);
                        } else if (mode.equals(LetterSetViewMode.ForceSetTokenName)) {
                            Bus eventBus = EventBus.getInstance();
                            eventBus.post(new ForceSetTokenNameEvent(letterName));
                        }
					}
				});
				
				horizLayouts[rowNum].addView(b);
				
				ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams) b.getLayoutParams();
				layoutParams.height = 54;
				layoutParams.width = 88;
				b.setLayoutParams(layoutParams);
			}
			
		}

	}
	
	private void setLetters() {
		if ( letters != null && letterDisplayNames != null ) {
			setLetters(letters, letterDisplayNames);
		}
	}

    public LetterSetViewMode getMode() {
        return mode;
    }

    public void setMode(LetterSetViewMode mode) {
        this.mode = mode;
    }
}
