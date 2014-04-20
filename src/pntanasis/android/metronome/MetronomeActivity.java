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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class MetronomeActivity extends Activity {
	
	private final short minBpm = 40;
	private final short maxBpm = 208;
	
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
    private TextView currentBeat;
    private HoloCircularProgressBar holoCircularProgressBar;
    private ObjectAnimator progressBarAnimator;
    protected boolean animationhasEnded = false;
    
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
						currentBeat.setTextColor(Color.RED);
						startStop.setTextColor(Color.RED);
						Log.d("message beat", String.valueOf(message));
					} else {
						currentBeat.setTextColor(getResources().getColor(
								R.color.black));
						startStop.setTextColor(getResources().getColor(
								R.color.black));
					}
					currentBeat.setText(message);
					startStop.setText(message);
					
					animateAdapter();
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
        
        TextView bpmText = (TextView) findViewById(R.id.bps);
        bpmText.setText(""+bpm);
        
        TextView timeSignatureText = (TextView) findViewById(R.id.timesignature);
        timeSignatureText.setText(""+beats+"/"+noteValue);
        
        plusButton = (Button) findViewById(R.id.plus);
        plusButton.setOnLongClickListener(plusListener);
        
        minusButton = (Button) findViewById(R.id.minus);
        minusButton.setOnLongClickListener(minusListener);
        
        currentBeat = (TextView) findViewById(R.id.currentBeat);
        currentBeat.setTextColor(Color.RED);
        
        startStop = (Button) findViewById(R.id.startstop);
        
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
    	String buttonText = startStop.getText().toString();
    	if(buttonText.equalsIgnoreCase("start")) {
    		// startStop.setText(R.string.stop);
    		isStopped = false;
    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    			metroTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
    			startAnimation();
    		}
    		else {
    			metroTask.execute(); 
    			startAnimation();
    		}
    	} else {
    		metroTask.stop();
    		metroTask = new MetronomeAsyncTask();
    		Runtime.getRuntime().gc();
    		startStop.setText(R.string.start); 
    		startStop.setTextColor(Color.BLACK);
    		
    		animationhasEnded = true;
			progressBarAnimator.cancel();
    	}
    }
    
    private void maxBpmGuard() {
        if(bpm >= maxBpm) {
        	plusButton.setEnabled(false);
        	plusButton.setPressed(false);
        } else if(!minusButton.isEnabled() && bpm>minBpm) {
        	minusButton.setEnabled(true);
        }    	
    }
    
    public void onPlusClick(View view) {
    	bpm++;
    	TextView bpmText = (TextView) findViewById(R.id.bps);
        bpmText.setText(""+bpm);
        metroTask.setBpm(bpm);
        maxBpmGuard();
    }
    
    private OnLongClickListener plusListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			// TODO Auto-generated method stub
			bpm+=20;
			if(bpm >= maxBpm)
				bpm = maxBpm;
	    	TextView bpmText = (TextView) findViewById(R.id.bps);
	        bpmText.setText(""+bpm);
	        metroTask.setBpm(bpm);
	        maxBpmGuard();
			return true;
		}
    	
    };
    
    private void minBpmGuard() {
        if(bpm <= minBpm) {
        	minusButton.setEnabled(false);
        	minusButton.setPressed(false);
        } else if(!plusButton.isEnabled() && bpm<maxBpm) {
        	plusButton.setEnabled(true);
        }    	
    }
    
    public void onMinusClick(View view) {
    	bpm--;
    	TextView bpmText = (TextView) findViewById(R.id.bps);
        bpmText.setText(""+bpm);
        metroTask.setBpm(bpm);
        minBpmGuard();
    }
    
    private OnLongClickListener minusListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			// TODO Auto-generated method stub
			bpm-=20;
			if(bpm <= minBpm)
				bpm = minBpm;
	    	TextView bpmText = (TextView) findViewById(R.id.bps);
	        bpmText.setText(""+bpm);
	        metroTask.setBpm(bpm);
	        minBpmGuard();
			return true;
		}
    	
    };
    
    
    private OnItemSelectedListener beatsSpinnerListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
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
			// TODO Auto-generated method stub
			NoteValues noteValue = (NoteValues) arg0.getItemAtPosition(arg2);
			TextView timeSignature = (TextView) findViewById(R.id.timesignature);
			timeSignature.setText(""+beats+"/"+noteValue);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
    	
    };

  //  @Override
   // public boolean onKeyUp(int keycode, KeyEvent e) {
    	//SeekBar volumebar = (SeekBar) findViewById(R.id.volumebar);
    	//volume = (short) audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        //switch(keycode) {
        //    case KeyEvent.KEYCODE_VOLUME_UP:
        //    case KeyEvent.KEYCODE_VOLUME_DOWN: 
        //        volumebar.setProgress(volume);
       //     	break;                
    //    }

     //   return super.onKeyUp(keycode, e);
 //   }
    
    public void onBackPressed() {
    	metroTask.stop();
//    	metroTask = new MetronomeAsyncTask();
    	Runtime.getRuntime().gc();
		audio.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    	finish();    
    }
    
    private class MetronomeAsyncTask extends AsyncTask<Void,Void,String> {
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
		int duration = beats;
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
		
    	float barProgress = (Float.parseFloat((String) currentBeat.getText()) / beats);
    	
    	//for testing purposes
    	/*
    	String a = Float.toString(barProgress);
    	String b = Short.toString(noteValue);
    	String c = (String) currentBeat.getText();
    	
    	Log.d("notevalue", b);
    	Log.d("barProgress", a);
    	Log.d("currentbeat", c);
    	*/
    	
    	return barProgress;
    }
    
    private void animateAdapter() {
    	animate(holoCircularProgressBar, calculateBarProgress(), beats, new AnimatorListener() {

			@Override
			public void onAnimationCancel(final Animator animation) {
				animation.end();
			}

			@Override
			public void onAnimationEnd(final Animator animation) {
				if (!animationhasEnded) {
					animate(holoCircularProgressBar, this);
				} else {
					animationhasEnded = false;
				}
			}

			@Override
			public void onAnimationRepeat(final Animator animation) {
			}

			@Override
			public void onAnimationStart(final Animator animation) {
			}
		});
    	
    	
    }
    
    private void startAnimation() {
    	animate(holoCircularProgressBar, new AnimatorListener() {

			@Override
			public void onAnimationCancel(final Animator animation) {
				animation.end();
			}

			@Override
			public void onAnimationEnd(final Animator animation) {
				if (!animationhasEnded) {
					animate(holoCircularProgressBar, this);
				} else {
					animationhasEnded = false;
				}
			}

			@Override
			public void onAnimationRepeat(final Animator animation) {
			}

			@Override
			public void onAnimationStart(final Animator animation) {
			}
		});
    }
    
    
}