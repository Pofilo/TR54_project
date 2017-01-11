package com.utbm.tr54.robot.thread;
import com.utbm.tr54.robot.ColorRobot;

import lejos.robotics.SampleProvider;
import lejos.utility.Delay;


/**
 * The Class ColorSensorThread poll the color sensor in his own thread.
 */
public class ColorSensorThread extends Thread{
	
	/** The buffer containing the current color. */
	private ColorRobot m_buffer;
	
	/** The sample provider. */
	private SampleProvider m_provider;
	
	/** The mutex used to synchronize the buffer I/O. */
	private Object mutex = new Object();

	/** The boolean defining whether the thread is active. */
	private boolean isRunning = true;

	/**
	 * Instantiates a new color sensor thread.
	 *
	 * @param provider the sample provider
	 */
	public ColorSensorThread(final SampleProvider provider) {
		this.m_provider = provider;
		this.m_buffer = new ColorRobot(0, 0, 0);
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while(this.isRunning) {
			acquireData();
			Delay.msDelay(10);
		}
	}

	/**
	 * Checks if the buffer is empty.
	 *
	 * @return true, if successful
	 */
	public boolean isEmpty() {
		return this.m_buffer == null;
	}

	/**
	 * Gets the buffer content.
	 * Doesn't actually empty the buffer
	 *
	 * @return the color currently seen by the sensor
	 */
	public ColorRobot getData()	{
		ColorRobot color = null;
		synchronized (this.mutex) {
			color = this.m_buffer;
		}

		return color;
		
	}

	/**
	 * Acquire data from the sensor.
	 */
	private void acquireData() {
		synchronized (this.mutex) {
			ColorRobot color = new ColorRobot();

			float[] samples = new float[this.m_provider.sampleSize()];
			this.m_provider.fetchSample(samples, 0);

			color.r = samples[0];
			color.g = samples[1];
			color.b = samples[2];

			this.m_buffer = color;
		}
	}
}
