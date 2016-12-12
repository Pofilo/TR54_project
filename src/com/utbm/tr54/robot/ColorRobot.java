package com.utbm.tr54.robot;



/**
 * The Class ColorRobot represent a color seen by the color capteur.
 */
public class ColorRobot {
	
	/** The red composant of the color. */
	public float r;
	
	/** The green composant of the color. */
	public float g;
	
	/** The blue composant of the color. */
	public float b;
	
	/** The Constant PERFECT_BLUE. */
	static final ColorRobot PERFECT_BLUE  = new ColorRobot(0.003f, 0.155f, 0.178f);
	
	/** The Constant PERFECT_BLACK. */
	static final ColorRobot PERFECT_BLACK = new ColorRobot(0.f, 0.f, 0.f);
	
	/** The Constant PERFECT_ORANGE. */
	static final ColorRobot PERFECT_ORANGE  = new ColorRobot(0.2127451f, 0.0696f, 0.0185f);
	
	/**
	 * Instantiates a new color robot.
	 */
	public ColorRobot()
	{
		r = 0.f;
		g = 0.f;
		b = 0.f;
	}
	
	/**
	 * Instantiates a new color robot.
	 *
	 * @param _r the red composant
	 * @param _g the green composant
	 * @param _b the blue composant
	 */
	public ColorRobot(float _r, float _g, float _b)
	{
		r = _r;
		g = _g;
		b = _b;
	}
	
	/**
	 * Checks if the color is blue.
	 *
	 * @return true, if successful
	 */
	public boolean isBlue()
	{
		return equals(this, PERFECT_BLUE, 0.2f);
	}
	
	/**
	 * Checks if the color is black.
	 *
	 * @return true, if successful
	 */
	public boolean isBlack()
	{
		return equals(this, PERFECT_BLACK, 0.07f);
	}
	
	/**
	 * Checks if the color is white.
	 *
	 * @return true, if successful
	 */
	public boolean isWhite()
	{
		return (r > 0.15 && g > 0.15 && b > 0.06);
	}
	
	/**
	 * Checks if the color is orange.
	 *
	 * @return true, if successful
	 */
	public boolean isOrange()
	{
		return equals(this, PERFECT_ORANGE, 0.1f);
	}
	
	/**
	 * return true is the two color are equal or close enough.
	 *
	 * @param color1 the first color
	 * @param color2 the second color
	 * @param epsilon the error we can afford when comparing the two color
	 * @return true, if successful
	 */
	public static boolean equals(ColorRobot color1, ColorRobot color2, float epsilon)
	{
		float error = 0;
		
		error += Math.abs(color1.r - color2.r);
		error += Math.abs(color1.g - color2.g);
		error += Math.abs(color1.b - color2.b);
		
		return (error < epsilon);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ColorRobot [r=" + r + ", g=" + g + ", b=" + b + "]";
	}
}
