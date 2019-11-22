/**
 * 
 */
package com.prj.bundle.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author neha
 *
 */
public class AI_PatternModel {

	/**
	 * @param string 
	 * @return 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * 
	 */
	protected NodeList loadXMLFile(String filePath) throws ParserConfigurationException, SAXException, IOException {
		/**
		 * Check if the input file is in json or BioC format
		 */
		//String path = DataStreaming.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		//System.out.println("\n"+path+"\n");
		/**
		 *
		BufferedReader buffReader = null;
		System.out.println("\n\t Enter the name of the file\n");
		String resourcePath = System.getProperty("user.dir");
		buffReader = new BufferedReader(new InputStreamReader(System.in));
		resourcePath = resourcePath.concat("/src/com/prj/pattern/resource/");
		String inputFileName = resourcePath.concat(buffReader.readLine());
		**/
		File fileInStream = new File(filePath);
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document xmlFileDoc = docBuilder.parse(fileInStream);
		xmlFileDoc.getDocumentElement().normalize();
		return(xmlFileDoc.getElementsByTagName("document"));
	}
	
	/**
	 * 
	 * @param currentNode
	 * @return 
	 */
	private static LinkedHashMap<String, ArrayList<String>> extractSentence(Node currentNode) {
		
		LinkedHashMap<String, ArrayList<String>> textMap = new LinkedHashMap<>();
		if(currentNode.getNodeType() == Node.ELEMENT_NODE){
			Element currentNodeElement = (Element) currentNode;
			//System.out.println(Thread.currentThread().getName()+":- Start - \t"+currentNodeElement.getElementsByTagName("id").item(0).getTextContent());
			/**
			 * Take into account annotation tags for entity name retrievals 
			 */
			String docId = currentNodeElement.getAttribute("docId");
			//System.out.println("\n\t::"+docId);
			NodeList subNodeList = currentNodeElement.getElementsByTagName("abstract");
			ArrayList<String> sentenceHolder = new ArrayList<>();
			for(int subNodeNm=0;subNodeNm<subNodeList.getLength();subNodeNm++){
				Node subNode = subNodeList.item(subNodeNm);
				if(subNode.getNodeType() == Node.ELEMENT_NODE){
					Element subElement = (Element) subNode;
					String tempAbstract = subElement.getTextContent();
					// club all the sentences from the document together
					sentenceHolder.add(tempAbstract);
				}
			}
			textMap.put(docId,sentenceHolder);
		}
		return(textMap);
	}
	
	/**
	 * @param args
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws ParserConfigurationException 
	 */
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		
		AI_PatternModel variantModel = new AI_PatternModel();
		BoundaryReturn boundarReturnInstance = new BoundaryReturn();
		int index = 0;
		try {
			LearningFeatureExtractor learningFeatureInstance = new LearningFeatureExtractor();
			AI_ExecutionModule executionInstance = new AI_ExecutionModule();
			SentientBoundary boundaryIdentifier = new SentientBoundary();
			long beginSysTime = System.currentTimeMillis();
			Properties systemProperties = new Properties();
            InputStream propertyStream  = new FileInputStream("config.properties");
            systemProperties.load(propertyStream);
            NodeList xmlPOSNodeTree = variantModel.loadXMLFile(systemProperties.getProperty("processedPOSTaggedXmlFile"));
            NodeList xmlOrgNodeTree = variantModel.loadXMLFile(systemProperties.getProperty("processedOriginalXMLFile"));
			System.out.println("START*************************");
			System.out.println("\t total instances>"+xmlPOSNodeTree.getLength());
			Integer threadPoolSize;
			if(xmlPOSNodeTree.getLength() > 1){
				threadPoolSize = (xmlPOSNodeTree.getLength()/2);
			}else{
				threadPoolSize = 1;
			}
			System.out.println("\n"+threadPoolSize);
			ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(threadPoolSize);
			while(index < xmlPOSNodeTree.getLength()){
				Node currentPOSNode = xmlPOSNodeTree.item(index);
				Node currentOrgNode = xmlOrgNodeTree.item(index);
				LinkedHashMap<String, ArrayList<String>> posSentenceCluster = extractSentence(currentPOSNode);
				LinkedHashMap<String, ArrayList<String>> orgSentenceCluster = extractSentence(currentOrgNode);
				//System.out.println("\n\t main ::"+posSentenceCluster+"\n\t>>"+orgSentenceCluster);
				//System.out.println("\n\t main ::"+posSentenceCluster.keySet());
				System.out.println("\t index>"+index);
				SentientBoundary workerThread = new SentientBoundary(boundarReturnInstance, 
						posSentenceCluster, orgSentenceCluster);
				Future<BoundaryReturn> taskCollector =
						threadPoolExecutor.submit(workerThread);
				try {
					//boundarReturnInstance = boundaryIdentifier.
						//	sentenceBoundaryIdentification(posSentenceCluster,orgSentenceCluster);
					boundarReturnInstance = taskCollector.get();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					System.err.println("Exception generated >"+e.getLocalizedMessage());
					//index++;
					//continue;
				}finally {
					index++;
				}
				//index++;
			}
			
			Set<String> documentCount = new HashSet<>();
			NavigableMap<Integer, HashMap<String, ArrayList<String>>> descendingPOSTagMap = 
					boundarReturnInstance.posTaggedSentenceBundle.descendingMap();
			Iterator<Map.Entry<Integer, HashMap<String, ArrayList<String>>>> tier1Itr = 
					descendingPOSTagMap.entrySet().iterator();
			int posInd=0,negInd=0;
			FileWriter fileWS = new FileWriter(systemProperties.getProperty("trainingRelationSentence"),false);
			BufferedWriter buffWS = new BufferedWriter(fileWS);
			while(tier1Itr.hasNext()){
				Map.Entry<Integer, HashMap<String, ArrayList<String>>> tier1MapValue = tier1Itr.next();
				Iterator<Map.Entry<String, ArrayList<String>>> tier2Itr = 
						tier1MapValue.getValue().entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<String, ArrayList<String>> tier2MapValue = tier2Itr.next();
					for(String currentSent : tier2MapValue.getValue()){
						buffWS.write(String.valueOf(tier1MapValue.getKey()).concat("\t:"));
						buffWS.write(String.valueOf(tier2MapValue.getKey()).concat("\t:"));
						buffWS.write(currentSent);
						buffWS.newLine();
						String key = tier2MapValue.getKey().replaceAll("(\\@(\\d)+)+R(\\d)+", "");
						documentCount.add(key);
						if(tier1MapValue.getKey() > 0){
							posInd++;
						}else{
							negInd++;
						}
					}
				}
			}
			System.out.println("\t positive instances>"+posInd+"\t negative instances>"+negInd);
			System.out.println("\t total instances>"+documentCount.size());
			buffWS.flush();
			
			NavigableMap<Integer, HashMap<String, ArrayList<String>>> descendingOrgMap = 
					boundarReturnInstance.orgSentenceBundle.descendingMap(); 
			tier1Itr = descendingOrgMap.entrySet().iterator();
			fileWS = new FileWriter(systemProperties.getProperty("trainingOriginalSentence"),false);
			buffWS = new BufferedWriter(fileWS);
			while(tier1Itr.hasNext()){
				Map.Entry<Integer, HashMap<String, ArrayList<String>>> tier1MapValue = tier1Itr.next();
				Iterator<Map.Entry<String, ArrayList<String>>> tier2Itr = 
						tier1MapValue.getValue().entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<String, ArrayList<String>> tier2MapValue = tier2Itr.next();
					for(String currentSent : tier2MapValue.getValue()){
						buffWS.write(String.valueOf(tier1MapValue.getKey()).concat("\t:"));
						buffWS.write(String.valueOf(tier2MapValue.getKey()).concat("\t:"));
						buffWS.write(currentSent);
						buffWS.newLine();
						String key = tier2MapValue.getKey().replaceAll("(\\@(\\d)+)+R(\\d)+", "");
						documentCount.add(key);
					}
				}
			}
			buffWS.flush();
			
			fileWS = new FileWriter(systemProperties.getProperty("trainingRelationKeywords"),false);
			buffWS = new BufferedWriter(fileWS);
			Iterator<String> tier3Itr = boundarReturnInstance.mainRelationVerbList.keySet().iterator();
			while(tier3Itr.hasNext()){
				String currStr = tier3Itr.next();
				buffWS.write(currStr);
				Iterator<String> secItr = boundarReturnInstance.mainRelationVerbList.get(currStr).iterator();
				buffWS.write("\t:");
				while(secItr.hasNext()){
					buffWS.write(secItr.next());
					buffWS.write(", ");
				}
				buffWS.newLine();
			}
			buffWS.flush();
			buffWS.close();
			
			/**
			index=0;
			DensityFeatureMap primaryFeatureinstance = new DensityFeatureMap();
			ContextFrameFeatureMap secondaryFeatureInstance = new ContextFrameFeatureMap();
			for(Integer instanceType : boundarReturnInstance.posTaggedSentenceBundle.descendingKeySet()){
				HashMap<String, ArrayList<String>> decoyHash = boundarReturnInstance.
						posTaggedSentenceBundle.get(instanceType);
				Iterator<Map.Entry<String, ArrayList<String>>> hashTier1Itr = decoyHash.entrySet().iterator();
				while(hashTier1Itr.hasNext()){
					Map.Entry<String, ArrayList<String>> hashTier1MapValue = hashTier1Itr.next();
					for(String currentSentence : hashTier1MapValue.getValue()){
						if(instanceType > 0){
							//extracting document frequencies per sentence with a relation based id and index
							primaryFeatureinstance = learningFeatureInstance.
									densityFeatureExtractor(hashTier1MapValue.getKey(), currentSentence);
						}
						
						//extracting context pattern per sentence with a given document id and index
						secondaryFeatureInstance = learningFeatureInstance.
								learnContextFrameComposition(instanceType, hashTier1MapValue.getKey(),currentSentence);
					}
				}
			}
			
			//INTEGRITY CHECK
			/**
			System.out.println("\n\t***********************");
			System.out.println("\tchemicalTermFrequency:"+primaryFeatureinstance.getChemicalTermFrequency().size());
			System.out.println("\tdiseaseTermFrequency:"+primaryFeatureinstance.getGeneTermFrequency().size());
			System.out.println("\trelationTermFrequency:"+primaryFeatureinstance.getRelationTermFrequency().size());
			System.out.println("\tchemicalDiseaseAssociation:"+primaryFeatureinstance.getVerbChemicalGeneAssociation().size());
			System.out.println("\tchemicalVerbAssociation:"+primaryFeatureinstance.getVerbChemicalAssociation().size());
			System.out.println("\tverbDiseaseAssociation:"+primaryFeatureinstance.getVerbGeneAssociation().size());
			System.out.println("\n\tgetContextFrameMap:"+secondaryFeatureInstance.getContextFrameMap().get(5).size());
			System.out.println("\n\tgetChemicalFrameMap:"+secondaryFeatureInstance.getChemicalFrameMap().get(5).get(3).size());
			System.out.println("\n\tgetDiseaseFrameMap:"+secondaryFeatureInstance.getGeneFrameMap().get(5).get(3).size());
			System.out.println("\n\tgetRelationFrameMap:"+secondaryFeatureInstance.getRelationFrameMap().get(5).size());
			**/
			/**
			executionInstance.scoreFeatures(primaryFeatureinstance, secondaryFeatureInstance, 
					boundarReturnInstance.posTaggedSentenceBundle);**/
			System.out.println("\n Total Execution Time:-"+(System.currentTimeMillis()-beginSysTime)/1000);	
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
