package com.futonredemption.mylocation.tasks;

import com.futonredemption.mylocation.Debugging;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public abstract class EventBasedContextAwareCallable<V> extends ContextAwareCallable<V> {

	V result = null;
	Looper looper = null;
	Handler handler = null;
	Exception error = null;
	
	public EventBasedContextAwareCallable(Context context) {
		super(context);
	}

	protected Handler createHandler() {
		return new Handler(looper);
	}
	
	public final V call() throws Exception {
		Thread.currentThread().setName(this.getClass().getSimpleName());
		Looper.prepare();
		looper = Looper.myLooper();
		handler = new Handler(looper);
		
		try {
			onBeginTask();
			Looper.loop();
		} catch(Exception e) {
			finishWithError(e);
			Debugging.e(e);
		} finally {
			onFinishTask();
			looper.quit();
		}
		
		return this.result;
	}

	protected abstract void onBeginTask();
	protected abstract void onFinishTask();
	
	public void finish() {
		looper.quit();
	}
	
	public void finishWithResult(V result) {
		this.result = result;
		looper.quit();
	}
	
	public void finishWithError(Exception error) {
		this.result = null;
		this.error = error;
		looper.quit();
	}
}
