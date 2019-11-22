/**
 * 
 */
package com.prj.bundle.tsvSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.TreeMap;

/**
 * @author neha
 *
 */
public class PermuteCPRTSV {
	
	private Properties systemProperties;
	
	/**
	 * Constructors
	 * @throws IOException 
	 */
	public PermuteCPRTSV() throws IOException {
		
		this.systemProperties = new Properties();
		InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
	}
	
	private LinkedHashMap<String, String> loadAbstracts() throws IOException {

		LinkedHashMap<String, String> decoyMap = new LinkedHashMap<>();
		String fileName = systemProperties.getProperty("abstractTrainingFile");
		FileReader fileRS = new FileReader(fileName);
		BufferedReader buffRS = new BufferedReader(fileRS);
		String readLine = buffRS.readLine();
		while(null != readLine){
			String[] abstractSubset = readLine.split("\t");
			decoyMap.put(abstractSubset[0], abstractSubset[1].concat(" "+abstractSubset[2]));
			readLine = buffRS.readLine();
		}
		buffRS.close();
		return(decoyMap);
	}
	
	private LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> loadEntities() 
			throws IOException {

		LinkedHashMap<String,LinkedHashMap<String,ArrayList<String>>> decoyMap = 
				new LinkedHashMap<>();
		String fileName = systemProperties.getProperty("entitiesTrainingFile");
		FileReader fileRS = new FileReader(fileName);
		BufferedReader buffRS = new BufferedReader(fileRS);
		String readLine = buffRS.readLine();
		while(null != readLine){
			ArrayList<String> entityList = new ArrayList<>(Arrays.asList(readLine.split("\t")));
			LinkedHashMap<String,ArrayList<String>> subDecoyMap = new LinkedHashMap<>();
			if(decoyMap.containsKey(entityList.get(0))){
				subDecoyMap = decoyMap.get(entityList.get(0));
			}
			ArrayList<String> decoyList = new ArrayList<>();
			for(int i=2;i<entityList.size();i++){
				decoyList.add(entityList.get(i));
			}
			subDecoyMap.put(entityList.get(1),decoyList);
			decoyMap.put(entityList.get(0), subDecoyMap);
			readLine = buffRS.readLine();
		}
		buffRS.close();
		return(decoyMap);
	}
	
	private TreeMap<Integer, ArrayList<String>> reorganizeEntityMap(
			LinkedHashMap<String, ArrayList<String>> entityHashMap) {
		
		Iterator<Map.Entry<String, ArrayList<String>>> tier1Itr = 
				entityHashMap.entrySet().iterator();
		TreeMap<Integer, ArrayList<String>> entityRangeMap = new TreeMap<>();
		while(tier1Itr.hasNext()){
			Map.Entry<String, ArrayList<String>> tier1MapValue = tier1Itr.next();
			ArrayList<String> decoyList = tier1MapValue.getValue();
			entityRangeMap.put(
					Integer.parseInt(decoyList.get(1)),new ArrayList<>(
							Arrays.asList(decoyList.get(0),tier1MapValue.getKey())));
		}
		//System.out.println("\n\t>>"+entityRangeMap);
		return(entityRangeMap);
	}
	
	private int generateRelationPairs(LinkedHashMap<String, ArrayList<String>> entityHashMap, 
			String abstractText, String docId, int appendFile) throws IOException {
		
		HashSet<String> relationPair = new HashSet<>();
		HashSet<Integer> indexPaired = new HashSet<>();
		TreeMap<Integer, ArrayList<String>> reorderedEntityMap = reorganizeEntityMap(entityHashMap);
		ArrayList<Integer> sequenceIndexList = new ArrayList<>(reorderedEntityMap.keySet());
		for(int i=0;i<sequenceIndexList.size()-1;i++){
			ArrayList<String> firstArray = reorderedEntityMap.get(sequenceIndexList.get(i));
			for(int j=i+1;j<sequenceIndexList.size();j++){
				ArrayList<String> secondArray = reorderedEntityMap.get(sequenceIndexList.get(j));
				String rangeText = abstractText.
						substring(sequenceIndexList.get(i),sequenceIndexList.get(j));
				// compare if the sentence terminal delimiter exists in substring
				Matcher sentenceSplitMatcher = Pattern.compile(
						"((\\.\\s)|(\\?\\s)|(\\!\\s))([A-Z0-9])").matcher(rangeText);
				int delimiterCount = 0;
				while(sentenceSplitMatcher.find()){
					delimiterCount++;
				}
				// check if entities exists between 2 adjacent sentences with single delimiter in between
				// or entities are present within the same sentence
				//(delimiterCount == 1 && (!indexPaired.contains(sequenceIndexList.get(i)))) ||
				if( (delimiterCount == 0)){
					// relations should always be in the form of chemical and gene
					if(!firstArray.get(0).contentEquals(secondArray.get(0))){
						if(Integer.parseInt(firstArray.get(1).replaceAll("T", "")) 
								< Integer.parseInt(secondArray.get(1).replaceAll("T", ""))){
							relationPair.add("Arg1:".concat(firstArray.get(1)+"\tArg2:"+secondArray.get(1)));
						}else{
							relationPair.add("Arg1:".concat(secondArray.get(1)+"\tArg2:"+firstArray.get(1)));
						}
						indexPaired.add(sequenceIndexList.get(i));
					}
				}else{
					break;
				}
			}
		}
		TreeMap<Integer, String> cprClassType = new TreeMap<>();
		FileReader fileRead = new FileReader(systemProperties.getProperty("cprClassType"));
		BufferedReader buffRead = new BufferedReader(fileRead);
		String lineRead = buffRead.readLine();
		while(null != lineRead){
			String[] typeTokens = lineRead.split("\t");
			cprClassType.put(
					Integer.parseInt(typeTokens[0]),typeTokens[1]);
			lineRead = buffRead.readLine();
		}
		buffRead.close();
		ArrayList<String> decoyList = new ArrayList<>(); 
		for(String eachPair : relationPair){
			for(Integer classType : cprClassType.keySet()){
				if((classType==3)||(classType==4)||(classType==5)||(classType==6)
						||(classType==9)||(classType==10)){
					String completeRelation = docId.concat("\tCPR:"+String.valueOf(classType)).
							concat("\tY\t"+cprClassType.get(classType)+"\t").concat(eachPair);
					decoyList.add(completeRelation);
				}
			}
		}
		if(!decoyList.isEmpty()){
			FileWriter fileWS;
			if(appendFile > 0){
				fileWS = new FileWriter(systemProperties.getProperty("relationTrainingFile"),true);
			}else{
				fileWS = new FileWriter(systemProperties.getProperty("relationTrainingFile"),false);
			}
			BufferedWriter buffWS = new BufferedWriter(fileWS);
			for(String currrentPair : decoyList){
				buffWS.write(currrentPair);
				buffWS.newLine();
			}
			buffWS.close();
			appendFile++;
		}
		return(appendFile);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			PermuteCPRTSV permuteInstance = new PermuteCPRTSV();
			LinkedHashMap<String, String> abstractMap = permuteInstance.loadAbstracts();
			LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> entityMap = 
					permuteInstance.loadEntities();
			Iterator<Map.Entry<String, String>> tier1Itr = abstractMap.entrySet().iterator();
			int appendFile = 0;
			while(tier1Itr.hasNext()){
				Map.Entry<String, String> tier1MapValue = tier1Itr.next();
				if(entityMap.containsKey(tier1MapValue.getKey())){
					LinkedHashMap<String, ArrayList<String>> entityHashMap = 
							entityMap.get(tier1MapValue.getKey());
					appendFile = permuteInstance.generateRelationPairs(entityHashMap, 
							tier1MapValue.getValue(),tier1MapValue.getKey(),appendFile);
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
