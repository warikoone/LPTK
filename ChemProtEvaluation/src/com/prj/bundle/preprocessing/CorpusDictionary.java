/**
 * 
 */
package com.prj.bundle.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author neha
 *
 */
public class CorpusDictionary implements Callable<Hashtable<String, 
LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>>>> {

	private String currentDocId;
	private LinkedHashMap<String, ArrayList<String>> corpusMap;
	private LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>> processChemicalEntities;
	private LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>> processGeneEntities;
	private LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>> processAbstractCollection;
	private LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>> processRelationCollection;
	
	/**
	 * Constructors
	 */
	public CorpusDictionary() {
		
	}
	
	public CorpusDictionary(String currDocId, LinkedHashMap<String, ArrayList<String>> corpusMap, 
			Hashtable<String, LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>>> resultHolder) {
		
		this.currentDocId = currDocId;
		this.corpusMap = corpusMap;
		this.processChemicalEntities = resultHolder.get("Chemical");
		this.processGeneEntities = resultHolder.get("Gene");
		this.processAbstractCollection = resultHolder.get("Abstract");
		this.processRelationCollection = resultHolder.get("Relation");
	}

	/**
	 * Generate patterns for each token data
	 * @param chemicalName
	 * @return 
	 */
	public String patternBuilder(String tokenString) {
		
		Matcher tempMatch = Pattern.compile("(\\W)").matcher(tokenString);
		StringBuilder tempPatternBuilder = new StringBuilder();
		while(tempMatch.find()){
			int startIndex = tempMatch.start();
			int endIndex = (tempMatch.end()-1);
			String nonCharGroup = tempMatch.group(0);
			String replacePattern = "";
			//System.out.println("\n\t patternBuilder 2 ::"+nonCharGroup +"\t>>"+ startIndex+"\t>>"+endIndex);
			if(!nonCharGroup.matches("\\s")){
				// add escape characters to non letter, non digit and non space match 
				replacePattern = "\\".concat(nonCharGroup);
			}else{
				// keep space as it is
				replacePattern = nonCharGroup;
			}
			
			if((startIndex == 0) && (endIndex != tokenString.length()-1)){
				tempPatternBuilder.append(replacePattern);
				tokenString = tokenString.substring(endIndex+1,tokenString.length());
				//System.out.println("\nstart 0 \t"+tokenString+"\t"+tempPatternBuilder.toString());
			}else if((startIndex != 0) && (endIndex == tokenString.length()-1)){
				tempPatternBuilder.append(tokenString.substring(0,startIndex).concat(replacePattern));
				tokenString = "";
				//System.out.println("\n end full\t"+tokenString+"\t"+tempPatternBuilder.toString());
			}else if((startIndex != 0) && (endIndex != tokenString.length()-1)){
				//System.out.println("^^^^\t"+tokenString.substring(0, startIndex).concat(replacePattern));
				tempPatternBuilder.append(tokenString.substring(0, startIndex).concat(replacePattern));
				tokenString = tokenString.substring(endIndex+1,tokenString.length());
				//System.out.println("\n middle \t"+tokenString+"\t"+tempPatternBuilder.toString());
			}else if((startIndex == 0) && (endIndex == 0)){
				tempPatternBuilder.append(replacePattern);
				tokenString="";
				break;
			}else{
				break;
			}
			tempMatch = Pattern.compile("(\\W)").matcher(tokenString);
		}
		if(tempPatternBuilder.length()!=0){
			tokenString = tempPatternBuilder.append(tokenString).toString().trim();
		}
		return(tokenString);
	}
	
	/**
	 * Create Entity based NE dictionary
	 * @param docId 
	 * @param currentResource
	 * @param processEntities
	 * @return 
	 */
	private LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>> generateCorpusEntity(
			String docId, ArrayList<String> currentResource, 
			LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>> processEntities) {
		
		Long entityId = Long.parseLong(docId);
		//System.out.println("\n\t id>>"+entityId);
		String entityName = currentResource.get(5);
		entityName = patternBuilder(entityName);
		Integer entitySize = entityName.length();
		TreeMap<Integer, ArrayList<String>> decoyHash = new TreeMap<>();
		ArrayList<String> decoyList = new ArrayList<>();
		if(processEntities.containsKey(entityId)){
			decoyHash = processEntities.get(entityId);
			if(decoyHash.containsKey(entitySize)){
				decoyList = decoyHash.get(entitySize);
			}
		}
		if(!decoyList.contains(entityName)){
			decoyList.add(entityName);
			decoyHash.put(entitySize, decoyList);
			processEntities.put(entityId, decoyHash);
		}
		return(processEntities);
	}
	
	/**
	 * 
	 * @param docText
	 * @param docPos
	 * @param docId
	 */
	private void assembleAbstract(String docText, int docPos, String docId) {
		
		Long docNo = Long.parseLong(docId);
		String abstractText = docText;
		TreeMap<Integer, ArrayList<String>> decoyHash = new TreeMap<>();
		ArrayList<String> decoyList = new ArrayList<>();
		if(processAbstractCollection.containsKey(docNo)){
			decoyHash = processAbstractCollection.get(docNo);
			if(decoyHash.containsKey(docPos)){
				decoyList = decoyHash.get(docPos);
			}
		}
		if(!decoyList.contains(abstractText)){
			decoyList.add(abstractText);
			decoyHash.put(docPos, decoyList);
			processAbstractCollection.put(docNo, decoyHash);
		}
	}
	
	/**
	 * 
	 * @param entitiesCorpus
	 * @param currRelationId 
	 * @param string 
	 * @param textContent
	 * @param textContent2
	 */
	private void findRelation(ArrayList<ArrayList<String>> entitiesCorpus, String docId, 
			int currRelationId, String chemicalIdentifier, String geneIdentifier, 
			String relationType) {
		
		/**
		System.out.println("\n\t>>"+docId+"\t>>"+currRelationId);
		System.out.println("\n\t>>"+entitiesCorpus);
		System.out.println("\n\t chemicalIdentifier>>"+chemicalIdentifier+"\t geneIdentifier>>"+geneIdentifier);**/
		Long docNo = Long.parseLong(docId);
		String tempName;
		int relCount=0, typeFlag = 0,identifierFlag = 0;
		for(int currEntIndex=0;currEntIndex<entitiesCorpus.size();currEntIndex++){
			ArrayList<String> currentEntity = entitiesCorpus.get(currEntIndex);
			if(!currentEntity.isEmpty()){
				typeFlag = 0;identifierFlag = 0;
				if(chemicalIdentifier.equalsIgnoreCase(currentEntity.get(1)) 
							|| geneIdentifier.equalsIgnoreCase(currentEntity.get(1))){
					typeFlag = 1;
					if(currentEntity.get(2).matches("CHEMICAL")){
						identifierFlag = 1;
					}else if(currentEntity.get(2).matches("GENE\\-\\w{1}")){
						identifierFlag = 2;
					}
				}
				if((typeFlag == 1) && (identifierFlag == 1)){
					//System.out.println("\n\t"+typeFlag+"\t"+identifierFlag+"\t>>"+currentEntity);
					tempName = "CHEMICAL#".concat(relationType+"#").concat(patternBuilder(currentEntity.get(5)));
					//System.out.println("\n\t in chemical>>"+tempName);
					TreeMap<Integer, ArrayList<String>> decoyHash = new TreeMap<>();
					ArrayList<String> decoyList = new ArrayList<>(); 
					if(processRelationCollection.containsKey(docNo)){
						decoyHash = processRelationCollection.get(docNo);
						if(decoyHash.containsKey(currRelationId)){
							decoyList = decoyHash.get(currRelationId);
						}
					}
					if(!decoyList.contains(tempName)){
						decoyList.add(tempName);
						decoyHash.put(currRelationId,decoyList);
						//System.out.println("\n\t>."+decoyHash);
						processRelationCollection.put(docNo, decoyHash);
					}
					relCount++;
				}else if((typeFlag == 1) && (identifierFlag == 2)){
					//System.out.println("\n\t"+typeFlag+"\t"+identifierFlag+"\t>>"+currentEntity);
					tempName = "GENEPRO#".concat(relationType+"#").concat(patternBuilder(currentEntity.get(5)));
					//System.out.println("\n\t in gene>>"+tempName);
					TreeMap<Integer, ArrayList<String>> decoyHash = new TreeMap<>();
					ArrayList<String> decoyList = new ArrayList<>(); 
					if(processRelationCollection.containsKey(docNo)){
						decoyHash = processRelationCollection.get(docNo);
						if(decoyHash.containsKey(currRelationId)){
							decoyList = decoyHash.get(currRelationId);
						}
					}
					if(!decoyList.contains(tempName)){
						decoyList.add(tempName);
						decoyHash.put(currRelationId,decoyList);
						processRelationCollection.put(docNo, decoyHash);
					}
					relCount++;
				}
			}// end if-else
			if(relCount == 2){
				//System.out.println("breakpoint:"+relCount);
				break;
			}
		}// end for
	}
	
	/**
	 * 
	 * @param corpusList
	 * @return
	 */
	private ArrayList<ArrayList<String>> isolateCurrentIdCorpus(ArrayList<String> corpusList) {
		
		ArrayList<ArrayList<String>> returnSelectedCorpus = new ArrayList<>();
		ArrayList<String> currentCorpusResource;
		for(String currentEntity : corpusList){
			currentCorpusResource = new ArrayList<>(Arrays.asList(currentEntity.split("\t")));
			if(currentCorpusResource.get(0).equals(currentDocId)){
				returnSelectedCorpus.add(currentCorpusResource);
			}
		}
		return(returnSelectedCorpus);
	}
	
	/**
	 * Put the current thread to sleep 
	 */
	private void haltThreadProcess() {
		try {
			Thread.sleep(3);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Create NE dictionary from the corpus data
	 */
	@Override
	public Hashtable<String, LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>>> call() throws Exception {
		
		Hashtable<String, LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>>> resultSet = 
				new Hashtable<>();
		if(currentDocId != null){
			/**
			 * Extract entities for specific id from corpus
			 */
			ArrayList<ArrayList<String>> selectedCorpusResource = 
					isolateCurrentIdCorpus(corpusMap.get("Entities"));
			if(!selectedCorpusResource.isEmpty()){
				for(ArrayList<String> currentResource : selectedCorpusResource){
					if(currentResource.get(2).matches("CHEMICAL")){
						//System.out.println("\n Chemical"+"\tprocessEntities>>"+processChemicalEntities);
						processChemicalEntities = generateCorpusEntity(currentDocId, 
								currentResource, processChemicalEntities);
					}else if(currentResource.get(2).matches("GENE\\-\\w{1}")){
						//System.out.println("\n Gene"+"\tprocessEntities>>"+processGeneEntities);
						processGeneEntities = generateCorpusEntity(currentDocId, 
								currentResource, processGeneEntities);
					}
				}
			}
			resultSet.put("Chemical",processChemicalEntities);
			resultSet.put("Gene",processGeneEntities);
			
			ArrayList<ArrayList<String>> entitiesCorpus = selectedCorpusResource; 
			selectedCorpusResource = 
					isolateCurrentIdCorpus(corpusMap.get("Relation"));
			if(!selectedCorpusResource.isEmpty()){
				for(int subRelIndex=0;subRelIndex<selectedCorpusResource.size();subRelIndex++){
					ArrayList<String> currentResource = selectedCorpusResource.get(subRelIndex);
					findRelation(entitiesCorpus, currentDocId, subRelIndex,
							currentResource.get(4).split("\\:")[1],
							currentResource.get(5).split("\\:")[1], 
							currentResource.get(1).split("\\:")[1]);
				}
			}
			resultSet.put("Relation", processRelationCollection);
					
			selectedCorpusResource = 
					isolateCurrentIdCorpus(corpusMap.get("Abstract"));
			if (!selectedCorpusResource.isEmpty()) {
				for(ArrayList<String> currentResource : selectedCorpusResource){
					assembleAbstract(currentResource.get(1), 0, currentDocId);
					assembleAbstract(currentResource.get(2), 1, currentDocId);
				}
			}
			resultSet.put("Abstract",processAbstractCollection);
			haltThreadProcess();
			//System.out.println(Thread.currentThread().getName()+":- End - \t"+currentNodeElement.getElementsByTagName("id").item(0).getTextContent());
		}
		return resultSet;
	}
}
