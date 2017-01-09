package com.utbm.tr54.robot.thread;

import lejos.hardware.Button;

public class ShutdownThread extends Thread {

	public ShutdownThread() {
	}
	
	public void run() {
		// Switch off the LED
		ClientThread.getInstance().setRunning(false);
		Button.LEDPattern(0);
	}
	
}
