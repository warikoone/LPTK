/**
 * 
 */
package com.prj.bundle.multiClassSimulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author neha
 *
 */
public class SVM_BinaryClassifierOvA {

	private Properties systemProperties;
	/**
	 * Constructors 
	 * @throws IOException 
	 */
	public SVM_BinaryClassifierOvA() throws IOException {
		
		this.systemProperties = new Properties();
        InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
	}
	
	private LinkedHashMap<String, String> replaceFeaturePrefixTag(LinkedHashMap<String, String> featureMap, 
			Set<Integer> selectedInstances, TreeMap<String, Integer> selectedFeatureId, 
			LinkedHashMap<String, TreeMap<Integer,String>> selectedInstanceFeature,
			String pattern, String replaceInstance) {

		for(String docId : selectedInstanceFeature.keySet() ){
			Entry<Integer, String> subInstanceFeature = 
					(Entry<Integer, String>) selectedInstanceFeature.get(docId).firstEntry();
			String currFeature = subInstanceFeature.getValue();
			if(selectedInstances.contains(subInstanceFeature.getKey())){
				if(replaceInstance.equals("-1")){
					ArrayList<Integer> replaceVectorId = new ArrayList<>();
					/**
					ArrayList<String> decoySubFeature = new ArrayList<>(
							Arrays.asList(currFeature.split("\\|ET\\|")));
					//System.out.println("\n\t>>"+decoySubFeature);
					int subIndex=0;
					while(subIndex < decoySubFeature.size()-1){
						String subTree = decoySubFeature.get(subIndex).
								replaceAll("(|-)\\d+\\s+\\|BT\\|", "").trim();
						//System.out.println("\n\tsubtree>>"+subTree);
						if(selectedFeatureId.containsKey(subTree)){
							replaceVectorId.add(selectedFeatureId.get(subTree));
							//System.out.println("\n\t found>>"+classInstance.testFeatureId.get(subTree));
						}
						subIndex++;
					}**/
					
					// remove the decoration subtree
					Matcher decorationMatcher = Pattern.
							compile("\\(CLASS\\s+CPR\\d+\\)\\s+").matcher(currFeature);
					if(decorationMatcher.find()){
						//System.out.println("\n\t found>>"+decorationMatcher.group(0));
						currFeature = currFeature.replace(decorationMatcher.group(0), "");
						//currFeature = currFeature.replaceAll("\\(RELATION\\s+", "(VBN ");
						if(selectedFeatureId.containsKey(decorationMatcher.group(0).trim())){
							replaceVectorId.add(selectedFeatureId.get(decorationMatcher.group(0).trim()));
							//System.out.println("\n\t found>>"+selectedFeatureId.get(decorationMatcher.group(0).trim()));
						}
					}
					
					// remove the decoration tree vector feature
					if((!replaceVectorId.isEmpty()) && (replaceVectorId.size() >= 1)){
						//System.out.println("\n"+replaceVectorId);
						for(Integer vectorId : replaceVectorId){
							//currFeature = currFeature.replaceFirst(
								//	"\\s+"+String.valueOf(vectorId).concat("\\:\\d+\\s+"), " ");
							//currFeature = currFeature.replaceFirst("\\s+1\\:\\d+\\s+", " 1:2 ");
						}
					}
					/**
					else{
						currFeature = currFeature.replaceFirst(
								"\\s*1\\:\\d+\\s*", " ");
					}**/
				}
				/**
				if(replaceInstance.equals("1")){
					Matcher decorationMatcher = Pattern.
							compile("\\(CLASS\\s+CPR\\d+\\)\\s+").matcher(currFeature);
					if(decorationMatcher.find()){
						//System.out.println("\n\t found>>"+decorationMatcher.group(0));
						currFeature = currFeature.replace(decorationMatcher.group(0), " ");
					}
				}**/
				currFeature = currFeature.replaceFirst(pattern, replaceInstance.concat(" |BT|"));
				/**
				if(instanceType > 0){
					currFeature = currFeature.replaceFirst(
							"CLASS\\s+CPR\\d+", "CLASS CPR".concat(replaceInstance));
				}**/
				featureMap.put(docId, currFeature);
			}
		}
		return(featureMap);
	}
	
	private boolean callSVMFunction() throws IOException, InterruptedException {

		boolean svmExecutionStatus = false;
		int exitCode = -1;
		Process executionProcess;
		BufferedReader buffRead;
		String sptkIpSettings = "-c 2.5 -j 1.5 -t 5 -C V -N 3 -S 1 -d 2 -T 25 -L 0.1";
		String sptkOpSettings = "-v 3 -f 1";
		String modelLearning = systemProperties.getProperty("svmLearnProcedure").concat(" ")
				.concat(sptkIpSettings).concat(" ")
				.concat(systemProperties.getProperty("decoyTrainingFeature")).concat(" ")
				.concat(systemProperties.getProperty("decoySvmAIModel"));
		executionProcess = Runtime.getRuntime().exec(modelLearning);
		exitCode = executionProcess.waitFor();
		if(exitCode == 0){
			buffRead = new BufferedReader(new InputStreamReader(executionProcess.getInputStream()));
			String currentOut = buffRead.readLine();
			System.out.println("\n*****************Learning************************");
			while(currentOut != null){
				System.out.println("\n\t"+currentOut);
				currentOut = buffRead.readLine();
			}
			buffRead.close();
			exitCode = -1;
			String classifyModel = systemProperties.getProperty("svmClassifyProcedure").concat(" ")
					//.concat(sptkOpSettings).concat(" ")
					.concat(systemProperties.getProperty("decoyTestingFeature")).concat(" ")
					.concat(systemProperties.getProperty("decoySvmAIModel")).concat(" ")
					.concat(systemProperties.getProperty("decoySvmClassifyPrediction"));
			executionProcess = Runtime.getRuntime().exec(classifyModel);
			exitCode = executionProcess.waitFor();
			if(exitCode == 0){
				buffRead = new BufferedReader(new InputStreamReader(executionProcess.getInputStream()));
				currentOut = buffRead.readLine();
				System.out.println("\n********************Classification************************");
				while(currentOut != null){
					System.out.println("\n\t"+currentOut);
					currentOut = buffRead.readLine();
				}
				buffRead.close();
				svmExecutionStatus = true;
			}else{
				System.err.println("\n\t callSVMFunction() ~  Incomplete Classification with error code >>"+exitCode);
				svmExecutionStatus = false;
			}
		}else{
			System.err.println("\n\t callSVMFunction() ~  IncompleteSVMModel with error code >>"+exitCode);
			svmExecutionStatus = false;
		}
		return(svmExecutionStatus);
	}

	public MultiClassOvASimulation updateReferences(MultiClassOvASimulation classInstance, 
			Set<Integer> positiveInstances, 
			Set<Integer> negativeInstances) throws IOException, InterruptedException {
		
		LinkedHashMap<String,TreeMap<Integer,Double>> tempInstanceMap = 
				new LinkedHashMap<>(classInstance.testInstances);
		LinkedHashMap<String, TreeMap<Integer,String>> tempTestInstanceFeature = 
				new LinkedHashMap<>(classInstance.testFeatures);
		LinkedHashMap<String, TreeMap<Integer,String>> tempTrainInstanceFeature = 
				new LinkedHashMap<>(classInstance.trainingFeatures);
		TreeMap<String, Integer> tempTestFeatureId = 
				new TreeMap<>(classInstance.testFeatureId);
		TreeMap<String, Integer> tempTrainFeatureId = 
				new TreeMap<>(classInstance.trainingFeatureId);
		
		
		LinkedHashMap<String, String> testFeatureMap = new LinkedHashMap<>();
		LinkedHashMap<String, String> trainFeatureMap = new LinkedHashMap<>();
		// cater to +ve instances first
		String pattern = "(|-)\\d+\\s+\\|BT\\|";
		testFeatureMap = replaceFeaturePrefixTag(testFeatureMap, positiveInstances, tempTestFeatureId, 
				tempTestInstanceFeature, pattern,"1");
		trainFeatureMap = replaceFeaturePrefixTag(trainFeatureMap, positiveInstances, tempTrainFeatureId,
				tempTrainInstanceFeature, pattern,"1");
		
		
		// cater to -ve instances next
		pattern = "(|-)\\d+\\s\\|BT\\|";
		testFeatureMap = replaceFeaturePrefixTag(testFeatureMap, negativeInstances, tempTestFeatureId,
				tempTestInstanceFeature, pattern,"-1");
		trainFeatureMap = replaceFeaturePrefixTag(trainFeatureMap, negativeInstances, tempTrainFeatureId,
				tempTrainInstanceFeature, pattern,"-1");
		
		FileWriter fileWS = new FileWriter(systemProperties.getProperty("decoyTrainingFeature"));
		BufferedWriter buffWS = new BufferedWriter(fileWS);
		for(String currentValue : trainFeatureMap.values()){
			buffWS.write(currentValue);
			buffWS.newLine();
		}
		buffWS.flush();
		fileWS = new FileWriter(systemProperties.getProperty("decoyTestingFeature"));
		buffWS = new BufferedWriter(fileWS);
		for(String currentValue : testFeatureMap.values()){
			buffWS.write(currentValue);
			buffWS.newLine();
		}
		buffWS.close();
		
		boolean svmStatus = callSVMFunction();
		if(svmStatus){
			
			boolean removeInstances = false;
			if((negativeInstances.size()==1) 
					&& negativeInstances.contains(-1)){
				positiveInstances = new HashSet<>(Arrays.asList(1));
				//removeInstances = true;
			}
			/**
			if(!removeInstances){
				for(Integer currInst : positiveInstances){
					if(currInst < 0){
						positiveInstances.remove(currInst);
					}
				}
				System.out.println("\n\t updated posIns>>"+positiveInstances);
			}**/
			ArrayList<String> instanceDocSet = new ArrayList<>(testFeatureMap.keySet());
			int index=0;
			FileReader fileRead = new FileReader(systemProperties.getProperty("decoySvmClassifyPrediction"));
			BufferedReader buffRead = new BufferedReader(fileRead);
			String lineRead = buffRead.readLine();
			TreeMap<Integer,TreeMap<Integer,ArrayList<String>>> instanceClassList = new TreeMap<>();
			int currIndex = 0;
			while(null != lineRead){
				//System.out.println("\n\t>>"+index);
				Double predScore = Double.parseDouble(lineRead);
				/**
				if(removeInstances){
					if(predScore < 0){
						// remove -ve instances
						tempInstanceMap.remove(instanceDocSet.get(index));
						// remove -ve features
						tempTestInstanceFeature.remove(instanceDocSet.get(index));
					}
				}**/
				// append current +ve scores
				
				/**
				if(!instance2.get(index).equalsIgnoreCase(instanceDocSet.get(index))){
					System.err.println("\n\t::incorrect order"+instanceDocSet.get(index)+
							"\t"+instance2.get(index));
				}**/
				if(tempInstanceMap.containsKey(instanceDocSet.get(index))){
					TreeMap<Integer,Double> decoyTreeMap = tempInstanceMap.get(instanceDocSet.get(index));
					for(Integer posInst : positiveInstances){
						if(predScore > 0.6){
							int updateCounter = 1;
							if(decoyTreeMap.containsKey(posInst)){
								// update only the maximum prediction score in instance variable
								if(decoyTreeMap.get(posInst) > predScore){
									updateCounter = 0;
								}
							}
							if(updateCounter == 1){
								decoyTreeMap.put(posInst, predScore);
							}
							TreeMap<Integer, ArrayList<String>> subIndexMap = new TreeMap<>();
							ArrayList<String> decoyArray = new ArrayList<>();
							if(instanceClassList.containsKey(posInst)){
								subIndexMap = instanceClassList.get(posInst);
								if(subIndexMap.containsKey(currIndex)){
									decoyArray = subIndexMap.get(currIndex);
								}
							}
							decoyArray.add(instanceDocSet.get(index));
							if(currIndex != -1){
							}else{
								subIndexMap.put(currIndex,decoyArray);
							}
							instanceClassList.put(posInst, subIndexMap);
						}
					}
					tempInstanceMap.put(instanceDocSet.get(index), decoyTreeMap);
				}
				lineRead = buffRead.readLine();
				index++;
			}
			buffRead.close();
			/**
			if(removeInstances){
				Iterator<Map.Entry<String, TreeMap<Integer, String>>> tier1Itr = 
						tempTrainInstanceFeature.entrySet().iterator();
				while(tier1Itr.hasNext()){
					Map.Entry<String, TreeMap<Integer, String>> tier1MapValue = tier1Itr.next();
					if(negativeInstances.contains(tier1MapValue.getValue().firstKey())){
						tier1Itr.remove();
					}
				}
			}**/
		}else{
			System.err.println("\n\t updateReferences() ~ Critical Error. Abort Execution");
			//System.exit(0);
		}
		
		return(new MultiClassOvASimulation(tempInstanceMap, tempTestInstanceFeature, tempTrainInstanceFeature, 
				tempTestFeatureId, tempTrainFeatureId));
	}

}
