package dpt.phases;
import dpt.sharedobjects.*;
import dpt.sharedobjects.forest.*;
import dpt.sharedobjects.tracedb.*;
import dpt.sharedobjects.tracedb.rs.*;

public class PrefixTree {
	RSEvent_RealStop realStop = new RSEvent_RealStop();
	
	double normIncr = 1;


	public Forest buildForest(TraceDatabase rsDB, RSS rss, int[] heightArr) {	
		Forest forest = new Forest(rss, heightArr); 		//all the heights are the same
		return buildForestFromForest(forest, rsDB, rss, heightArr);
	}	
		

	public Forest addForest(Forest forest, TraceDatabase topDB, RSS rss,
			int[] heightArr) {
		return buildForestFromForest(forest, topDB, rss, heightArr);
	}
	
	
	private Forest buildForestFromForest(Forest forest, TraceDatabase rsDB, RSS rss, int[] heightArr){	

		System.err.println("normalized");
		
		for(int personId=0; personId<rsDB.getSize(); personId++){
			PersonalTrace trace = rsDB.getPersonalTrace(personId);

			normIncr = 1.0 / normalize(trace, heightArr[0]);

			for(int trajId=0; trajId<trace.getSize(); trajId++){
				Trajectory segment = trace.getTrajectory(trajId); 
				addSegment(segment, forest);
			} //trace			
		} //person
		return forest;
	}


	private void addSegment(Trajectory segment, Forest forest){
		
		if(segment== null || segment.getLength()<=1){
			System.err.println("traj length <=1 or null");
			return;
		}
		
		//identify which RS this segment is in; if segment contains a RealStart symbol
		int rsId = 0;
		boolean isRealStart = false;
		RSLocation startLoc = (RSLocation) segment.getEvent(0).getLoc(); 
		rsId = startLoc.getRs();
		if(rsId==-2) { //is a start segment
			segment.removeFirstEvent();
			startLoc = (RSLocation) segment.getEvent(0).getLoc();
			rsId = startLoc.getRs();
			isRealStart = true;
		}	
		
		//identify which RS this segment continues on for the next segment with 2 cases: stop symbol, or other reference system
		RSLocation endLoc = (RSLocation) segment.getEvent(segment.getLength()-1).getLoc();
		int endRsId = endLoc.getRs();
		
		//add real/false starting grams to the identified RSTree
		RSTree rsTree = forest.getRSTree(rsId);
		addStartSeg(segment,rsTree, isRealStart);			

		//add the main grams to the identified RSTree
		int maxHeight = rsTree.getHeight();
		int segLength = segment.getLength();
		for(int i=0; i<(segLength-1); i++){
			String kgram = "";
			rsTree.increaseRootCount(normIncr); 
			
			for(int k=0; k<maxHeight; k++){
				if(i+k<(segLength-1)){				
					kgram = kgram + segment.getEvent(i+k).toString()+";";
					rsTree.increaseNodeCount(new RSNode(k+1,kgram,0), normIncr);
				}else if(i+k==(segLength-1)){ //the last node, definitely having different id as rsId
					if(endRsId == -2){
						kgram = kgram + realStop.toString()+";"; //set endnull node as the last node
						rsTree.increaseNodeCount(new RSNode(k+1,kgram,0),normIncr);					
					}else if(endRsId != rsId){
						int tranType = endRsId - rsId;
						kgram = kgram + segment.getEvent(i+k-1).toString()+";"; //set the last node of same loc as the 2nd last loc
						rsTree.increaseNodeCount(new RSNode(k+1,kgram,tranType),normIncr);
					}
					break;
				}
			}			
		}		
	}
	
	

	private void addStartSeg(Trajectory segment, RSTree rsTree, boolean isRealStart){
		//printSeg(segment);
		
		RSEvent start;		
		if(isRealStart){
			start = new RSEvent_RealStart(); //add real start symbol			
		}else{
			start = new RSEvent_FalseStart(); //add false start symbol			
		}
		
		String startStr = start.toString();		
		int maxHeight = rsTree.getHeight();
		int segLength = segment.getLength();
		
		
		for(int i=0; i<(maxHeight-1); i++){
			String kgram = "";
			rsTree.increaseRootCount(normIncr); 
		
			for(int k=1; k<maxHeight-i; k++){
				kgram = kgram+startStr+";";
				rsTree.increaseNodeCount(new RSNode(k,kgram,0), normIncr);
			}
			
			for(int k=0; k<=i;k++){
				if(k<segLength-1){
					kgram = kgram + segment.getEvent(k).toString()+";";
					rsTree.increaseNodeCount(new RSNode(maxHeight-i+k,kgram,0), normIncr);	
				} else if (k==segLength-1){ 
					RSLocation endLoc = (RSLocation) segment.getEvent(segment.getLength()-1).getLoc();
					int endRsId = endLoc.getRs();
					int rsId = rsTree.getRsId();
					
					if(endRsId == -2){
						kgram = kgram + realStop.toString()+";"; 
						rsTree.increaseNodeCount(new RSNode(maxHeight-i+k,kgram,0),normIncr);					
					}else if(endRsId != rsId){
						kgram = kgram + segment.getEvent(k-1).toString()+";"; 
						rsTree.increaseNodeCount(new RSNode(maxHeight-i+k,kgram,endRsId-rsId),normIncr);					
					}		
					break;
					
				} else{
					break;
				}
			}	
		}		
	}
	

	
	private double normalize(PersonalTrace trace, int maxHeight){
		double length=0;
		for(int trajId=0; trajId<trace.getSize(); trajId++){		
			length += (trace.getTrajectory(trajId).getLength() + maxHeight-1); //adding real/false start counts
		}
		return length;
	}	
}
