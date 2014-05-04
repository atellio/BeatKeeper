package pntanasis.android.metronome;

import de.passsy.holocircularprogressbar.HoloCircularProgressBar;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class MetronomeActivity extends Activity {
	
	private final short minBpm = 40;
	private final short maxBpm = 300;
	
	private short bpm = 100;
	private short noteValue = 4;
	private short beats = 4;
	// private short volume;
	private short initialVolume;
	private double beatSound = 2440;
	private double sound = 6440;
	private boolean isStopped = true;
	private AudioManager audio;
    private MetronomeAsyncTask metroTask;
    
    private Button plusButton;
    private Button minusButton;
    private Button startStop;
    private SeekBar bpmSeekBar;
    private float currentBeatFloat;
    private TextView bpmText;
    
    private HoloCircularProgressBar holoCircularProgressBar;
    private ObjectAnimator progressBarAnimator;
    protected boolean animationHasEnded = false;
    
    private Handler mHandler;
    
    // have in mind that: http://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler
    // in this case we should be fine as no delayed messages are queued
	private Handler getHandler() {
		return new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String message = (String) msg.obj;
				if (!isStopped) {
					if (message.equals("1")) {
						startStop.setTextColor(Color.RED);
					} else {
						startStop.setTextColor(getResources().getColor(
								R.color.black));
					}
					startStop.setText(message);
					currentBeatFloat = (Float.parseFloat((String) message));
					
				}
			}
		};

	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        metroTask = new MetronomeAsyncTask();
        /* Set values and listeners to buttons and stuff */
        
        bpmText = (TextView) findViewById(R.id.bps);
        bpmText.setText(""+bpm);
        
        TextView timeSignatureText = (TextView) findViewById(R.id.timesignature);
        timeSignatureText.setText(""+beats+"/"+noteValue);
        
        startStop = (Button) findViewById(R.id.startstop);
        
        bpmSeekBar = (SeekBar) findViewById(R.id.bpmSeekBar);
        bpmSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        
        Spinner beatSpinner = (Spinner) findViewById(R.id.beatspinner);
        ArrayAdapter<Beats> arrayBeats =
        new ArrayAdapter<Beats>(this,
      	      android.R.layout.simple_spinner_item, Beats.values());
        beatSpinner.setAdapter(arrayBeats);
        beatSpinner.setSelection(Beats.four.ordinal());
        arrayBeats.setDropDownViewResource(R.layout.spinner_dropdown);
        beatSpinner.setOnItemSelectedListener(beatsSpinnerListener);
        
        Spinner noteValuesdSpinner = (Spinner) findViewById(R.id.notespinner);
        ArrayAdapter<NoteValues> noteValues =
        new ArrayAdapter<NoteValues>(this,
      	      android.R.layout.simple_spinner_item, NoteValues.values());
        noteValuesdSpinner.setAdapter(noteValues);
        noteValues.setDropDownViewResource(R.layout.spinner_dropdown);
        noteValuesdSpinner.setOnItemSelectedListener(noteValueSpinnerListener);
        
        holoCircularProgressBar = (HoloCircularProgressBar) findViewById(R.id.holoCircularProgressBar1);
        
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
    	initialVolume = (short) audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public synchronized void onStartStopClick(View view) {
    	// String buttonText = startStop.getText().toString();
    	if(isStopped) {
    		// startStop.setText(R.string.stop);
    		isStopped = false;
    		startStop.setBackgroundResource(Color.TRANSPARENT);
    		
    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    			metroTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
    			startAnimation();
    		}
    		else {
    			metroTask.execute(); 
    			startAnimation();
    		}
    	} else {
    		isStopped = true;
    		startStop.setBackgroundResource(R.drawable.ic_play);
    		metroTask.stop();
    		metroTask = new MetronomeAsyncTask();
    		Runtime.getRuntime().gc();
    		startStop.setText(R.string.start); 
    		startStop.setTextColor(Color.BLACK);
    		
    		startAnimation();  // if metronome is disabled, cancels animator
    		
    		if (progressBarAnimator != null) {
				progressBarAnimator.cancel();
			}
    		
			animate(holoCircularProgressBar, 0f, 1000, null); // on finish to reset marker position?
			holoCircularProgressBar.setMarkerProgress(0f); // on finish to reset marker position? wtf is 0f though?
    		
    	}
    }
    /*
    private void maxBpmGuard() {
        if(bpm >= maxBpm) {
        	plusButton.setEnabled(false);
        	plusButton.setPressed(false);
        } else if(!minusButton.isEnabled() && bpm>minBpm) {
        	minusButton.setEnabled(true);
        }    	
    }
    */
    
    
    private OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		 public void onProgressChanged(SeekBar seekBar, int progress,
		            boolean fromUser) {
		    	bpmText = (TextView) findViewById(R.id.bps);
		        short value = (short) (progress + 40);
		        bpmText.setText(""+value);
		        bpm = value;
		        metroTask.setBpm(value);
		    }
	};

	/*
    private void minBpmGuard() {
        if(bpm <= minBpm) {
        	minusButton.setEnabled(false);
        	minusButton.setPressed(false);
        } else if(!plusButton.isEnabled() && bpm<maxBpm) {
        	plusButton.setEnabled(true);
        }    	
    }
    */
	
    private OnItemSelectedListener beatsSpinnerListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			Beats beat = (Beats) arg0.getItemAtPosition(arg2);
			Log.d("beat value", beat.toString()); // logging beat numerator 
			TextView timeSignature = (TextView) findViewById(R.id.timesignature);
			timeSignature.setText(""+beat+"/"+noteValue);
			metroTask.setBeat(beat.getNum());
			beats = beat.getNum();
			Log.d("metroTask beat", String.valueOf(beat.getNum()));
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
    	
    };
    
    private OnItemSelectedListener noteValueSpinnerListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			NoteValues noteValue = (NoteValues) arg0.getItemAtPosition(arg2);
			TextView timeSignature = (TextView) findViewById(R.id.timesignature);
			timeSignature.setText(""+beats+"/"+noteValue);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
    	
    };
    
    public void onBackPressed() {
    	metroTask.stop();
//    	metroTask = new MetronomeAsyncTask();
    	Runtime.getRuntime().gc();
		audio.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    	finish();    
    }
    
    private class MetronomeAsyncTask extends AsyncTask<Void,Float,String> {
    	Metronome metronome;
    	
    	MetronomeAsyncTask() {
            mHandler = getHandler();
    		metronome = new Metronome(mHandler);
    	}

		protected String doInBackground(Void... params) {
			metronome.setBeat(beats);
			metronome.setNoteValue(noteValue);
			metronome.setBpm(bpm);
			metronome.setBeatSound(beatSound);
			metronome.setSound(sound);

			metronome.play();
			
			return null;		
		}
		

		
		public void stop() {
			metronome.stop();
			startStop.setText(R.string.start); 
    		startStop.setTextColor(Color.BLACK);
    		isStopped = true;
			metronome = null;
			currentBeatFloat = 1;  // reset the animation position?
		}
		
		public void setBpm(short bpm) {
			metronome.setBpm(bpm);
			metronome.calcSilence();
		}
		
		public void setBeat(int beat) {
			if(metronome != null)
				metronome.setBeat(beat);
		}
    	
    }

    private void animate(final HoloCircularProgressBar progressBar, final AnimatorListener listener) {
		final float progress = calculateBarProgress();
		int duration = 150;
		animate(progressBar, progress, duration, listener);
	}
    
    private void animate(final HoloCircularProgressBar progressBar,
			final float progress, final int duration, final AnimatorListener listener) {

		progressBarAnimator = ObjectAnimator.ofFloat(progressBar, "progress", progress);
		progressBarAnimator.setDuration(duration);
		
		progressBarAnimator.addListener(new AnimatorListener() {

			@Override
			public void onAnimationCancel(final Animator animation) {
			
			}

			@Override
			public void onAnimationEnd(final Animator animation) {
				progressBar.setProgress(progress);
			}

			@Override
			public void onAnimationRepeat(final Animator animation) {
				
				
			}

			@Override
			public void onAnimationStart(final Animator animation) {
			}
		});
		if (listener != null) {
			progressBarAnimator.addListener(listener);
		}
		progressBarAnimator.reverse();
		progressBarAnimator.addUpdateListener(new AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(final ValueAnimator animation) {
				progressBar.setProgress((Float) animation.getAnimatedValue());
			}
		});
		progressBar.setMarkerProgress(progress);
		progressBarAnimator.start();
		
		
	}
    
    private float calculateBarProgress() {
		
    	float barProgress = currentBeatFloat / beats;

    	return barProgress;
    }
    

	private void startAnimation() {

		if (!isStopped) {
			animate(holoCircularProgressBar, new AnimatorListener() {

				@Override
				public void onAnimationCancel(final Animator animation) {
					animation.end();
				}

				@Override
				public void onAnimationEnd(final Animator animation) {
					if (!animationHasEnded) {
						animate(holoCircularProgressBar, this);
					} else {
						animationHasEnded = false;
					}
				}

				@Override
				public void onAnimationRepeat(final Animator animation) {
				}

				@Override
				public void onAnimationStart(final Animator animation) {
				}

			});
		} else {
			animationHasEnded = true;
			progressBarAnimator.cancel();
		}
	}
    
    
}