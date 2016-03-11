package com.Torvald.Terrarum.GameMap;

import com.Torvald.Point.Point2f;

import java.io.Serializable;


public class MapPoint {
	private Point2f startPoint;
	private Point2f endPoint;
	
	public MapPoint(){
		
	}
	
	public MapPoint(Point2f p1, Point2f p2){
		setPoint(p1, p2);
	}
	
	public MapPoint(int x1, int y1, int x2, int y2){
		setPoint(x1, y1, x2, y2);
	}
	
	public void setPoint(Point2f p1, Point2f p2){
		startPoint = p1;
		endPoint = p2;
	}
	
	public void setPoint(int x1, int y1, int x2, int y2){
		startPoint = new Point2f(x1, y1);
		endPoint = new Point2f(x2, y2);
	}
	
	public Point2f getStartPoint(){
		return startPoint;
	}
	
	public Point2f getEndPoint(){
		return endPoint;
	}
}
