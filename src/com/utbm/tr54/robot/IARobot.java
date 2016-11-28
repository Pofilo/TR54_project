package com.utbm.tr54.robot;

import lejos.utility.Stopwatch;

public class IARobot extends AbstractRobot {

	final static float SPEED = 1f;
	final static float WEAK_SPEED = 0.18f * SPEED;
	final static float STRONG_SPEED = 0.4f * SPEED;
	final static float SPEEDY = 0.4f;

	private enum State {
		BLACK, BLUE, WHITE, ORANGE
	};

	private final boolean m_isServer;
	
	private boolean sawOrange = false;

	private ColorSensorThread colorProvider;

	public IARobot(boolean isServer) {
		super();
		m_isServer = isServer;

		assert (WEAK_SPEED <= 1.f);
		assert (STRONG_SPEED <= 1.f);
		assert (SPEEDY <= 1.f);

		System.out.println("activate sensor");
		this.ActivateColorSensor();
		System.out.println("create thread");
		colorProvider = new ColorSensorThread(m_colorSampleProvider);
	}

	@Override
	public void LaunchIA() {
		System.out.println("run thread");
		colorProvider.start();
		System.out.println("begin");

		State currentState = State.BLACK;
		ColorRobot lastSeenColor = new ColorRobot(0, 0, 0);
		Stopwatch orangeSw = new Stopwatch();

		while (true) {
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
				this.SetSpeedLeft(WEAK_SPEED);
				this.SetSpeedRight(STRONG_SPEED);
				this.Forward();
				break;
			}
			case BLUE: {
				this.SetSpeed(SPEEDY);
				this.Forward();
				break;
			}
			case WHITE: {
				this.SetSpeedLeft(STRONG_SPEED);
				this.SetSpeedRight(WEAK_SPEED);
				this.Forward();
				break;
			}
			default:
				break;
			}
		}
	}

}
