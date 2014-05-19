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
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class MetronomeActivity extends Activity {

	private short tempo = 100;
	private short noteValue = 4;
	private short beats = 4;
	private short initialVolume;
	private double beatSound = 2440;
	private double sound = 6440;
	private boolean isStopped = true;
	private AudioManager audio;
	private MetronomeAsyncTask metroTask;

	private ImageButton startStop;
	private SeekBar bpmSeekBar;
	private SeekBar timeSignatureSeekBar;
	private float currentBeatFloat;
	private TextView tempoTextView;
	private TextView timeSignature;

	private HoloCircularProgressBar holoCircularProgressBar;
	private ObjectAnimator progressBarAnimator;
	protected boolean animationHasEnded = false;

	private Handler mHandler;

	// receives current beat from Metronome.java
	// have in mind that:
	// http://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler
	// in this case we should be fine as no delayed messages are queued
	private Handler getHandler() {
		return new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String message = (String) msg.obj;
				if (!isStopped) {

					timeSignature.setText(message + "/" + noteValue);
					currentBeatFloat = (Float.parseFloat((String) message));

				}
			}
		};

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		metroTask = new MetronomeAsyncTask();
		
		tempoTextView = (TextView) findViewById(R.id.tempo);
		tempoTextView.setText("" + tempo);

		timeSignature = (TextView) findViewById(R.id.timesignature);
		timeSignature.setText("1" + "/" + noteValue);

		startStop = (ImageButton) findViewById(R.id.startstop);

		bpmSeekBar = (SeekBar) findViewById(R.id.bpmSeekBar);
		bpmSeekBar.setOnSeekBarChangeListener(tempoSeekBarChangeListener);
		
		timeSignatureSeekBar = (SeekBar) findViewById(R.id.timeSignatureSeekBar);
		timeSignatureSeekBar.setOnSeekBarChangeListener(beatSeekBarChangeListener);

		holoCircularProgressBar = (HoloCircularProgressBar) findViewById(R.id.holoCircularProgressBar1);

		audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		initialVolume = (short) audio
				.getStreamVolume(AudioManager.STREAM_MUSIC);
	}

	// When the play/pause button is pressed, start or stop the metronome
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public synchronized void onStartStopClick(View view) {

		if (isStopped) {
			isStopped = false;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				metroTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
						(Void[]) null);
				startOrStopAnimation();
			} else {
				metroTask.execute();
				startOrStopAnimation();
			}
		} else {
			isStopped = true;
			metroTask.stop();
			metroTask = new MetronomeAsyncTask();
			Runtime.getRuntime().gc();
			timeSignature.setText("1" + "/" + noteValue);

			// if metronome is disabled, cancels animator
			startOrStopAnimation(); 

			if (progressBarAnimator != null) {
				progressBarAnimator.cancel();
			}

			animate(holoCircularProgressBar, 0f, 1000, null); 
			
			holoCircularProgressBar.setMarkerProgress(0f); 
		}
	}

	// seekbar defines tempo (beats per minute)
	private OnSeekBarChangeListener tempoSeekBarChangeListener = new OnSeekBarChangeListener() {

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
			
			// tempo text positioned inside holo circular progress bar
			tempoTextView = (TextView) findViewById(R.id.tempo);
			// the "+ 40" is to ensure that the bpm doesn't go below 40bpm.
			short value = (short) (progress + 40);
			tempoTextView.setText("" + value);
			tempo = value;
			// set bpm value to the metronome async task
			metroTask.setBpm(value);
		}
	};

	// choose time signature numerator (e.g. 3/4, 4/4, etc)
	private OnSeekBarChangeListener beatSeekBarChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			Log.d("beat value", Integer.toString(progress)); // logging beat numerator
			TextView timeSignature = (TextView) findViewById(R.id.timesignature);
			timeSignature.setText("" + Integer.toString(progress) + "/" + noteValue);
			metroTask.setBeat(progress);
			beats = (short) progress;
			Log.d("metroTask beat", Integer.toString(progress));
		}
	};

	// for implementing option for denominator. 
	/*
	private OnItemSelectedListener noteValueSpinnerListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			NoteValues noteValue = (NoteValues) arg0.getItemAtPosition(arg2);
			TextView timeSignature = (TextView) findViewById(R.id.timesignature);
			timeSignature.setText("" + beats + "/" + noteValue);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
		}
	};
	*/
	
	public void onBackPressed() {
		metroTask.stop();
		// metroTask = new MetronomeAsyncTask();
		Runtime.getRuntime().gc();
		audio.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume,
				AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
		finish();
	}

	private class MetronomeAsyncTask extends AsyncTask<Void, Float, String> {
		Metronome metronome;

		MetronomeAsyncTask() {
			mHandler = getHandler();
			metronome = new Metronome(mHandler);
		}

		protected String doInBackground(Void... params) {
			metronome.setBeat(beats);
			metronome.setNoteValue(noteValue);
			metronome.setBpm(tempo);
			metronome.setBeatSound(beatSound);
			metronome.setSound(sound);

			metronome.play();

			return null;
		}

		public void stop() {
			metronome.stop();
			isStopped = true;
			metronome = null;
			currentBeatFloat = 1; 
		}

		public void setBpm(short bpm) {
			metronome.setBpm(bpm);
			metronome.calcSilence();
		}

		public void setBeat(int beat) {
			if (metronome != null)
				metronome.setBeat(beat);
		}

	}

	private void animate(final HoloCircularProgressBar progressBar,
			final AnimatorListener listener) {
		final float progress = calculateBarProgress();
		int duration = 100; // speed of animation
		animate(progressBar, progress, duration, listener);
	}

	private void animate(final HoloCircularProgressBar progressBar,
			final float progress, final int duration,
			final AnimatorListener listener) {

		progressBarAnimator = ObjectAnimator.ofFloat(progressBar, "progress",
				progress);
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

	private void startOrStopAnimation() {

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