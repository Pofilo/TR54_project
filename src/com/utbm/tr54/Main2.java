package com.utbm.tr54;

import com.utbm.tr54.robot.AbstractRobot;
import com.utbm.tr54.robot.RobotIA;
import com.utbm.tr54.robot.thread.ClientThread;
import com.utbm.tr54.robot.thread.ServerThread;
import com.utbm.tr54.robot.thread.ShutdownThread;

import lejos.hardware.Button;

public class Main2 {

	public static void main(String[] args) {
		System.out.println("UP  : Server");
		System.out.println("ANY : Client");
		
		AbstractRobot robot = null;
		ServerThread server = null;
		
		do {
			final int button = Button.waitForAnyPress();

			// We are the server
			if (button == Button.ID_UP) {
				server = new ServerThread();
			}
			
			// If a valid arrow is pressed, we create the robot IA
			if (button == Button.ID_UP || button == Button.ID_RIGHT || button == Button.ID_LEFT || button == Button.ID_DOWN) {
				robot = new RobotIA();
			}
		} while (robot == null);
		
		if(server != null){
			server.start();
		}
		
		ClientThread.getInstance().init((RobotIA)robot);
		ClientThread.getInstance().start();
		
		ShutdownThread shutdown = new ShutdownThread();
		Runtime.getRuntime().addShutdownHook(shutdown);
		
		robot.launchIA();
	}
}
