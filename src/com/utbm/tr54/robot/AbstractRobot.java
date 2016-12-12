package com.utbm.tr54.robot;
import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;


/**
 * The Class AbstractRobot represent every function available to a robot.
 */
public abstract class AbstractRobot implements IRobotIA{

	/** The left motor. */
	protected BaseRegulatedMotor m_motorLeft;
	
	/** The right motor. */
	protected BaseRegulatedMotor m_motorRight;

	/** The distance sensor. */
	private EV3UltrasonicSensor m_distanceSensor;
	
	/** The distance sample provider. */
	protected SampleProvider m_distanceSampleProvider;
	
	/** Ultrasound sensor activated. */
	private boolean m_ultrasoundSensorActivated = false;

	/** The color sensor. */
	private EV3ColorSensor m_colorSensor;
	
	/** The color sample provider. */
	protected SampleProvider m_colorSampleProvider;
	
	/** Color sensor activated. */
	private boolean m_colorSensorActivated = false;

	/** The left motor left. */
	private Port m_motorLeftPort = MotorPort.B;
	
	/** The right motor port. */
	private Port m_motorRightPort = MotorPort.C;
	
	/** The distance sensor port. */
	private Port m_distanceSensorPort = SensorPort.S3;
	
	/** The color sensor port. */
	private Port m_colorSensorPort = SensorPort.S2;

	/**
	 * Instantiates a new abstract robot.
	 */
	public AbstractRobot()
	{
		activateMotors();
	}

	/**
	 * Activate motors.
	 */
	protected void activateMotors()
	{
		m_motorLeft = new EV3LargeRegulatedMotor(m_motorLeftPort);
		m_motorRight = new EV3LargeRegulatedMotor(m_motorRightPort);

		m_motorLeft.synchronizeWith(new RegulatedMotor[] {m_motorRight});
	}

	/**
	 * Make the robot go forward.
	 */
	protected void forward()
	{
		m_motorLeft.startSynchronization();
		m_motorLeft.forward();
		m_motorRight.forward();
		m_motorLeft.endSynchronization();
	}

	/**
	 * Rotate the robot.
	 *
	 * @param angle the angle to rotate the robot, in degre.
	 */
	protected void rotate(int angle)
	{
		m_motorLeft.startSynchronization();
		angle *= 2;
		if(angle > 0)
		{
			m_motorLeft.rotate(angle, true);
			m_motorRight.rotate(-angle, true);
		}
		else
		{
			m_motorRight.rotate(angle, true);
			m_motorLeft.rotate(-angle, true);
		}
		m_motorLeft.endSynchronization();

		m_motorLeft.waitComplete();
		m_motorRight.waitComplete();

	}

	/**
	 * Stop the robot from advancing.
	 */
	protected void stop()
	{
		m_motorLeft.startSynchronization();

		m_motorLeft.stop(true);
		m_motorRight.stop(true);

		m_motorLeft.endSynchronization();

		m_motorLeft.waitComplete();
		m_motorRight.waitComplete();
	}

	/**
	 * Sets the speed of the robot.
	 *
	 * @param percent the percent of max speed used by the robot
	 */
	protected void setSpeed(float percent)
	{
		m_motorLeft.setSpeed(m_motorLeft.getMaxSpeed() * percent);
		m_motorRight.setSpeed(m_motorRight.getMaxSpeed() * percent);
	}
	
	/**
	 * Sets the speed of the left motor.
	 *
	 * @param percent the percent of max speed used by left motor
	 */
	protected void setSpeedLeft(float percent)
	{
		m_motorLeft.setSpeed(m_motorLeft.getMaxSpeed() * percent);
	}
	
	/**
	 * Sets the speed of the right motor.
	 *
	 * @param percent the percent of max speed used by right motor
	 */
	protected void setSpeedRight(float percent)
	{
		m_motorRight.setSpeed(m_motorRight.getMaxSpeed() * percent);
	}

	/**
	 * Gets the speed of the robot.
	 *
	 * @return the mean of each motor speed in percent of the max speed.
	 */
	protected float getSpeed()
	{
		float speed = 0;
		speed += m_motorLeft.getSpeed();
		speed += m_motorRight.getSpeed();
		speed /= 2.f;
		return speed;

	}

	/**
	 * Activate ultrasound sensor.
	 */
	protected void activateUltrasoundSensor()
	{
		if(m_ultrasoundSensorActivated)
		{
			m_distanceSensor.enable();
		}
		m_distanceSensor = new EV3UltrasonicSensor(m_distanceSensorPort);
		m_distanceSensor.setCurrentMode("Distance");
		m_distanceSampleProvider = m_distanceSensor.getDistanceMode();
		m_ultrasoundSensorActivated = m_distanceSensor.isEnabled();
	}

	/**
	 * Deactivate ultrasound sensor.
	 */
	protected void deactivateUltrasoundSensor()
	{
		m_distanceSensor.disable();
	}

	/**
	 * Checks if is ultrasonic sensor is activated.
	 *
	 * @return true, if successful
	 */
	protected boolean isUltrasonicSensorActivated()
	{
		return m_ultrasoundSensorActivated && m_distanceSensor.isEnabled();
	}

	/**
	 * Activate the color sensor.
	 */
	protected void activateColorSensor()
	{
		m_colorSensor = new EV3ColorSensor(m_colorSensorPort);
		m_colorSensor.setCurrentMode("RGB");
		m_colorSampleProvider = m_colorSensor.getRGBMode();
		m_colorSensorActivated = true;
	}

	/**
	 * Checks if is color sensor is activated.
	 *
	 * @return true, if successful
	 */
	protected boolean isColorSensorActivated()
	{
		return m_colorSensorActivated;
	}

	/**
	 * Return the distance capted by the ultrasonic sensor.
	 *
	 * @return a distance in meter
	 */
	protected float getDistance()
	{
		return getDistance(10);
	}

	/**
	 * Return the distance capted by the ultrasonic sensor.
	 *
	 * @param n the number of sample we take before returning the result
	 * @return a distance in meter
	 */
	protected float getDistance(int n)
	{
		if(!isUltrasonicSensorActivated())
		{
			return -1;
		}

		float dist = 0;

		for(int i=0; i < n; ++i)
		{
			float[] samples = new float[m_distanceSampleProvider.sampleSize()];
			m_distanceSampleProvider.fetchSample(samples, 0);
			dist += samples[0];
		}
		dist /= n;
		return dist;
	}

	/**
	 * Return the color saw by the color sensor.
	 *
	 * @return a colorRobot object
	 */
	protected ColorRobot seeColor()
	{		
		ColorRobot color = new ColorRobot();

		float[] samples = new float[m_colorSampleProvider.sampleSize()];
		m_colorSampleProvider.fetchSample(samples, 0);

		color.r = samples[0];
		color.g = samples[1];
		color.b = samples[2];

		return color;
	}
}
