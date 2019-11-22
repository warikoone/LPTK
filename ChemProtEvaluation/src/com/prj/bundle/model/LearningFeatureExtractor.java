/**
 * 
 */
package com.prj.bundle.model;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.trees.PennTreebankTokenizer;

/**
 * @author neha
 *
 */
public class LearningFeatureExtractor{
	
	
	private LinkedHashMap<String, Set<String>> relevantEntityRelationTable;
	private LinkedHashMap<String, ArrayList<String>> relevantVerbTable;
	
	private LinkedHashMap<String, Integer> chemicalVerbGeneAssociation;
	private LinkedHashMap<String, Integer> verbChemicalAssociation;
	private LinkedHashMap<String, Integer> verbGeneAssociation;
	private LinkedHashMap<String, Integer> chemicalTermFrequency;
	private LinkedHashMap<String, Integer> geneTermFrequency;
	private LinkedHashMap<String, Integer> relationTermFrequency;
	private LinkedHashMap<Integer,LinkedHashMap<String, Set<String>>> contextFrameMap;
	private HashMap<Integer,HashMap<Integer,HashMap<String, Set<String>>>> chemicalFrameMap;
	private HashMap<Integer,HashMap<Integer,HashMap<String, Set<String>>>> geneFrameMap;
	private HashMap<Integer,HashMap<Integer,HashMap<String, Set<String>>>> relationFrameMap;
	/**
	 * Constructor 
	 * @throws IOException 
	 */
	public LearningFeatureExtractor(){
		chemicalTermFrequency = new LinkedHashMap<>();
		geneTermFrequency = new LinkedHashMap<>();
		relationTermFrequency = new LinkedHashMap<>();
		chemicalVerbGeneAssociation = new LinkedHashMap<>();
		verbChemicalAssociation = new LinkedHashMap<>();
		verbGeneAssociation = new LinkedHashMap<>();
		contextFrameMap = new LinkedHashMap<>();
		chemicalFrameMap = new LinkedHashMap<>();
		geneFrameMap = new LinkedHashMap<>();
		relationFrameMap = new LinkedHashMap<>();
	}
	
	public LinkedHashMap<String, Set<String>> populateHashMapWithStringSet(LinkedHashMap<String,Set<String>> 
	tempHashMap, String currKey, String currValue) {
		
		Set<String> prevSet = new LinkedHashSet<>();
		if(!tempHashMap.isEmpty()){
			if(tempHashMap.containsKey(currKey)){
				prevSet = tempHashMap.get(currKey);
			}
		}
		prevSet.add(currValue);
		tempHashMap.put(currKey,prevSet);
		return(tempHashMap);
	}
	
	/**
	 * populate hash map to set entity resources counter
	 * @param entitySentenceProbabity
	 * @param currKey
	 * @return 
	 */
	public LinkedHashMap<String, Integer> populateHashMapWithIntegers(
			LinkedHashMap<String, Integer> probabityHashTable, String currKey) {
		
		int currValue = 1;
		if(probabityHashTable.isEmpty()){
			probabityHashTable.put(currKey,currValue);	
		}else{
			if(probabityHashTable.keySet().contains(currKey)){
				int prevValue = probabityHashTable.get(currKey);
				probabityHashTable.put(currKey,prevValue+1);
			}else{
				probabityHashTable.put(currKey,currValue);
			}
		}
		return(probabityHashTable);
	}
	
	/**
	 * add up all the occurrences of each key term respectively 
	 * @param termFrequency
	 * @param docId
	 * @param runSize
	 * @return
	 */
	private LinkedHashMap<String, Integer> callVariablePopulateCounter(
			LinkedHashMap<String, Integer> termFrequency, 
			String docId, int runSize) {
		
		for(int i=0;i<runSize;i++){
			termFrequency = populateHashMapWithIntegers(termFrequency, docId);
		}
		return(termFrequency);
	}
	
	/**
	 * calculate frequency of unary variables to be used
	 * in the algebraic invariance model
	 * @param countHash
	 * @param docId
	 */
	private void unaryVariableFrequencyEstimator(HashMap<Integer, Set<Integer>> countHash, 
			String docId) {
		
		int counterSize = 0;
		if(countHash.containsKey(0)){
			counterSize = (countHash.get(0).size());
			chemicalTermFrequency = callVariablePopulateCounter(
					chemicalTermFrequency, docId, counterSize);
		}
		if(countHash.containsKey(1)){
			counterSize = (countHash.get(1).size());
			geneTermFrequency = callVariablePopulateCounter(
					geneTermFrequency, docId, counterSize);
		}
		if(countHash.containsKey(2)){
			counterSize = (countHash.get(2).size());
			relationTermFrequency = callVariablePopulateCounter(
					relationTermFrequency, docId, counterSize);
		}
	}
	
	/**
	 * calculate frequency of quadratic variables to be used
	 * in the algebraic invariance model
	 * @param countHash
	 * @param docId
	 */
	private void quadraticVariableFrequencyEstimator(
			HashMap<Integer, Set<Integer>> countHash, String docId) {
		
		int counterSize = 0;
		if(countHash.containsKey(210)){
			counterSize = (countHash.get(210).size());
			//System.out.println("\n\t CD:"+counterSize);
			chemicalVerbGeneAssociation = callVariablePopulateCounter(
					chemicalVerbGeneAssociation, docId, counterSize);
		}
		if(countHash.containsKey(21)){
			counterSize = (countHash.get(21).size());
			//System.out.println("\n\t DV:"+counterSize);
			verbGeneAssociation = callVariablePopulateCounter(
					verbGeneAssociation, docId, counterSize);
		}
		if(countHash.containsKey(20)){
			counterSize = (countHash.get(20).size());
			//System.out.println("\n\t CV:"+counterSize);
			verbChemicalAssociation = callVariablePopulateCounter(
					verbChemicalAssociation, docId, counterSize);
		}
	}
	
	/**
	 * Identify the relation id's mentioned in each sentence of the abstract
	 * @param docId
	 * @param currentSentence
	 * @param decoyEntityRelationSet
	 * @param decoyVerbSet 
	 * @param docId 
	 * @return
	 */
	private HashMap<Integer, Set<Integer>> countEntityTerm(String currentSentence, String docId) {
		
		Matcher termMatcher;
		// marks all the starting points for different terms
		HashMap<Integer, Set<Integer>> countHash = new LinkedHashMap<>();
		Set<Integer> distinctCounts = new HashSet<>();
		int index=0;
		Set<Integer> intersectSet01 = new HashSet<>();
		Set<Integer> intersectSet0 = new HashSet<>();
		Set<Integer> intersectSet1 = new HashSet<>();
		for(String pattern : new ArrayList<>(Arrays.asList("CHEMICALPRI","GENEPROPRI"))){
			termMatcher = Pattern.compile(pattern).matcher(currentSentence);
			distinctCounts = new HashSet<>();
			while(termMatcher.find()){
				if(countHash.containsKey(index)){
					distinctCounts = countHash.get(index);
				}
				distinctCounts.add(termMatcher.start());
				// CHEMICAL
				countHash.put(index, distinctCounts);
			}
			index++;
		}
		
		if(countHash.containsKey(0) && (countHash.containsKey(1))){
			intersectSet01.add(Integer.parseInt(docId.split("R")[1]));
			intersectSet0.add(Integer.parseInt(docId.split("R")[1]));
			intersectSet1.add(Integer.parseInt(docId.split("R")[1]));
		}else if(countHash.containsKey(0)){
			intersectSet0.add(Integer.parseInt(docId.split("R")[1]));
		}else if(countHash.containsKey(1)){
			intersectSet1.add(Integer.parseInt(docId.split("R")[1]));
		}
		
		distinctCounts = new HashSet<>();
		termMatcher = Pattern.compile("RELATION\\d+").matcher(currentSentence);
		// check for a relation term in a sentence only once 
		if(termMatcher.find()){
			index=2;
			if(countHash.containsKey(index)){
				distinctCounts = countHash.get(index);
			}
			distinctCounts.add(termMatcher.start());
			// RELATION
			countHash.put(index, distinctCounts);
		}
		
		if((!intersectSet01.isEmpty()) && (countHash.containsKey(2))){
			distinctCounts = new HashSet<>();
			for(Integer relationId : intersectSet01){
				index = 210;
				if(countHash.containsKey(index)){
					distinctCounts = countHash.get(index);
				}
				distinctCounts.add(relationId);
				// RELATION-GENE-CHEMICAL
				countHash.put(index, distinctCounts);
			}
		}
		if((!intersectSet0.isEmpty()) && (countHash.containsKey(2))){
			distinctCounts = new HashSet<>();
			for(Integer relationId : intersectSet0){
				index = 20;
				if(countHash.containsKey(index)){
					distinctCounts = countHash.get(index);
				}
				distinctCounts.add(relationId);
				// RELATION-CHEMICAL
				countHash.put(index, distinctCounts);
			}
		}
		if((!intersectSet1.isEmpty()) && (countHash.containsKey(2))){
			distinctCounts = new HashSet<>();
			for(Integer relationId : intersectSet1){
				index = 21;
				if(countHash.containsKey(index)){
					distinctCounts = countHash.get(index);
				}
				distinctCounts.add(relationId);
				// RELATION-GENE
				countHash.put(index, distinctCounts);
			}
		}
		//System.out.println("\n\t::"+countHash);
		return(countHash);
	}
	
	public String reshapeContextFrame(String contextString) {
		
		//System.out.println("\n\tBefore change::"+contextString);
		Matcher extenderTermMatch = Pattern.compile("((\\s)*@(\\s)*)+").matcher(contextString);
		while(extenderTermMatch.find()){
			//System.out.println("\n\t>>>>>>>>>>>>>>>>>>>>>"+extenderTermMatch.group(0)+"\tlengty"+extenderTermMatch.group(0).length());
			if((extenderTermMatch.group(0).length() < contextString.length())
					&& (extenderTermMatch.group(0).length() > 1)){
				contextString = contextString.replaceAll(extenderTermMatch.group(0),"");
			}else if((extenderTermMatch.group(0).length() == contextString.length())){
				contextString = contextString.replaceAll(extenderTermMatch.group(0)," @ ");
			}
		}
		contextString = contextString.trim();
		//System.out.println("\n\t:::"+contextString);
		return(contextString);
	}
	
	private HashMap<Integer,HashMap<Integer, HashMap<String, Set<String>>>> generateFrameDataStructure(
			HashMap<Integer,HashMap<Integer, HashMap<String, Set<String>>>> entityFrameMap, String docId,
			String frameSequence, int frameCount, int instanceType) {
		
		HashMap<Integer,HashMap<String,Set<String>>> decoyTier1Hash = new LinkedHashMap<>();
		HashMap<String, Set<String>> decoyTier2Hash = new LinkedHashMap<>();
		Set<String> decoySet = new LinkedHashSet<>();
		if(entityFrameMap.containsKey(frameCount)){
			decoyTier1Hash = entityFrameMap.get(frameCount);
			if(decoyTier1Hash.containsKey(instanceType)){
				decoyTier2Hash = decoyTier1Hash.get(instanceType);
				if(decoyTier2Hash.containsKey(docId)){
					decoySet = decoyTier2Hash.get(docId);
				}
			}
		}
		decoySet.add(frameSequence);
		decoyTier2Hash.put(docId, decoySet);
		decoyTier1Hash.put(instanceType, decoyTier2Hash);
		entityFrameMap.put(frameCount, decoyTier1Hash);
		//System.out.println("\n\t>>>>>"+entityFrameMap);
		return(entityFrameMap);
	}
	
	private void populateFrameDesignBySize(String frameSequence, String docId,
			int frameCount, String keyTerm, Integer instanceType) {
		
		//System.out.println("\n\tTERM::"+keyTerm);
		
		Matcher keyTermMatch = Pattern.compile("PRI[\\W&&[^\\s]]*|\\d+[\\W&&[^\\s]]*").matcher(keyTerm);
		if(keyTermMatch.find()){
			//System.out.println("\n\t>>>>>>>>>>>>>>>>>"+keyTermMatch.group(0));
			keyTerm = keyTerm.replaceAll(keyTermMatch.group(0), "");
		}
		//System.out.println("\n\tAfter Term::"+keyTerm);
		switch (keyTerm) {
		case "CHEMICAL":
			chemicalFrameMap = generateFrameDataStructure(chemicalFrameMap, docId, 
					frameSequence, frameCount, instanceType);
			break;
		case "GENEPRO":
			geneFrameMap = generateFrameDataStructure(geneFrameMap, docId, 
					frameSequence, frameCount, instanceType);
			break;
		case "RELATION":
			relationFrameMap = generateFrameDataStructure(relationFrameMap, docId, 
					frameSequence, frameCount, instanceType);
			break;
		}
	}
	
	private void generateContextFrame(int startIndex, int endIndex,
			ArrayList<String> splitTokens, String docId,
			int frameCase, String keyTerm, Integer instanceType) {
		
		StringBuilder contextFrame = new StringBuilder();
		String supplantTerm;
		Matcher keyTermMatch;
		//System.out.println("\n\t1::"+"["+startIndex+" -- "+endIndex+"]");
		while(startIndex < endIndex){
			supplantTerm = splitTokens.get(startIndex);
			keyTermMatch = Pattern.compile("PRI[\\W&&[^\\s]]+").matcher(supplantTerm);
			if(keyTermMatch.find()){
				//System.out.println("\n\t>>>"+keyTermMatch.group(0)+"\t>>"+supplantTerm);
				supplantTerm = supplantTerm.replaceAll(keyTermMatch.group(0), "PRI");
				
			}
			contextFrame.append(supplantTerm.concat(" "));
			startIndex++;
		}
		String contextString = contextFrame.toString().trim();
		//System.out.println("\n sent to "+keyTerm+" ::"+contextString);
		populateFrameDesignBySize(contextString, docId, frameCase, keyTerm, instanceType);
		//populating sub frames for calculation
		if(contextString.contains("@")){
			contextString = reshapeContextFrame(contextString);
		}
		//System.out.println("\nsent to contextMap ::"+contextString);
		LinkedHashMap<String, Set<String>> decoyContext = new LinkedHashMap<>();
		if(!contextFrameMap.isEmpty()){
			if(contextFrameMap.containsKey(instanceType)){
				decoyContext = contextFrameMap.get(instanceType);
			}
		}
		//System.out.println("\nsent to decoyContext ::"+decoyContext);
		decoyContext = populateHashMapWithStringSet(decoyContext, contextString, docId);
		contextFrameMap.put(instanceType, decoyContext);
	}
	
	private void generateContextFrame(int startIndex, int endIndex,
			ArrayList<String> splitTokens, String docId,
			int frameCount , Integer instanceType) {

		//populating sub frames for calculation
		int tempStartIndex = startIndex;
		String contextString, supplantTerm;
		Matcher keyTermMatch;
		StringBuilder contextFrame = new StringBuilder();
		if(frameCount != 1){
			//System.out.println("\n\t2::"+"["+startIndex+" -- "+endIndex+"]");
			while(startIndex <= endIndex){
				supplantTerm = splitTokens.get(startIndex);
				keyTermMatch = Pattern.compile("PRI[\\W&&[^\\s]]+").matcher(supplantTerm);
				if(keyTermMatch.find()){
					//System.out.println("\n\t>>>"+keyTermMatch.group(0));
					supplantTerm = supplantTerm.replaceAll(keyTermMatch.group(0), "PRI");
				}
				contextFrame.append(supplantTerm.concat(" "));
				startIndex++;
			}
			contextString = contextFrame.toString().trim();
			if(contextString.contains("@")){
				contextString = reshapeContextFrame(contextString);
			}
			//System.out.println("\nsent to contextMap until 1 ::"+contextString);
			LinkedHashMap<String, Set<String>> decoyContext = new LinkedHashMap<>();
			if(!contextFrameMap.isEmpty()){
				if(contextFrameMap.containsKey(instanceType)){
					decoyContext = contextFrameMap.get(instanceType);
				}
			}
			decoyContext = populateHashMapWithStringSet(decoyContext, contextString, docId);
			contextFrameMap.put(instanceType, decoyContext);
		}
		
		startIndex = tempStartIndex;
		contextFrame = new StringBuilder();
		//System.out.println("\n\t3::"+"["+startIndex+" -- "+(endIndex-1)+"]");
		while(startIndex <= (endIndex-1)){
			supplantTerm = splitTokens.get(startIndex);
			keyTermMatch = Pattern.compile("PRI[\\W&&[^\\s]]+").matcher(supplantTerm);
			if(keyTermMatch.find()){
				supplantTerm = supplantTerm.replaceAll(keyTermMatch.group(0), "PRI");
			}
			contextFrame.append(supplantTerm.concat(" "));
			startIndex++;
		}
		contextString = contextFrame.toString().trim();
		if(contextString.contains("@")){
			contextString = reshapeContextFrame(contextString);
		}
		//System.out.println("\nsent to contextMap ::"+contextString);
		if(contextString.length() > 0){
			//System.out.println("\n\t"+docId+"\t\t"+frameCount+"\t\t"+contextString);
			LinkedHashMap<String, Set<String>> decoyContext = new LinkedHashMap<>();
			if(!contextFrameMap.isEmpty()){
				if(contextFrameMap.containsKey(instanceType)){
					decoyContext = contextFrameMap.get(instanceType);
				}
			}
			decoyContext = populateHashMapWithStringSet(decoyContext, contextString, docId);
			contextFrameMap.put(instanceType, decoyContext);
		}
	}
	
	private void runFrameIteration(int keyTermIndex, ArrayList<String> splitTokens,
			String docId, Integer instanceType) {
		
		String keyTerm = splitTokens.get(keyTermIndex);
		int frameCount=5, frameSize=5, endIndex=0,startIndex=keyTermIndex;
		//System.out.println("\n\t:::"+docId+"\t\t"+keyTerm);
		while(frameCount > 0){
			endIndex = (startIndex+frameSize);
			//System.out.println("\t*****************"+frameCount+"*******************************");
			if((startIndex >= 0) && (endIndex <= splitTokens.size())){
				//size sufficient
				generateContextFrame(startIndex,endIndex,splitTokens,docId,frameCount,
						keyTerm, instanceType);
				generateContextFrame(startIndex,keyTermIndex,splitTokens,docId,
						frameCount, instanceType);
				startIndex--;
			}
			frameCount--;
		}
	}

	private void callFrameGenerator(ArrayList<String> keyFrameTermList,
			String currentSentence, String docId, Integer instanceType) {

		ArrayList<String> splitTokens = new ArrayList<>(
				Arrays.asList(currentSentence.split("(\\s)+")));
		Iterator<String> frameTermItr = keyFrameTermList.iterator();
		int keyTermIndex = 0;
		while(frameTermItr.hasNext()){
			keyTermIndex = splitTokens.indexOf(frameTermItr.next());
			//System.out.println("\n\t frameIndex::"+keyTermIndex);
			runFrameIteration(keyTermIndex, splitTokens, docId, instanceType);
		}
	}
	
	private ArrayList<String> extractKeyFrameTerms(HashMap<Integer, Set<Integer>> countHash,
			ArrayList<String> pivotalFrameTerms, String currentSentence, Integer instanceType) {
		
		String tempString;
		for(Integer keyIndex : countHash.keySet()){
			// include all the terms of chemical and gene names only
			if((keyIndex==0) || (keyIndex==1)){
				for(int index : countHash.get(keyIndex)){
					tempString = currentSentence.substring(index);
					tempString = tempString.split("\\s")[0];
					pivotalFrameTerms.add(tempString);
				}
			}
			// add relation term as one of the context terms only for +ve instances
			if((keyIndex==2) && (instanceType > 0)){
				for(int index : countHash.get(keyIndex)){
					tempString = currentSentence.substring(index);
					tempString = tempString.split("\\s")[0];
					pivotalFrameTerms.add(tempString);
				}
			}
		}
		return(pivotalFrameTerms);
	}
	
	private String restructureSentence(String currentSentence) {
		
		StringBuilder restructureText = new StringBuilder();
		String frameExtender = " @ @ @ @ @ ";
		restructureText.append(frameExtender);
		restructureText.append(currentSentence);
		restructureText.append(frameExtender);
		return(restructureText.toString().trim());
	}

	public ContextFrameFeatureMap learnContextFrameComposition(Integer instanceType, String docId, 
			String currentSentence) throws IOException {
		
		String extendedFrameText;
		ArrayList<String> pivotalFrameTerms = new ArrayList<>();
		HashMap<Integer, Set<Integer>> countHash = countEntityTerm(currentSentence, docId);
		extendedFrameText = restructureSentence(currentSentence);
		//append all key terms in one pivotal variable
		pivotalFrameTerms = extractKeyFrameTerms(countHash, pivotalFrameTerms, currentSentence, instanceType);
		//System.out.println("\n\t>>>>"+countHash+"\n\n\t:"+extendedFrameText+"\n\n\t<<<<<<"+pivotalFrameTerms);
		//if(docId.equalsIgnoreCase("19884587@10")){
		//System.out.println("\n\t>>>>"+countHash+"\n\n\t:"+extendedFrameText+"\n\n\t<<<<<<"+pivotalFrameTerms);
		callFrameGenerator(pivotalFrameTerms, extendedFrameText, docId, instanceType);
		//}
		
		
		
		//System.out.println("\n\t>>>>>>>>>>>>>>>>>>>>>>>>");
		//System.out.println("\n\n\t"+contextFrameMap.size());
		
		FileWriter fileWS;
		try {
			Properties systemProperties = new Properties();
	        InputStream propertyStream  = new FileInputStream("config.properties");
	        systemProperties.load(propertyStream);
			fileWS = new FileWriter(systemProperties.getProperty("trainingContextPattern"));
			BufferedWriter buffWS = new BufferedWriter(fileWS);
			Iterator<Map.Entry<Integer, LinkedHashMap<String, Set<String>>>> tier1Itr 
			= contextFrameMap.entrySet().iterator();
			while(tier1Itr.hasNext()){
				Map.Entry<Integer, LinkedHashMap<String, Set<String>>> tier1Val
				= tier1Itr.next();
				Iterator<String> tier2Itr =
						tier1Val.getValue().keySet().iterator();
				while(tier2Itr.hasNext()){
					buffWS.write(String.valueOf(tier1Val.getKey()).concat("#"));
					String key = tier2Itr.next();
					buffWS.write(key.concat("#"));
					buffWS.write(String.valueOf(tier1Val.getValue().get(key).size()));
					buffWS.newLine();
				}
			}
			buffWS.flush();
			buffWS.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		/**
		System.out.println("\n\t>>>>>>>>>>>>>>>>>>>>>>>>");
		Iterator<String> itr = contextFrameMap.keySet().iterator();
		while(itr.hasNext()){
			String str = itr.next();
			System.out.println("\n\t"+str+"\t\t"+contextFrameMap.get(str));
		}**/
		ContextFrameFeatureMap contextFeatureInstance = new ContextFrameFeatureMap(chemicalFrameMap, geneFrameMap,
				relationFrameMap, contextFrameMap);
		return(contextFeatureInstance);
	}
	
	public DensityFeatureMap densityFeatureExtractor(String docId, String currentSentence) {
		
		// single document id with multiple sentences in each cluster
		HashMap<Integer, Set<Integer>> countHash = countEntityTerm(currentSentence, docId);
		if(!countHash.isEmpty()){
			if(docId.contains("@")){
				docId = docId.replaceAll("(\\@(\\d)+)+", "").trim();
			}
			//System.out.println("\n\tcountHash>>"+countHash+"\t\t>>"+docId);
			// calculate unary and binary frequency instances for feature map
			unaryVariableFrequencyEstimator(countHash, docId);
			quadraticVariableFrequencyEstimator(countHash, docId);
		}
		// Mandatory check to see if all the DS are of same size i.e, they all have same docID's 
		
		DensityFeatureMap featureInstance = new DensityFeatureMap(chemicalVerbGeneAssociation,
				verbChemicalAssociation,verbGeneAssociation,
				chemicalTermFrequency,geneTermFrequency,relationTermFrequency);
		return(featureInstance);
	}
	
}
