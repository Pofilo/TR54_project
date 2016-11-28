package com.utbm.tr54.robot;

public class ColorRobot {
	public float r;
	public float g;
	public float b;
	
	static final ColorRobot PERFECT_BLUE  = new ColorRobot(0.003f, 0.155f, 0.178f);
	static final ColorRobot PERFECT_BLACK = new ColorRobot(0.f, 0.f, 0.f);
	static final ColorRobot PERFECT_ORANGE  = new ColorRobot(0.2127451f, 0.0696f, 0.0185f);
	
	public ColorRobot()
	{
		r = 0.f;
		g = 0.f;
		b = 0.f;
	}
	
	public ColorRobot(float _r, float _g, float _b)
	{
		r = _r;
		g = _g;
		b = _b;
	}
	
	public boolean IsBlue()
	{
		return Equals(this, PERFECT_BLUE, 0.2f);
	}
	
	public boolean IsBlack()
	{
		return Equals(this, PERFECT_BLACK, 0.07f);
	}
	
	public boolean IsWhite()
	{
		return (r > 0.15 && g > 0.15 && b > 0.10);
	}
	
	public boolean IsOrange()
	{
		return Equals(this, PERFECT_ORANGE, 0.1f);
	}
	
	public static boolean Equals(ColorRobot a, ColorRobot b, float epsilon)
	{
		float error = 0;
		
		error += Math.abs(a.r - b.r);
		error += Math.abs(a.g - b.g);
		error += Math.abs(a.b - b.b);
		
		return (error < epsilon);
	}

	@Override
	public String toString() {
		return "ColorRobot [r=" + r + ", g=" + g + ", b=" + b + "]";
	}
}
