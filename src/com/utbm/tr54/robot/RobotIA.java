package com.utbm.tr54.robot;

import com.utbm.tr54.robot.thread.ClientThread;
import com.utbm.tr54.robot.thread.ColorSensorThread;
import com.utbm.tr54.robot.thread.DistanceThread;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.utility.Delay;
import lejos.utility.Stopwatch;

/**
 * The Class RobotIA.
 */
public class RobotIA extends AbstractRobot {

	/** The speed. */
	private float speed = 1f;
	
	/** The weak speed. */
	private float weakSpeed = 0.18f * speed;
	
	/** The strong speed. */
	private float strongSpeed = 0.4f * speed;
	
	/** The blue speed. */
	private float blueSpeed = 0.6f * speed;
	
	/** The position of the robot. */
	private float position = 100;

	/**
	 * The Enum State.
	 */
	private enum State {
		/** The black. */
		BLACK, 
		/** The blue. */
		BLUE, 
		/** The white. */
		WHITE, 
		/** The orange. */
		ORANGE
	};

	/** The boolean corresponding that if the robot is in the dangerZone. */
	private boolean dangerZone = false;
	
	/** The sawFirstOrange. */
	private boolean sawFirstOrange = false;
	
	/** The Constant PERIOD. */
	private static final int PERIOD = 50;

	/** The thread giving us the color. */
	private ColorSensorThread colorProvider;
	
	/** The thread dealing with the distance of the other robots. */
	private DistanceThread distanceProvider;

	/**
	 * Instantiates a new robot IA.
	 */
	public RobotIA() {
		super();

		assert(this.weakSpeed <= 1.f);
		assert(this.strongSpeed <= 1.f);
		assert(this.blueSpeed <= 1.f);

		System.out.println("activate sensor");
		this.activateColorSensor();
		this.activateUltrasoundSensor();
		System.out.println("create thread");
		this.colorProvider = new ColorSensorThread(this.m_colorSampleProvider);
		this.distanceProvider = new DistanceThread(this.m_distanceSampleProvider);
	}

	/* (non-Javadoc)
	 * @see com.utbm.tr54.robot.IRobotIA#launchIA()
	 */
	@Override
	public void launchIA() {
		System.out.println("run thread");
		this.colorProvider.start();
		this.distanceProvider.start();
		System.out.println("begin");

		State currentState = State.BLACK;
		ColorRobot lastSeenColor = new ColorRobot(0, 0, 0);
		Stopwatch orangeSw = new Stopwatch();

		while (true) {
			// we update the speed and position variables or the robot
			updateSpeed();
			updatePosition();
			
			// we check if we are in the danger zone and if we can advance or
			// not
			if (this.dangerZone && !ClientThread.getInstance().isCanAdvance()) {
				if(!(this.position < 7 || (this.position > 50 && this.position < 57))) this.stop();
				Button.LEDPattern(5);
				Delay.msDelay(PERIOD);
			}
			
			if (!this.colorProvider.isEmpty()) {
				lastSeenColor = this.colorProvider.getData();
			}

			if (lastSeenColor.isBlack()) {
				currentState = State.BLACK;
			} else if (lastSeenColor.isBlue()) {
				currentState = State.BLUE;
			} else if (lastSeenColor.isWhite()) {
				currentState = State.WHITE;
			} else if (lastSeenColor.isOrange()) {
				this.position = 0; // first orange
				this.m_motorLeft.resetTachoCount();
								
				if (orangeSw.elapsed() > 2000) {
					this.position = 0;
					this.m_motorLeft.resetTachoCount();
					
					sawFirstOrange = !sawFirstOrange;
					
					// we make a little sound when the robot sees orange color 
					// to be sure that he well see the orange
					if(sawFirstOrange) {
						Sound.beepSequence();
					} else {
						Sound.beep();
					}
					orangeSw.reset();
				} else {
					orangeSw.reset();
				}
			}

			switch (currentState) {
			// the color is black, we turn left
			case BLACK: {
				this.setSpeedLeft(this.weakSpeed);
				this.setSpeedRight(this.strongSpeed);
				this.forward();
				break;
			}
			// the color is blue, we go faster
			case BLUE: {
				this.setSpeed(this.blueSpeed);
				this.forward();
				break;
			}
			// the color is white, we turn right
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

	/**
	 * Update the speed value .
	 */
	private void updateSpeed() {
		this.speed = this.distanceProvider.getSpeed();
		this.weakSpeed = 0.18f * this.speed;
		this.strongSpeed = 0.4f * this.speed;
		this.blueSpeed = 0.6f * this.speed;
	}

	/**
	 * Update the position position.
	 */
	private void updatePosition() {
		
			// when we see orange, we recalibrate the position
			if (sawFirstOrange) {
				this.position = (this.m_motorLeft.getTachoCount() / 11000f) * 100;
			} else {
				this.position = ((this.m_motorLeft.getTachoCount() / 11000f) * 100) + 50;
			}

			if ((this.position >= 0 && this.position < 15) || (this.position >= 50 && this.position < 65) ) {
				this.dangerZone = true;
				ClientThread.getInstance().sendClientInformation();
				Delay.msDelay(50);
				ClientThread.getInstance().sendAccessRequest();
			} else {
				this.dangerZone = false;
			}
	}

	/**
	 * Gets the speed.
	 *
	 * @return the speed
	 */
	public float getSpeed() {
		return this.speed;
	}

	/**
	 * Gets the position.
	 *
	 * @return the position
	 */
	public float getPosition() {
		return this.position;
	}
}
