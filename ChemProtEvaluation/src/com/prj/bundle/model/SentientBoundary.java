package com.prj.bundle.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.prj.bundle.preprocessing.NormaliseAbstracts;

import edu.stanford.nlp.util.Sets;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.InvalidFormatException;

public class SentientBoundary implements Callable<BoundaryReturn>{

	protected LinkedHashMap<String, Set<String>> entityRelationIdentifierTable;
	protected LinkedHashMap<String, ArrayList<String>> mainRelationVerbList;
	protected TreeMap<Integer,HashMap<String,ArrayList<String>>> posTaggedSentenceAssembler;
	protected TreeMap<Integer,HashMap<String,ArrayList<String>>> originalSentenceAssembler;
	protected HashMap<String,HashMap<String,ArrayList<TreeSet<Integer>>>> sentenceEntityRelationManager;
	protected HashMap<String, Set<HashMap<String, String>>> negativeInstances;
	protected LinkedHashMap<String, ArrayList<String>> sentenceCluster;
	protected LinkedHashMap<String, ArrayList<String>> orgSentenceCluster;
	private Properties systemProperties;
	
	/**
	 * Constructor
	 * @throws IOException 
	 */
	public SentientBoundary() throws IOException {
		this.entityRelationIdentifierTable = new LinkedHashMap<>();
		this.mainRelationVerbList = new LinkedHashMap<>();
		this.posTaggedSentenceAssembler = new TreeMap<>();
		this.originalSentenceAssembler = new TreeMap<>();
		this.sentenceEntityRelationManager = new LinkedHashMap<>();
		this.negativeInstances = new LinkedHashMap<>();
		this.systemProperties = new Properties();
        InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
	}
	
	public SentientBoundary(BoundaryReturn boundarReturnInstance,
			LinkedHashMap<String, ArrayList<String>> posSentenceCluster,
			LinkedHashMap<String, ArrayList<String>> orgSentenceCluster) throws IOException {
		
		this.sentenceEntityRelationManager = new LinkedHashMap<>();
		this.systemProperties = new Properties();
		InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
        this.sentenceCluster = posSentenceCluster;
        this.orgSentenceCluster = orgSentenceCluster;
        this.posTaggedSentenceAssembler = boundarReturnInstance.posTaggedSentenceBundle;
        this.originalSentenceAssembler = boundarReturnInstance.orgSentenceBundle;
        this.entityRelationIdentifierTable = boundarReturnInstance.entityRelationIdentifierTable;
        this.mainRelationVerbList = boundarReturnInstance.mainRelationVerbList;
        this.negativeInstances = boundarReturnInstance.negativeInstances;
	}

	public LinkedHashMap<String, ArrayList<String>> populateHashMapWithStringList(
			LinkedHashMap<String,ArrayList<String>> 
	tempHashMap, String currKey, String currValue) {
		
		ArrayList<String> prevList = new ArrayList<>();
		if(!tempHashMap.isEmpty()){
			if(tempHashMap.containsKey(currKey)){
				prevList = tempHashMap.get(currKey);
			}
		}
		prevList.add(currValue);
		tempHashMap.put(currKey,prevList);
		return(tempHashMap);
	}
	
	private ArrayList<String> sortVerb(Set<String> setValues) {
		
		//2. Check for verb ordering
		String[] verbOrder = {"VBZ","VBP","VBN","VBG","VBD"};
		ArrayList<String> verbList = new ArrayList<>(setValues);
		ArrayList<String> sortedVerbList = new ArrayList<>();
		int i=0,j=0;
		while(j < verbOrder.length){
			i=0;
			while(i < verbList.size()){
				//System.out.println("\n\t1:: "+verbList.get(i)+"\t"+j);
				if(verbList.get(i).split("#")[1].equalsIgnoreCase(verbOrder[j])){
					sortedVerbList.add(verbList.get(i));
					verbList.remove(i);
					i=0;
				}else{
					i++;
				}
			}
			j++;
		}
		//System.out.println("\n\t sort V."+sortedVerbList);
		return(sortedVerbList);
	}
	
	private int calculateEntityDistance(Set<Integer> indexStart, int verbStart) {
		
		HashMap<Integer, Integer> decoyHash = new HashMap<>();
		for(Integer distance: indexStart){
			decoyHash.put(distance,(distance-verbStart));
		}
		List<Map.Entry<Integer, Integer>> decoyCompare = new LinkedList<>(decoyHash.entrySet());
		//ascending order
		Collections.sort(decoyCompare, new Comparator<Map.Entry<Integer, Integer>>() {

			@Override
			public int compare(Map.Entry<Integer, Integer> currItem, Map.Entry<Integer, Integer> nextItem) {
				return (currItem.getValue().compareTo(nextItem.getValue()));
			}
		});
		/**
		ArrayList<Integer> decoyStart = new ArrayList<>(decoyHash.values());
		Collections.sort(decoyStart);
		for(Integer distance : decoyHash.keySet()){
			if(decoyHash.get(distance) == decoyStart.get(0)){
				return(distance);
			}
		}**/
		return(decoyCompare.get(0).getKey());
	}
	
	private int calculateFrameDistance(ArrayList<Integer> entityStart) {
		
		int retValue = -1;
		if(!entityStart.isEmpty()){
			Collections.sort(entityStart);
			//System.out.println("\n\tordered collection>>"+entityStart);
			retValue = entityStart.get(entityStart.size()-1)-entityStart.get(0);
		}
		return(retValue);
	}
	
	/**
	 * @return 
	 * @throws IOException 
	 * 
	 */
	private ArrayList<String> loadNegationWords() throws IOException {
		
		FileReader negationFile = new FileReader(systemProperties.getProperty("negationTermFile"));
		BufferedReader buffNegationReader = new BufferedReader(negationFile);
		String negationWord = buffNegationReader.readLine();
		ArrayList<String> negationWordList = new ArrayList<>();
		while (negationWord != null) {
			negationWordList.add(negationWord);
			negationWord = buffNegationReader.readLine();
		}
		buffNegationReader.close();
		return(negationWordList);
	}
	
	private LinkedList<Entry<String, Integer>> compareMapEntries(LinkedList<Entry<String, Integer>> linkedList) {
		
		Collections.sort(linkedList, new Comparator<Map.Entry<String, Integer>>() {
			// ascending order
			@Override
			public int compare(Map.Entry<String, Integer> currItem, Map.Entry<String, Integer> nextItem) {
				return (currItem.getValue().compareTo(nextItem.getValue()));
			}
		});
		return(linkedList);
	}
	
	private TreeSet<Integer> checkForIntermediateEntities(ArrayList<String> currentSentenceArray, 
			Integer minContextIndex, Integer maxContextIndex, int replaceIndex) {
		
		TreeSet<Integer> intermediateIndices = new TreeSet<>();
		intermediateIndices.add(replaceIndex);
		for(int index=0;index < currentSentenceArray.size();index++){
			String currrentToken = currentSentenceArray.get(index);
			if(currrentToken.matches("[\\W&&[^\\s]]*CHEMICAL(R\\d+T\\d+)*[\\W&&[^\\s]]*"
					+ "|[\\W&&[^\\s]]*GENEPRO(R\\d+T\\d+)*[\\W&&[^\\s]]*")){
				if((index > minContextIndex) 
						&& (index < maxContextIndex)){
					intermediateIndices.add(index);
				}
			}
		}
		
		TreeSet<Integer> returnIndice = new TreeSet<>();
		ArrayList<Integer> manageIndice = new ArrayList<>(intermediateIndices);
		int pivotIndex = manageIndice.indexOf(replaceIndex);
		//System.out.println("\n\t>>"+manageIndice);
		if(manageIndice.size() > 1){
			if((pivotIndex != 0) && (pivotIndex != intermediateIndices.size()-1)){
				// intermediate
				returnIndice.add(manageIndice.get(pivotIndex-1));
				returnIndice.add(manageIndice.get(pivotIndex+1));
			}else if(pivotIndex == 0){
				// start
				returnIndice.add(manageIndice.get(pivotIndex+1));
			}else if(pivotIndex == intermediateIndices.size()-1){
				// last
				returnIndice.add(manageIndice.get(pivotIndex-1));
			}
		}
		return(returnIndice);
	}
	
	private ArrayList<String> checkValidRelationVerb(Integer intersectSet, Set<String> setValues,
			String currentSentence, String originalSentence) throws IOException {
		
		//1. Check for verb proximity to entities
		//System.out.println("\n\tcurSen>>"+originalSentence+"\n\t>>"+currentSentence);
		ArrayList<String> returnVector = new ArrayList<>();
		Set<String> relationVector = new HashSet<>();
		ArrayList<String> sortedVerb = sortVerb(setValues);
		HashMap<String,Integer> relevantVerb = new LinkedHashMap<>();
		HashMap<Integer,HashMap<String,Integer>> negativeVerb = new LinkedHashMap<>();
		ArrayList<String> currentSentenceArray = new ArrayList<>(Arrays.asList(currentSentence.split("(\\s)+")));
		ArrayList<String> originalSentenceArray = new ArrayList<>(Arrays.asList(originalSentence.split("(\\s)+")));
		//SIZE CHECK
		if(currentSentenceArray.size() != originalSentenceArray.size()){
			System.err.println("\n\t ERRONEOUS SPLIT in checkValidRelationVerb()"+currentSentenceArray.size()
			+"\t\t>>"+originalSentenceArray.size());
		}
		// enlist negative words
		ArrayList<String> negationWordList = loadNegationWords();
		//check for negative words in sentence
		int containsNegativeTerms = 0;
		ArrayList<Integer> negativeIndices = new ArrayList<>();
		for(String negativeWord : negationWordList){
			String pattern = negativeWord;
			if(negativeWord.contentEquals("n't")){
				pattern = "(\\w)+".concat(negativeWord);
			}
			for(int negIndex=0;negIndex<originalSentenceArray.size();negIndex++){
				if(originalSentenceArray.get(negIndex).matches(pattern)){
					//System.out.println("\n\tpattern>>"+pattern+"\tneg>>"+originalSentenceArray.get(negIndex));
					String replaceString = "NEG".concat(currentSentenceArray.get(negIndex));
					currentSentenceArray.set(negIndex, replaceString);
					negativeIndices.add(negIndex);
					containsNegativeTerms = 1;
				}
			}
		}
		//System.out.println("\n\tnegativeIndices>>"+negativeIndices);

		HashMap<String, ArrayList<Integer>> verbIndexMap = new LinkedHashMap<>();
		for(String currVerb : sortedVerb){
			//System.out.println("\n\tcurrVerb>>"+currVerb);
			for(int verbIndex=0;verbIndex<currentSentenceArray.size();verbIndex++){
				int frameDistance = 0;
				if(currentSentenceArray.get(verbIndex).matches(currVerb)){
					//complete match
					ArrayList<Integer> entityStart = new ArrayList<>();
					String decoyVerb = currVerb.concat("@").concat(String.valueOf(verbIndex));
					String[] patternType = {"[\\W&&[^\\s]]*CHEMICALR","[\\W&&[^\\s]]*GENEPROR"};
					for(String pattern : patternType){
						Set<Integer> decoyStart = new HashSet<>();						
						String entityPattern = pattern.concat(
								String.valueOf(intersectSet)).concat("[T\\d+\\W&&[^\\s]]*");
						for(int entityIndex=0;entityIndex<currentSentenceArray.size();entityIndex++){
							if(currentSentenceArray.get(entityIndex).matches(entityPattern)){
								//System.out.println("\n\t>>"+currentSentenceArray.get(entityIndex)+"\t>>"+entityPattern);
								relationVector.add(currentSentenceArray.get(entityIndex).
										replaceAll("CHEMICAL|GENEPRO", ""));
								decoyStart.add(entityIndex);
							}
						}
						//System.out.println("\n\t>>"+decoyStart);
						//secure the closest entity index to current verb
						entityStart.add(calculateEntityDistance(decoyStart,verbIndex));
					}
					entityStart.add(verbIndex);
					//System.out.println("\n\tentityStart>>"+entityStart);
					frameDistance = calculateFrameDistance(entityStart);
					if(frameDistance > -1){
						relevantVerb.put(decoyVerb, frameDistance);
						verbIndexMap.put(decoyVerb, entityStart);
					}else{
						System.err.println("ALERT! ERROENOUS INDEX IN calculateFrameDistance()");
					}
					//access the negative words distance from verbs
					if(containsNegativeTerms == 1){
						for(Integer negIndex : negativeIndices){
							HashMap<String,Integer> decoyMap = new LinkedHashMap<>();
							if(!negativeVerb.isEmpty()){
								if(negativeVerb.containsKey(negIndex)){
									decoyMap = negativeVerb.get(negIndex); 
								}
							}
							//verb should be before negative word(adverb)
							if((negIndex-verbIndex) >= 0){
								decoyMap.put(decoyVerb,Math.abs(negIndex-verbIndex));
								negativeVerb.put(negIndex, decoyMap);
							}
						}
						//System.out.println("\n\tnegativeVerb>>"+negativeVerb);
					}
				}
			}
		}
		List<Map.Entry<String, Integer>> relationCompareList = new LinkedList<>();
		relationCompareList = compareMapEntries(new LinkedList<>(relevantVerb.entrySet()));		
		relevantVerb.clear();
		//System.out.println("\n\t>>"+relationCompareList);
		
		// in only case where no verb add first
		String verbReturn = relationCompareList.get(0).getKey();
		for(Map.Entry<String, Integer> entry : relationCompareList){
			//add variation to length for experimentation
			if(entry.getKey().split("#")[0].length() > 2){
				verbReturn = entry.getKey();
				break;
			}
		}
		//System.out.println("\t>>>"+verbReturn);
		//System.out.println("\tverbIndexMap>>>"+verbIndexMap);
		
		Integer maxContextIndex = Collections.max(verbIndexMap.get(verbReturn));
		Integer minContextIndex = Collections.min(verbIndexMap.get(verbReturn));
		boolean status = true;
		if(!negativeVerb.isEmpty()){
			Iterator<Map.Entry<Integer, HashMap<String, Integer>>> tier1Itr = negativeVerb.entrySet().iterator();
			while(tier1Itr.hasNext() ){
				Map.Entry<Integer, HashMap<String, Integer>> tier1ItrValue = tier1Itr.next();
				relationCompareList = compareMapEntries(new LinkedList<>(tier1ItrValue.getValue().entrySet()));
				if(relationCompareList.get(0).getKey().equalsIgnoreCase(verbReturn)){
					// remove the item from negative tree if matches main verb
					tier1Itr.remove();
				}else{
					/**
					//add new hash map to it,set value to first element
					HashMap<String, Integer> decoyHash = new LinkedHashMap<>(); 
					decoyHash.put(relationCompareList.get(0).getKey(),relationCompareList.get(0).getValue());
					negativeVerb.put(tier1ItrValue.getKey(),decoyHash);
					**/
					// change the corresponding pos tagged sentence values
					int replaceIndex = Integer.parseInt(relationCompareList.get(0).getKey().split("@")[1]);
					String[] replaceString = currentSentenceArray.get(replaceIndex).split("#");
					currentSentenceArray.set(replaceIndex,replaceString[0].concat("#NEG").concat(replaceString[1]));
					//System.out.println("\n\t comparative indices>>"+replaceIndex+"\t>>"+maxContextIndex+"\t>>"+minContextIndex);
					if((replaceIndex < maxContextIndex) && (replaceIndex > minContextIndex)){
						TreeSet<Integer> intermediateIndices  = checkForIntermediateEntities(
								currentSentenceArray, minContextIndex, maxContextIndex, replaceIndex);
						//System.out.println("\tverbIndexMap>>>"+verbIndexMap.get(verbReturn));
						//System.out.println("\n\t intermediateIndices>>"+intermediateIndices);
						//System.out.println("\n\t replaceIndex>>"+replaceIndex);
						if(!intermediateIndices.isEmpty()){
							Set<Integer> contextRange = new HashSet<>(verbIndexMap.get(verbReturn));
							Set<Integer> remainderSet =  Sets.diff(intermediateIndices,contextRange);
							//System.out.println("\n\t remainderSet>>"+remainderSet);
							if(!remainderSet.isEmpty()){
								for(Integer currIndice : remainderSet){
									if(currIndice < replaceIndex){
										status = true;
									}else{
										status = false;
										break;
									}
								}
							}
						}else{
							status = false;
						}
					}
				}
			}
		}
		
		// check if the selected verb isn't succeeded by any other verb past the last entity
		/**
		for(ArrayList<Integer> verbIndex : verbIndexMap.values()){
			if(verbIndex.get(verbIndex.size()-1) > maxContextIndex){
				status = false;
			}
		}**/
		if(status == true){
			returnVector.add(verbReturn);
			if(relationVector.size() > 1){
				System.err.println("\n\t checkValidRelationVerb() ~ multiple relationTypes>>"+relationVector);
			}else{
				returnVector.add(
						relationVector.iterator().next().replaceAll("R\\d+T", ""));
			}
			StringBuilder decoyString = new StringBuilder();
			for(String token : currentSentenceArray){
				decoyString.append(token.concat(" "));
			}
			returnVector.add(decoyString.toString().trim());
		}
		return(returnVector);
	}

	private Set<String> populateEntityGroup(Set<String> tempPopulate, String matchGroup, String docId) {

		matchGroup = matchGroup.replaceAll("(R\\d+)+", "");
		matchGroup = matchGroup.replaceAll("\\W*", "");
		HashMap<String, ArrayList<TreeSet<Integer>>> decoyHash = new LinkedHashMap<>();
		ArrayList<TreeSet<Integer>> decoyList = new ArrayList<>();
		TreeSet<Integer> decoyTreeSet = new TreeSet<>();
		for(String item:tempPopulate){
			if(item.matches("\\d+")){
				decoyTreeSet.add(Integer.parseInt(item));
			}
		}
		decoyTreeSet = (TreeSet<Integer>) decoyTreeSet.descendingSet();
		if(!sentenceEntityRelationManager.isEmpty()){
			if(sentenceEntityRelationManager.containsKey(docId)){
				decoyHash = sentenceEntityRelationManager.get(docId);
				if(decoyHash.containsKey(matchGroup)){
					decoyList = decoyHash.get(matchGroup);
				}
			}
		}
		decoyList.add(decoyTreeSet);
		Set<TreeSet<Integer>> decoySet = new HashSet<>(decoyList);
		decoyList = new ArrayList<>(decoySet);
		decoyHash.put(matchGroup, decoyList);
		sentenceEntityRelationManager.put(docId, decoyHash);
		tempPopulate.clear();
		for(Integer relationItem : decoyTreeSet){
			tempPopulate.add(String.valueOf(relationItem));
		}
		return(tempPopulate);
	}
	
	
	private StringBuilder replaceEntity(String decoySentence, int startIndex, int endIndex, 
			StringBuilder decoyBuilder, int caseType) {

		String tempBuffer;
		if(endIndex > startIndex){
			tempBuffer = decoySentence.substring(startIndex, endIndex);
			switch (caseType) {
			case 0:
				tempBuffer = tempBuffer.replaceAll("[\\W&&[^\\s]]*CHEMICAL(R\\d+T\\d+)+[\\W&&[^\\s]]*", "CHEMICAL");
				tempBuffer = tempBuffer.replaceAll("[\\W&&[^\\s]]*GENEPRO(R\\d+T\\d+)+[\\W&&[^\\s]]*", "GENEPRO");
				tempBuffer = tempBuffer.replaceAll("[\\W&&[^\\s]]*(\\w)+(\\W)*\\#", "");
				break;

			case 1:
				tempBuffer = tempBuffer.replaceAll("(R\\d+T\\d+)+", "");
				tempBuffer = tempBuffer.replaceAll("(\\W)+", "");
				tempBuffer = tempBuffer.concat("PRI");
				break;
			}
			decoyBuilder.append(tempBuffer);
		}
		return(decoyBuilder);
	}
	
	/**
	 * 
	 * @param docId
	 * @param currentSentence
	 * @param originalSentence
	 * @param currRelationId
	 * @param index
	 */
	private void generateNegativeInstance(String docId, String currentSentence, 
			String originalSentence, String currRelationId, String index) {
		
		//System.err.println("\n\tneg Sent>>"+currentSentence);
		ArrayList<String> patternArray = new ArrayList<>(
				Arrays.asList("[\\W&&[^\\s]]*CHEMICAL(R\\d+T\\d+)*[\\W&&[^\\s]]*",
				"[\\W&&[^\\s]]*GENEPRO(R\\d+T\\d+)*[\\W&&[^\\s]]*"));
		Matcher entityMatcher1,entityMatcher2;
		int i = 0, negRelIndex=0, instanceType=-1;
		entityMatcher1 = Pattern.compile(patternArray.get(i)).matcher(currentSentence);
		while(entityMatcher1.find()){
			//System.out.println("\n\t entityMatcher1>>"+entityMatcher1.group(0));
			entityMatcher2 = Pattern.compile(patternArray.get(i+1)).matcher(currentSentence);
			while(entityMatcher2.find()){
				//System.out.println("\n\t entityMatcher2>>"+entityMatcher2.group(0));
				String decoySentence = currentSentence;
				int chemStartIndex = entityMatcher1.start(), geneStartIndex = entityMatcher2.start();
				int chemEndIndex = entityMatcher1.end(), geneEndIndex =  entityMatcher2.end();
				if(docId.split("\\@").length == 3){
					//System.out.println("\n\thappens>>"+docId);
					int sentenceSeparatorIndex = currentSentence.indexOf(".");
					// split the entities between two sentences
					if(((chemStartIndex < sentenceSeparatorIndex) 
							&& (geneStartIndex < sentenceSeparatorIndex))
							|| ((chemStartIndex > sentenceSeparatorIndex) 
									&& (geneStartIndex > sentenceSeparatorIndex))){
						continue;
					}
				}
				if((entityMatcher1.group(0).matches("[\\W&&[^\\s]]*CHEMICAL(R\\d+T\\d+)+[\\W&&[^\\s]]*"))
						&& (entityMatcher2.group(0).matches("[\\W&&[^\\s]]*GENEPRO(R\\d+T\\d+)+[\\W&&[^\\s]]*"))){
					continue;
				}else{
					StringBuilder decoyBuilder = new StringBuilder();
					if(chemStartIndex < geneStartIndex){
						decoyBuilder = replaceEntity(decoySentence,0,chemStartIndex,decoyBuilder,0);
						decoyBuilder = replaceEntity(decoySentence,chemStartIndex,chemEndIndex,decoyBuilder,1);
						decoyBuilder = replaceEntity(decoySentence,chemEndIndex,geneStartIndex,decoyBuilder,0);
						decoyBuilder = replaceEntity(decoySentence,geneStartIndex,geneEndIndex,decoyBuilder,1);
						decoyBuilder = replaceEntity(decoySentence,geneEndIndex,decoySentence.length(),decoyBuilder,0);
					}else{
						decoyBuilder = replaceEntity(decoySentence,0,geneStartIndex,decoyBuilder,0);
						decoyBuilder = replaceEntity(decoySentence,geneStartIndex,geneEndIndex,decoyBuilder,1);
						decoyBuilder = replaceEntity(decoySentence,geneEndIndex,chemStartIndex,decoyBuilder,0);
						decoyBuilder = replaceEntity(decoySentence,chemStartIndex,chemEndIndex,decoyBuilder,1);
						decoyBuilder = replaceEntity(decoySentence,chemEndIndex,decoySentence.length(),decoyBuilder,0);
					}
					decoySentence = decoyBuilder.toString().trim();
					negRelIndex++;
				}
				
				//populate sentence
				int flag = 0;
				LinkedHashMap<String, ArrayList<String>> decoyHash = new LinkedHashMap<>();
				ArrayList<String> decoyList = new ArrayList<>();
				if(posTaggedSentenceAssembler.containsKey(instanceType)){
					decoyHash = (LinkedHashMap<String, ArrayList<String>>) 
							posTaggedSentenceAssembler.get(instanceType);
					for(ArrayList<String> currArray : decoyHash.values()){
						decoyList.addAll(currArray);
					}
				}
				if(!decoyList.contains(decoySentence)){
					decoyHash = populateHashMapWithStringList(decoyHash, docId.concat("@").
							concat(String.valueOf(negRelIndex)).concat("R").concat(currRelationId), decoySentence);
					posTaggedSentenceAssembler.put(instanceType, decoyHash);
					flag=1;
				}
				
				if(flag == 1){
					decoyHash = new LinkedHashMap<>();
					if(originalSentenceAssembler.containsKey(instanceType)){
						decoyHash = (LinkedHashMap<String, ArrayList<String>>) 
								originalSentenceAssembler.get(instanceType);
					}
					decoyHash = populateHashMapWithStringList(decoyHash, docId.concat("@").
							concat(String.valueOf(negRelIndex)).concat("R").concat(currRelationId), originalSentence);
					originalSentenceAssembler.put(instanceType, decoyHash);
				}
			}
		}
		
	}

	/**
	 * gathering relation based relevance of current sentence
	 * @param currentSentence
	 * @param docId 
	 * @param originalSentence 
	 * @param entityTagSet
	 * @param index 
	 * @param sentIndex 
	 * @param string 
	 * @return 
	 * @throws IOException 
	 */
	private int gatherIntraSentientRelationRelevance(String docId, String currentSentence,
			String originalSentence, LinkedHashMap<Integer, Set<String>> entityTagSet, String currRelationId, 
			String index) throws IOException, Exception {
		
		int relationStatus = 0;
		Matcher associationMatch;
		Set<String> intersect = new HashSet<>();
		Set<String> intersectDecoy = new HashSet<>();
		String bufferSentence = currentSentence;
		//update docId string
		docId = docId.concat("@").concat(String.valueOf(index));
		//System.out.println("\n\t rType>>"+currRelationId+"\t:::"+intersectDecoy+"\t-->"+entityTagSet);
		if(!entityTagSet.isEmpty()){
			if((entityTagSet.keySet().contains(0)) && (entityTagSet.keySet().contains(1)) 
					&& (entityTagSet.keySet().contains(2))){
				intersectDecoy = Sets.intersection(entityTagSet.get(0), entityTagSet.get(1));
				//System.out.println("\n\t:::"+intersectDecoy+"\t"+entityTagSet.get(2));
				if((!intersectDecoy.isEmpty()) && (!entityTagSet.get(2).isEmpty())){
					Iterator<String> intersectItr = intersectDecoy.iterator();
					while(intersectItr.hasNext()){
						String currDigit = intersectItr.next();
						if((currDigit.matches("(\\d)+"))){
							intersect.add(currDigit);
						}
					}
				}
			}
		}else{
			if(relationStatus == 0){
				relationStatus = -1;
				generateNegativeInstance(docId, bufferSentence, originalSentence, currRelationId, index);
			}
		}
		
		//System.out.println("\n\t rType>>"+currRelationId+"\t:::"+intersectDecoy+"\t-->"+entityTagSet);
		
		// check for the existence of a true relation in the sentence
		if((intersect.size() > 0) && (intersect.contains(currRelationId))){
			//System.out.println("\n\t********"+intersect+"\t"+entityTagSet.get(2));
			
			//clean the original sentence by collapsing irrelevant relation id's
			//System.out.println("\n\t currentSentence>>"+currentSentence);
			associationMatch = Pattern.compile("R\\d+").matcher(currentSentence);
			TreeSet<Integer> relationSort = new TreeSet<>();
			while(associationMatch.find()){
				relationSort.add(Integer.parseInt(
						associationMatch.group(0).replaceAll("R", "")));
			}
			// clean sentence if irrelevant id's present in pos tagged sentences
			if((!relationSort.isEmpty()) && 
					(relationSort.contains(Integer.parseInt(currRelationId)))){
				relationSort = (TreeSet<Integer>) relationSort.descendingSet();
				//System.out.println("\n\t::"+relationSort+"\n\t::"+currRelationId);
				HashSet<Integer> decoyIntersect = (HashSet<Integer>) Sets.diff(relationSort,
						new TreeSet<>(Arrays.asList(Integer.parseInt(currRelationId))));
				if(!decoyIntersect.isEmpty()){
					//clean the original sentence by collapsing irrelevant relation id's
					TreeSet<Integer> sortedDecoy = new TreeSet<>(decoyIntersect);
					sortedDecoy = (TreeSet<Integer>) sortedDecoy.descendingSet();
					//System.out.println(">>>>"+sortedDecoy+"\t::"+currRelationId);
					for(Integer currentIdentifier : sortedDecoy){
						associationMatch = Pattern.compile(
								"R".concat(String.valueOf(currentIdentifier)).concat("T\\d+"))
								.matcher(currentSentence);
						StringBuffer strBuff = new StringBuffer();
						while(associationMatch.find()){
							//System.out.println("\n\t associationMatch>>"+associationMatch.group(0));
							if((associationMatch.end() == currentSentence.length())
									|| (!Character.isDigit(currentSentence.charAt(associationMatch.end())))){
								associationMatch.appendReplacement(strBuff,"");
							}
						}
						associationMatch.appendTail(strBuff);
						currentSentence = strBuff.toString();
						//System.out.println("\n\t>>"+currentSentence);
					}
				}
			}
			
			//per sentence only one valid verb can be relation identifier
			ArrayList<String> retValue = checkValidRelationVerb(Integer.parseInt(currRelationId),entityTagSet.get(2),
					currentSentence, originalSentence);
			//System.out.println("\n\t retVal>>"+retValue);
			
			if(!retValue.isEmpty()){
				// get relation verb
				String relationVerb = retValue.get(0);
				String relationType = retValue.get(1);
				currentSentence = retValue.get(2);
				// identify the relation tag
				ArrayList<String> currentSentenceArray = new ArrayList<>(Arrays.asList(currentSentence.split(" ")));
				String[] stringSplit = relationVerb.split("@");
				int relationIndex = Integer.parseInt(stringSplit[1]);
				relationVerb = stringSplit[0];
				StringBuilder decoyString= new StringBuilder();
				String temp;
				for(int i=0;i<currentSentenceArray.size();i++){
					temp = currentSentenceArray.get(i); 
					if((i == relationIndex)  && (!relationType.contentEquals("10"))){
						decoyString.append("RELATION".concat(relationType+" "));
					}else if(temp.contains("#")){
						decoyString.append(temp.split("#")[1].concat(" "));
					}else{
						decoyString.append(currentSentenceArray.get(i).concat(" "));
					}
				}
				currentSentence = decoyString.toString().trim();
				currentSentence = currentSentence.
						replaceAll("(R\\d+T\\d+)+[\\W&&[^\\s]]*", "PRI");
				//System.out.println("\n\t>>"+currentSentence);
				
				int instanceType = Integer.parseInt(relationType);
				
				/**
				ArrayList<Integer> acceptedClassTypes = 
						new ArrayList<>(Arrays.asList(3,4,5,6,9,10));
				boolean instanceStatus = false;
				//Check for accepted instance class type with current value
				if(acceptedClassTypes.contains(instanceType)){
					instanceStatus = true;
				}**/
				// replace class type 10 as negative
				if(instanceType == 10){
					instanceType = -1;
				}

				//if(instanceStatus){
					//populate sentence
					int flag = 0;
					LinkedHashMap<String, ArrayList<String>> decoyHash = new LinkedHashMap<>();
					ArrayList<String> decoyList = new ArrayList<>();
					if(posTaggedSentenceAssembler.containsKey(instanceType)){
						decoyHash = (LinkedHashMap<String, ArrayList<String>>) 
								posTaggedSentenceAssembler.get(instanceType);
						/**
						for(ArrayList<String> currArray : decoyHash.values()){
							decoyList.addAll(currArray);
						}**/
					}
					decoyHash = populateHashMapWithStringList(decoyHash, 
							docId.concat("R").concat(currRelationId), currentSentence);
					posTaggedSentenceAssembler.put(instanceType, decoyHash);
					/**
					if(!decoyList.contains(currentSentence)){
						flag=1;
						decoyHash = populateHashMapWithStringList(decoyHash, 
								docId.concat("R").concat(currRelationId), currentSentence);
						posTaggedSentenceAssembler.put(instanceType, decoyHash);
					}**/
					
					//if(flag==1){
						decoyHash = new LinkedHashMap<>();
						if(originalSentenceAssembler.containsKey(instanceType)){
							decoyHash = (LinkedHashMap<String, ArrayList<String>>) 
									originalSentenceAssembler.get(instanceType);
						}
						decoyHash = populateHashMapWithStringList(decoyHash, 
								docId.concat("R").concat(currRelationId), originalSentence);
						originalSentenceAssembler.put(instanceType, decoyHash);
						
						//populate relative lists
						mainRelationVerbList = populateHashMapWithStringList(
								mainRelationVerbList, docId, relationVerb);
					//}
					
					
					//entity name identifier tags
					entityRelationIdentifierTable = new LearningFeatureExtractor().
							populateHashMapWithStringSet(
									entityRelationIdentifierTable, docId, currRelationId);
				//}
				
				relationStatus = 6;
				if(instanceType != -1){
					generateNegativeInstance(docId, bufferSentence, originalSentence, currRelationId, index);
				}
			}
		}
		if(relationStatus == 0){
			if((entityTagSet.keySet().contains(0)) && (entityTagSet.keySet().contains(2))){
				if((entityTagSet.get(0).contains(currRelationId)) && (!entityTagSet.get(2).isEmpty())){
					relationStatus = 4;
					generateNegativeInstance(docId, bufferSentence, originalSentence, currRelationId, index);
				}
			}
		}
		if(relationStatus == 0){
			if((entityTagSet.keySet().contains(1)) && (entityTagSet.keySet().contains(2))){
				if((entityTagSet.get(1).contains(currRelationId)) && (!entityTagSet.get(2).isEmpty())){
					relationStatus = 5;
					generateNegativeInstance(docId, bufferSentence, originalSentence, currRelationId, index);
				}
			}
		}
		if(relationStatus == 0){
			relationStatus = -1;
			generateNegativeInstance(docId, bufferSentence, originalSentence, currRelationId, index);
		}
		return(relationStatus);
	}
	
	/**
	 * @param docId 
	 * @param index 
	 * @param relationEntitySet2 
	 * @return 
	 * @throws IOException 
	**/
	
	private LinkedHashMap<String, LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>>> 
	gatherRelationIndices(String currentSentence, String docId, int index, 
			LinkedHashMap<String, LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>>> relationEntitySet) 
					throws IOException {
		
		Matcher associationMatch;
		LinkedHashMap<Integer, Set<String>> entityTagSet = new LinkedHashMap<>();
		Set<String> relationList = new LinkedHashSet<>();
		NormaliseAbstracts normaliseInstance = new NormaliseAbstracts();
		String[] relationPatterns = {"[\\W&&[^\\s]]*CHEMICAL(R\\d+T\\d+)+[\\W&&[^\\s]]*"
				,"[\\W&&[^\\s]]*GENEPRO(R\\d+T\\d+)+[\\W&&[^\\s]]*","(\\w|[\\W&&[^\\s]])+#VB\\w{1}"};//change to 3 letter verbs
		
		for(int startMatch=0;startMatch<relationPatterns.length;startMatch++){
			associationMatch = Pattern.compile(relationPatterns[startMatch]).matcher(currentSentence);
			Set<String> temp = new HashSet<>();
			while(associationMatch.find()){
				String matchGroup = associationMatch.group(0);
				if(startMatch == 0 || startMatch == 1){
					//add matched chemical or gene name to the list
					//System.out.println("\n\tclub group >>"+matchGroup);
					//purge relation type from entity relation set 
					matchGroup = matchGroup.replaceAll("T\\d+", "");
					//System.out.println("\n\tclub group after >>"+matchGroup);
					Set<String> tempPopulate = normaliseInstance.populateSet(matchGroup);
					if(!tempPopulate.isEmpty()){
						tempPopulate = populateEntityGroup(tempPopulate,matchGroup,docId);
						//System.out.println("\n\t 2 >>"+tempPopulate);
						relationList.addAll(tempPopulate);
						temp.addAll(tempPopulate);
					}
					//System.out.println("\n\t 1>>"+startMatch);
				}else{
					//add the matched verb name to the list
					if(entityTagSet.containsKey(0) || entityTagSet.containsKey(1)){
						temp.add(matchGroup);
					}
					//System.out.println("\n\t 2>>"+startMatch);
				}
				if(!temp.isEmpty()){
					entityTagSet.put(startMatch,temp);
				}
			}
		}
		//System.out.println("\n\t docid>>"+docId+"\t>>>"+index+"\t>>"+entityTagSet);
		
		if(!relationList.isEmpty()){
			for(String currRelation : relationList){
				LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>> abstractIndexComponents = 
						new LinkedHashMap<>();
				if(!entityTagSet.isEmpty()){
					if(relationEntitySet.containsKey(currRelation)){
						abstractIndexComponents = relationEntitySet.get(currRelation);
					}
					abstractIndexComponents.put(index, entityTagSet);
					relationEntitySet.put(currRelation, abstractIndexComponents);
				}
			}
		}else{
			LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>> abstractIndexComponents = 
					new LinkedHashMap<>();
			if(relationEntitySet.containsKey("-1")){
				abstractIndexComponents = relationEntitySet.get("-1");
			}
			abstractIndexComponents.put(index, entityTagSet);
			relationEntitySet.put(String.valueOf("-1"), abstractIndexComponents);
		}
		return(relationEntitySet);
	}

	/**
	 * Checking boundary identification
	 * @param sentenceCluster
	 * @param orgSentenceCluster 
	 * @param orgSentenceCluster 
	 * @return 
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 */
	 public BoundaryReturn sentenceBoundaryIdentification(LinkedHashMap<String, ArrayList<String>> sentenceCluster,
			 LinkedHashMap<String, ArrayList<String>> orgSentenceCluster) 
					 throws InvalidFormatException, IOException, Exception {
		
		Iterator<String> tagIterator = sentenceCluster.keySet().iterator();
		while(tagIterator.hasNext()){
			String docId = tagIterator.next();
			ArrayList<String> posTagSentences = new ArrayList<>(sentenceCluster.get(docId));
			ArrayList<String> orgSentences = new ArrayList<>(orgSentenceCluster.get(docId));
			
			// ARRAY SIZE INTEGRITY TEST
			if(sentenceCluster.get(docId).size() != orgSentenceCluster.get(docId).size()){
				System.err.println("\n\t>>"+posTagSentences.size()+"\t>>"+orgSentenceCluster.size()+"\t\t>"+docId);
				System.err.println("\n\t>>"+posTagSentences+"\n\t>>"+orgSentenceCluster);
			}

			//if(docId.equals("23318471")){
			int index=0;
			LinkedHashMap<String, LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>>> relationEntitySet = 
						new LinkedHashMap<>();
			while(index < posTagSentences.size()){
				String currentSentence = posTagSentences.get(index).trim();
				String originalSentence = orgSentences.get(index).trim();
				currentSentence = currentSentence.substring(0, (currentSentence.length()-1)).trim();
				originalSentence = originalSentence.substring(0, (originalSentence.length()-1)).trim();
				relationEntitySet = gatherRelationIndices(currentSentence, docId, index, relationEntitySet);
				index++;
			}
			if(relationEntitySet.containsKey("-1")){
				LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>> temp = 
						new LinkedHashMap<>(relationEntitySet.get("-1"));
				relationEntitySet.put(String.valueOf(relationEntitySet.size()-1), temp);
				relationEntitySet.remove("-1");
			}
			//System.out.println("\n\t relation itr>>>"+ relationEntitySet);
			
			Iterator<Map.Entry<String, LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>>>> tier1Itr = 
					relationEntitySet.entrySet().iterator();
			while(tier1Itr.hasNext()){
				int sentenceAdded=0,prevIndex=-1;
				String sentIndex="", clubSentIndex="";
				StringBuilder bufferPosSentence = new StringBuilder();
				StringBuilder bufferOrgSentence = new StringBuilder();
				Map.Entry<String, LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>>> tier1MapValue = 
						tier1Itr.next();
				//System.out.println("\n\t relationType>>"+tier1MapValue.getKey()+"\t indices>>"+tier1MapValue.getValue());
				ArrayList<Integer> decoyList = new ArrayList<>(tier1MapValue.getValue().keySet());
				posTagSentences = new ArrayList<>(sentenceCluster.get(docId));
				orgSentences = new ArrayList<>(orgSentenceCluster.get(docId));
				ArrayList<String> decoyPosTagSentences = new ArrayList<>(posTagSentences);
				ArrayList<String> decoyOrgSentences = new ArrayList<>(orgSentences);
				int i=0;
				//System.out.println("\n\t ES***************>>"+entityComponentTier1);
				while(i < decoyList.size()){
					//System.out.println("\n\t i value>>"+i);
					Integer currIndex = decoyList.get(i);
					String currentSentence = posTagSentences.get(currIndex).trim();
					String originalSentence = orgSentences.get(currIndex).trim();
					//System.out.println("\n\t currIndex>"+currIndex+"\n\t sentence b4::"+currentSentence+"\t>>"+originalSentence);
					currentSentence = currentSentence.substring(0, (currentSentence.length()-1)).trim();
					originalSentence = originalSentence.substring(0, (originalSentence.length()-1)).trim();
					//System.out.println("\n\t currIndex>"+currIndex+"\n\t sentence after::"+currentSentence+"\t>>"+originalSentence);
					sentIndex = sentIndex.concat(String.valueOf(currIndex));
					
					LinkedHashMap<Integer, Set<String>> entityTagSet = new LinkedHashMap<>(tier1MapValue.getValue().get(currIndex));
					entityTagSet.putAll(tier1MapValue.getValue().get(currIndex));
					if(sentIndex.contains("@")){
						//System.out.println("\n\t compounded >>"+sentIndex);
						if(!tier1MapValue.getValue().get(decoyList.get(i-1)).isEmpty()){
							Iterator<Map.Entry<Integer, Set<String>>> tier2Itr = 
									tier1MapValue.getValue().get(decoyList.get(i-1)).entrySet().iterator();
							while(tier2Itr.hasNext()){
								Set<String> decoySet = new HashSet<>();
								Map.Entry<Integer, Set<String>> tier2MapValue = tier2Itr.next();
								//System.out.println("\n\t mark3 >>"+tier1MapValue.getValue().get(currIndex));
								if(entityTagSet.containsKey(tier2MapValue.getKey())){
									decoySet = new HashSet<>(entityTagSet.get(tier2MapValue.getKey()));
								}
								//System.out.println("\n\t mark4 >>"+tier1MapValue.getValue().get(currIndex));
								decoySet.addAll(tier2MapValue.getValue());
								//System.out.println("\n\t mark1 >>"+tier1MapValue.getValue().get(currIndex));
								entityTagSet.put(tier2MapValue.getKey(), decoySet);
								//System.out.println("\n\t mark2 >>"+tier1MapValue.getValue().get(currIndex));
							}
						}
					}
					//System.out.println("\n\t>>"+entityTagSet);
					//System.out.println("\n\t mark >>"+tier1MapValue.getValue().get(currIndex));
					if(tier1MapValue.getValue().get(currIndex).containsKey(0)){
						//System.out.println("\n\t mark0 >>"+tier1MapValue.getValue().get(currIndex).get(0).size());
					}else if(tier1MapValue.getValue().get(currIndex).containsKey(1)){
						//System.out.println("\n\t mark1 >>"+tier1MapValue.getValue().get(currIndex).get(1).size());
					}
					
					boolean sizeStatus = true;
					if(tier1MapValue.getValue().get(currIndex).containsKey(0)){
						if(tier1MapValue.getValue().get(currIndex).get(0).size() > 1000){
							sizeStatus = false;
							sentenceAdded = 0;
							break;
						}
					}else if (tier1MapValue.getValue().get(currIndex).containsKey(1)){
						if(tier1MapValue.getValue().get(currIndex).get(1).size() > 1000){
							sizeStatus = false;
							sentenceAdded = 0;
							break;
						}
					}
					if(sizeStatus){
						sentenceAdded = gatherIntraSentientRelationRelevance(docId, currentSentence, 
								originalSentence, entityTagSet, tier1MapValue.getKey(), sentIndex);
					}
					entityTagSet.clear();
					if(sentenceAdded == 6){
						//System.out.println("\n current sentence has relation "+currIndex);
						//Remove relation sentences and the previous buffer
						if(bufferPosSentence.length() > 0){
							//System.out.println("\n removing "+ bufferOrgSentence.toString());
							bufferPosSentence = new StringBuilder();
							bufferOrgSentence = new StringBuilder();
							prevIndex = -1;
						}
						sentIndex="";
						i++;
					}else if((sentenceAdded == 4)||(sentenceAdded==5)){
						/**
						if(bufferPosSentence.length()==0){
							//System.out.println("\n adding to buffer "+currIndex+"\t>>"+ originalSentence);
							bufferPosSentence.append(currentSentence.concat(" . "));
							bufferOrgSentence.append(originalSentence.concat(" . "));
							clubSentIndex = String.valueOf(currIndex).concat("@");
							sentIndex="";
							prevIndex = i;
							i++;
						}else{
							//System.out.println("\n\t 1 value>>"+(i-prevIndex)+"\t 2 value >>"+(currIndex-decoyList.get(i-1)));
							//|| (1 == (currIndex-decoyList.get(i-1)))
							if((1 == (i-prevIndex))){
								//System.out.println("\n succesive addition to buffer "+currIndex+"\t>>"+ originalSentence);
								bufferPosSentence.append(currentSentence.concat(" . "));
								bufferOrgSentence.append(originalSentence.concat(" . "));
								posTagSentences.set(currIndex, bufferPosSentence.toString());
								orgSentences.set(currIndex, bufferOrgSentence.toString());
								sentIndex = clubSentIndex;
								prevIndex = -1;
							}else{
								String prevString = decoyPosTagSentences.get(currIndex).trim();
								prevString = prevString.substring(0, (prevString.length()-1)).trim();
								posTagSentences.set(currIndex, prevString);
								bufferPosSentence = new StringBuilder();
								bufferPosSentence.append(prevString.concat(" . "));
								prevString = decoyOrgSentences.get(currIndex).trim();
								prevString = prevString.substring(0, (prevString.length()-1)).trim();
								orgSentences.set(currIndex, prevString);
								bufferOrgSentence = new StringBuilder();
								bufferOrgSentence.append(prevString.concat(" . "));
								//System.out.println("\n readjusting buffer "+currIndex+"\t>>"+ bufferOrgSentence.toString());
								clubSentIndex = String.valueOf(currIndex).concat("@");
								sentIndex="";
								prevIndex = i;
								i++;
							}
						}
						**/
						sentIndex="";
						i++;
					}else{
						//System.out.println("\n current sentence has no relation "+currIndex);
						if(bufferPosSentence.length() > 0){
							//System.out.println("\n removing "+ bufferOrgSentence.toString());
							bufferPosSentence = new StringBuilder();
							bufferOrgSentence = new StringBuilder();
							prevIndex = -1;
						}
						sentIndex="";
						i++;
					}
				}
				//System.out.println("\n\t FINAL >>"+tier1MapValue.getValue());
			}
			// list should not be empty and should have more than 1 element
			if(!sentenceEntityRelationManager.isEmpty()){
				
				//System.out.println("\n\t>>"+sentenceEntityRelationManager);
				HashMap<String, ArrayList<TreeSet<Integer>>> decoyHash = new LinkedHashMap<>();
				decoyHash = sentenceEntityRelationManager.get(docId);
				
				// check for all  the relations before identifying negatives
				Set<Integer> completeEntityRelationSet = new HashSet<>();
				for(String entity : decoyHash.keySet()){
					for(TreeSet<Integer> decoyTree : decoyHash.get(entity)){
						completeEntityRelationSet.addAll(decoyTree);
					}
				}
				Set<Integer> acceptedEntityRelationSet = new HashSet<>();
				for(Integer instanceId : posTaggedSentenceAssembler.keySet()){
					if(instanceId > 0){
						//System.out.println("\n\t>>"+instanceId);
						String acceptedInstanceId = posTaggedSentenceAssembler.get(instanceId).keySet().toString();
						acceptedInstanceId = acceptedInstanceId.replaceAll("\\[|\\]", "").trim();
						Matcher relationInclusionMatcher = Pattern.compile(docId.concat("(\\@(\\d)+)+R(\\d)+"))
								.matcher(acceptedInstanceId);
						while(relationInclusionMatcher.find()){
							acceptedEntityRelationSet.add(
									Integer.parseInt(relationInclusionMatcher.group(0).split("R")[1]));
						}
					}
				}
				if(completeEntityRelationSet.size() != acceptedEntityRelationSet.size()){
					if(completeEntityRelationSet.size() > acceptedEntityRelationSet.size()){
						System.err.println("\n\t sentenceBoundaryIdentification() ~ Incomplete Relation Addition "
								+Sets.diff(completeEntityRelationSet,acceptedEntityRelationSet)+"\t>"+docId);
					}else{
						System.err.println("\n\t sentenceBoundaryIdentification() ~ Incomplete Relation Addition "
								+Sets.diff(acceptedEntityRelationSet,completeEntityRelationSet)+"\t>"+docId);
					}
				}
				/**
				Set<HashMap<String, String>> negativeSet = generateNegativeInstances(decoyHash);
				if(!negativeSet.isEmpty()){
					negativeInstances.put(docId, negativeSet);
					for(HashMap<String, String> temp : negativeInstances.get(docId)){
						//System.out.println("\n\t>>>"+temp);
					}
				}else{
					sentenceEntityRelationManager.remove(docId);
					//System.err.println("\n\t generateNegativeInstances() error solved");
				}**/
			}else{
				System.err.println("\n\t sentenceBoundaryIdentification() ~ No Relation Present");
			}

			//}
		}
		
		
		/**
		System.out.println("\n\t>>>>>>>>>>>>>>>>>>>>>>>>");
		Iterator<String> itr = entityRelationIdentifierTable.keySet().iterator();
		while(itr.hasNext()){
			String str = itr.next();
			System.out.println("\t"+str+"\t"+entityRelationIdentifierTable.get(str));
		}**/
		
		return(new BoundaryReturn(posTaggedSentenceAssembler, originalSentenceAssembler, 
				entityRelationIdentifierTable, mainRelationVerbList,negativeInstances));
	}
	 
	 /**
	 * Put the current thread to sleep 
	 */
	private void haltThreadProcess() {
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public BoundaryReturn call() throws Exception {
		
		Iterator<String> tagIterator = sentenceCluster.keySet().iterator();
		while(tagIterator.hasNext()){
			String docId = tagIterator.next();
			ArrayList<String> posTagSentences = new ArrayList<>(sentenceCluster.get(docId));
			ArrayList<String> orgSentences = new ArrayList<>(orgSentenceCluster.get(docId));
			// ARRAY SIZE INTEGRITY TEST
			if(sentenceCluster.get(docId).size() != orgSentenceCluster.get(docId).size()){
				System.err.println("\n\t>>"+posTagSentences.size()+"\t>>"+orgSentenceCluster.size()+"\t\t>"+docId);
				System.err.println("\n\t>>"+posTagSentences+"\n\t>>"+orgSentenceCluster);
			}

			//if(docId.equals("17203585")){
			int index=0;
			//System.out.println("\n\t>>"+index);
			LinkedHashMap<String, LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>>> relationEntitySet = 
						new LinkedHashMap<>();
			while(index < posTagSentences.size()){
				String currentSentence = posTagSentences.get(index).trim();
				String originalSentence = orgSentences.get(index).trim();
				currentSentence = currentSentence.substring(0, (currentSentence.length()-1)).trim();
				originalSentence = originalSentence.substring(0, (originalSentence.length()-1)).trim();
				relationEntitySet = gatherRelationIndices(currentSentence, docId, index, relationEntitySet);
				index++;
			}
			if(relationEntitySet.containsKey("-1")){
				LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>> temp = 
						new LinkedHashMap<>(relationEntitySet.get("-1"));
				relationEntitySet.put(String.valueOf(relationEntitySet.size()-1), temp);
				relationEntitySet.remove("-1");
			}
			//System.out.println("\n\t relation itr>>>"+ relationEntitySet);
			
			Iterator<Map.Entry<String, LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>>>> tier1Itr = 
					relationEntitySet.entrySet().iterator();
			while(tier1Itr.hasNext()){
				int sentenceAdded=0,prevIndex=-1;
				String sentIndex="", clubSentIndex="";
				StringBuilder bufferPosSentence = new StringBuilder();
				StringBuilder bufferOrgSentence = new StringBuilder();
				Map.Entry<String, LinkedHashMap<Integer, LinkedHashMap<Integer, Set<String>>>> tier1MapValue = 
						tier1Itr.next();
				//System.out.println("\n\t relationType>>"+tier1MapValue.getKey()+"\t indices>>"+tier1MapValue.getValue());
				ArrayList<Integer> decoyList = new ArrayList<>(tier1MapValue.getValue().keySet());
				posTagSentences = new ArrayList<>(sentenceCluster.get(docId));
				orgSentences = new ArrayList<>(orgSentenceCluster.get(docId));
				ArrayList<String> decoyPosTagSentences = new ArrayList<>(posTagSentences);
				ArrayList<String> decoyOrgSentences = new ArrayList<>(orgSentences);
				int i=0;
				//System.out.println("\n\t ES***************>>"+entityComponentTier1);
				while(i < decoyList.size()){
					//System.out.println("\n\t i value>>"+i);
					Integer currIndex = decoyList.get(i);
					String currentSentence = posTagSentences.get(currIndex).trim();
					String originalSentence = orgSentences.get(currIndex).trim();
					//System.out.println("\n\t currIndex>"+currIndex+"\n\t sentence b4::"+currentSentence+"\t>>"+originalSentence);
					currentSentence = currentSentence.substring(0, (currentSentence.length()-1)).trim();
					originalSentence = originalSentence.substring(0, (originalSentence.length()-1)).trim();
					//System.out.println("\n\t currIndex>"+currIndex+"\n\t sentence after::"+currentSentence+"\t>>"+originalSentence);
					sentIndex = sentIndex.concat(String.valueOf(currIndex));
					
					LinkedHashMap<Integer, Set<String>> entityTagSet = new LinkedHashMap<>(tier1MapValue.getValue().get(currIndex));
					entityTagSet.putAll(tier1MapValue.getValue().get(currIndex));
					if(sentIndex.contains("@")){
						//System.out.println("\n\t compounded >>"+sentIndex);
						if(!tier1MapValue.getValue().get(decoyList.get(i-1)).isEmpty()){
							Iterator<Map.Entry<Integer, Set<String>>> tier2Itr = 
									tier1MapValue.getValue().get(decoyList.get(i-1)).entrySet().iterator();
							while(tier2Itr.hasNext()){
								Set<String> decoySet = new HashSet<>();
								Map.Entry<Integer, Set<String>> tier2MapValue = tier2Itr.next();
								//System.out.println("\n\t mark3 >>"+tier1MapValue.getValue().get(currIndex));
								if(entityTagSet.containsKey(tier2MapValue.getKey())){
									decoySet = new HashSet<>(entityTagSet.get(tier2MapValue.getKey()));
								}
								//System.out.println("\n\t mark4 >>"+tier1MapValue.getValue().get(currIndex));
								decoySet.addAll(tier2MapValue.getValue());
								//System.out.println("\n\t mark1 >>"+tier1MapValue.getValue().get(currIndex));
								entityTagSet.put(tier2MapValue.getKey(), decoySet);
								//System.out.println("\n\t mark2 >>"+tier1MapValue.getValue().get(currIndex));
							}
						}
					}
					//System.out.println("\n\t>>"+entityTagSet);
					//System.out.println("\n\t mark >>"+tier1MapValue.getValue().get(currIndex));
					if(tier1MapValue.getValue().get(currIndex).containsKey(0)){
						//System.out.println("\n\t mark0 >>"+tier1MapValue.getValue().get(currIndex).get(0).size());
					}else if(tier1MapValue.getValue().get(currIndex).containsKey(1)){
						//System.out.println("\n\t mark1 >>"+tier1MapValue.getValue().get(currIndex).get(1).size());
					}
					
					boolean sizeStatus = true;
					if(tier1MapValue.getValue().get(currIndex).containsKey(0)){
						if(tier1MapValue.getValue().get(currIndex).get(0).size() > 1000){
							sizeStatus = false;
							sentenceAdded = 0;
							break;
						}
					}else if (tier1MapValue.getValue().get(currIndex).containsKey(1)){
						if(tier1MapValue.getValue().get(currIndex).get(1).size() > 1000){
							sizeStatus = false;
							sentenceAdded = 0;
							break;
						}
					}
					if(sizeStatus){
						sentenceAdded = gatherIntraSentientRelationRelevance(docId, currentSentence, 
								originalSentence, entityTagSet, tier1MapValue.getKey(), sentIndex);
					}
					entityTagSet.clear();
					if(sentenceAdded == 6){
						//System.out.println("\n current sentence has relation "+currIndex);
						//Remove relation sentences and the previous buffer
						if(bufferPosSentence.length() > 0){
							//System.out.println("\n removing "+ bufferOrgSentence.toString());
							bufferPosSentence = new StringBuilder();
							bufferOrgSentence = new StringBuilder();
							prevIndex = -1;
						}
						sentIndex="";
						i++;
					}else if((sentenceAdded == 4)||(sentenceAdded==5)){
						/**
						if(bufferPosSentence.length()==0){
							//System.out.println("\n adding to buffer "+currIndex+"\t>>"+ originalSentence);
							bufferPosSentence.append(currentSentence.concat(" . "));
							bufferOrgSentence.append(originalSentence.concat(" . "));
							clubSentIndex = String.valueOf(currIndex).concat("@");
							sentIndex="";
							prevIndex = i;
							i++;
						}else{
							//System.out.println("\n\t 1 value>>"+(i-prevIndex)+"\t 2 value >>"+(currIndex-decoyList.get(i-1)));
							//|| (1 == (currIndex-decoyList.get(i-1)))
							if((1 == (i-prevIndex))){
								//System.out.println("\n succesive addition to buffer "+currIndex+"\t>>"+ originalSentence);
								bufferPosSentence.append(currentSentence.concat(" . "));
								bufferOrgSentence.append(originalSentence.concat(" . "));
								posTagSentences.set(currIndex, bufferPosSentence.toString());
								orgSentences.set(currIndex, bufferOrgSentence.toString());
								sentIndex = clubSentIndex;
								prevIndex = -1;
							}else{
								String prevString = decoyPosTagSentences.get(currIndex).trim();
								prevString = prevString.substring(0, (prevString.length()-1)).trim();
								posTagSentences.set(currIndex, prevString);
								bufferPosSentence = new StringBuilder();
								bufferPosSentence.append(prevString.concat(" . "));
								prevString = decoyOrgSentences.get(currIndex).trim();
								prevString = prevString.substring(0, (prevString.length()-1)).trim();
								orgSentences.set(currIndex, prevString);
								bufferOrgSentence = new StringBuilder();
								bufferOrgSentence.append(prevString.concat(" . "));
								//System.out.println("\n readjusting buffer "+currIndex+"\t>>"+ bufferOrgSentence.toString());
								clubSentIndex = String.valueOf(currIndex).concat("@");
								sentIndex="";
								prevIndex = i;
								i++;
							}
						}
						**/
						sentIndex="";
						i++;
					}else{
						//System.out.println("\n current sentence has no relation "+currIndex);
						if(bufferPosSentence.length() > 0){
							//System.out.println("\n removing "+ bufferOrgSentence.toString());
							bufferPosSentence = new StringBuilder();
							bufferOrgSentence = new StringBuilder();
							prevIndex = -1;
						}
						sentIndex="";
						i++;
					}
				}
				//System.out.println("\n\t FINAL >>"+tier1MapValue.getValue());
				//haltThreadProcess();
			}
			// list should not be empty and should have more than 1 element
			if(!sentenceEntityRelationManager.isEmpty()){
				
				//System.out.println("\n\t>>"+sentenceEntityRelationManager);
				HashMap<String, ArrayList<TreeSet<Integer>>> decoyHash = new LinkedHashMap<>();
				decoyHash = sentenceEntityRelationManager.get(docId);
				
				// check for all  the relations before identifying negatives
				Set<Integer> completeEntityRelationSet = new HashSet<>();
				for(String entity : decoyHash.keySet()){
					for(TreeSet<Integer> decoyTree : decoyHash.get(entity)){
						completeEntityRelationSet.addAll(decoyTree);
					}
				}
				Set<Integer> acceptedEntityRelationSet = new HashSet<>();
				for(Integer instanceId : posTaggedSentenceAssembler.keySet()){
					if(instanceId > 0){
						//System.out.println("\n\t>>"+instanceId);
						String acceptedInstanceId = posTaggedSentenceAssembler.get(instanceId).keySet().toString();
						acceptedInstanceId = acceptedInstanceId.replaceAll("\\[|\\]", "").trim();
						Matcher relationInclusionMatcher = Pattern.compile(docId.concat("(\\@(\\d)+)+R(\\d)+"))
								.matcher(acceptedInstanceId);
						while(relationInclusionMatcher.find()){
							acceptedEntityRelationSet.add(
									Integer.parseInt(relationInclusionMatcher.group(0).split("R")[1]));
						}
					}
				}
				if(completeEntityRelationSet.size() != acceptedEntityRelationSet.size()){
					if(completeEntityRelationSet.size() > acceptedEntityRelationSet.size()){
						System.err.println("\n\t sentenceBoundaryIdentification() ~ Incomplete Relation Addition "
								+Sets.diff(completeEntityRelationSet,acceptedEntityRelationSet)+"\t>"+docId);
					}else{
						System.err.println("\n\t sentenceBoundaryIdentification() ~ Incomplete Relation Addition "
								+Sets.diff(acceptedEntityRelationSet,completeEntityRelationSet)+"\t>"+docId);
					}
				}
				/**
				Set<HashMap<String, String>> negativeSet = generateNegativeInstances(decoyHash);
				if(!negativeSet.isEmpty()){
					negativeInstances.put(docId, negativeSet);
					for(HashMap<String, String> temp : negativeInstances.get(docId)){
						//System.out.println("\n\t>>>"+temp);
					}
				}else{
					sentenceEntityRelationManager.remove(docId);
					//System.err.println("\n\t generateNegativeInstances() error solved");
				}**/
			}else{
				System.err.println("\n\t sentenceBoundaryIdentification() ~ No Relation Present");
			}

			//}
			haltThreadProcess();
		}
		
		/**
		System.out.println("\n\t>>>>>>>>>>>>>>>>>>>>>>>>");
		Iterator<String> itr = entityRelationIdentifierTable.keySet().iterator();
		while(itr.hasNext()){
			String str = itr.next();
			System.out.println("\t"+str+"\t"+entityRelationIdentifierTable.get(str));
		}**/
		
		return(new BoundaryReturn(posTaggedSentenceAssembler, originalSentenceAssembler, 
				entityRelationIdentifierTable, mainRelationVerbList,negativeInstances));
	}
	 
}
