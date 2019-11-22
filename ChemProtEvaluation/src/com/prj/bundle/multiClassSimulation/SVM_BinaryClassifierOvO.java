/**
 * 
 */
package com.prj.bundle.multiClassSimulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author neha
 *
 */
public class SVM_BinaryClassifierOvO {
	
	private Properties systemProperties;
	/**
	 * Constructors 
	 * @throws IOException 
	 */
	public SVM_BinaryClassifierOvO() throws IOException {
		this.systemProperties = new Properties();
        InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
	}
	

	private LinkedHashMap<String, String> replaceFeaturePrefixTag(LinkedHashMap<String, String> featureMap, 
			Integer classValue, TreeMap<String, Integer> selectedFeatureId, 
			LinkedHashMap<String, TreeMap<Integer,String>> selectedInstanceFeature,
			String pattern, String replaceInstance) {

		for(String docId : selectedInstanceFeature.keySet() ){
			Entry<Integer, String> subInstanceFeature = 
					(Entry<Integer, String>) selectedInstanceFeature.get(docId).firstEntry();
			String currFeature = subInstanceFeature.getValue();
			if(classValue == subInstanceFeature.getKey()){
				if(replaceInstance.equals("-1")){
					ArrayList<Integer> replaceVectorId = new ArrayList<>();
					// remove the decoration subtree
					Matcher decorationMatcher = Pattern.
							compile("\\(CLASS\\s+CPR\\d+\\)\\s+").matcher(currFeature);
					if(decorationMatcher.find()){
						//System.err.println("\n\t found>>"+decorationMatcher.group(0));
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
							//currFeature = currFeature.replaceFirst("\\s+1\\:\\d+\\s+", " 1:1 ");
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
		String sptkIpSettings = "-c 2.5 -j 1.5 -t 5 -C T -N 3 -S 1 -d 1 -T 29 -L 0.1";
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

	public MultiClassOvOSimulation updateReferences(MultiClassOvOSimulation classInstance, 
			TreeMap<Integer, Integer> classTypes) throws IOException, InterruptedException {
		
		LinkedHashMap<String,TreeMap<Integer,TreeMap<Double,Double>>> tempInstanceMap = 
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
		testFeatureMap = replaceFeaturePrefixTag(testFeatureMap, classTypes.get(1), tempTestFeatureId, 
				tempTestInstanceFeature, pattern,"1");
		trainFeatureMap = replaceFeaturePrefixTag(trainFeatureMap, classTypes.get(1), tempTrainFeatureId,
				tempTrainInstanceFeature, pattern,"1");
		
		
		// cater to -ve instances next
		pattern = "(|-)\\d+\\s\\|BT\\|";
		testFeatureMap = replaceFeaturePrefixTag(testFeatureMap, classTypes.get(-1), tempTestFeatureId,
				tempTestInstanceFeature, pattern,"-1");
		trainFeatureMap = replaceFeaturePrefixTag(trainFeatureMap, classTypes.get(-1), tempTrainFeatureId,
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
			FileReader fileRead = new FileReader(systemProperties.getProperty("decoySvmAIModel"));
			BufferedReader buffRead = new BufferedReader(fileRead);
			String lineRead = buffRead.readLine();
			int lineCount=0;
			Double classifyThreshold = 1.0;
			while(null != lineRead){
				if(lineCount == 20){
					//System.err.println("\n\t"+lineRead);
					String[] lineSplit = lineRead.split("#");
					if(lineSplit[1].trim().startsWith("threshold")){
						classifyThreshold = Math.abs(Double.parseDouble(lineSplit[0].trim()));
						System.out.println("\n\tthreshold set for >>"+classTypes+"\t>>"+classifyThreshold);
						if(classTypes.get(-1) < 0){
							//classifyThreshold = (0.8*classifyThreshold);
						}else{
							//classifyThreshold = (0.9*classifyThreshold);
						}
						//classifyThreshold = (0.6*classifyThreshold);
						classifyThreshold = 0.6;
						System.out.print("\t updated limit>>"+classifyThreshold);
					}
					break;
				}
				lineCount++;
				lineRead = buffRead.readLine();
			}
			buffRead.close();
			ArrayList<String> instanceDocSet = new ArrayList<>(testFeatureMap.keySet());
			int index=0;
			fileRead = new FileReader(systemProperties.getProperty("decoySvmClassifyPrediction"));
			buffRead = new BufferedReader(fileRead);
			lineRead = buffRead.readLine();
			while(null != lineRead){
				//System.out.println("\n\t>>"+index);
				Double predScore = Double.parseDouble(lineRead.trim());
				// append current +ve scores
				if(tempInstanceMap.containsKey(instanceDocSet.get(index))){
					TreeMap<Integer,TreeMap<Double,Double>> decoyTreeMap = tempInstanceMap.get(instanceDocSet.
							get(index));
					Integer instanceUpdate = 0;
					if(predScore > 0.0){
						instanceUpdate = classTypes.get(1);
					}else{
						instanceUpdate = classTypes.get(-1);
					}
					//for(Integer instanceUpdate : classTypes.values()){
						//if(instanceUpdate > 0){
							if((Math.abs(predScore) >= classifyThreshold )){
								predScore = Math.abs(predScore);
								Double voteUpdate = 0.0;
								TreeMap<Double, Double> decoySubTreeMap = new TreeMap<>(); 
								if(decoyTreeMap.containsKey(instanceUpdate)){
									decoySubTreeMap = decoyTreeMap.get(instanceUpdate);
									voteUpdate = decoySubTreeMap.lastKey();
									predScore = predScore + decoySubTreeMap.get(voteUpdate);
								}
								decoySubTreeMap.clear();
								decoySubTreeMap.put(voteUpdate+1.0, predScore);
								decoyTreeMap.put(instanceUpdate, decoySubTreeMap);
							}
						//}
					//}
					tempInstanceMap.put(instanceDocSet.get(index), decoyTreeMap);
				}
				lineRead = buffRead.readLine();
				index++;
			}
			buffRead.close();
		}else{
			System.err.println("\n\t updateReferences() ~ Critical Error. Abort Execution");
			//System.exit(0);
		}
		
		return(new MultiClassOvOSimulation(tempInstanceMap, tempTestInstanceFeature, tempTrainInstanceFeature, 
				tempTestFeatureId, tempTrainFeatureId));
	}

}
