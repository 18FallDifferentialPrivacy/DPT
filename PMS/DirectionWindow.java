package dpt.sharedobjects.tracedb;

public class DirectionWindow {
	int[] window;
	double[] weights;
	double prevX, prevY;
	
	public DirectionWindow(){
		this(5); //5 is the default size
	}
	
	public DirectionWindow(int windowSize){
		window = new int[windowSize];
		for(int i=0; i<window.length; i++){
			window[i] = 4; //cannot be 4
		}
		weights = new double[9];
		prevX = 0;
		prevY = 0;
	}
	
	
	public void addPoint(double currX, double currY){
		//System.err.println(currX +","+currY);
		double changeX = 0, changeY = 0;
		
		if(prevX>0 && prevY>0){
			changeX = currX - prevX;
			changeY = currY - prevY;
			shift(changeX, changeY);
		}

		prevX = currX;
		prevY = currY;
	}
	
	
	public double[] getWeights(){
		return weights;
	}
	
	private void shift(double changeX, double changeY){
		int dir = getDirection(changeX, changeY);
		if(dir==4){ //stay in the same place
			//don't shift or change anything: will lose information in the front
			return;
		}
		int dirStart = window[0];
		
		for(int i=1; i<window.length; i++){
			window[i-1] = window[i]; 
		}
		window[window.length-1] = dir;
		
		if(weights[dirStart]>0){ //dirStart could be 4, always of count 0
			weights[dirStart] = weights[dirStart]-1;
		}
		weights[dir] = weights[dir] +1;
	}
	
	
	/**
	 * same setting as GridRS.java
	 * 0,1,2, --> y increases
	 * 3,4,5, 
	 * 6,7,8,
	 * going down x increases
	 * 
	 * (x,y)
	 * (-1,-1) (-1, 0) (-1,+1)
 	 * (0,-1)  (0, 0)  (0,+1)
	 * (+1,-1) (+1, 0) (+1,+1)

	 * the whole direction is not same as xy-graph
	 */
	private int getDirection(double changeX, double changeY){
		int dir = 4;
		if(changeX < 0 && changeY >0){ //in direction of (-1,+1)
			dir = 2;
		}else if(changeX >0 && changeY >0){ //in direction of (+1,+1)
			dir = 8;
		}else if(changeX >0 && changeY <0){ // in direction of (+1,-1)
			dir = 6;
		}else if(changeX <0 && changeY <0){ // in direction of (-1,-1)
			dir = 0;
		} else if(changeX ==0 && changeY>0){ //(0,+1)
			dir = 5;
		} else if(changeX ==0 && changeY<0){ //(0,-1)
			dir = 3;	
		} else if(changeX >0 && changeY ==0){ //(+1,0)
			dir = 7;
		} else if(changeX <0 && changeY ==0){ //(-1,0)
			dir = 1;
		} else {
			dir = 4;
		}
		//System.err.println(dir);
		return dir;
	}

	
	
	
}
