package com.utbm.tr54.robot;

/**
 * The Class RobotData represent what the server know about a robot.
 * Attribute aren't final because the server will continuously update them.
 */
public class RobotData {
	
	/** The speed of the robot. */
	public float speed;
	
	/** The position of the robot. */
	public float position;
		
	/**
	 * Instantiates a new robotData.
	 */
	public RobotData(){
		speed = 0;
		position = 0;
	}
	
	/**
	 * Instantiates a new robot data.
	 *
	 * @param _speed the speed of the robot
	 * @param _position the position of the robot
	 */
	public RobotData(float _speed, float _position){
		speed = _speed;
		position = _position;
	}
}
