package com.utbm.tr54.robot;

import com.utbm.tr54.robot.thread.ColorSensorThread;
import com.utbm.tr54.robot.thread.DistanceThread;

import lejos.utility.Delay;
import lejos.utility.Stopwatch;

public class IARobot extends AbstractRobot {

	float speed = 1f;
	float weakSpeed = 0.18f * speed;
	float strongSpeed = 0.4f * speed;

	private enum State {
		BLACK, BLUE, WHITE, ORANGE
	};

	private final boolean m_isServer;
	
	private boolean sawOrange = false;

	private ColorSensorThread colorProvider;
	private DistanceThread distanceProvider;

	public IARobot(boolean isServer) {
		super();
		m_isServer = isServer;

		assert (weakSpeed <= 1.f);
		assert (strongSpeed <= 1.f);

		System.out.println("activate sensor");
		this.ActivateColorSensor();
		this.ActivateUltrasonicSensor();
		System.out.println("create thread");
		colorProvider = new ColorSensorThread(m_colorSampleProvider);
		distanceProvider = new DistanceThread(m_distanceSampleProvider);
	}

	@Override
	public void LaunchIA() {
		System.out.println("run thread");
		colorProvider.start();
		distanceProvider.start();
		System.out.println("begin");

		State currentState = State.BLACK;
		ColorRobot lastSeenColor = new ColorRobot(0, 0, 0);
		Stopwatch orangeSw = new Stopwatch();

		while (true) {
			updateSpeed();
			
			if (!colorProvider.IsEmpty()) {
				lastSeenColor = colorProvider.GetData();
			}

			if (lastSeenColor.IsBlack()) {
				currentState = State.BLACK;
			} else if (lastSeenColor.IsBlue()) {
				currentState = State.BLUE;
			} else if (lastSeenColor.IsWhite()) {
				currentState = State.WHITE;
			} else if (lastSeenColor.IsOrange()) {
				if (orangeSw.elapsed() > 2000) {
					if (sawOrange) {
						// premiere bande
					} else {
						// deuxieme bande
					}
					sawOrange = !sawOrange;
					orangeSw.reset();
				} else {
					orangeSw.reset();
				}

			}

			switch (currentState) {
			case BLACK: {
				this.SetSpeedLeft(this.weakSpeed);
				this.SetSpeedRight(this.strongSpeed);
				this.Forward();
				break;
			}
			case BLUE: {
				this.SetSpeed(this.speed);
				this.Forward();
				break;
			}
			case WHITE: {
				this.SetSpeedLeft(this.strongSpeed);
				this.SetSpeedRight(this.weakSpeed);
				this.Forward();
				break;
			}
			default:
				break;
			}
		}
	}
	
	public void updateSpeed() {
		this.speed = this.distanceProvider.getSpeed();
		this.weakSpeed = 0.18f * speed;
		this.strongSpeed = 0.4f * speed;
	}

}
