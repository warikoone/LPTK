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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.util.Sets;

/**
 * @author neha
 *
 */
public class MultiClassOvOSimulation {

	private Properties systemProperties;
	public LinkedHashMap<String,TreeMap<Integer,TreeMap<Double,Double>>> testInstances;
	public LinkedHashMap<String, TreeMap<Integer,String>> testFeatures;
	public LinkedHashMap<String, TreeMap<Integer,String>> trainingFeatures;
	public TreeMap<String, Integer> testFeatureId;
	public TreeMap<String, Integer> trainingFeatureId;
	
	/**
	 * Constructors
	 * @throws IOException 
	 */
	public MultiClassOvOSimulation() throws IOException {
		this.systemProperties = new Properties();
        InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
        this.testFeatures = new LinkedHashMap<>();
        this.trainingFeatures = new LinkedHashMap<>();
        this.testFeatureId = new TreeMap<>();
        this.trainingFeatureId = new TreeMap<>();
        this.testInstances = new LinkedHashMap<>();
	}
	
	public MultiClassOvOSimulation(LinkedHashMap<String,TreeMap<Integer,TreeMap<Double,Double>>> testInstances,
			LinkedHashMap<String, TreeMap<Integer, String>> testFeatures,
			LinkedHashMap<String, TreeMap<Integer, String>> trainingFeatures, 
			TreeMap<String, Integer> testFeatureId,
			TreeMap<String, Integer> trainingFeatureId) {

		this.testInstances = testInstances;
		this.testFeatures = testFeatures;
		this.trainingFeatures = trainingFeatures;
		this.testFeatureId = testFeatureId;
		this.trainingFeatureId = trainingFeatureId;
	}
	
	private LinkedHashMap<String,TreeMap<Integer,TreeMap<Double,Double>>> loadFeatureInstanceFile(String fileName) 
			throws IOException {
		
		LinkedHashMap<String,TreeMap<Integer,TreeMap<Double,Double>>> featureInstanceMap = new LinkedHashMap<>();
		String lineRead;
		FileReader fileRead = new FileReader(systemProperties.getProperty(fileName));
		BufferedReader buffRead = new BufferedReader(fileRead);
		lineRead = buffRead.readLine();
		while(null != lineRead){
			String[] instanceValue = lineRead.split("\t:");
			TreeMap<Integer, TreeMap<Double,Double>> decoyTreeMap = new TreeMap<>();
			TreeMap<Double,Double> decoySubTreeMap = new TreeMap<>();
			decoySubTreeMap.put(0.0, 0.0);
			decoyTreeMap.put(Integer.parseInt(instanceValue[1].trim()), decoySubTreeMap);
			featureInstanceMap.put(instanceValue[0].trim(), decoyTreeMap);
			lineRead = buffRead.readLine();
		}
		buffRead.close();
		return(featureInstanceMap);
	}

	private LinkedHashMap<String, TreeMap<Integer,String>> loadSVMModelFile(String fileName,
			ArrayList<String> decoyList) 
			throws IOException {
		
		LinkedHashMap<String, TreeMap<Integer,String>> modelInstanceFeature = new LinkedHashMap<>();
		String lineRead;
		FileReader fileRead = new FileReader(systemProperties.getProperty(fileName));
		BufferedReader buffRead = new BufferedReader(fileRead);
		lineRead = buffRead.readLine();
		int index=0;
		while(null != lineRead){
			Integer instanceType = Integer.parseInt(lineRead.split("\\s")[0].trim());
			TreeMap<Integer,String> decoyTreeMap = new TreeMap<>();
			decoyTreeMap.put(instanceType, lineRead);
			if(!decoyList.isEmpty()){
				modelInstanceFeature.put(decoyList.get(index),decoyTreeMap);
			}else{
				modelInstanceFeature.put(String.valueOf(index),decoyTreeMap);
			}
			lineRead = buffRead.readLine();
			index++;
		}
		buffRead.close();
		return(modelInstanceFeature);
	}
	
	private void manageInstances(MultiClassOvOSimulation classInstance) throws IOException {
		
		SortedSet<Integer> instanceSet = new TreeSet<>(Arrays.asList(3,4,5,6,9));
		TreeMap<Integer, ArrayList<String>> triggerWordTreeMap = new TreeMap<>();
		for(Integer instanceType : instanceSet){
			ArrayList<String> decoyList = new ArrayList<>();
			switch (instanceType) {
			case 3:
				//decoyList.addAll(Arrays.asList("regulation","activ"));
				break;
			case 4:
				decoyList.addAll(Arrays.asList("down-regulate","inhibit","reduce","decrease"));
				break;
			case 5:
				decoyList.addAll(Arrays.asList("agonist","inverse agonist"));
				break;
			case 6:
				decoyList.addAll(Arrays.asList("antagonist"));
				break;
			case 9:
				decoyList.addAll(Arrays.asList("enzyme","inhibit","catalyze","synthesis"));
				break;
			}
			triggerWordTreeMap.put(instanceType, decoyList);
		}
		System.out.println("\n\t>>"+triggerWordTreeMap);
		
		LinkedHashMap<String,TreeMap<Integer, String>> originalHashMap = new LinkedHashMap<>();
		String lineRead;
		FileReader fileRead = new FileReader(systemProperties.getProperty("trainingOriginalSentence"));
		BufferedReader buffRead = new BufferedReader(fileRead);
		lineRead = buffRead.readLine();
		while(null != lineRead){
			String[] sentenceSubSection = lineRead.split("\t:");
			Integer instanceType = Integer.parseInt(sentenceSubSection[0].trim());
			String docId = sentenceSubSection[1].trim();
			TreeMap<Integer, String> decoyTreeMap = new TreeMap<>();
			if(originalHashMap.containsKey(docId)){
				decoyTreeMap = originalHashMap.get(docId);
			}
			decoyTreeMap.put(instanceType, sentenceSubSection[2].trim());
			originalHashMap.put(docId, decoyTreeMap);
			lineRead = buffRead.readLine();
		}
		buffRead.close();
		
		Matcher triggerWordMatcher;
		Iterator<Map.Entry<String, TreeMap<Integer,String>>> tier1Itr = classInstance.
				testFeatures.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<String, TreeMap<Integer,String>> tier1MapValue = tier1Itr.next();
			if(originalHashMap.containsKey(tier1MapValue.getKey())){
				Map.Entry<Integer, String> tier1MapEntry = tier1MapValue.getValue().lastEntry();
				if(originalHashMap.get(tier1MapValue.getKey()).containsKey(tier1MapEntry.getKey())){
					if(triggerWordTreeMap.containsKey(tier1MapEntry.getKey())){
						int triggerWordFound = 0;
						//System.out.println("\n\t>>"+triggerWordTreeMap.get(tier1MapEntry.getKey()));
						for(String triggerWord : triggerWordTreeMap.get(tier1MapEntry.getKey())){
							String originalSentence = originalHashMap.get(tier1MapValue.getKey()).get(tier1MapEntry.getKey());
							triggerWordMatcher = Pattern.compile(triggerWord,Pattern.CASE_INSENSITIVE).
									matcher(originalSentence);
							while(triggerWordMatcher.find()){
								triggerWordFound++;
							}
						}
						if(triggerWordFound == 0){
							//System.out.println("\n\tremoved instance>>"+tier1MapValue.getKey()+"\t>>"+tier1MapEntry.getKey());
							classInstance.testInstances.remove(tier1MapValue.getKey());
							tier1Itr.remove();
						}
					}
				}
			}
		}
		
	}
	
	private TreeMap<String, Integer> loadSVMFeatureFile(String fileName) 
			throws IOException {
		
		TreeMap<String, Integer> featureIdMap = new TreeMap<>();
		String lineRead;
		FileReader fileRead = new FileReader(systemProperties.getProperty(fileName));
		BufferedReader buffRead = new BufferedReader(fileRead);
		lineRead = buffRead.readLine();
		while(null != lineRead){
			String[] featureValues = lineRead.split("\\@");
			featureIdMap.put(featureValues[0].trim(), Integer.parseInt(featureValues[1]));
			lineRead = buffRead.readLine();
		}
		buffRead.close();
		return(featureIdMap);
	}
	
	private TreeMap<Integer, TreeMap<Integer,String>> loadOriginalRelationFile() 
			throws IOException {
		
		TreeMap<Integer,TreeMap<Integer,String>> relationMap = new TreeMap<>();
		String lineRead;
		FileReader fileRead = new FileReader(systemProperties.getProperty("relationTrainingFile"));
		BufferedReader buffRead = new BufferedReader(fileRead);
		lineRead = buffRead.readLine();
		int universalGroupCounter = 0;
		while(null != lineRead){
			String[] relationSubParts = lineRead.split("\\t");
			Integer docId = Integer.parseInt(relationSubParts[0]);
			String relationSpecifics = relationSubParts[1].concat("\t"+relationSubParts[4]).
					concat("\t"+relationSubParts[5]);
			int index=0;
			TreeMap<Integer, String> relationSubMap = new TreeMap<>();
			if(relationMap.containsKey(docId)){
				relationSubMap = relationMap.get(docId);
				if(!relationSubMap.keySet().isEmpty()){
					index = relationSubMap.lastKey()+1;
				}
			}
			if(relationSpecifics.startsWith("CPR:3")){
				universalGroupCounter++;
			}
			relationSpecifics = String.valueOf(universalGroupCounter).
					concat("#"+relationSpecifics);
			relationSubMap.put(index, relationSpecifics);
			relationMap.put(docId, relationSubMap);
			lineRead = buffRead.readLine();
		}
		buffRead.close();
		return(relationMap);
	}
	
	
	private void screenForCPRPairs(LinkedHashMap<String, TreeMap<Integer, TreeMap<Double, Double>>> testInstances) throws IOException {
		
		System.out.println("\n\t new >>"+testInstances.size());
		Iterator<Map.Entry<String, TreeMap<Integer, TreeMap<Double, Double>>>> tier1Itr = testInstances.entrySet().iterator();
		TreeMap<Integer, TreeMap<Integer,TreeMap<Double, TreeMap<Integer,Double>>>> cprInstances = new TreeMap<>();
		ArrayList<Integer> finalAcceptedInstances = new ArrayList<>(Arrays.asList(3,4,5,6,9));
		while(tier1Itr.hasNext()){
			Map.Entry<String, TreeMap<Integer, TreeMap<Double, Double>>> tier1MapValue = tier1Itr.next();
			TreeMap<Integer, TreeMap<Double,Double>> predScoreMap = new TreeMap<>();
			TreeMap<Double,Double> subPredScoreMap = new TreeMap<>();
			// rank the votes for each class and normalize the fraction
			for(Map.Entry<Integer, TreeMap<Double, Double>> tier1Entry : tier1MapValue.getValue().entrySet()){
				Double voteCounts = tier1Entry.getValue().lastKey();
				Double acceptanceMeasure = (voteCounts/5.0);
				subPredScoreMap.put(acceptanceMeasure, tier1Entry.getValue().get(voteCounts));
				predScoreMap.put(tier1Entry.getKey(), subPredScoreMap);
			}
			
			List<Map.Entry<Integer, TreeMap<Double,Double>>> predScoreInstanceList = new LinkedList<>(
					predScoreMap.entrySet());
			Collections.sort(predScoreInstanceList, new Comparator<Map.Entry<Integer, TreeMap<Double,Double>>>() {
				// descending order
				@Override
				public int compare(Map.Entry<Integer, TreeMap<Double,Double>> currItem, 
						Map.Entry<Integer, TreeMap<Double,Double>> nextItem) {
					return (nextItem.getValue().lastKey().compareTo(currItem.getValue().lastKey()));
				}
			});
			
			Double thresholdValue = 1.0;
			if((finalAcceptedInstances.contains(predScoreInstanceList.get(0).getKey()))
					&& (predScoreInstanceList.get(0).getValue().lastKey() >= thresholdValue)){
				String[] instanceSubParts =  tier1MapValue.getKey().split("R");
				//System.out.println("\n\t>>"+tier1MapValue.getKey()+"\t>>"+tier1MapValue.getValue());
				Integer docId = Integer.parseInt(instanceSubParts[0].replaceAll("(\\@+\\d+)*","").trim());
				Integer relationId = Integer.parseInt(instanceSubParts[1].trim());
				TreeMap<Integer,TreeMap<Double, TreeMap<Integer,Double>>> decoyTreeMap = new TreeMap<>();
				TreeMap<Double, TreeMap<Integer,Double>> subDecoyTreeMap = new TreeMap<>();
				TreeMap<Integer,Double> subsetDecoyTreeMap = new TreeMap<>();
				Entry<Double, Double> scoreEntry = predScoreInstanceList.get(0).getValue().lastEntry();
				if(cprInstances.containsKey(docId)){
					decoyTreeMap = cprInstances.get(docId);
					if(decoyTreeMap.containsKey(relationId)){
						subDecoyTreeMap = decoyTreeMap.get(relationId);
						//System.out.println("\n\t key >>"+tier1MapValue.getKey());
						//System.out.println("\n\t previous >>"+decoyTreeMap.get(relationId)+"\t>>"+relationId);
						//System.out.println("wromg>>"+docId+"\t>>"+relationId+"\t>>"+predScoreInstanceList.get(0));
					}
				}
				int flag = 0;
				if(!subDecoyTreeMap.isEmpty()){
					if(subDecoyTreeMap.lastKey() > scoreEntry.getKey()){
						flag = 1;
					}
				}
				if(flag == 0){
					subsetDecoyTreeMap.put(predScoreInstanceList.get(0).getKey(), 
							scoreEntry.getValue());
					subDecoyTreeMap.clear();
					subDecoyTreeMap.put(scoreEntry.getKey(), subsetDecoyTreeMap);
				}
				if(subDecoyTreeMap.size() > 1 | subDecoyTreeMap.size()==0 ){
					System.err.println("\n\t major error");
				}
				decoyTreeMap.put(relationId,subDecoyTreeMap);
				cprInstances.put(docId,decoyTreeMap);
			}
		}
		System.out.println("\n\tmax instances>>"+cprInstances.size());
		
		TreeMap<Integer,TreeMap<Integer, String>> originalRelationMap = loadOriginalRelationFile();
		FileWriter fileWS = new FileWriter(systemProperties.getProperty("predictedCPRInstances"));
		BufferedWriter buffWS = new BufferedWriter(fileWS);
		for(Map.Entry<Integer,TreeMap<Integer,TreeMap<Double, TreeMap<Integer,Double>>>> cprMapEntry : cprInstances.entrySet()){
			if(originalRelationMap.containsKey(cprMapEntry.getKey())){
				TreeMap<Integer,TreeMap<Double, TreeMap<Integer,Double>>> decoyMap = cprMapEntry.getValue();
				//System.out.println("\n\t entrySet >>"+decoyMap.keySet());
				//System.out.println("\n\t OriginalentrySet >>"+originalRelationMap.get(cprMapEntry.getKey()));
				TreeMap<Integer,TreeMap<String,TreeMap<Double,Integer>>> transientFilterMap = new TreeMap<>();
				for(Integer relationIndex : decoyMap.keySet()){
					if(originalRelationMap.get(cprMapEntry.getKey()).keySet().contains(relationIndex)){
						String relationSpecifics = originalRelationMap.get(cprMapEntry.getKey()).
								get(relationIndex);
						String[] subRelationSpecifics = relationSpecifics.split("#");
						relationSpecifics = subRelationSpecifics[1].trim();
						Integer instanceIndex = Integer.parseInt(subRelationSpecifics[0].trim());
						TreeMap<String,TreeMap<Double,Integer>> subtransientFilterMap = new TreeMap<>();
						Entry<Double, TreeMap<Integer, Double>> tempDecoyEntry = 
								decoyMap.get(relationIndex).lastEntry();
						Entry<Integer, Double> subTempDecoyEntry = tempDecoyEntry.getValue().lastEntry();
						int flag=0;
						// screen for multiples classes of the same instances
						if(transientFilterMap.containsKey(instanceIndex)){
							//System.err.println("\n\t multiples 2");
							subtransientFilterMap = transientFilterMap.get(instanceIndex);
							Map.Entry<String, TreeMap<Double,Integer>> transEntry = 
									subtransientFilterMap.lastEntry();
							Entry<Double, Integer> subTransEntry = transEntry.getValue().lastEntry();
							if((transEntry.getValue().lastKey() > tempDecoyEntry.getKey())
									//&& (decoyMap.get(relationIndex).get(subTransEntry.getKey()).
										//	get(subTransEntry.getValue()) > subTempDecoyEntry.getValue())
									){
								flag=1;
							}
						}
						if(flag == 0){
							subtransientFilterMap.clear();
							TreeMap<Double, Integer> tempMap = new TreeMap<>();
							tempMap.put(tempDecoyEntry.getKey(), tempDecoyEntry.getValue().lastKey());
							subtransientFilterMap.put(relationSpecifics,tempMap);
						}
						if(subtransientFilterMap.size() > 1){
							System.err.println("\n\t major error in 2");
						}
						transientFilterMap.put(instanceIndex, subtransientFilterMap);
					}
				}
				//System.out.println("\n\t finalMap>>"+transientFilterMap);
				Iterator<Map.Entry<Integer, TreeMap<String,TreeMap<Double,Integer>>>> tier2Itr = 
						transientFilterMap.entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<Integer, TreeMap<String,TreeMap<Double,Integer>>> tier2MapValue = tier2Itr.next();
					if(tier2MapValue.getValue().size() > 1){
						System.err.println("\n\t Erroneous values>>"+tier2MapValue.getValue()+
								"\t>>"+cprMapEntry.getKey());
					}
					Iterator<Map.Entry<String, TreeMap<Double,Integer>>> tier3Itr = 
							tier2MapValue.getValue().entrySet().iterator();
					while(tier3Itr.hasNext()){
						Map.Entry<String, TreeMap<Double,Integer>> tier3MapValue = tier3Itr.next();
						Integer classType = Integer.parseInt(
								tier3MapValue.getKey().split("\t")[0].replaceAll("CPR\\:",""));
						String argText = tier3MapValue.getKey().replaceFirst("CPR\\:\\d+", 
								"CPR:".concat(String.valueOf(tier3MapValue.getValue().
										lastEntry().getValue())));
						if(finalAcceptedInstances.contains(classType)){
							buffWS.write(cprMapEntry.getKey()+"\t"+tier3MapValue.getKey());
							//buffWS.write(cprMapEntry.getKey()+"\t"+argText);
							buffWS.newLine();
						}else if(classType == 10){
							buffWS.write(cprMapEntry.getKey()+"\t"+argText);
							buffWS.newLine();
						}else{
							System.err.println("\n\t Incorrect>>"+tier3MapValue.getKey());
						}
					}
				}
			}
		}
		buffWS.close();
	}

	private void initiateBinaryGrouping(MultiClassOvOSimulation classInstance, 
			ArrayList<Integer> acceptedClassTypes) throws IOException, InterruptedException {
		
		SVM_BinaryClassifierOvO binaryClassInstances = new SVM_BinaryClassifierOvO();
		TreeMap<Integer, Integer> classTypes = new TreeMap<>();
		for(int i=0;i<acceptedClassTypes.size()-1;i++){
			for(int j=i+1;j<acceptedClassTypes.size();j++){
				if(acceptedClassTypes.get(i) > 0){
					classTypes.put(1, acceptedClassTypes.get(i));
					classTypes.put(-1, acceptedClassTypes.get(j));
				}else{
					classTypes.put(1, acceptedClassTypes.get(j));
					classTypes.put(-1, acceptedClassTypes.get(i));
				}
				classInstance = binaryClassInstances.updateReferences(
						classInstance,classTypes);
				FileWriter fileWS = new FileWriter(systemProperties.getProperty("interimScorevalues"));
				BufferedWriter buffWS = new BufferedWriter(fileWS);
				for(Map.Entry<String,TreeMap<Integer, TreeMap<Double, Double>>> mapEntry : classInstance.testInstances.entrySet()){
						buffWS.write("\t key>>"+mapEntry.getKey()+"\t>>"+mapEntry.getValue());
						buffWS.newLine();
				}
				buffWS.close();
			}
			
		}
		screenForCPRPairs(classInstance.testInstances);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MultiClassOvOSimulation classInstance = new MultiClassOvOSimulation();
			classInstance.testInstances = classInstance.loadFeatureInstanceFile("svmTestingFeatureInstances");
			ArrayList<String> instanceIdList = new ArrayList<>(classInstance.testInstances.keySet());
			classInstance.testFeatures = classInstance.loadSVMModelFile("svmTestingFeature",instanceIdList);
			System.out.println("\n\t b4 testInstances>>"+classInstance.testInstances.size());
			System.out.println("\n\t b4 testFeatures>>"+classInstance.testFeatures.size());
			classInstance.manageInstances(classInstance);
			System.out.println("\n\t after testInstances>>"+classInstance.testInstances.size());
			System.out.println("\n\t after testFeatures>>"+classInstance.testFeatures.size());
			instanceIdList = new ArrayList<>();
			classInstance.trainingFeatures = classInstance.loadSVMModelFile("svmTrainingFeature",instanceIdList);
			classInstance.testFeatureId = classInstance.loadSVMFeatureFile("svmTestingFeatureId");
			classInstance.trainingFeatureId = classInstance.loadSVMFeatureFile("svmTrainingFeatureId");
			ArrayList<Integer> acceptedClassTypes = 
					new ArrayList<>(Arrays.asList(9,6,5,4,3,-1));
			Collections.sort(acceptedClassTypes);
			/**
			System.out.println("\n\t testInstances>>"+classInstance.testInstances.size());
			System.out.println("\n\t testFeatures>>"+classInstance.testFeatures.size());
			System.out.println("\n\t trainingFeatures>>"+classInstance.trainingFeatures.size());
			System.out.println("\n\t testFeatureId>>"+classInstance.testFeatureId.size());
			System.out.println("\n\t trainingFeatureId>>"+classInstance.trainingFeatureId.size());
			System.out.println("\n\t acceptedClassTypes>>"+acceptedClassTypes);**/
			classInstance.initiateBinaryGrouping(classInstance, acceptedClassTypes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}