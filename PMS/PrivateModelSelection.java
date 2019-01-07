package PMS;

import dpt.phases.RSTransform;
import dpt.sharedfunctions.*;
import dpt.sharedobjects.GridRSS;
import dpt.sharedobjects.RSS;
import dpt.sharedobjects.forest.RSTree;
import dpt.sharedobjects.laplace.LaplaceGenerator;
import dpt.sharedobjects.tracedb.TraceDatabase;

public class PrivateModelSelection {
	
	static int maxRSId;
	static double[] fullHRSSpeedArr;//F_full
	static double[] optHRSSpeedArr;//F+*
	static int optHeight;
	
	
	public void privateModelSelection(Configure conf, FileIO io, double selectionBudget, double remainBudget) {
		//step1:count moves 
		fullHRSSpeedArr=conf.getFullHRSSpeedArr();
    	System.err.println("selection from full HRS "+conf.getFullHRSSpeedArrStr());
    	RSS fullHRS = new GridRSS(conf.getMinX(),conf.getMaxX(),conf.getMinY(),conf.getMaxY(),fullHRSSpeedArr);
    	double[] trueRootCounts = rootCounts(conf,io,fullHRS);
    	
    	//step2:add noise  
    	double[] noisyRootCounts = addLapNoise(selectionBudget,trueRootCounts);
    	
    	//step3:select M*
    	maxRSId = selectMaxRSIndex(selectionBudget, noisyRootCounts,fullHRS);
    	System.err.println("max RS id:" + maxRSId +" of speed "+fullHRSSpeedArr[maxRSId]);

    	//step4:iterate k from maximum height till minimum height 2
    	int optK = 8;//k_max
    	while(optK>=2) {
    		double[] biasFullHRS = estimateFullHRSBias(noisyRootCounts, optK);
    		double[] noiseFullHRS = estimateFullHRSNoise(fullHRS,optK,remainBudget);
    		double[] speedArr = optModelSelection(biasFullHRS, noiseFullHRS, optK);//结果
    		if(speedArr != null) {
    			optHeight=optK;
    			optHRSSpeedArr=speedArr;
    			break;
    		}
    		optK--;
    	}
    	if(optK <2){
    		optHeight = 2;
    		optHRSSpeedArr = new double[1];
    		optHRSSpeedArr[0] = fullHRSSpeedArr[maxRSId];
    	}
    	
	}
	
	public int getOptHeight(){
    	return optHeight;
    }
    
    public double[] getOptHRS(){
    	return optHRSSpeedArr;
    }
	
	
	public double[] rootCounts(Configure conf,FileIO io,RSS fullHRS) {
		RSTransform rsTransform = new RSTransform();
		String[] inputFilenames = conf.getStandRawDBFile();
		
		double[] trueCountsBin=new double[fullHRS.getSize()];
		for(int i=0;i<conf.getRawNumOfFiles();i++) {
			TraceDatabase rawDB = io.readRawDB(inputFilenames[i]);
			double[] countPerFile = rsTransform.countDistr(rawDB, fullHRS);
			for(int j=0;j<countPerFile.length;j++) {
				trueCountsBin[j] += countPerFile[j];
			}
		}
		
		return trueCountsBin;
	}
	
	private double[] addLapNoise(double budgetSelection,double[] trueCountsBin) {
		LaplaceGenerator lapGen = new LaplaceGenerator();
		double[] noisyCountsBin = new double[trueCountsBin.length];
		for(int i=0;i<trueCountsBin.length;i++) {
			double lambda=1.0/budgetSelection;
			double noise=lapGen.generateNoise(lambda);
			noisyCountsBin[i]=(trueCountsBin[i]+noise);
		}
		return noisyCountsBin;
	}
	
	
	private int selectMaxRSIndex(double budgetSelection, double[] noisyCountsBin, RSS fullHRS) {
		double threshold=Math.sqrt(2.0)/budgetSelection;
		for(int i=noisyCountsBin.length+1;i>=0;i--) {
			if(noisyCountsBin[i]>(threshold+fullHRS.getRSbyID(i).getSize())) {
				return i;
			}
		}
		return 0;
	}
	
	private double[] estimateFullHRSBias(double[] noisyRootCounts, int height) {
		int size=maxRSId+1;//?
		double[] biasFullHRS=new double[size];
		for(int i=0;i<size;i++) {
			biasFullHRS[i]=Math.pow(noisyRootCounts[1], 2)*height;
		}
		return biasFullHRS;
	}
	
	private double[] estimateFullHRSNoise(RSS fullHRS,int height,double budget) {
		int size=maxRSId+1;//?
		double[] noiseFullHRS=new double[size];
		double[] geoBudgetByGram=setGeometricEps(budget,height);
		for(int i=0;i<size;i++) {
			RSTree tree=new RSTree(height,fullHRS.getRSbyID(i),i,size);
			double noise=0;	
			for(int level=0;level<height;level++) {
				double noisePerNode=Math.sqrt(2.0)/geoBudgetByGram[height-level];
				double dilute;//???
				dilute=Math.pow(1/2,height-level-1);
				//???
				double levelNum=tree.leafNumOfLocationSubtree(height-level-1)*fullHRS.getRSbyID(i).getSize()*dilute;
				noise+=Math.pow(noisePerNode*levelNum,2);
			}
			noiseFullHRS[i]=noise;
		}
		return noiseFullHRS;
		
	}
	
	//把预算分到各个level
	private double[] setGeometricEps(double budget,int height) {
		double[] geoBudgetByGram=new double[height];
		double total=0;
		double power1=1.0/3.0*(height);
		double power2=1.0/3.0;
		double epsBase=budget*(Math.pow(2,power2)-1)/(Math.pow(2,power1)-1);//epsilo_0
		for(int gram=0;gram<height;gram++) {
			geoBudgetByGram[gram]=epsBase*Math.pow(2,power2*gram);
			total+=geoBudgetByGram[gram];
		}
		return geoBudgetByGram;
	}
	
	private double[] optModelSelection(double[] biasFullHRS, double[] varFullHRS,int height) {
		int combinationNum=(int)Math.pow(2, maxRSId);
		double minError=Double.MAX_VALUE;
		int optIndex=0;
		boolean[][] allBinary=new boolean[combinationNum][];
		for(int i=0;i<combinationNum;i++) {
			allBinary[i]=toBoolean(i,maxRSId);
			double error=computeError(biasFullHRS,varFullHRS,allBinary[i]);
			if(error<minError) {
				minError=error;
				optIndex=i;
			}
		}
		return selectedHRS(allBinary[optIndex]);
	}
	
	//判断选择和未选择？？？
	private boolean[] toBoolean(int decimalIndex, int size) {
		boolean[] boolIndex=new boolean[size];
		String binary=Integer.toBinaryString(decimalIndex);//二进制
		int diffLen=size-binary.length();
		for(int i=(binary.length()-1);i>=0;i--) {
			if(binary.substring(i,i+1).equals("1")) {
				boolIndex[diffLen+i]=true;
			}
		}
		return boolIndex;
	}
	
	//计算error
	private double computeError(double[] biasFullHRS,double[] varFullHRS,boolean[] binaryIndex) {
		double loss = 0;
		double noise = 0;
		for(int i=0; i<binaryIndex.length; i++){
			if(!binaryIndex[i]){ //RS not selected + information loss
				loss += biasFullHRS[i];
			}else{ //RS selected + noise
				noise += varFullHRS[i];
			}
		}
		//maxRSId always included
		noise += varFullHRS[maxRSId];
		
		return noise + loss;
	}
	
	private double[] selectedHRS(boolean[] boolIndex) {
		int selectedSize=0;
		for(int i=0; i<boolIndex.length; i++){
			if(boolIndex[i]){
				selectedSize++;
			}else{
			}
		}
		if (selectedSize >0){
			double[] speedArr = new double[selectedSize+1];
			int index=0;
			for(int i = 0; i<boolIndex.length; i++){
				if(boolIndex[i]){
					speedArr[index] = fullHRSSpeedArr[i];
					index++;
				}
			}
			speedArr[selectedSize] = fullHRSSpeedArr[maxRSId];		
			return speedArr;
		}
		return null;
	}
	
	
	public void printOptHRSforAllHeight(Configure conf, FileIO io, double selectionBudget, double remainBudget){
    	//Step 1: compute true counts of moves in each reference system of full reference systems
    	fullHRSSpeedArr = conf.getFullHRSSpeedArr();
    	System.err.println("selection from full HRS "+conf.getFullHRSSpeedArrStr());
    	RSS fullHRS = new GridRSS(conf.getMinX(),conf.getMaxX(),conf.getMinY(),conf.getMaxY(),fullHRSSpeedArr); 
    	double[] trueRootCounts = rootCounts(conf, io, fullHRS);   	    	
 
    	//Step 2: add noise to noisyRootCountsBin
    	double[] noisyRootCounts = addLapNoise(selectionBudget, trueRootCounts);

    	//Step 3: select largest index M*
    	maxRSId = selectMaxRSIndex(selectionBudget, noisyRootCounts, fullHRS);
    	System.err.println("max RS id:" + maxRSId +" of speed "+fullHRSSpeedArr[maxRSId]);
    	
    	//Step 4: iterate k from maximum height till minimum height (2)
    	for(int optK=2; optK<=5; optK++){
       	    double[] biasFullHRS = estimateFullHRSBias(noisyRootCounts, optK);
       		double[] noiseFullHRS = estimateFullHRSNoise(fullHRS, optK, remainBudget);    		
       		double[] speedArr = optModelSelection(biasFullHRS, noiseFullHRS, optK);  
       		if(speedArr==null){
       			speedArr = new double[1];
       			speedArr[0] = fullHRSSpeedArr[maxRSId];
       		}
       		System.err.println("k"+optK + "\t"+conf.convertSpeedArr(speedArr));
       	}
    }
	
public static void main(String[] args){
		
		//read configuration file
		String configureFilename = "DatasetConfigure.properties";
		System.err.println(configureFilename);
		String mainWorkingDir = System.getProperty("user.dir");
		System.out.println("Working Directory = " + mainWorkingDir);
		
		Configure conf = new Configure(configureFilename, mainWorkingDir);
		//input is full rss 100,200,400,800,1600,3200

		PrivateModelSelection ms = new PrivateModelSelection();
		double[] totalBudgetArr = {10000};
		
        for(int i=0; i<totalBudgetArr.length; i++){
        	double totalBudget = totalBudgetArr[i];
			System.err.println("budget: "+totalBudget);
			ms.printOptHRSforAllHeight(conf, new FileIO(), 0.01*totalBudget, 0.99*totalBudget);
        }
	}
	
}


