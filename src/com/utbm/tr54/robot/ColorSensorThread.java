package com.utbm.tr54.robot;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class ColorSensorThread extends Thread{
	private ColorRobot m_buffer;
	private SampleProvider m_provider;
	private Object mutex = new Object();

	public boolean IsRunning = true;

	public ColorSensorThread(SampleProvider provider)
	{
		m_provider = provider;
		m_buffer = new ColorRobot(0, 0, 0);
	}

	public void run()
	{
		while(IsRunning)
		{
			AcquireData();
			Delay.msDelay(10);
		}
	}

	public boolean IsEmpty()
	{
		return m_buffer == null;
	}

	public ColorRobot GetData()
	{
		ColorRobot color = null;
		synchronized (mutex) {
			color = m_buffer;
		}
		return color;
	}

	private void AcquireData()
	{
		synchronized (mutex)
		{
			ColorRobot color = new ColorRobot();

			float[] samples = new float[m_provider.sampleSize()];
			m_provider.fetchSample(samples, 0);

			color.r = samples[0];
			color.g = samples[1];
			color.b = samples[2];

			m_buffer = color;
		}
	}
}
