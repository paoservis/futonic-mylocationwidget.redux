package com.futonredemption.mylocation.tasks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.beryl.location.LocationMonitor;
import org.beryl.location.ProviderSelectors;

import com.futonredemption.mylocation.Debugging;
import com.futonredemption.mylocation.MyLocationRetrievalState;
import com.futonredemption.mylocation.exceptions.CannotObtainAccurateFixException;
import com.futonredemption.mylocation.exceptions.NoLocationProvidersEnabledException;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;

public class RetrieveLocationTask extends EventBasedContextAwareCallable<MyLocationRetrievalState> {

	private static final int TIMEOUT_PERIOD = 60000;
	
	final Future<MyLocationRetrievalState> future;
	public RetrieveLocationTask(Context context, Future<MyLocationRetrievalState> future) {
		super(context);
		this.future = future;
	}

	LocationMonitor monitor = null;
	BestLocationListener locationListener = null;
	
	private void startTimeoutWatchdog() {
		final Handler handler = createHandler();
		handler.postDelayed(AutoCancelMethod, TIMEOUT_PERIOD);
	}
	
	private void setupLocationMonitor() {
		monitor = new LocationMonitor(context);
		monitor.setProviderSelector(ProviderSelectors.AllFree);
		locationListener = new BestLocationListener(monitor);
		monitor.addListener(locationListener);
	}
	
	@Override
	protected void onBeginTask() {
		try {
			final MyLocationRetrievalState state = future.get();
			this.result = state;
			
			// Don't run the location retrieval if the location is already there.
			if(this.result != null && this.result.hasLocation()) {
				finishWithResult(this.result);
				return;
			}
			
			startTimeoutWatchdog();
			setupLocationMonitor();

			if(monitor.isAnyProviderEnabled()) {
				monitor.startListening(2000, 0);
			} else {
				finishWithError(new NoLocationProvidersEnabledException());
			}
		} catch (InterruptedException e) {
			finishWithError(e);
		} catch (ExecutionException e) {
			finishWithError(e);
		}
	};

	@Override
	protected void onFinishTask() {
		if(monitor != null) {
			monitor.stopListening();
		}
		
		if(result == null) {
			result = new MyLocationRetrievalState();
		}
		
		if(locationListener != null) {
			Location location = locationListener.getLocation();
			result.setLocation(location);
			result.setStateLocationIndicator(locationListener.isStaleLocation());
		}
		
		// If we got some fix before the timeout but it wasn't accurate enough then still continue.
		if(result.isError(CannotObtainAccurateFixException.class) && result.hasLocation()) {
			this.error = null;
		}
		
		result.setError(this.error);
	}

	private Runnable AutoCancelMethod = new Runnable() {
		public void run() {
			finishWithError(new CannotObtainAccurateFixException());
		}
	};
	
	class BestLocationListener implements LocationListener {

		private final float DESIRED_ACCURACY = 50.0f;
		private Location baselineLocation = null;
		private Location bestLocation = null;
		
		private final Object lock = new Object();
		
		public BestLocationListener(LocationMonitor monitor) {
			this.baselineLocation = monitor.getBestStaleLocation();
		}

		public boolean isStaleLocation() {
			final Location location = getLocation();
			if(location == baselineLocation) {
				return true;
			} else {
				return false;
			}
		}
		
		public Location getLocation() {
			Location location;
			
			synchronized(lock) {
				if(bestLocation != null) {
					location = bestLocation;
				} else {
					location = baselineLocation;
				}
			}
			
			return location;
		}
		
		public void onLocationChanged(final Location location) {
			Debugging.w("onLocationChanged");
			synchronized(lock) {
				if(bestLocation == null || bestLocation.getAccuracy() > location.getAccuracy()) {
					bestLocation = location;
				}
			}
			
			Debugging.haltForAWhile();
			if(location.getAccuracy() <= DESIRED_ACCURACY) {
				finishWithResult(result);
			}
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}
}
