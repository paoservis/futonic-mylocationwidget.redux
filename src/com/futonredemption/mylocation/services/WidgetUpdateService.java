package com.futonredemption.mylocation.services;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.beryl.app.AbstractService;
import org.beryl.diagnostics.Logger;

import com.futonredemption.mylocation.MyLocationBundle;
import com.futonredemption.mylocation.tasks.RetrieveAddressTask;
import com.futonredemption.mylocation.tasks.RetrieveLocationTask;
import com.futonredemption.mylocation.tasks.UpdateWidgetsTask;

import android.content.Intent;
import android.location.Location;

public class WidgetUpdateService extends AbstractService {

	@Override
	protected int handleOnStartCommand(Intent intent, int flags, int startId) {
		final String method = intent.getStringExtra("method");
		
		if(method.equalsIgnoreCase("FullUpdate")) {
			beginFullUpdate();
		} else {
			setRequestCompleted();
		}

		/*
		try {
			MyLocationBundle bundle = futureBundle.get();
			Logger.d(bundle.toString());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		return 0;
	}

	private void beginFullUpdate() {
		Thread.currentThread().setName("WidgetUpdateService");
		Logger.w("Starting WidgetUpdateService");
		final ExecutorService service = Executors.newSingleThreadExecutor();
		Future<MyLocationBundle> futureLocation;
		
		RetrieveLocationTask locationGet = new RetrieveLocationTask(this);
		futureLocation = service.submit(locationGet);
		
		UpdateWidgetsTask widgetUpdateTask = new UpdateWidgetsTask(this, futureLocation);
		futureLocation = service.submit(widgetUpdateTask);
		
		RetrieveAddressTask addressGet = new RetrieveAddressTask(this, futureLocation);
		futureLocation = service.submit(addressGet);
		
		widgetUpdateTask = new UpdateWidgetsTask(this, futureLocation);
		futureLocation = service.submit(widgetUpdateTask);
	}
}
