package com.utbm.tr54.robot.thread;

import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

/**
 * The Class DistanceThread.
 */
public class DistanceThread extends Thread {
	
	/** The delay between each loop while the thread is running. */
	private static final int PERIOD = 10;
	
	/** The Constant COEF_A. */
	private static final int COEF_A = 200;
	
	/** The Constant COEF_D. */
	private static final float COEF_D = 0.1699f;
	
	/** The boolean representing if the thread is running or not. */
	private boolean isRunning = true;
	
	/** The m_buffer. */
	private float m_buffer;
	
	/** The m_provider. */
	private SampleProvider m_provider;
	
	/** The mutex used to synchronize the buffer I/O. */
	private Object mutex = new Object();
	
	/**
	 * Instantiates a new distance thread.
	 *
	 * @param provider the provider
	 */
	public DistanceThread(final SampleProvider provider) {
		this.m_provider = provider;
		this.isRunning = true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while(this.isRunning) {
			acquireSpeed();
			Delay.msDelay(PERIOD);
		}
	}
	
	/**
	 * Gets the speed.
	 *
	 * @return the speed the robot should use according to obstacles
	 */
	public float getSpeed() {
		float percentSpeed;
		
		synchronized (this.mutex) {
			percentSpeed = this.m_buffer;
		}
		
		return percentSpeed;
	}
	
	/**
	 * Acquire speed from the sensor.
	 */
	private void acquireSpeed() {
		synchronized (this.mutex) {
			float distance;
			float[] sample = new float[this.m_provider.sampleSize()];
			this.m_provider.fetchSample(sample, 0);
			distance = sample[0];
			this.m_buffer = (Math.max(Math.min(60, COEF_A*(distance-COEF_D)), 0))/100;
		}
	}
}
