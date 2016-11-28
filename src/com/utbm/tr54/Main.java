package com.utbm.tr54;

import com.utbm.tr54.robot.AbstractRobot;

import lejos.hardware.Button;

public class Main {

	public static void main(String[] args) {
		System.out.println("UP  : Server");
		System.out.println("ANY : Client");
		
		AbstractRobot robot = null;

		do {
			final int button = Button.waitForAnyPress();

			if (button == Button.ID_UP) {
				// robot = new IARobot(true)
				// launch thread Server
			} else if (button == Button.ID_RIGHT || button == Button.ID_LEFT || button == Button.ID_DOWN) {
				// robot = new IARobot(false)
			}
		} while (robot == null);
		
		robot.LaunchIA();
	}
}
