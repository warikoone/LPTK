/**
 * 
 */
package com.prj.bundle.svmLearning;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.DocFlavor.STRING;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

/**
 * @author neha
 *
 */
public class SVM_AIFeatureKernel {

	public HashMap<Integer, HashMap<String, String>> posTaggedRelationSentence;
	public HashMap<Integer, HashMap<String, String>> originalRelationSentence;
	public HashMap<Integer, 
	HashMap<String, HashMap<String, HashMap<String, ArrayList<Integer>>>>> instanceAIContextPatterns;
	/**
	 * Constructors
	 */
	public SVM_AIFeatureKernel() {
		posTaggedRelationSentence = new LinkedHashMap<>();
		originalRelationSentence = new LinkedHashMap<>();
		instanceAIContextPatterns = new LinkedHashMap<>();
	}
	
	public ArrayList<Integer> findKeyTermIndex(String patternToken, String pattern) {
		
		if(!patternToken.equalsIgnoreCase("RELATION")){
			patternToken = patternToken.concat("PRI");
		}
		ArrayList<String> patternArray = new ArrayList<>(Arrays.asList(pattern.split(" ")));
		ArrayList<Integer> patternIndex = new ArrayList<>();
		for(int i=0;i<patternArray.size();i++){
			if(patternArray.get(i).contentEquals(patternToken)){
				patternIndex.add(i);
			}
		}
		//System.out.println("\n\t>>"+patternToken+"\t>>"+patternIndex);
		return(patternIndex);
	}
	
	private void manageInstances(SVM_AIFeatureKernel featureInstance) {

		SortedSet<Integer> instanceSet = new TreeSet<>(Arrays.asList(3,4,5,6));
		TreeMap<Integer, ArrayList<String>> triggerWordTreeMap = new TreeMap<>();
		for(Integer instanceType : instanceSet){
			ArrayList<String> decoyList = new ArrayList<>();
			switch (instanceType) {
			case 3:
				decoyList.addAll(Arrays.asList("regula","activ"));
				break;
			case 4:
				decoyList.addAll(Arrays.asList("down regulate","inhibit"));
				break;
			case 5:
				decoyList.addAll(Arrays.asList("agonist"));
				break;
			case 6:
				decoyList.addAll(Arrays.asList("antagonist"));
				break;
			}
			triggerWordTreeMap.put(instanceType, decoyList);
		}
		System.out.println("\n\t>>"+triggerWordTreeMap);
		
		Matcher triggerWordMatcher;
		Iterator<Map.Entry<Integer,HashMap<String, String>>> tier1Itr = featureInstance.originalRelationSentence.
				entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer,HashMap<String, String>> tier1MapValue = tier1Itr.next();
			if(triggerWordTreeMap.containsKey(tier1MapValue.getKey())){
				Iterator<Map.Entry<String, String>> tier2Itr = tier1MapValue.getValue().entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<String, String> tier2MapValue = tier2Itr.next();
					int triggerWordFound = 0;
					//System.out.println("\n\t>>"+triggerWordTreeMap.get(tier1MapValue.getKey()));
					for(String triggerWord : triggerWordTreeMap.get(tier1MapValue.getKey())){
						String originalSentence = tier2MapValue.getValue();
						triggerWordMatcher = Pattern.compile(triggerWord,Pattern.CASE_INSENSITIVE).
								matcher(originalSentence);
						while(triggerWordMatcher.find()){
							triggerWordFound++;
						}
					}
					if(triggerWordFound == 0){
						//System.out.println("\n\tremoved instance>>"+tier1MapValue.getKey()+"\t>>"+tier1MapEntry.getKey());
						HashMap<String, String> tempHash = featureInstance.posTaggedRelationSentence.
								get(tier1MapValue.getKey());
						if(tempHash.containsKey(tier2MapValue.getKey())){
							tempHash.remove(tier2MapValue.getKey());
						}
						featureInstance.posTaggedRelationSentence.put(tier1MapValue.getKey(), tempHash);
						tier2Itr.remove();
					}
				}
			}
		}
	}
	
	private void screenPatternsForTestInstances(
			HashMap<Integer, HashMap<String, Set<String>>> aivScoredUniqueInstancesHash, 
			SVM_AIFeatureKernel featureInstance) {
		
		TreeMap<Integer, Integer> minInstance = new TreeMap<>();
		for(Integer instanceType: aivScoredUniqueInstancesHash.keySet()){
			int totalSize = 0;
			totalSize = aivScoredUniqueInstancesHash.get(instanceType).size();
			/**
			for(String patternType : aivScoredUniqueInstancesHash.get(instanceType).keySet()){
				totalSize = totalSize + aivScoredUniqueInstancesHash.get(instanceType).
						get(patternType).size();
			}**/
			minInstance.put(totalSize, instanceType);
		}
		int patternSize = 0;
		// change for minimum/maximum switch
		NavigableMap<Integer, Integer> instanceSizeMap =  minInstance.descendingMap();
		//patternSize = instanceSizeMap.firstKey();
		
		for(Integer sizeVal : instanceSizeMap.keySet()){
			if(instanceSizeMap.get(sizeVal) > 0){
				patternSize = (patternSize + sizeVal);
			}
		}
		patternSize = (patternSize/instanceSizeMap.size());
		System.out.println("\n\tmaxSize>>"+patternSize);
		TreeMap<Integer, Set<String>> acceptedPatterns = new TreeMap<>();
		for(Integer instanceType : aivScoredUniqueInstancesHash.keySet()){
			Set<String> decoyPatternSet = new HashSet<>();
			HashMap<String, Set<String>> decoyHash = new LinkedHashMap<>();
			decoyHash = aivScoredUniqueInstancesHash.get(instanceType);
			int index = 0, instanceIndex=0;
			System.out.println("\n\t accepted >>"+instanceType+"\t>>"+decoyHash.size());
			if(instanceType < 0){
				patternSize = 0;
				for(String pattern : decoyHash.keySet()){
					patternSize = patternSize + decoyHash.get(pattern).size();
				}
				patternSize = patternSize/2;
			}
			System.out.println("\n\t -ve >>"+patternSize+"\t>>"+decoyHash.size());
			while(instanceIndex < patternSize){
				//System.out.println("\n\t -ve >>"+patternSize+"\t>>"+decoyHash.size());
				//System.out.println("\n\t accepted >>"+instanceIndex+"\t>>"+decoyHash.size());
				int exitFlag = 0;
				for(Map.Entry<String, Set<String>> tempEntry : decoyHash.entrySet()){
					//System.out.println("\n\t>>"+tempEntry.getKey()+"\t>>"+instanceType);
					ArrayList<String> decoyList = new ArrayList<>(tempEntry.getValue());
					if(index < decoyList.size()){
						//System.out.println("\n\t2.>>"+index+"\t2.>>"+decoyList.size());
						if(instanceIndex < patternSize){
							//System.out.println("\n\tadded>>"+decoyList.get(index));
							if(!decoyPatternSet.contains(tempEntry.getKey())){
								decoyPatternSet.add(tempEntry.getKey());
							}
							instanceIndex++;
						}else{
							break;
						}
					}else{
						exitFlag++;
					}
				}
				if(exitFlag == decoyHash.size()){
					break;
				}
				index++;
			}
			acceptedPatterns.put(instanceType, decoyPatternSet);
			System.out.println("\n\t update>>"+instanceType+"\t>>"+decoyPatternSet.size());
		}
		if(!acceptedPatterns.isEmpty()){
			for(Integer instanceType : acceptedPatterns.keySet()){
				Set<String> patterns = acceptedPatterns.get(instanceType);
				if(featureInstance.instanceAIContextPatterns.containsKey(instanceType)){
					HashMap<String, HashMap<String, HashMap<String, ArrayList<Integer>>>> patternSet = 
							featureInstance.instanceAIContextPatterns.get(instanceType);
					Iterator<Map.Entry<String, HashMap<String, HashMap<String, ArrayList<Integer>>>>> tier1Itr = 
							patternSet.entrySet().iterator();
					while(tier1Itr.hasNext()){
						Map.Entry<String, HashMap<String, HashMap<String, ArrayList<Integer>>>> tier1MapValue = 
								tier1Itr.next();
						if(!patterns.contains(tier1MapValue.getKey().trim())){
							tier1Itr.remove();
						}
					}
				}
			}
		}
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			SVM_AIFeatureKernel featureInstance = new SVM_AIFeatureKernel();
			SVM_ParseTreeFeature_FeatureVectorSplitInstance parseTreeInstance = 
					new SVM_ParseTreeFeature_FeatureVectorSplitInstance();
			Properties systemProperties = new Properties();
			InputStream propertyStream  = new FileInputStream("config.properties");
			systemProperties.load(propertyStream);
			ArrayList<Integer> acceptedClassTypes = 
					new ArrayList<>(Arrays.asList(9,6,5,4,3,-1));
			FileReader fileRead;
			BufferedReader buffRead;
			String lineRead;
			fileRead = new FileReader(systemProperties.getProperty("trainingRelationSentence"));
			buffRead = new BufferedReader(fileRead);
			lineRead = buffRead.readLine();
			while(null != lineRead){
				String[] lineArray = lineRead.split("\t:");
				Integer instance = Integer.parseInt(lineArray[0].trim());
				if(acceptedClassTypes.contains(instance)){
					HashMap<String, String> decoyHash = new LinkedHashMap<>();
					if(featureInstance.posTaggedRelationSentence.containsKey(instance)){
						decoyHash = featureInstance.posTaggedRelationSentence.get(instance);
					}
					String reasembleString="";
					for(int i=2;i<lineArray.length;i++){
						reasembleString = reasembleString.concat(lineArray[i]);
					}
					decoyHash.put(lineArray[1].trim(),reasembleString);
					if(!decoyHash.isEmpty()){
						featureInstance.posTaggedRelationSentence.put(instance,decoyHash);
					}
				}
				lineRead = buffRead.readLine();
			}
			fileRead = new FileReader(systemProperties.getProperty("trainingOriginalSentence"));
			buffRead = new BufferedReader(fileRead);
			lineRead = buffRead.readLine();
			while(null != lineRead){
				String[] lineArray = lineRead.split("\t:");
				Integer instance = Integer.parseInt(lineArray[0].trim());
				if(acceptedClassTypes.contains(instance)){
					HashMap<String, String> decoyHash = new LinkedHashMap<>();
					if(featureInstance.originalRelationSentence.containsKey(instance)){
						decoyHash = featureInstance.originalRelationSentence.get(instance);
					}
					String reasembleString="";
					for(int i=2;i<lineArray.length;i++){
						reasembleString = reasembleString.concat(lineArray[i]);
					}
					decoyHash.put(lineArray[1].trim(),reasembleString);
					if(!decoyHash.isEmpty()){
						featureInstance.originalRelationSentence.put(instance,decoyHash);
					}
				}
				lineRead = buffRead.readLine();
			}
			for(Integer instance : featureInstance.posTaggedRelationSentence.keySet()){
				System.out.println("\n\t b4 posatgged size>>"+featureInstance.posTaggedRelationSentence.get(instance).size());
				System.out.println("\n\t b4 originalSent size>>"+featureInstance.originalRelationSentence.get(instance).size());
			}
			// remove unwanted instances
			/**
			featureInstance.manageInstances(featureInstance);
			for(Integer instance : featureInstance.posTaggedRelationSentence.keySet()){
				System.out.println("\n\t aftr posatgged size>>"+featureInstance.posTaggedRelationSentence.get(instance).size());
				System.out.println("\n\tafter originalSent size>>"+featureInstance.originalRelationSentence.get(instance).size());
			}**/
			
			// select patterns to be read
			HashMap<Integer,HashMap<String, Set<String>>> aivScoredUniqueInstancesHash = new LinkedHashMap<>();
			fileRead = new FileReader(systemProperties.getProperty("trainingAivScoresPatterns"));
			buffRead = new BufferedReader(fileRead);
			lineRead = buffRead.readLine();
			while(null != lineRead){
				String[] lineArray = lineRead.split("#");
				HashMap<String, Set<String>> decoyHash = new LinkedHashMap<>();
				Set<String> decoySet = new HashSet<>();
				if(!aivScoredUniqueInstancesHash.isEmpty()){
					if(aivScoredUniqueInstancesHash.containsKey(Integer.parseInt(lineArray[0]))){
						decoyHash = aivScoredUniqueInstancesHash.get(Integer.parseInt(lineArray[0]));
						if(decoyHash.containsKey(lineArray[1])){
							decoySet = decoyHash.get(lineArray[1]);
						}
					}
				}
				decoySet.addAll(Arrays.asList(lineArray[4].replaceAll("\\[|\\]", "").trim().split(",")));
				decoyHash.put(lineArray[1], decoySet);
				aivScoredUniqueInstancesHash.put(Integer.parseInt(lineArray[0]), decoyHash);
				lineRead = buffRead.readLine();
			}
			buffRead.close();

			// pattern reading
			fileRead = new FileReader(systemProperties.getProperty("aiFeaturePatterns"));
			buffRead = new BufferedReader(fileRead);
			lineRead = buffRead.readLine();
			String header="",currentHeader="";
			HashMap<String,HashMap<String, HashMap<String, ArrayList<Integer>>>> 
			decoyContextPattern = new LinkedHashMap<>();
			HashMap<String, HashMap<String, ArrayList<Integer>>> decoyPattern = 
					new LinkedHashMap<>();
			Integer instanceType=0,finalInstance=0;
			while(null != lineRead){
				instanceType=0;
				String patternString="";
				if(lineRead.matches("pattern(|-)\\d+\\:\\d+")){
					patternString = lineRead.replaceAll("pattern(|-)\\d+\\:", "pattern");
					instanceType = Integer.parseInt(lineRead.split("\\:")[0].replaceAll("pattern", ""));
				}
				if(instanceType != 0){
					currentHeader = patternString.trim();
					if(!decoyPattern.isEmpty()){
						decoyContextPattern = new LinkedHashMap<>();
						if(!featureInstance.instanceAIContextPatterns.isEmpty()){
							if(featureInstance.instanceAIContextPatterns.
									containsKey(instanceType)){
								decoyContextPattern = featureInstance.
										instanceAIContextPatterns.get(instanceType);
							}
						}
						decoyContextPattern.put(header, decoyPattern);
						featureInstance.instanceAIContextPatterns.put(
								instanceType,decoyContextPattern);
						decoyPattern = new LinkedHashMap<>();
					}
					header = currentHeader;
					finalInstance = instanceType;
					instanceType = 0;
				}else{
					if(!lineRead.equals("")){
						String[] lineArray = lineRead.split("\t\\|");
						HashMap<String, ArrayList<Integer>> decoyPatternIndex = 
								new LinkedHashMap<>(); 
						decoyPatternIndex.put(lineArray[1].trim(), featureInstance.
								findKeyTermIndex(lineArray[0].trim(),lineArray[1].trim()));
						decoyPattern.put(lineArray[0].trim(),decoyPatternIndex);
					}
				}
				lineRead = buffRead.readLine();
			}
			decoyContextPattern = new LinkedHashMap<>();
			if(!featureInstance.instanceAIContextPatterns.isEmpty()){
				if(featureInstance.instanceAIContextPatterns.containsKey(finalInstance)){
					decoyContextPattern = 
							featureInstance.instanceAIContextPatterns.get(finalInstance);
				}
			}
			decoyContextPattern.put(header, decoyPattern);
			featureInstance.instanceAIContextPatterns.put(finalInstance, decoyContextPattern);
			buffRead.close();
			for(Integer instance : featureInstance.instanceAIContextPatterns.keySet()){
				Iterator<Map.Entry<String, HashMap<String, HashMap<String, ArrayList<Integer>>>>> aiPatternItr =
						featureInstance.instanceAIContextPatterns.get(instance).entrySet().iterator();
				System.out.println("\n\t>>"+instance+"\t size>>"+
						featureInstance.instanceAIContextPatterns.get(instance).keySet().size());
				while(aiPatternItr.hasNext()){
					Map.Entry<String, HashMap<String, HashMap<String, ArrayList<Integer>>>> aiPatternValue = 
							aiPatternItr.next();
					//System.out.println("\n\t>>"+aiPatternValue.getKey()+"\t>>"+aiPatternValue.getValue());
				}
			}
			
			featureInstance.screenPatternsForTestInstances(aivScoredUniqueInstancesHash,featureInstance);
			
			for(Integer instance : featureInstance.instanceAIContextPatterns.keySet()){
				Iterator<Map.Entry<String, HashMap<String, HashMap<String, ArrayList<Integer>>>>> aiPatternItr =
						featureInstance.instanceAIContextPatterns.get(instance).entrySet().iterator();
				System.out.println("\n\t after>>"+instance+"\t size>>"+
						featureInstance.instanceAIContextPatterns.get(instance).keySet().size());
				while(aiPatternItr.hasNext()){
					Map.Entry<String, HashMap<String, HashMap<String, ArrayList<Integer>>>> aiPatternValue = 
							aiPatternItr.next();
					//System.out.println("\n\t>>"+aiPatternValue.getKey()+"\t>>"+aiPatternValue.getValue());
				}
			}
			
			parseTreeInstance.createAIPatternBasedParseTree(featureInstance);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}