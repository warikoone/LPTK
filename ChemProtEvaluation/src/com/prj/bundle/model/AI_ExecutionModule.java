package com.prj.bundle.model;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JColorChooser;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.Sets;

/**
 * @author neha
 *
 */

public class AI_ExecutionModule extends AI_PatternModel{
	
	private HashMap<String,HashMap<String, Double>> variableDensityEstimatorPerAbstract; 
	private HashMap<String,HashMap<Integer,HashMap<Integer,HashMap<String, HashMap<String, Double>>>>> 
	variableContextProbabilityPerAbstract;
	private HashMap<Integer,HashMap<String, InvariantParameters>> invariantScoreHash;
	private HashMap<Integer,HashMap<Integer, HashMap<String, String>>> invariantBasedPattern;

	public AI_ExecutionModule(){
		variableDensityEstimatorPerAbstract = new LinkedHashMap<>();
		variableContextProbabilityPerAbstract= new LinkedHashMap<>();
		invariantScoreHash = new LinkedHashMap<>();
		invariantBasedPattern = new LinkedHashMap<>();
	}
	
	private ArrayList<String> determineVariables(ArrayList<String> multiDVariables) {
		
		ArrayList<String> hyperVariables = new ArrayList<>();
		for(int i=0;i==0;i++){
			String lastAdd =multiDVariables.get(i);
			for(int j=(i+1);j<multiDVariables.size();j++){
				hyperVariables.add(multiDVariables.get(i).concat("-").concat(multiDVariables.get(j)));
				lastAdd = lastAdd.concat("-").concat(multiDVariables.get(j));
			}
			hyperVariables.add(lastAdd);
		}
		//System.out.println("\n\t::"+hyperVariables);
		return(hyperVariables);
	}
	
	private Double calculateSum(Collection<Integer> inputValues) {
		
		//System.out.println("\n\tcollection>>"+inputValues);
		Integer cumulativeSum=0;
		for(Integer currValue : inputValues){
			cumulativeSum = (cumulativeSum + currValue);
		}
		return(cumulativeSum.doubleValue());
	}

	private HashMap<String, ArrayList<Double>> normalizeVariables(
			HashMap<String, ArrayList<Double>> normalisedValueHolder, 
			LinkedHashMap<String, Integer> abstractFrequencyVariable,Double documentFrequencyVariable) {
		
		if(documentFrequencyVariable == 0.0){
			System.err.println("\n\tALARM");
			documentFrequencyVariable = 1.0;
		}
		Iterator<String> freqVariableKeyItr = abstractFrequencyVariable.keySet().iterator();
		while(freqVariableKeyItr.hasNext()){
			ArrayList<Double> decoyValueArray = new ArrayList<>();
			String freqVariableKey =freqVariableKeyItr.next();
			if(normalisedValueHolder.containsKey(freqVariableKey)){
				//System.out.println("\n\t"+squareVariableKey+"\t"+abstractFrequencyVariable.get(squareVariableKey));
				decoyValueArray = normalisedValueHolder.get(freqVariableKey);
			}
			decoyValueArray.add(((
					abstractFrequencyVariable.get(freqVariableKey)).doubleValue()
					/(documentFrequencyVariable).doubleValue()));
			normalisedValueHolder.put(freqVariableKey,decoyValueArray);
		}
		//System.out.println("\n\t"+normalisedValueHolder);
		return(normalisedValueHolder);
	}
	
	private void calculateVariableDensity(LinkedHashMap<String, Integer> squaredVariable,
			LinkedHashMap<String, Integer> unaryVariable1, LinkedHashMap<String, 
			Integer> unaryVariable2, String variable) {
		
		//System.out.println("\nvariable>>"+variable);
		HashMap<String, ArrayList<Double>> normalisedValueHolder = new LinkedHashMap<>();
		Double squaredVariableDocumentFrequency = calculateSum(squaredVariable.values());
		Double unaryVariable1DocumentFrequency = calculateSum(unaryVariable1.values());
		Double unaryVariable2DocumentFrequency = calculateSum(unaryVariable2.values());
		/**
		System.out.println("\n\t squaredVariableDocumentFrequency::"+squaredVariableDocumentFrequency
				+"\t uniaryVariable1DocumentFrequency::"+uniaryVariable1DocumentFrequency
				+"\t uniaryVariable2DocumentFrequency::"+uniaryVariable2DocumentFrequency);**/
		normalisedValueHolder = normalizeVariables(
				normalisedValueHolder,squaredVariable, squaredVariableDocumentFrequency);
		normalisedValueHolder = normalizeVariables(
				normalisedValueHolder,unaryVariable1, unaryVariable1DocumentFrequency);
		normalisedValueHolder = normalizeVariables(
				normalisedValueHolder,unaryVariable2, unaryVariable2DocumentFrequency);
		Iterator<Map.Entry<String, ArrayList<Double>>> valueHoldItr = normalisedValueHolder.entrySet().iterator();
		HashMap<String, Double> decoyValueHolder = new LinkedHashMap<>();
		while(valueHoldItr.hasNext()){
			Map.Entry<String, ArrayList<Double>> currEntryValue = valueHoldItr.next();
			Double densityValue = (currEntryValue.getValue().get(0)/
					((currEntryValue.getValue().get(1)*currEntryValue.getValue().get(2))));
			decoyValueHolder.put(currEntryValue.getKey(), densityValue);
		}
		variableDensityEstimatorPerAbstract.put(variable, decoyValueHolder);
	}
	
	private ArrayList<Integer> calculateFrameFrequency(String currentFrame,
			LinkedHashMap<String, Set<String>> contextFrameMap,
			int startIndex, int endIndex) throws IOException {
		
		ArrayList<String> splitterArray = new ArrayList<>(Arrays.asList(currentFrame.split("(\\s)+")));
		ArrayList<Integer> returnCount = new ArrayList<>();
		StringBuilder rebuildString = new StringBuilder();
		String decoyString;
		while(startIndex <= endIndex){
			rebuildString.append(splitterArray.get(startIndex).concat(" "));
			startIndex++;
		}
		decoyString = rebuildString.toString().trim();
		//System.out.println("\n\tbefore::"+decoyString);
		decoyString = new LearningFeatureExtractor().reshapeContextFrame(decoyString);
		returnCount.add(decoyString.length());
		//decoyString = decoyString.replaceAll("[\\W&&[^\\s]]*(CHEMICAL)(R\\d+)+[\\W&&[^\\s]]*", "[\\\\W&&[^\\\\s]]*(CHEMICAL)(R\\\\d+)+[\\\\W&&[^s]]*");
		//decoyString = decoyString.replaceAll("[\\W&&[^\\s]]*(GENEPRO)(R\\d+)+[\\W&&[^\\s]]*", "[\\\\W&&[^\\\\s]]*(GENEPRO)(R\\\\d+)+[\\\\W&&[^s]]*");
		//System.out.println("\n\t after::"+decoyString);
		Integer frameCount=0;
		if(contextFrameMap.containsKey(decoyString)){
			//adjust the score based on frame length
			frameCount = (frameCount+contextFrameMap.get(decoyString).size());
			//add adjacency pattern matrix
		}
		/**
		Iterator<Map.Entry<String, Set<String>>> contextMapItr = contextFrameMap.entrySet().iterator();
		while(contextMapItr.hasNext()){
			Map.Entry<String, Set<String>> contextKeyValue = contextMapItr.next();
			frameMatcher = Pattern.compile(decoyString).matcher(contextKeyValue.getKey());
			System.out.println("\n\t after::"+decoyString+"\t"+contextKeyValue.getKey());
			if(decoyString.contentEquals(contextKeyValue.getKey())){
				//adjust the score based on frame length
				frameCount = (frameCount+contextKeyValue.getValue().size());
				break;
				//add adjacency pattern matrix
			}
		}**/
		if(frameCount == 0){
			System.err.println("\n\t calculateFrameFrequency() ~ No matching frame with context frames>>>"
					+decoyString +"\t>>>"+currentFrame);
		}
		returnCount.add(frameCount);
		//System.out.println("\n\t counter::"+returnCount);
		return(returnCount);
	}
	
	private double calculateContextScore(ArrayList<ArrayList<Integer>> frameFrequency) {
		
		int i=0,j=0;
		double fractionValue;
		double entityGramScore =1.0;
		double fringeValue = 0.0000000001;
		for(i=0,j=i+1;(i<(frameFrequency.size()-1))&&(j<(frameFrequency.size()));i++,j++){
			// calculate score only if next element isn't of probability 0
			if(frameFrequency.get(j).get(1) != 0){
				fractionValue = (frameFrequency.get(i).get(1).doubleValue())
						/(frameFrequency.get(j).get(1).doubleValue());
				// verify based on frame length if frames generated are of same size
				// if true reduce the amount by which probability is added as the
				// component isn't variable
				if((fractionValue == 1.0) && (frameFrequency.get(i).get(0) == frameFrequency.get(j).get(0))){
						entityGramScore = entityGramScore*fringeValue;
				}else{
					entityGramScore = entityGramScore*fractionValue;
				}
			}else{
				fractionValue = 0.0;
			}
		}
		//System.out.println(""+entityGramScore+"\t:"+frameFrequency);
		return(entityGramScore);
	}
	
	private double generateContextFrameProbability(String currentFrame, int currentFrameSize,
			String docIdKey, LinkedHashMap<String, Set<String>> contextFrameMap) throws IOException {
		
		int frameSize = 4,recursionDecrementer = 2;
		ArrayList<ArrayList<Integer>> frameFrequency = new ArrayList<>();
		int startIndex=0, endIndex = frameSize;
		//System.out.println("\n\t"+startIndex+"\t"+endIndex+"\t"+currentFrameSize+"\t"+recursionDecrementer);
		while((endIndex >= 0)&&(endIndex <= frameSize)){
			recursionDecrementer--;
			frameFrequency.add(calculateFrameFrequency(currentFrame,contextFrameMap,startIndex,endIndex));
			if(recursionDecrementer < 0){
				recursionDecrementer = 100;
			}
			if(currentFrameSize == 1){
				endIndex = (frameSize - currentFrameSize);
				currentFrameSize = 100;
			}else{
				endIndex = ((frameSize - currentFrameSize)+recursionDecrementer);
			}
			//System.out.println("\n\t"+startIndex+"\t"+endIndex+"\t"+currentFrameSize+"\t"+recursionDecrementer);
		}
		double contextScore = calculateContextScore(frameFrequency);
		//System.out.println("\n\tfreqArray::"+frameFrequency +"\t  >>>>"+frameFrequency.size()+"\t *******"+val);
		return(contextScore);
	}

	private void assembleContextFrameProbability(
			HashMap<Integer,HashMap<Integer, HashMap<String, Set<String>>>> variableFrameMap,
			LinkedHashMap<Integer, LinkedHashMap<String, Set<String>>> decoyContextFrameMap,
			String variable) throws IOException {
		/**
		System.out.println("\n\t>>>>>>>>>>>>>>>>>>>>>>>>");
		Iterator<String> itr = contextFrameMap.keySet().iterator();
		while(itr.hasNext()){
			String str = itr.next();
			System.out.println("\n\t"+str);
		}**/
		Iterator<Map.Entry<Integer, HashMap<Integer,HashMap<String, Set<String>>>>> tier1Itr = 
				variableFrameMap.entrySet().iterator();
		HashMap<Integer,HashMap<Integer,HashMap<String, HashMap<String, Double>>>> tier1Score = 
				new LinkedHashMap<>();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, HashMap<Integer,HashMap<String, Set<String>>>> tier1MapValue = 
					tier1Itr.next();
			int currentFrameSize = tier1MapValue.getKey();
			//if(currentFrameSize == 4){
				Iterator<Map.Entry<Integer,HashMap<String, Set<String>>>> tier2Itr =  
						tier1MapValue.getValue().entrySet().iterator();
				HashMap<Integer,HashMap<String, HashMap<String, Double>>>  tier2Score = 
						new LinkedHashMap<>();
				while(tier2Itr.hasNext()){
					Map.Entry<Integer,HashMap<String, Set<String>>> tier2MapValue = tier2Itr.next();
					int currentInstanceType = tier2MapValue.getKey();
					Iterator<Map.Entry<String, Set<String>>> tier3Itr = 
							tier2MapValue.getValue().entrySet().iterator();
					HashMap<String, HashMap<String, Double>> tier3Score = new LinkedHashMap<>();
					int count=0;
					while(tier3Itr.hasNext()){
						//doc Count
						count++;
						Map.Entry<String, Set<String>> tier3MapValue = tier3Itr.next();
						/**
						if(frameData.getKey().equalsIgnoreCase("1420741@6@7R0")){
							System.out.println("\n\t>>"+frameData.getValue());
						}**/
						ArrayList<Double> patternScore = new ArrayList<>();
						String decoyString;
						HashMap<String, Double> tier4Score = new LinkedHashMap<>();
						LinkedHashMap<String, Set<String>> contextFrameMap = 
								decoyContextFrameMap.get(currentInstanceType);
						for(String currentFrame : tier3MapValue.getValue()){
							//System.out.println("\n\t******************************"+currentFrame+"*******************************"+"\t"+frameData.getKey());
							patternScore.add(generateContextFrameProbability(currentFrame,
									currentFrameSize,tier3MapValue.getKey(),contextFrameMap));
							//generateContextFrameProbability(currentFrame,currentFrameSize,frameData.getKey(),contextFrameMap);
						}
						
						double maxValue = Collections.max(patternScore);
						int patternIndex = patternScore.indexOf(Collections.max(patternScore));
						ArrayList<String> patternArray = new ArrayList<>(tier3MapValue.getValue());
						decoyString = patternArray.get(patternIndex);
						decoyString = new LearningFeatureExtractor().reshapeContextFrame(decoyString);
						//decoyString = decoyString.replaceAll("[\\W&&[^\\s]]*(CHEMICAL)(R\\d+)+[\\W&&[^\\s]]*", "[\\\\W&&[^\\\\s]]*(CHEMICAL)(R\\\\d+)+[\\\\W&&[^s]]*");
						//decoyString = decoyString.replaceAll("[\\W&&[^\\s]]*(GENEPRO)(R\\d+)+[\\W&&[^\\s]]*", "[\\\\W&&[^\\\\s]]*(GENE)(R\\\\d+)+[\\\\W&&[^s]]*");
						tier4Score.put(decoyString, maxValue);
						tier3Score.put(tier3MapValue.getKey(), tier4Score);
						//break;
					}
					//System.out.println("\n\t count::"+count);
					if(!tier3Score.isEmpty()){
						tier2Score.put(currentInstanceType,tier3Score);
					}
				}
				//System.out.println("\n\n\t*************"+currentFrameSize+"***************");
				//System.out.println("\n\t::"+tier2Score.get(currentFrameSize));
			//}
			if(!tier2Score.isEmpty()){
				tier1Score.put(currentFrameSize,tier2Score);
			}	
		}
		variableContextProbabilityPerAbstract.put(variable, tier1Score);
		//System.out.println("\n\t:::"+variableContextProbabilityPerAbstract);
	}
	
	private HashMap<String, Double> generateAdjacencyArray(ArrayList<String> matchedPatternIndices,
			String associationVariable,
			Integer currFrameSize, String pattern, Integer instanceType) {
		
		//System.out.println("\n\t***********"+matchedPatternIndices.size()+"\t&&&&&&&&&&&&&&&"+matchedPatternIndices);
		HashMap<String, Double> adjacencyArray = new LinkedHashMap<>();
		// initialize array
		for(String docId : variableContextProbabilityPerAbstract
				.get(associationVariable).get(currFrameSize).get(instanceType).keySet()){
			adjacencyArray.put(docId, 0.0);
		}
		//fill relevant id's with conf values
		for(String docId: matchedPatternIndices){
			ArrayList<String> matchForIdentical = new ArrayList<>(variableContextProbabilityPerAbstract.
					get(associationVariable).get(currFrameSize).get(instanceType).get(docId).keySet());
			if(matchForIdentical.get(0).contentEquals(pattern)){
				//System.out.println("\n\t>>>>"+docId+"\t\t"+pattern+"\t\t"+matchForIdentical);
				double frameConfValue = variableContextProbabilityPerAbstract
						.get(associationVariable).get(currFrameSize).get(instanceType).get(docId).get(pattern);
				adjacencyArray.put(docId, frameConfValue);
			}
			
		}
		/**
		Iterator<Map.Entry<String, Double>> itr = adjacencyArray.entrySet().iterator();
		while(itr.hasNext()){
			Map.Entry<String, Double> map = itr.next();
			System.out.println("\n\t\t:::"+map.getKey()+"\t\t>>"+map.getValue());
		}**/
		return(adjacencyArray);
	}
	
	/**
	 * Sum up all the non zero values for each row of frame Index
	 * @param associationContextAdjacencyMatrix
	 * @param docId
	 * @param frameIndex
	 * @return
	 */
	private double matrixCoallesing(
			ArrayList<HashMap<Integer, HashMap<String, Double>>> associationContextAdjacencyMatrix,
			String docId, Integer frameIndex) {
		
		double returnSum = 0.0;
		for(int i=0;i<associationContextAdjacencyMatrix.size();i++){
			if(associationContextAdjacencyMatrix.get(i).get(frameIndex).containsKey(docId)){
				returnSum = (returnSum + associationContextAdjacencyMatrix.get(i).get(frameIndex).get(docId));
			}else{
				returnSum = 0.0;
				break;
			}
		}
		return(returnSum);
	}
	
	private HashMap<Integer, Double> evaluatePolynomialCoefficient(
			ArrayList<HashMap<Integer, HashMap<String, Double>>> associationContextAdjacencyMatrix,
			String variable, Integer instanceType) {
		
		int i=0;
		ArrayList<Integer> frameSize = new ArrayList<>(associationContextAdjacencyMatrix.get(i).keySet());
		HashMap<Integer, Double> associationContextMatrix = new LinkedHashMap<>();
		for(Integer frameIndex : frameSize){
			ArrayList<String> docIdList = 
					new ArrayList<>(associationContextAdjacencyMatrix.get(i).get(frameIndex).keySet());
			//HashMap<String, Double> decoyHash = new LinkedHashMap<>();
			double frameValueSummation = 0.0;
			for(String docId : docIdList){
				// combine the corresponding values from all the variable combinations
				double summationValue = matrixCoallesing(associationContextAdjacencyMatrix,docId,frameIndex); 
				if(summationValue != 0.0){
					double amplificationValue = 1.0;
					/**
					if(instanceType == 1){
						String densityDocId  = docId.replaceAll("(\\@(\\d)+)+", "").trim();
						amplificationValue = variableDensityEstimatorPerAbstract.
								get(variable).get(densityDocId).doubleValue();
					}else{
						amplificationValue = 1.0;
					}**/
					
					//System.out.println("\t>>"+docId.split("@")[0]+"\t\t\t::"+(summationValue*amplificationValue));
					frameValueSummation = (frameValueSummation + (summationValue*amplificationValue));
				}
			}
			associationContextMatrix.put(frameIndex, frameValueSummation);
		}
		
		
		List<Map.Entry<Integer, Double>> relationCompareList = new LinkedList<>(associationContextMatrix.entrySet());
		Collections.sort(relationCompareList, new Comparator<Map.Entry<Integer, Double>>() {
			// descending order
			@Override
			public int compare(Map.Entry<Integer, Double> currItem, Map.Entry<Integer, Double> nextItem) {
				return (nextItem.getValue().compareTo(currItem.getValue()));
			}
		});
		associationContextMatrix = new LinkedHashMap<>();
		associationContextMatrix.put(relationCompareList.get(0).getKey(), relationCompareList.get(0).getValue());
		
		return(associationContextMatrix);
	}
	
	private InvariantParameters evaluateInvarianceFunctional(
			HashMap<String, HashMap<Integer, Double>> decoyHash,
			ArrayList<String> hyperVariables, String docId, Integer instanceType) {

		double p11Sq = 0.0,p20Sq = 0.0,p02Sq=0.0;
		int selectedIndex =0;
		for(String variable : hyperVariables){
 			int frameIndex = (decoyHash.get(variable).keySet().iterator().next());
			switch (variable) {
			case "RELATION-CHEMICAL-GENEPRO":
				p11Sq = Math.pow(decoyHash.get(variable).get(frameIndex),2.0);
				selectedIndex = frameIndex;
				break;
			case "RELATION-CHEMICAL":
				p20Sq = Math.pow(decoyHash.get(variable).get(frameIndex),2.0);
				break;
			case "RELATION-GENEPRO":
				p02Sq = Math.pow(decoyHash.get(variable).get(frameIndex),2.0);
				break;
			}
		}
		LinkedHashMap<String,String> patternHash = new LinkedHashMap<>();
		double polyEval = (p20Sq + (p11Sq/2) + p02Sq);
		// select the pattern from the all the 3 variable based context
		Set<String> variableSet = new LinkedHashSet<>(Arrays.asList("RELATION-CHEMICAL-GENEPRO".split("-")));
		for(String variable : variableSet){
			if(variableContextProbabilityPerAbstract.
					get(variable).get(selectedIndex).containsKey(instanceType)){
				patternHash.put(variable,
						variableContextProbabilityPerAbstract.
						get(variable).get(selectedIndex).get(instanceType).get(docId).keySet().iterator().next());
			}
		}
		return(new InvariantParameters(selectedIndex, polyEval, patternHash));
	}	

	private void evaluateDataPointInvariance(ArrayList<String> hyperVariables, 
			TreeMap<Integer,HashMap<String,ArrayList<String>>> sentenceBundle,
			LinkedHashMap<Integer, LinkedHashMap<String, Set<String>>> decoyContextFrameMap) {
		
		for(Integer instanceType : sentenceBundle.descendingKeySet()){
			//if(instanceType == -1){
			LinkedHashMap<String, Set<String>> contextFrameMap = 
					new LinkedHashMap<>(decoyContextFrameMap.get(instanceType));
			HashMap<String, InvariantParameters> docDecoyHash = 
					new LinkedHashMap<>();
			for(String docId : sentenceBundle.get(instanceType).keySet()){
				//String docId = "354896@0R0";//"1378968@10@11R2";//"3800626@3";//"1967484@0";//"435349@0";//"7881871@1";//"6287825@5";//
				// manage the pool of generated list according to instance type
				HashMap<String,HashMap<Integer, Double>> decoyHash = new LinkedHashMap<>();
				for(String variable : hyperVariables){
				//String variable = hyperVariables.get(0);
					Set<String> variableSet = new LinkedHashSet<>(Arrays.asList(variable.split("-")));
					//System.out.println("\n\t>>>"+variableSet);
					ArrayList<HashMap<Integer,HashMap<String,Double>>> associationContextAdjacencyMatrix = 
							new ArrayList<>();
					for(String associationVariable : variableSet){
					//String associationVariable = new ArrayList<>(variableSet).get(0);
					//System.out.println("\n\t>>>"+associationVariable);
						Iterator<Integer> frameSizeItr = variableContextProbabilityPerAbstract
								.get(associationVariable).keySet().iterator();
						HashMap<Integer,HashMap<String,Double>> contextAdjacencyMatrix = new LinkedHashMap<>();
						while(frameSizeItr.hasNext()){
							Integer currFrameSize = frameSizeItr.next();
							//if(currFrameSize == 5){
							// inserted due to absence of "RELATION" tag in negative instances
							if(variableContextProbabilityPerAbstract.
									get(associationVariable).get(currFrameSize).containsKey(instanceType)){
								if(variableContextProbabilityPerAbstract.
										get(associationVariable).get(currFrameSize).get(instanceType).containsKey(docId)){
									/**
									System.out.println("\n\t>>"+instanceType+"\t>>"+docId+"\n\t>>>"+variableContextProbabilityPerAbstract.
										get(associationVariable).get(currFrameSize).get(instanceType));**/	
									for(String pattern : variableContextProbabilityPerAbstract.
											get(associationVariable).get(currFrameSize).get(instanceType).
											get(docId).keySet()){
										ArrayList<String> matchedPatternIndices = 
												new ArrayList<>(contextFrameMap.get(pattern));
										//generate adjacency matrix for context frame of each size
										contextAdjacencyMatrix.put(currFrameSize,
												generateAdjacencyArray(matchedPatternIndices,associationVariable,
														currFrameSize, pattern, instanceType));
									}
								}
							}
							//}
						}
						// add pattern adjacency matrices for different size frames
						if(!contextAdjacencyMatrix.isEmpty()){
							if(contextAdjacencyMatrix.size()!= 5){
								System.err.println("\n\twrong matrix>>"+docId+"\n\t>>"+contextAdjacencyMatrix.keySet());
								System.exit(0);
							}
							associationContextAdjacencyMatrix.add(contextAdjacencyMatrix);
						}
					}
					if(!associationContextAdjacencyMatrix.isEmpty()){
						decoyHash.put(variable,
								evaluatePolynomialCoefficient(associationContextAdjacencyMatrix, 
										variable,instanceType));
					}else{
						System.err.println("\n\t>>"+variable+" not present in "+docId);
					}
				}
				if(!decoyHash.isEmpty()){
					docDecoyHash.put(docId,evaluateInvarianceFunctional(decoyHash,hyperVariables,docId,instanceType));
				}
			//}
				if(!docDecoyHash.isEmpty()){
					invariantScoreHash.put(instanceType,docDecoyHash);
				}
			}
		}
		
		//System.out.println("\n\t>>"+invariantScoreHash.size());
		//System.out.println("\n\t>>"+invariantScoreHash.get("1420741@6@7R0").getInvariantPolynomialValue());
	}
	
	/**
	 * arrange the obtained patterns in descending AInvar score
	 * hashOutPolynomialScores()
	 * @param negativeInstances
	 * @return
	 */
	
	private HashMap<Integer, HashMap<Double, ArrayList<String>>> hashOutPolynomialScores() {
		
		HashMap<Integer,HashMap<Double, ArrayList<String>>> instanceScoreHash = new LinkedHashMap<>();
		InvariantParameters invariantInstance = new InvariantParameters();
		Iterator<Map.Entry<Integer,HashMap<String,InvariantParameters>>> instanceHashItr = 
				invariantScoreHash.entrySet().iterator();
		// categorize the docid's for each of the instance based on AIV scores
		while(instanceHashItr.hasNext()){
			Map.Entry<Integer,HashMap<String,InvariantParameters>> instanceHashMapValue = 
					instanceHashItr.next();
			Iterator<Map.Entry<String, InvariantParameters>> scoreHashItr = 
					instanceHashMapValue.getValue().entrySet().iterator();
			while(scoreHashItr.hasNext()){
				Map.Entry<String, InvariantParameters> scoreHashMapValue = scoreHashItr.next();
				invariantInstance = invariantScoreHash.get(instanceHashMapValue.getKey())
						.get(scoreHashMapValue.getKey());
				ArrayList<String> docList = new ArrayList<>();
				HashMap<Double, ArrayList<String>> decoyScoreHash = new LinkedHashMap<>();
				if(instanceScoreHash.containsKey(instanceHashMapValue.getKey())){
					decoyScoreHash = instanceScoreHash.get(instanceHashMapValue.getKey());
					if(decoyScoreHash.containsKey(invariantInstance.getInvariantPolynomialValue())){
						docList = decoyScoreHash.get(invariantInstance.getInvariantPolynomialValue());
					}
				}
				docList.add(scoreHashMapValue.getKey());
				decoyScoreHash.put(invariantInstance.getInvariantPolynomialValue(),docList);
				instanceScoreHash.put(instanceHashMapValue.getKey(), decoyScoreHash);
			}
		}
		//arrange the docId instances based on descending order of their scores
		for(Integer intstanceType : instanceScoreHash.keySet()){
			ArrayList<Double> decoyScoreHolder = new ArrayList<>(instanceScoreHash.get(intstanceType).keySet());
			Collections.sort(decoyScoreHolder,Collections.reverseOrder());
			HashMap<Double, ArrayList<String>> orderedScoreHash = new LinkedHashMap<>();
			Iterator<Double> scoreArrayItr = decoyScoreHolder.iterator();
			while(scoreArrayItr.hasNext()){
				Double scoreValue = scoreArrayItr.next();
				//System.out.println("\n\t>>"+scoreValue+"\t\t>>>>"+decoyScoreHash.get(scoreValue));
				orderedScoreHash.put(scoreValue,instanceScoreHash.get(intstanceType).get(scoreValue));
			}
			//System.out.println("\n\t>>>"+orderedScoreHash.size());
			instanceScoreHash.put(intstanceType, orderedScoreHash);
		}
		
		return(instanceScoreHash);
	}
	
	/**
	 * Equal indices distribution
	 * @param stringText
	 * @param stringIndex
	 * @param frameSize
	 * @return
	 */
	
	private ArrayList<String> fillerIndices(ArrayList<String> stringText, int stringIndex, int frameSize) {
		
		ArrayList<String> returnPatternText = new ArrayList<>();
		if(stringIndex >= frameSize){
			frameSize = stringIndex+1;
		}
		int prefixFillerWindowSize = frameSize - (stringIndex + 1);
		int suffixFillerWindowSize = frameSize - (stringText.size()- stringIndex);
		if(prefixFillerWindowSize > 0){
			for(int i=0;i<prefixFillerWindowSize;i++){
				returnPatternText.add("#");
			}
		}
		for(int i=0;i<stringIndex+1;i++){
			returnPatternText.add(stringText.get(i));
		}
		for(int i=(stringIndex+1);i<stringText.size();i++){
			returnPatternText.add(stringText.get(i));
		}
		if(suffixFillerWindowSize > 0){
			for(int i=0;i<suffixFillerWindowSize;i++){
				returnPatternText.add("#");
			}
		}
		//System.out.println("\n\t returnPatternText >>"+returnPatternText);
		return(returnPatternText);
	}
	
	private HashMap<ArrayList<Integer>,Integer> scoreMatrixCell(int[][] dynamicScoreMatrix, int rowIndex, int columnIndex, int incrementVal) {
		
		HashMap<ArrayList<Integer>,Integer> tripleScore = new HashMap<>();
		ArrayList<Integer> cellIndex = new ArrayList<>(Arrays.asList((rowIndex-1),(columnIndex-1)));
		tripleScore.put(cellIndex,dynamicScoreMatrix[rowIndex-1][columnIndex-1]+incrementVal);
		cellIndex = new ArrayList<>(Arrays.asList((rowIndex-1),(columnIndex)));
		tripleScore.put(cellIndex,dynamicScoreMatrix[rowIndex-1][columnIndex]-2);
		cellIndex = new ArrayList<>(Arrays.asList((rowIndex),(columnIndex-1)));
		tripleScore.put(cellIndex,dynamicScoreMatrix[rowIndex][columnIndex-1]-2);
		int maxValue = Collections.max(tripleScore.values());
		cellIndex = new ArrayList<>();
		Iterator<Map.Entry<ArrayList<Integer>, Integer>> scoreItr = tripleScore.entrySet().iterator();
		while(scoreItr.hasNext()){
			Map.Entry<ArrayList<Integer>, Integer> scoreValue = scoreItr.next();
			if((dynamicScoreMatrix[scoreValue.getKey().get(0)][scoreValue.getKey().get(1)]==(maxValue+2)) || 
					(dynamicScoreMatrix[scoreValue.getKey().get(0)][scoreValue.getKey().get(1)]==(maxValue-incrementVal))){
				cellIndex.add(dynamicScoreMatrix[scoreValue.getKey().get(0)][scoreValue.getKey().get(1)]);
			}
		}
		//System.out.println("\n\t tripleScore>>"+tripleScore+"\t\t"+maxValue);
		//System.out.println("\n\t>>"+Collections.max(cellIndex));
		scoreItr = tripleScore.entrySet().iterator();
		while(scoreItr.hasNext()){
			Map.Entry<ArrayList<Integer>, Integer> scoreValue = scoreItr.next();
			if((scoreValue.getValue() != maxValue)||
					(dynamicScoreMatrix[scoreValue.getKey().get(0)][scoreValue.getKey().get(1)] 
							< Collections.max(cellIndex))){
				scoreItr.remove();
			}
		}
		return(tripleScore);
	}
	
	private HashMap<String, Double> dynamicAlignment(ArrayList<String> patternText, ArrayList<String> matchText,
			int start, int end, HashMap<String, Double> eminentPattern) {
		
		int matrixSize = (end-start)+1;
		int dynamicScoreMatrix[][] = new int[matrixSize][matrixSize];
		HashMap<Integer,HashMap<Integer,ArrayList<Integer>>> dynamicTraceArray = new LinkedHashMap<>();
		HashMap<Integer,HashMap<Integer,LinkedList<String>>> patternTraceArray = new LinkedHashMap<>();
		HashMap<ArrayList<Integer>,Integer> tripleScore = new HashMap<>();
		//initialize vectors
		int stepValue=0;
		for(int i=0;i<matrixSize;i++){
			dynamicScoreMatrix[i][0] = stepValue ;
			dynamicScoreMatrix[0][i] = stepValue ;
			stepValue = (stepValue-2);
		}
		HashMap<Integer,ArrayList<Integer>> decoyTraceArray = new LinkedHashMap<>();
		for(int i=1;i<matrixSize;i++){
			decoyTraceArray.put(i, new ArrayList<>(Arrays.asList(0,i-1)));
		}
		dynamicTraceArray.put(0,decoyTraceArray);
		HashMap<Integer,LinkedList<String>> decoyPatternArray = new LinkedHashMap<>();
		decoyPatternArray.put(0,new LinkedList<>(Arrays.asList("#","#")));
		for(int mStart=start,i=1;mStart<end;i++,mStart++){
			decoyPatternArray.put(i,new LinkedList<>(Arrays.asList("#",matchText.get(mStart))));
		}
		patternTraceArray.put(0,decoyPatternArray);
		
		//start comparison
		int incrementVal = 1;
		int pStart = start;
		for(int i=1;i<matrixSize;i++){
			int mStart = start;
			decoyTraceArray = new LinkedHashMap<>();
			decoyPatternArray = new LinkedHashMap<>();
			decoyTraceArray.put(0, new ArrayList<>(Arrays.asList(i-1,0)));
			decoyPatternArray.put(0,new LinkedList<>(Arrays.asList(patternText.get(pStart),"#")));
			for(int j=1;j<matrixSize;j++){
				//System.out.println("\n\t"+patternText.get(pStart)+"\t\t"+matchText.get(mStart));
				if((patternText.get(pStart).contentEquals(matchText.get(mStart)))||
						((patternText.get(pStart).equals(".")) || (matchText.get(mStart).equals(".")))){
					incrementVal = 1;
					tripleScore = scoreMatrixCell(dynamicScoreMatrix, i, j, incrementVal);
					dynamicScoreMatrix[i][j] = tripleScore.entrySet().iterator().next().getValue();
					decoyTraceArray.put(j,tripleScore.entrySet().iterator().next().getKey());
					decoyPatternArray.put(j,new LinkedList<>(
							Arrays.asList(patternText.get(pStart),matchText.get(mStart))));
				}else{
					incrementVal = -1;
					tripleScore = scoreMatrixCell(dynamicScoreMatrix, i, j, incrementVal);
					dynamicScoreMatrix[i][j] = tripleScore.entrySet().iterator().next().getValue();
					decoyTraceArray.put(j,tripleScore.entrySet().iterator().next().getKey());
					decoyPatternArray.put(j,new LinkedList<>(
							Arrays.asList(patternText.get(pStart),matchText.get(mStart))));
				}
				mStart++;
				//System.out.println("\n\t>>>>"+i+"\t"+j+"\t"+tripleScore);
			}
			dynamicTraceArray.put(i,decoyTraceArray);
			patternTraceArray.put(i,decoyPatternArray);
			pStart++;
		}
		
		/**
		for(int i=0;i<matrixSize;i++){
			System.out.println("\nrow"+i);
			for(int j=0;j<matrixSize;j++){
				System.out.print("\t\t"+j+" :"+dynamicScoreMatrix[i][j]);
			}
		}
		for(int i=0;i<matrixSize;i++){
			System.out.println("\nrow"+i);
			for(int j=0;j<matrixSize;j++){
				System.out.print("\t\t"+j+" :"+dynamicTraceArray.get(i).get(j));
			}
		}**/
		
		// traceback the alignment
		int rowIndex=1,colIndex=1,i,j;
		Integer patternScore=0;
		String patternBuilder="";
		for(i=(matrixSize-1),j=(matrixSize-1);(rowIndex>0 || colIndex>0);){
			//System.out.println("\n\t DTB >>"+dynamicTraceArray.get(i).get(j)+"\t"+i+"\t"+j);
			rowIndex = dynamicTraceArray.get(i).get(j).get(0);
			colIndex = dynamicTraceArray.get(i).get(j).get(1);
			patternScore = (patternScore + dynamicScoreMatrix[i][j]);
			String pattern = patternTraceArray.get(i).get(j).get(0);
			String match = patternTraceArray.get(i).get(j).get(1);
			//System.out.println("\n\t pattern >>"+pattern+"\t\t match>>"+match);
			if(rowIndex == i){
				// insert row gap
				//System.out.println("\trow gap");
				if(!match.contentEquals("#")){
					patternBuilder = ".".concat(" ").concat(patternBuilder);
				}
			}else if(colIndex== j){
				// insert column gap
				//System.out.println("\tcolumn gap");
				if(!pattern.contentEquals("#")){
					patternBuilder = ".".concat(" ").concat(patternBuilder);
				}
			}else{
				// match or mismatch with straight alignment
				if(pattern.contentEquals(match)){
					// complete match
					if((!pattern.contentEquals("#")) && (!match.contentEquals("#"))){
						//System.out.println("\t content match");
						patternBuilder = pattern.concat(" ").concat(patternBuilder);
					}
				}else if((pattern.equals(".")) || (match.equals("."))){
					// generic match
					//System.out.println("\tgeneric match");
					patternBuilder = ".".concat(" ").concat(patternBuilder);
				}else{
					// mismatch
					if(pattern.contentEquals("#")){
						//System.out.println("\t pattern");
						patternBuilder = match.concat(" ").concat(patternBuilder);
					}else if(match.contentEquals("#")){
						//System.out.println("\t match");
						patternBuilder = pattern.concat(" ").concat(patternBuilder);
					}else{
						//System.out.println("\t mismatch");
						patternBuilder = ".".concat(" ").concat(patternBuilder);
					}
				}
			}
			i = rowIndex;
			j = colIndex;
			//System.out.println("\n\t string>>"+patternBuilder);
		}
		double score = 0.0;
		String patternString = "";
		if(!eminentPattern.isEmpty()){
			score = score + (eminentPattern.entrySet().iterator().next().getValue());
			patternString = eminentPattern.entrySet().iterator().next().getKey().concat(" "); 
			eminentPattern = new LinkedHashMap<>();
		}
		score = score + patternScore.doubleValue();
		patternString = patternString.concat(patternBuilder.toString().trim());
		eminentPattern.put(patternString.trim(),score);
		//System.out.println("\n\t>>"+eminentPattern);
		return(eminentPattern);
	}

	private HashMap<String, Double> alignPatterns(ArrayList<String> patternText,
			ArrayList<String> matchText, int patternIndex,
			int matchIndex) {
		
		HashMap<String, Double> eminentPattern = new LinkedHashMap<>();
		//System.out.println("\n\t pattern>>"+patternText);
		// add suffix and prefix for enabling alignment
		patternText = fillerIndices(patternText,patternIndex,5);
		//System.out.println("\n\t match>>"+matchText);
		matchText = fillerIndices(matchText,matchIndex,5);
		//StringBuilder patternString = new StringBuilder();
		//double patternScore=0.0;
		//prefixPattern
		// call for prefix alignment from the central context term
		eminentPattern = dynamicAlignment(patternText,matchText,0,
				(Math.round(patternText.size()/2)),eminentPattern);
		//keyPattern
		double score = 0.0;
		String patternString = "";
		if(!eminentPattern.isEmpty()){
			score = score + (eminentPattern.entrySet().iterator().next().getValue());
			patternString = eminentPattern.entrySet().iterator().next().getKey().concat(" ");
			eminentPattern = new LinkedHashMap<>();
		}
		score = score + 1.0;
		String modifyTerm = patternText.get(Math.round(patternText.size()/2));
		/**
		if(modifyTerm.contains("CHEMICAL")){
			modifyTerm = "PCHEMICAL";
		}else if(modifyTerm.contains("GENE")){
			modifyTerm = "PGENE";
		}**/
		patternString = patternString.concat(modifyTerm);
		eminentPattern.put(patternString.trim(),score);
		// call for suffix alignment from the central context term
		eminentPattern = dynamicAlignment(patternText,matchText,
				(Math.round(patternText.size()/2)+1),patternText.size(),eminentPattern);
		/**
		for(int i=0,j=0;i < patternText.size();i++,j++){
			//System.out.println("\n\t match index>>"+matchText.get(j)+"\t\t pattern index>>"+patternText.get(i));
			if(matchText.get(j).contentEquals(patternText.get(i))){
				//System.out.println("\n\t match");
				if((!patternText.get(i).contentEquals("#"))&&
						(!matchText.get(j).contentEquals("#"))){
					//perfect match case
					patternScore = (patternScore + 1.0);
					patternString.append(patternText.get(i).concat(" "));
				}
			}else{
				//System.out.println("\n\t no match");
				if((!patternText.get(i).contentEquals("#"))&&
					(!matchText.get(j).contentEquals("#"))){
					//total mismatch case
					patternScore = (patternScore - 0.05);
					patternString.append(".".concat(" "));
				}else if((patternText.get(i).contentEquals("#")) |
						(matchText.get(j).contentEquals("#"))){
					//insertion case
					patternScore = (patternScore + 0.01);
					if(patternText.get(i).contentEquals("#")){
						patternString.append(matchText.get(i).concat(" "));
					}else{
						patternString.append(patternText.get(i).concat(" "));
					}
				}
			}
		}**/
		//eminentPattern.put(patternString.toString().trim(),patternScore);
		return(eminentPattern);
	}

	/**
	 * Identify the key terms against their representations in candidate sentences
	 * CHEMICALPRI ~ CHEMICAL / GENEPROPRI ~ GENEPRO / RELATION ~ RELATION 
	 * @param term
	 * @param stringText
	 * @return
	 */
	private ArrayList<Integer> addTermIndex(String term, ArrayList<String> stringText) {
		
		ArrayList<Integer> termIndex = new ArrayList<>();
		for(int i=0;i<stringText.size();i++){
			if(Pattern.compile(term).matcher(stringText.get(i)).find()){
				if((term.length() != stringText.get(i).length()) || (term.equalsIgnoreCase("RELATION"))){
					termIndex.add(i);
				}
			}
		}
		//System.out.println("\n\tTerma iNdex"+termIndex);
		return(termIndex);
	}
	
	private HashMap<String, String> compareTermAlignments(
			HashMap<String, String> seedDominantPattern, String docId2, 
			Integer instanceType) {
		
		HashMap<String, String> dominatingPattern = new LinkedHashMap<>();
		Iterator<String> termItr = invariantScoreHash.get(instanceType)
				.get(docId2).getContextualFrames().keySet().iterator();
		while(termItr.hasNext()){
			String term = termItr.next();
			String seedTerm = seedDominantPattern.get(term);
			//seedTerm = seedTerm.replaceAll("\\,|\\;|\\?", "");
			ArrayList<String> patternText = 
					new ArrayList<>(Arrays.asList(seedTerm.split(" ")));
			ArrayList<String> matchText = new ArrayList<>();
			// change the pattern or matching text preference based on size
			// this is simply to adjust the insertion/deletion
			String currentContextPattern = invariantScoreHash.get(instanceType)
					.get(docId2).getContextualFrames().get(term);
			//currentContextPattern = currentContextPattern.replaceAll("\\,|\\;|\\?", "");
			if(patternText.size() > currentContextPattern.split(" ").length){
				matchText = patternText;
				patternText = new ArrayList<>(Arrays.asList(currentContextPattern.split(" ")));
			}else{
				matchText = new ArrayList<>(Arrays.asList(currentContextPattern.split(" ")));
			}
			//System.out.println("\n\t\t term ----"+term+"\t\tpoatternText-----"+patternText+"\t\tmatch text----"+matchText);
			ArrayList<Integer> patternIndex = addTermIndex(term,patternText);
			ArrayList<Integer> matchIndex = addTermIndex(term,matchText);
			ArrayList<HashMap<String, Double>> dominatingTerm = new ArrayList<>();
			//if(patternText.size() == matchText.size()){
			for(int i=0;i<patternIndex.size();i++){
				for(int j=0;j<matchIndex.size();j++){
					dominatingTerm.add(
							alignPatterns(patternText,matchText,patternIndex.get(i),matchIndex.get(j)));
					//System.out.println("\n\t Score:-"+dominatingTerm.get(dominatingTerm.size()-1));
				}
			}
			
			// if multiple Chemical or gene entries in the sentence then based 
			// on best score select the alignment
			if(dominatingTerm.size() > 1){
				ArrayList<Double> patternScoreArray = new ArrayList<>();
				for(int i=0;i<dominatingTerm.size();i++){
					Map.Entry<String, Double> mapValue = dominatingTerm.get(i).entrySet().iterator().next();
					patternScoreArray.add(mapValue.getValue());
				}
				double maxValue = Collections.max(patternScoreArray);
				dominatingTerm = new ArrayList<>(Arrays.asList(dominatingTerm.get(patternScoreArray.indexOf(maxValue))));
			}
			// store patterns for each context type
			dominatingPattern.put(term, dominatingTerm.get(0).keySet().iterator().next());
		}
		/**
		for(String str : dominatingPattern.keySet()){
			System.out.println("\tkeys>>"+str+"\t\t patterns>>:-"+dominatingPattern.get(str));
		}**/
		return(dominatingPattern);
	}

	public ArrayList<Integer> findKeyTermIndex(String patternToken, String pattern) {
		
		if(patternToken.equalsIgnoreCase("RELATION")){
			patternToken = patternToken.concat("\\d+");
		}else{
			patternToken = patternToken.concat("PRI");
		}
		ArrayList<String> patternArray = new ArrayList<>(Arrays.asList(pattern.split(" ")));
		//System.out.println("\n\tpatternArray>>"+patternArray);
		ArrayList<Integer> patternIndex = new ArrayList<>();
		for(int i=0;i<patternArray.size();i++){
			if(patternArray.get(i).matches(patternToken)){
				//System.out.println("\n\t patternToken>>"+patternToken+"\t>>"+i);
				patternIndex.add(i);
			}
		}
		//System.out.println("\n\t>>"+patternToken+"\t>>"+patternIndex);
		return(patternIndex);
	}
	
	private double compareContextPatterns(ArrayList<String> reframeTokens, ArrayList<String> patternArray) {

		// change the match criteria for test data especially for verbs
		//System.out.println("\n\treframeTokens>>"+reframeTokens+"\n\tpatternArray>>"+patternArray+"\n");
		double matchSum = 0.0;
		for(int index=0;index<patternArray.size();index++){
			if((!patternArray.get(index).equals("."))
					&& (patternArray.get(index).contentEquals(reframeTokens.get(index)))){
				//match
				matchSum = matchSum + 1.0;
				//System.out.print("\t1.0>>"+matchSum);
			}else if(((patternArray.get(index).equals("."))
					&& (!reframeTokens.get(index).equals(".")))
					|| ((!patternArray.get(index).equals("."))
							&& (reframeTokens.get(index).equals(".")))){
				// quasi token match
				matchSum = matchSum + 0.2;
				//System.out.print("\t0.2>>"+matchSum);
			}else if((patternArray.get(index).equals("."))
					&& (reframeTokens.get(index).equals("."))){
				// free match
				matchSum = matchSum + 0.01;
				//System.out.print("\t0.01>>"+matchSum);
			}else if((patternArray.get(index).matches("VB.{0,1}"))
					&& (reframeTokens.get(index)).matches("VB.{0,1}")){
				//+ve instance verb match
				matchSum = matchSum + 0.5;
				//System.out.print("\t0.5>>"+matchSum);
			}else if((patternArray.get(index).matches("NEGVB.{0,1}"))
					&& (reframeTokens.get(index)).matches("NEGVB.{0,1}")){
				//-ve instance verb match
				matchSum = matchSum + 0.5;
				//System.out.print("\t0.5>>"+matchSum);
			}else{
				matchSum = matchSum - 0.5;
				//System.out.print("\t-0.5>>"+matchSum);
				// no match
			}
		}
		//System.out.println("\n\tmatchSum>>"+matchSum);
		return(matchSum);
	}
	
	private double indexingAndMatching(
			ArrayList<Integer> patternIndices, 
			ArrayList<String> patternArray, int compareIndex,
			ArrayList<String> compareTokenArray) {

		//System.out.println("\n\t compareTokenArray>>"+compareTokenArray+"\n\tpatternArray>>"+patternArray);
		//System.out.println("\n\t patternIndices>>"+patternIndices+"\t>>"+compareIndex);
		for(Integer pivotIndex : patternIndices){
			//System.out.println("\n\t pivot>>>"+pivotIndex+"\t\tcompareIndex>>"+compareIndex);
			int decoyCompareIndex = compareIndex;
			ArrayList<String> reframeTokens = new ArrayList<>();
			int prefixFiller = (compareIndex - (pivotIndex-0));
			if(prefixFiller < 0){
				while(prefixFiller<0){
					reframeTokens.add(".");
					prefixFiller++;
				}
				//prefixFiller = 0;
			}
			//System.out.println("\n\tprefixFiller>>>"+prefixFiller+"\t>>>"+reframeTokens);
			while(prefixFiller < decoyCompareIndex){
				reframeTokens.add(compareTokenArray.get(prefixFiller));
				prefixFiller++;
			}
			//System.out.println("\n\treframeIndices>>>"+reframeIndices+"\t>>>"+reframeTokens);
			int suffixFiller = (compareIndex + ((patternArray.size()-1)-pivotIndex));
			int additionalIndex = 0;
			if(suffixFiller > (compareTokenArray.size()-1)){
				additionalIndex = (suffixFiller - (compareTokenArray.size()-1));
				suffixFiller = (compareTokenArray.size()-1);
			}
			//System.out.println("\n\tsuffixFiller>>>"+suffixFiller+"\t>>>"+reframeTokens);
			while(decoyCompareIndex <= suffixFiller){
				if(decoyCompareIndex == compareIndex){
					//replace Entity named with pattern entity
					reframeTokens.add(patternArray.get(pivotIndex));
				}else{
					// add other terms
					reframeTokens.add(compareTokenArray.get(decoyCompareIndex));
				}
				decoyCompareIndex++;
			}
			while(additionalIndex > 0){
				reframeTokens.add(".");
				additionalIndex--;
			}
			
			//System.out.println("\n\t final reframeIndices>>>"+reframeIndices+"\t>>>"+reframeTokens);
			// SIZE INTEGRITY CHECK
			if(reframeTokens.size() == patternArray.size()){
				double retVal = compareContextPatterns(reframeTokens,patternArray);
				return(retVal);
			}else{
				//System.out.println("\n\t string>>"+reframeTokens+"\n\t pattern>>"+patternArray);
				System.err.println(" indexingAndMatching() ~ SIZE CONSISTENCY CHECK ALERT"+reframeTokens+"\n\t>>"+patternArray);
			}
		}
		return(-100.0);
	}

	private ArrayList<Double> contextEntityMatchScore(String preservedEntity, 
			String seedEntity, String entity, ArrayList<Double> termScore) {
		
		ArrayList<Integer> patternIndex = findKeyTermIndex(entity,preservedEntity.trim());
		Integer seedIndex = findKeyTermIndex(entity,seedEntity.trim()).get(0);
		ArrayList<String> preservedArray = new ArrayList<>(
				Arrays.asList(preservedEntity.trim().split(" ")));
		//System.out.println("\n\t stratTerm>>"+preservedString+"\t****"+preservedArray);
		ArrayList<String> seedArray = new ArrayList<>(
				Arrays.asList(seedEntity.trim().split(" ")));
		
		int totalSize = seedArray.size();
		if(seedArray.size() > preservedArray.size()){
			totalSize = preservedArray.size();
		}
		double retScore = indexingAndMatching(patternIndex, preservedArray,
				seedIndex, seedArray);
		double thresholdPercentile = (0.80 * totalSize);
		//System.out.println("\n\tindex Score>>"+retScore+"\t threshold>>"+thresholdPercentile);
		if(retScore >= thresholdPercentile){
			double entityAcceptance = 1.0;
			if(termScore.isEmpty()){
				termScore.add(retScore);
				termScore.add(entityAcceptance);
			}else{
				double prevScore = retScore+termScore.get(0);
				termScore.set(0, prevScore);
				double prevIndex = entityAcceptance+termScore.get(1);
				termScore.set(1, prevIndex);
				//System.out.println("\n\t prevScore>>"+prevScore +"\t prevIndex>>"+prevIndex);
			}
		}
		//System.out.println("\n\t>>"+termScore +"\t>>"+ currentContext +"\tp>."+preservedContext);
		return(termScore);
	}
	
	/**
	 * 
	 * @param seedDominantPattern
	 * @param decoyList
	 * @return 
	 */
	private int compareEntryWithList(HashMap<String, String> seedDominantPattern,
			List<HashMap<String, String>> decoyList) {
		
		int returnIndex = 0;
		for(HashMap<String, String> decoyHash : decoyList){
			if(!seedDominantPattern.isEmpty()){
				ArrayList<Double> termScore = new ArrayList<>();
				//System.out.println("\n\t decoy>>"+decoyHash+"\n\t seed>>"+seedDominantPattern);
				for(String entity : seedDominantPattern.keySet()){
					if(decoyHash.containsKey(entity)){
						termScore = contextEntityMatchScore(decoyHash.get(entity), 
								seedDominantPattern.get(entity), entity, termScore);
						/**
						if(decoyHash.get(entity).contentEquals(seedDominantPattern.get(entity))){
							matchCount++;
						}**/
					}
				}
				//System.out.println("\n\t termScore>>"+termScore+"\tsize>>"+seedDominantPattern.size());
				if(!termScore.isEmpty()){
					double testSize = new Integer(seedDominantPattern.size()).doubleValue() ;
					if((termScore.get(1) == testSize)){
						double overlapScore = (termScore.get(0)/testSize);
						//System.out.println("\n\t OVErSCore>>"+overlapScore);
						if( overlapScore >= 0.80){
							return(returnIndex);
							//returnStatus = true;
							//break;
						}
						
					}
				}
			}
			returnIndex++;
		}
		//return(returnStatus);
		return(-1);
	}
	
	/**
	 * 
	 * @param seedDominantPattern
	 * @param invarianceBasedPatterns
	 * @param instanceType 
	 * @param decoyInvarianceBasedPatterns 
	 * @return
	 */
	private void aiContextPatternAddition(
			HashMap<String, String> seedDominantPattern, Integer instanceType) {
		
		boolean addStatus=false;
		double rawCount=0.0,totalSize=0.0;
		for(String entity : seedDominantPattern.keySet()){
			String patternTerm = seedDominantPattern.get(entity);
			patternTerm = patternTerm.replaceAll("\\,|\\;|\\?", "");
			totalSize = (totalSize + patternTerm.split("(\\s)+").length);
			Matcher localMatcher = Pattern.compile("\\.").matcher(patternTerm);
			while(localMatcher.find()){
				rawCount++;
			}
			seedDominantPattern.put(entity, patternTerm);
		}
		if((totalSize-rawCount) == 3){
			for(String entity : seedDominantPattern.keySet()){
				String currPattern = seedDominantPattern.get(entity).replaceFirst("\\.", "");
				currPattern = currPattern.replaceFirst("\\.", "");
				seedDominantPattern.put(entity, currPattern);
			}
			addStatus = true;
		}
		
		else if(((rawCount/totalSize)*100) <= 60.0){
			addStatus = true;
		}
		//addStatus=true;
		
		HashMap<Integer, HashMap<String, String>> decoyInvarianceBasedPatterns;
		ArrayList<Integer> patternKeyList;
		List<HashMap<String, String>> decoyList;
		if(addStatus == true){
			/**
			if(invariantBasedPattern.containsKey(instanceType)){
				System.out.println("\n\t mapSize>>"+invariantBasedPattern.get(instanceType).size()+"\tinstance>>"+instanceType);
			}**/
			
			// check if present in same instance type
			int currentMatchIndex = -1;
			if(invariantBasedPattern.containsKey(instanceType)){
				decoyInvarianceBasedPatterns = 
						new LinkedHashMap<>(invariantBasedPattern.get(instanceType));
				patternKeyList = new ArrayList<>(decoyInvarianceBasedPatterns.keySet());
				decoyList = new LinkedList<>(decoyInvarianceBasedPatterns.values());
				//System.out.println("\n\t within instance");
				currentMatchIndex = compareEntryWithList(seedDominantPattern, decoyList);
			}

			/**
			 * add the current pattern to the list if it isn't previously present 
			 * in any of the instance types or within the same instance type
			 */
			if(currentMatchIndex == -1){
				int index = 1;
				decoyInvarianceBasedPatterns = new LinkedHashMap<>();
				if(!invariantBasedPattern.containsKey(instanceType)){
					index = 1;
				}else{
					decoyInvarianceBasedPatterns = invariantBasedPattern.get(instanceType);
					index = decoyInvarianceBasedPatterns.size()+1;
				}
				//patternId = (instanceType*(index));
				decoyInvarianceBasedPatterns.put(index, seedDominantPattern);
				//System.out.println("\n\t pattern added>>"+patternId);
				invariantBasedPattern.put(instanceType, decoyInvarianceBasedPatterns);
			}
		}
	}

	/**
	 * generate separate patterns for each context based on dynamic programming alignment
	 * generateInvarianceBasedPatterns()
	 * @param orderedScoreHash
	 * @param instanceType
	 * @param aivScorePatterns2 
	 * @param decoyInvarianceBasedPatterns 
	 * @return 
	 * @throws IOException
	 */
	private HashMap<Integer, HashMap<Integer, HashMap<Double, ArrayList<String>>>> generateInvarianceBasedPatterns(
			HashMap<Double, ArrayList<String>> orderedScoreHash,
			Integer instanceType) 
					throws IOException {
		
		//HashMap<String, HashMap<String, String>> invarianceBasedPatterns = new LinkedHashMap<>();
		HashMap<Integer, HashMap<Integer,HashMap<Double, ArrayList<String>>>> aivScorePatterns = new LinkedHashMap<>();
		ArrayList<Double> scoreArray = new ArrayList<>(orderedScoreHash.keySet());
		ArrayList<Double> decoyList = new ArrayList<>(orderedScoreHash.keySet()); 
		double invarianceQuotient = 0.0;
		int actualIndex=0;
		HashMap<String, String> seedDominantPattern = 
				invariantScoreHash.get(instanceType).
				get(orderedScoreHash.get(scoreArray.get(0)).get(0)).getContextualFrames();
		for(int i=0;i<scoreArray.size();i++){
			
			boolean addStaus=true;
			//System.out.println("\n\t CURRENT INDEX>>>"+i+"\t\t>>"+orderedScoreHash.get(scoreArray.get(i))+"\tQUOTIENT VALUE>>"+invarianceQuotient);
			//String dominatingDocId = orderedScoreHash.get(scoreArray.get(i)).get(0);
			if(i != 0){
				DecimalFormat df = new DecimalFormat("#.#######");
				if(instanceType == -1){
					df = new DecimalFormat("#.#######");
				}
				//invarianceQuotient = Math.round(scoreArray.get(i-1)/scoreArray.get(i));
				invarianceQuotient = Double.valueOf(df.format(scoreArray.get(i-1)/scoreArray.get(i)));
				
				//System.out.println("\n\t QUOTIENT VALUE>>>"+invarianceQuotient+"\t\t CURRENT INDEX>>>"+i);
			}
			//generate alignment for first index or when invariance quotient is unit (delta=1)
			if((i==0) || (invarianceQuotient == 1.0)){
				ArrayList<String> docArray = new ArrayList<>(orderedScoreHash.get(scoreArray.get(i)));
				for(int patInd2=0;patInd2<orderedScoreHash.get(scoreArray.get(i)).size();patInd2++){
					//System.out.println("\n\t\tCOMPARISON >>>"+(patInd2+1));
					seedDominantPattern = compareTermAlignments(seedDominantPattern, 
							docArray.get(patInd2), instanceType);
				}
				// remove the same score id's
				if(i!=0){
					//System.out.println("\n\t REMOVAL INDEX>>>"+i);
					//System.out.println("\t QUOTIENT VALUE>>>"+invarianceQuotient+"\t\t CURRENT INDEX>>>"+i);
					scoreArray.remove(i);
					i=(i-1);
					//System.out.println("\n\t REMOVAL>>>"+scoreArray.size()+"\t\tNEXT INDEX>>"+i);
				}
			}else{
				// add the current seed pattern to hash map
				aiContextPatternAddition(seedDominantPattern,instanceType);
				// check for new addition or if seed pre-existed, then remove
				if((i!=0)){
					if(invariantBasedPattern.containsKey(instanceType)){
						//System.out.println("\n\tin here>>>"+i+"\t>>>"+invariantBasedPattern.get(instanceType).size());
						if(i > invariantBasedPattern.get(instanceType).size()){
							addStaus = false;
							scoreArray.remove(i-1);
							i=i-1;
						}
					}
				}
				//reset dominating seed pattern
				// get(0) is used to set first item from list for seed matching
				seedDominantPattern = invariantScoreHash.get(instanceType)
						.get(orderedScoreHash.get(scoreArray.get(i)).get(0)).getContextualFrames();
			}
			
			HashMap<Integer, HashMap<Double, ArrayList<String>>> decoyInstanceHash = new LinkedHashMap<>();
			HashMap<Double, ArrayList<String>> decoyScoreHash = new LinkedHashMap<>();
			
			if(addStaus){
				if(aivScorePatterns.containsKey(instanceType)){
					decoyInstanceHash = aivScorePatterns.get(instanceType);
					if(decoyInstanceHash.containsKey(i+1)){
						decoyScoreHash = decoyInstanceHash.get(i+1);
					}
				}
				decoyScoreHash.put(decoyList.get(actualIndex),
						orderedScoreHash.get(decoyList.get(actualIndex)));
				decoyInstanceHash.put(i+1, decoyScoreHash);
			}else{
				if(aivScorePatterns.containsKey(instanceType)){
					decoyInstanceHash = aivScorePatterns.get(instanceType);
					if(decoyInstanceHash.containsKey(i+1)){
						decoyInstanceHash.remove(i+1);
					}
				}
			}
			aivScorePatterns.put(instanceType,decoyInstanceHash);
			
			actualIndex++;
		}
		aiContextPatternAddition(seedDominantPattern, instanceType);
		return(aivScorePatterns);
	}
	
	private HashMap<Integer, HashMap<Integer, HashMap<Double, ArrayList<String>>>> removePatternRedundancies(
			HashMap<Integer, HashMap<Integer, HashMap<Double, ArrayList<String>>>> aivScorePatterns) {
		
		int previousMatchIndex = -1;
		HashMap<Integer, HashMap<String, String>> decoyInvarianceBasedPatterns;
		List<HashMap<String, String>> decoyList;
		ArrayList<Integer> acceptedClassTypes = 
				new ArrayList<>(Arrays.asList(9,6,5,4,3,-1));
		HashMap<Integer, HashMap<Integer, HashMap<String, String>>> bufferInvarianceBasedPatterns = 
				new HashMap<>();
		for(Map.Entry<Integer, HashMap<Integer, HashMap<String, String>>> tier0MapValue 
				: invariantBasedPattern.entrySet()){
			bufferInvarianceBasedPatterns.put(tier0MapValue.getKey(), new HashMap<>(tier0MapValue.getValue()));
		}
		for(Integer i : bufferInvarianceBasedPatterns.keySet()){
			System.out.println("\n\t>>b4>>>"+bufferInvarianceBasedPatterns.get(i).size());
		}
		
		//bufferInvarianceBasedPatterns = (HashMap<Integer, HashMap<Integer, HashMap<String, String>>>) invariantBasedPattern.clone();

		// check if present in other instance type
		for(Integer instanceType : acceptedClassTypes){
			System.out.println("\n\t>>Updated Set Size>>>"+instanceType+"\t"+invariantBasedPattern.get(instanceType).size());
			for(Integer contrastInstanceType : bufferInvarianceBasedPatterns.keySet()){
				System.out.println("\n\t>>ORiginal Set Size>>>"+contrastInstanceType+"\t"+bufferInvarianceBasedPatterns.get(contrastInstanceType).size());
				if((contrastInstanceType != instanceType)){
					Iterator<Map.Entry<Integer,HashMap<String, String>>> tier1Itr = 
							invariantBasedPattern.get(instanceType).entrySet().iterator();
					while(tier1Itr.hasNext()){
						Map.Entry<Integer,HashMap<String, String>> tier1MapValue = tier1Itr.next();
						HashMap<String, String> seedDominantPattern = 
								invariantBasedPattern.get(instanceType).get(tier1MapValue.getKey());
						decoyInvarianceBasedPatterns = 
								new HashMap<>(bufferInvarianceBasedPatterns.get(contrastInstanceType));
						decoyList = new LinkedList<>(decoyInvarianceBasedPatterns.values());
						previousMatchIndex = compareEntryWithList(seedDominantPattern, decoyList);
						// remove the current id instance if similar to other
						if(previousMatchIndex != -1){
							tier1Itr.remove();
						}
					}
				}
			}
		}
		
		// allow only the class types pre-mentioned for classification 
		Iterator<Map.Entry<Integer, HashMap<Integer, HashMap<String, String>>>> tier2Itr = 
				invariantBasedPattern.entrySet().iterator();
		while(tier2Itr.hasNext()){
			Map.Entry<Integer, HashMap<Integer, HashMap<String, String>>> tier2MapValue = 
					tier2Itr.next();
			if(!acceptedClassTypes.contains(tier2MapValue.getKey())){
				tier2Itr.remove();
				aivScorePatterns.remove(tier2MapValue.getKey());
			}
		}
		return(aivScorePatterns);
	}

	public void scoreFeatures(DensityFeatureMap primaryFeatureinstance,
			ContextFrameFeatureMap secondaryFeatureInstance, 
			TreeMap<Integer, HashMap<String, ArrayList<String>>> posTaggedSentenceBundle) 
					throws IOException {
		
		ArrayList<String> multiDVariables = new ArrayList<>(Arrays.asList("RELATION","CHEMICAL","GENEPRO"));
		ArrayList<String> hyperVariables = determineVariables(multiDVariables);
		
		for(String variable : hyperVariables){
			switch (variable) {
			case "RELATION-CHEMICAL-GENEPRO":
				calculateVariableDensity(primaryFeatureinstance.getVerbChemicalGeneAssociation(),
						primaryFeatureinstance.getVerbChemicalAssociation(),
						primaryFeatureinstance.getVerbGeneAssociation(),variable);
				break;
			case "RELATION-CHEMICAL":
				calculateVariableDensity(primaryFeatureinstance.getVerbChemicalAssociation(),
						primaryFeatureinstance.getChemicalTermFrequency(),
						primaryFeatureinstance.getRelationTermFrequency(),variable);
				break;
			case "RELATION-GENEPRO":
				calculateVariableDensity(primaryFeatureinstance.getVerbGeneAssociation(),
						primaryFeatureinstance.getGeneTermFrequency(),
						primaryFeatureinstance.getRelationTermFrequency(),variable);
				break;
			}
		}
		/**
		Iterator<Map.Entry<String, HashMap<String, Double>>> itr = variableDensityEstimatorPerAbstract.entrySet().iterator();
		while(itr.hasNext()){
			Map.Entry<String, HashMap<String, Double>> val = itr.next();
			System.out.println("\n\t::"+val.getKey()+"**************************");
			Iterator<Map.Entry<String, Double>> itr1 = val.getValue().entrySet().iterator();
			while(itr1.hasNext()){
				Map.Entry<String,Double> val1 = itr1.next();
				System.out.println("\n\t::"+val1.getKey()+"\t\t"+val1.getValue());
			}
		}**/
		
		LinkedHashMap<Integer, LinkedHashMap<String, Set<String>>> decoyContextFrameMap = 
				secondaryFeatureInstance.getContextFrameMap(); 
		for(String variable : multiDVariables){
			switch (variable) {
			case "CHEMICAL":
				assembleContextFrameProbability(secondaryFeatureInstance.getChemicalFrameMap(),
						decoyContextFrameMap,variable);
				break;
			case "GENEPRO":
				assembleContextFrameProbability(secondaryFeatureInstance.getGeneFrameMap(),
						decoyContextFrameMap,variable);
				break;
			case "RELATION":
				assembleContextFrameProbability(secondaryFeatureInstance.getRelationFrameMap(),
						decoyContextFrameMap,variable);
				break;
			}
		}
		//System.out.println("\n\t::>>>"+variableContextProbabilityPerAbstract.get("RELATION").get(1).size());
		evaluateDataPointInvariance(hyperVariables,posTaggedSentenceBundle,decoyContextFrameMap);
		HashMap<Integer,HashMap <Double, ArrayList<String>>> instanceScoreHash = 
				hashOutPolynomialScores();
		HashMap<Integer, HashMap<Integer,HashMap<Double, ArrayList<String>>>> aivScorePatterns = new LinkedHashMap<>();
		for(Integer instanceType : instanceScoreHash.keySet()){
			HashMap <Double, ArrayList<String>> orderedScoreHash = 
					new LinkedHashMap<>(instanceScoreHash.get(instanceType));
			HashMap<Integer, HashMap<Integer,HashMap<Double, ArrayList<String>>>> temp = 
					generateInvarianceBasedPatterns(orderedScoreHash,instanceType);
			aivScorePatterns.putAll(temp);
			if(temp.containsKey(-1)){
				//System.out.println("\n\t>>"+temp);
			}
			
		}
		// remove the cross class type similarities from each of the generated patterns
		// Remove the unwanted class types as well
		aivScorePatterns = removePatternRedundancies(aivScorePatterns);
		
		System.err.println("\n\t aivPat>>"+aivScorePatterns.size());
		System.err.println("\n\t invar>>"+invariantBasedPattern.size());
		
		// remove the missing pattern id's from aivScore based on constitutive patterns
		for(Integer type : invariantBasedPattern.keySet()){
			HashMap<Integer,HashMap<Double, ArrayList<String>>> decoySubSet = aivScorePatterns.get(type);
			Set<Integer> actualPatterns = new TreeSet<>(invariantBasedPattern.get(type).keySet());
			Set<Integer> totalPatterns = new TreeSet<>(aivScorePatterns.get(type).keySet());
			Set<Integer> redundantSet = Sets.diff(totalPatterns, actualPatterns);
			if(!redundantSet.isEmpty()){
				for(Integer removeIndex : redundantSet){
					decoySubSet.remove(removeIndex);
				}
				aivScorePatterns.put(type, decoySubSet);
			}
			/**
			for(Integer val:aivScorePatterns.get(type).keySet()){
				System.out.println("\n\t"+val+"\tinatsnce>>"+type);
			}**/
		}
		
		//System.out.println("\n\taivScorePatterns>>"+aivScorePatterns);
		//System.out.println("\n\t*********PATTERNS***********************");
		Properties systemProperties = new Properties();
		InputStream propertyStream  = new FileInputStream("config.properties");
		systemProperties.load(propertyStream);
		FileWriter fileWS = new FileWriter(systemProperties.getProperty("aiFeaturePatterns"),false);
		//System.out.println("\n\t>.print");
		BufferedWriter buffWS = new BufferedWriter(fileWS);
		Iterator<Map.Entry<Integer, HashMap<Integer, HashMap<String, String>>>> tier1Itr = 
				invariantBasedPattern.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, HashMap<Integer, HashMap<String, String>>> tier1MapValue = tier1Itr.next();
			Iterator<Map.Entry<Integer, HashMap<String, String>>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<Integer, HashMap<String, String>> tier2MapValue = tier2Itr.next();
				buffWS.write("pattern".concat(
						String.valueOf(tier1MapValue.getKey())+":"+String.valueOf(tier2MapValue.getKey())));
				buffWS.newLine();
				Iterator<Map.Entry<String, String>> tier3Itr = tier2MapValue.getValue().entrySet().iterator();
				while(tier3Itr.hasNext()){
					Map.Entry<String, String> keyValue = tier3Itr.next();
					buffWS.write(keyValue.getKey().concat("\t|"));
					buffWS.write(keyValue.getValue());
					buffWS.newLine();
				}
				buffWS.newLine();
			}
		}
		buffWS.flush();

		Iterator<Map.Entry<Integer, HashMap<Integer,HashMap<Double, ArrayList<String>>>>> tier4Itr = 
				aivScorePatterns.entrySet().iterator();
		fileWS = new FileWriter(systemProperties.getProperty("trainingAivScoresPatterns"),false);
		buffWS = new BufferedWriter(fileWS);
		while(tier4Itr.hasNext()){
			Map.Entry<Integer, HashMap<Integer,HashMap<Double, ArrayList<String>>>> tier4MapValue = 
					tier4Itr.next();
			Iterator<Map.Entry<Integer,HashMap<Double, ArrayList<String>>>> tier5Itr = 
					tier4MapValue.getValue().entrySet().iterator();
			while(tier5Itr.hasNext()){
				Map.Entry<Integer,HashMap<Double, ArrayList<String>>> tier5MapValue = tier5Itr.next();
				Iterator<Map.Entry<Double, ArrayList<String>>> tier6Itr = 
						tier5MapValue.getValue().entrySet().iterator();
				while(tier6Itr.hasNext()){
					Map.Entry<Double, ArrayList<String>> tier6MapValue = tier6Itr.next();
					buffWS.write(String.valueOf(tier4MapValue.getKey()).concat("#"));
					buffWS.write("pattern".concat(
							String.valueOf(tier5MapValue.getKey())).concat("#"));
					buffWS.write(String.valueOf(tier6MapValue.getKey()).concat("#"));
					buffWS.write(String.valueOf(tier6MapValue.getValue().size()).concat("#"));
					buffWS.write(String.valueOf(tier6MapValue.getValue().toString()));
					buffWS.newLine();
				}
			}
		}
		buffWS.close();
	}
	
}
