package com.utbm.tr54.robot;

import javax.swing.plaf.synth.SynthSeparatorUI;

import com.utbm.tr54.robot.thread.ClientThread;
import com.utbm.tr54.robot.thread.ColorSensorThread;
import com.utbm.tr54.robot.thread.DistanceThread;

import lejos.hardware.Button;
import lejos.utility.Delay;
import lejos.utility.Stopwatch;

public class RobotIA extends AbstractRobot {

	float speed = 1f;
	float weakSpeed = 0.18f * speed;
	float strongSpeed = 0.4f * speed;
	float blueSpeed = 0.6f * speed;
	float position = 100;

	private enum State {
		BLACK, BLUE, WHITE, ORANGE
	};

	private boolean dangerZone = false;
	private boolean sawFirstOrange = false;
	private static final int PERIOD = 50;

	private ColorSensorThread colorProvider;
	private DistanceThread distanceProvider;

	public RobotIA() {
		super();

		assert(weakSpeed <= 1.f);
		assert(strongSpeed <= 1.f);
		assert(blueSpeed <= 1.f);

		System.out.println("activate sensor");
		this.activateColorSensor();
		this.activateUltrasoundSensor();
		System.out.println("create thread");
		colorProvider = new ColorSensorThread(m_colorSampleProvider);
		distanceProvider = new DistanceThread(m_distanceSampleProvider);
	}

	@Override
	public void launchIA() {
		System.out.println("run thread");
		colorProvider.start();
		distanceProvider.start();
		System.out.println("begin");

		State currentState = State.BLACK;
		ColorRobot lastSeenColor = new ColorRobot(0, 0, 0);
		Stopwatch orangeSw = new Stopwatch();

		while (true) {
			updateSpeed();
			updatePosition();
			
			

			// we check if we are in the danger zone and if we can advance or
			// not
			if (dangerZone && !ClientThread.getInstance().isCanAdvance()) {
				this.stop();
				Button.LEDPattern(5);
				Delay.msDelay(PERIOD);
				continue;
			}
			
			if (!colorProvider.isEmpty()) {
				lastSeenColor = colorProvider.getData();
			}

			if (lastSeenColor.isBlack()) {
				currentState = State.BLACK;
			} else if (lastSeenColor.isBlue()) {
				currentState = State.BLUE;
			} else if (lastSeenColor.isWhite()) {
				currentState = State.WHITE;
			} else if (lastSeenColor.isOrange()) {
				this.position = 0; // premiere bande
				this.m_motorLeft.resetTachoCount();
				
				
				
				
				
				if (orangeSw.elapsed() > 2000) {
					this.position = 0;
					this.m_motorLeft.resetTachoCount();

					sawFirstOrange = !sawFirstOrange;
					orangeSw.reset();
				} else {
					orangeSw.reset();
				}

			}

			switch (currentState) {
			case BLACK: {
				this.setSpeedLeft(this.weakSpeed);
				this.setSpeedRight(this.strongSpeed);
				this.forward();
				break;
			}
			case BLUE: {
				this.setSpeed(this.blueSpeed);
				this.forward();
				break;
			}
			case WHITE: {
				this.setSpeedLeft(this.strongSpeed);
				this.setSpeedRight(this.weakSpeed);
				this.forward();
				break;
			}
			default:
				break;
			}
			Delay.msDelay(5);
		}
	}

	private void updateSpeed() {
		this.speed = this.distanceProvider.getSpeed();
		this.weakSpeed = 0.18f * speed;
		this.strongSpeed = 0.4f * speed;
		this.blueSpeed = 0.6f * speed;
	}

	private void updatePosition() {
		
			if (sawFirstOrange) {

				this.position = (this.m_motorLeft.getTachoCount() / 10800f) * 100;
			} else {

				this.position = ((this.m_motorLeft.getTachoCount() / 10800f) * 100) + 50;
			}

			if ((position > 0 && position < 15) || (position > 50 && position < 65) ) {
				this.dangerZone = true;
				ClientThread.getInstance().sendAccessRequest();
			} else {
				this.dangerZone = false;
			}


	}

	public float getSpeed() {
		return this.speed;
	}

	public float getPosition() {
		return this.position;
	}
}
