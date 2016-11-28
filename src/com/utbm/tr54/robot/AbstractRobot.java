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


public abstract class AbstractRobot implements IRobotIA{

	protected BaseRegulatedMotor m_motorLeft;
	protected BaseRegulatedMotor m_motorRight;

	private EV3UltrasonicSensor m_distanceSensor;
	protected SampleProvider m_distanceSampleProvider;
	private boolean m_ultrasonicSensorActivated = false;

	private EV3ColorSensor m_colorSensor;
	protected SampleProvider m_colorSampleProvider;
	private boolean m_colorSensorActivated = false;

	private Port m_motorLeftPort = MotorPort.B;
	private Port m_motorRightPort = MotorPort.C;
	private Port m_distanceSensorPort = SensorPort.S3;
	private Port m_colorSensorPort = SensorPort.S2;

	public AbstractRobot()
	{
		ActivateMotor();
	}

	protected void ActivateMotor()
	{
		m_motorLeft = new EV3LargeRegulatedMotor(m_motorLeftPort);
		m_motorRight = new EV3LargeRegulatedMotor(m_motorRightPort);

		m_motorLeft.synchronizeWith(new RegulatedMotor[] {m_motorRight});
	}

	protected void Forward()
	{
		m_motorLeft.startSynchronization();
		m_motorLeft.forward();
		m_motorRight.forward();
		m_motorLeft.endSynchronization();
	}

	protected void Rotate(int angle)
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

	protected void Stop()
	{
		m_motorLeft.startSynchronization();

		m_motorLeft.stop(true);
		m_motorRight.stop(true);

		m_motorLeft.endSynchronization();

		m_motorLeft.waitComplete();
		m_motorRight.waitComplete();
	}

	protected void SetSpeed(float percent)
	{
		m_motorLeft.setSpeed(m_motorLeft.getMaxSpeed() * percent);
		m_motorRight.setSpeed(m_motorRight.getMaxSpeed() * percent);
	}
	
	protected void SetSpeedLeft(float percent)
	{
		m_motorLeft.setSpeed(m_motorLeft.getMaxSpeed() * percent);
	}
	
	protected void SetSpeedRight(float percent)
	{
		m_motorRight.setSpeed(m_motorRight.getMaxSpeed() * percent);
	}

	protected float GetSpeed()
	{
		float speed = 0;
		speed += m_motorLeft.getSpeed();
		speed += m_motorRight.getSpeed();
		speed /= 2.f;
		return speed;

	}

	protected void ActivateUltrasonicSensor()
	{
		if(m_ultrasonicSensorActivated)
		{
			m_distanceSensor.enable();
		}
		m_distanceSensor = new EV3UltrasonicSensor(m_distanceSensorPort);
		m_distanceSensor.setCurrentMode("Distance");
		m_distanceSampleProvider = m_distanceSensor.getDistanceMode();
		m_ultrasonicSensorActivated = m_distanceSensor.isEnabled();
	}

	protected void DeactivateUltrasoneSensor()
	{
		m_distanceSensor.disable();
	}

	protected boolean IsUltrasonicSensorActivated()
	{
		return m_ultrasonicSensorActivated && m_distanceSensor.isEnabled();
	}

	protected void ActivateColorSensor()
	{
		m_colorSensor = new EV3ColorSensor(m_colorSensorPort);
		m_colorSensor.setCurrentMode("RGB");
		m_colorSampleProvider = m_colorSensor.getRGBMode();
		m_colorSensorActivated = true;
	}

	protected boolean IsColorSensorActivated()
	{
		return m_colorSensorActivated;
	}

	protected float Distance()
	{
		return Distance(10);
	}

	protected float Distance(int n)
	{
		if(!IsUltrasonicSensorActivated())
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

	protected ColorRobot SeeColor()
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
