package com.utbm.tr54.robot.thread;

import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class DistanceThread extends Thread {
	
	private static final int PERIOD = 10;
	private static final int COEF_A = 200;
	private static final float COEF_D = 0.1699f;
	
	private boolean isRunning;
	private float m_buffer;
	
	private SampleProvider m_provider;
	private Object mutex = new Object();
	
	public DistanceThread(SampleProvider provider) {
		this.m_provider = provider;
		this.isRunning = true;
	}
	
	public void run() {
		while(this.isRunning) {
			acquireSpeed();
			Delay.msDelay(PERIOD);
		}
	}

	public void setRunning(final boolean isRunning) {
		this.isRunning = isRunning;
	}
	
	public float getSpeed() {
		float percentSpeed;
		
		synchronized (mutex) {
			percentSpeed = m_buffer;
		}
		
		return percentSpeed;
	}
	
	public void acquireSpeed() {
		synchronized (this.mutex) {
			float distance;
			float[] sample = new float[this.m_provider.sampleSize()];
			this.m_provider.fetchSample(sample, 0);
			distance = sample[0];
			m_buffer = (Math.max(Math.min(60, COEF_A*(distance-COEF_D)), 0))/100;
		}
	}
}
