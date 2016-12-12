package com.utbm.tr54.robot;

public class RobotData {
	public float speed;
	public float position;
		
	public RobotData(){
		speed = 0;
		position = 0;
	}
	
	public RobotData(float _speed, float _position){
		speed = _speed;
		position = _position;
	}
}
