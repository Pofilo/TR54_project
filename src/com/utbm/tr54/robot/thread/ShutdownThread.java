package com.utbm.tr54.robot.thread;

import lejos.hardware.Button;

public class ShutdownThread extends Thread {

	public ShutdownThread() {
		// TODO Auto-generated constructor stub
	}
	
	public void run() {
		// Switch off the LED
		Button.LEDPattern(0);
	}
}
