package com.utbm.tr54.robot.thread;

import lejos.hardware.Button;

/**
 * The Class ShutdownThread.
 */
public class ShutdownThread extends Thread {

	/**
	 * Instantiates a new shutdown thread.
	 */
	public ShutdownThread() {
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		// Switch off the LED
		ClientThread.getInstance().setRunning(false);
		Button.LEDPattern(0);
	}
	
}
