package com.utbm.tr54.robot;

import com.utbm.tr54.robot.thread.ClientThread;
import com.utbm.tr54.robot.thread.ColorSensorThread;
import com.utbm.tr54.robot.thread.DistanceThread;

import lejos.utility.Delay;
import lejos.utility.Stopwatch;

public class RobotIA extends AbstractRobot {

	float speed = 1f;
	float weakSpeed = 0.18f * speed;
	float strongSpeed = 0.4f * speed;
	float blueSpeed = 0.6f * speed;
	float position = 0;

	private enum State {
		BLACK, BLUE, WHITE, ORANGE
	};
	
	private boolean dangerZone = false;
	private boolean sawFirstOrange = false;
	private static final int PERIOD = 50;

	private ColorSensorThread colorProvider;
	private DistanceThread distanceProvider;

	public RobotIA(boolean isServer) {
		super();
		
		assert (weakSpeed <= 1.f);
		assert (strongSpeed <= 1.f);
		assert (blueSpeed <= 1.f);
		
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
			updatePosition();
			System.out.println(this.position);
			
			// we check if we are in the danger zone and if we can advance or not
			if(!dangerZone && !ClientThread.getInstance().isCanAdvance()) {
				this.Stop();
				Delay.msDelay(PERIOD);
				continue;
			}
			
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
				this.position=0; //premiere bande
				this.m_motorLeft.resetTachoCount();
				if (orangeSw.elapsed() > 2000) {
					ClientThread.getInstance().sendAccessRequest();
					sawFirstOrange = !sawFirstOrange;
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
				this.SetSpeed(this.blueSpeed);
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
	
	private void updateSpeed() {
		this.speed = this.distanceProvider.getSpeed();
		this.weakSpeed = 0.18f * speed;
		this.strongSpeed = 0.4f * speed;
		this.blueSpeed = 0.6f * speed;
	}
	
	private void updatePosition(){
		if(this.position < 100)	{
			if (sawFirstOrange){
				
				this.position = (this.m_motorLeft.getTachoCount()/6000f)*100;
			} else {
				
				this.position = ((this.m_motorLeft.getTachoCount()/6000f)*100)+50;
			}
			
			if((position > 0 && position < 15) || (position > 50 && position < 65)) {
				this.dangerZone = true;
			} else {
				this.dangerZone = false;
			}
			
		} else {
			this.position = 0;
			this.m_motorLeft.resetTachoCount();
		}
		
	}
	
	public float getSpeed() {
		return this.speed;
	}
	
	public float getPosition() {
		return this.position;
	}
}
