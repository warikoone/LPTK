package com.prj.bundle.svmLearning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.prj.bundle.model.LearningFeatureExtractor;
import com.prj.bundle.preprocessing.CorpusDictionary;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SRL_ID;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;

/**
 * @author neha
 *
 */
public class SVM_ParseTreeFeature_FeatureVectorSplitInstance extends TreeTraversalAttributes implements 
Callable<HashMap<String,HashMap<Double, ArrayList<Integer>>>>{
	
	private LexicalizedParser stanfordLexParser;
	private HashMap<String, HashMap<String, HashMap<String, ArrayList<Integer>>>> aiContextPatterns;
	private ArrayList<String> originalTokens;
	private ArrayList<String> posTaggedTokens;
	private HashMap<Integer, HashMap<String,HashMap<String,ArrayList<String>>>> sptkPatterns;
	private HashMap<String,Integer> sptkFeatures;
	private Properties systemProperties;
	/**
	 * Constructors 
	 * @throws IOException 
	 */
	public SVM_ParseTreeFeature_FeatureVectorSplitInstance() throws IOException {
		this.systemProperties = new Properties();
        InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
		this.stanfordLexParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		this.sptkPatterns = new LinkedHashMap<>();
		this.sptkFeatures = new LinkedHashMap<>();
	}
	
	public SVM_ParseTreeFeature_FeatureVectorSplitInstance(ArrayList<String> orgSentenceSplit, ArrayList<String> posTaggedSentenceSplit,
			HashMap<String, HashMap<String, HashMap<String, ArrayList<Integer>>>> aiContextPatterns) {
		
		this.originalTokens = orgSentenceSplit;
		this.posTaggedTokens = posTaggedSentenceSplit;
		this.aiContextPatterns = aiContextPatterns;
	}
	
	private HashMap<Integer, Integer> populateIntegerHashWithInteger(HashMap<Integer, Integer> decoyHash, 
			Integer key, Integer value) {
		
		if(!decoyHash.isEmpty()){
			if(decoyHash.containsKey(key)){
				value = value + decoyHash.get(key);
			}
		}
		decoyHash.put(key, value);
		return(decoyHash);
	}
	
	private Tree treeTraversalAndPrunning(Tree parseTree, TreeTraversalAttributes treeInstance) {

		String parseTreeString = parseTree.deepCopy().toString();
		//System.out.println("\t*******"+parseTreeString);
		for(Tree subTree:parseTree.children()){
			//System.out.println("\t leaf check>>"+subTree.isLeaf()+"\t\t****"+subTree);
			//System.out.println("\t>>"+subTree.numChildren()+"\t>>"+treeInstance.indexCount+"\t\t>>"+treeInstance.currentMatchedIndices);
			if(subTree.numChildren() == 0){
				if((treeInstance.currentMatchedIndices.contains(treeInstance.indexCount))
						&&(treeInstance.currentMatchedIndices.size() > 0)){
					for(Integer currentIndex : treeInstance.currentMatchedIndices){
						if(treeInstance.indexCount == currentIndex){
							//move towards the next branch
							treeInstance.currentMatchedIndices.remove(currentIndex);
							treeInstance.indexCount=treeInstance.indexCount+1;
							//treeInstance.setIndexCount(treeInstance.currentMatchedIndices.get(0));
							//System.out.println("\tcurr val>>"+subTree.toString()+"\t"+indexCount+"\t"+currentMatchedIndices);
							//treeTraversalAndPrunning(subTree,treeInstance);
							treeInstance.setPruneFlag(0);
							break;
				    	}
					}
				}else{
		    		treeInstance.indexCount=treeInstance.indexCount+1;
		    		//System.out.println("\n\tOBJECT INDEX OF>>"+parseTree.objectIndexOf(subTree));
		    		//System.out.println("\tcurr val>>"+subTree.toString()+"\t"+indexCount+"\t"+currentMatchedIndices);
		    		//treeTraversalAndPrunning(subTree,treeInstance);
		    		//prune the tree
		    		treeInstance.setPruneFlag(1);
			    }
			}else{
				//System.out.println("leaving here");
				subTree = treeTraversalAndPrunning(subTree,treeInstance);
				if((treeInstance.getPruneFlag() == 1) && (parseTree.numChildren()>1)){
					int removalIndex = parseTree.objectIndexOf(subTree);
					//System.out.println("\n\tOBJECT INDEX OF>>"+removalIndex);
					parseTree.removeChild(removalIndex);
					//System.out.println("\n\tUPDATED PARSE TREE>>"+parseTree);
					treeInstance.setPruneFlag(0);
				}
			}
		}
		return(parseTree);
	}
	
	private String generateFeatureString(HashMap<String, ArrayList<String>> featureHash) {

		StringBuilder partialKernelTreeString = new StringBuilder();
		StringBuilder kernelTreeFeatureString = new StringBuilder();
		String finalFeatureString = "";
		int counter = 0;
		for(String currentPattern : featureHash.keySet()){
			// tree type
			partialKernelTreeString.append("|BT| ".concat(featureHash.get(currentPattern).get(1)).concat(" "));
			// feature vector type
			if(counter == 0){
				kernelTreeFeatureString.append("|ET| ".concat(featureHash.get(currentPattern).get(2)).concat(" "));
			}else{
				kernelTreeFeatureString.append("|BV| ".concat(featureHash.get(currentPattern).get(2)).concat(" "));
			}
			// instance type
			finalFeatureString = featureHash.get(currentPattern).get(0).concat(" ");
			counter++;
		}
		kernelTreeFeatureString.append("|EV| ");
		finalFeatureString = finalFeatureString.
				concat(partialKernelTreeString.toString()).
				concat(kernelTreeFeatureString.toString());
		//System.out.println("\n"+finalFeatureString);
		return(finalFeatureString);
	}
	
	private String recursiveTerminalSymbolCheck(String sentence, int sentenceSize) {
		
		// iteratively remove non period operators from the rare of the sentence
		//System.out.println("\n\t>>"+String.valueOf(sentence.charAt(sentenceSize))+"\tindex>>"+sentenceSize);
		if(String.valueOf(sentence.charAt(sentenceSize)).matches("\\w")){
			return(sentence);
		}else{
			//System.out.println("\n\t>>"+sentence.substring(0,sentenceSize));
			if(sentenceSize != 0){
				sentence = recursiveTerminalSymbolCheck(sentence.substring(0,sentenceSize), sentenceSize-1);
			}else{
				return null;
			}
		}
		//System.out.println("\n\t>>"+sentence);
		return sentence;
	}
	
	private boolean checkBracketConsistency(String parseTreeString) {
		
		boolean retVal = false;
		Matcher bracketMatch = Pattern.compile("\\(").matcher(parseTreeString.trim());
		int startBracketMatch=0,endBracketMatch=0;
		while(bracketMatch.find()){
			startBracketMatch++;
		}
		bracketMatch = Pattern.compile("\\)").matcher(parseTreeString.trim());
		while(bracketMatch.find()){
			endBracketMatch++;
		}
		if(startBracketMatch != endBracketMatch){
			retVal = true;
		}
		return(retVal);
	}
	
	private boolean checkRedundantSingleToken(String featureString) {
		
		ArrayList<String> redundantTerm = new ArrayList<>(Arrays.asList(featureString
				.replaceAll("\\(|\\)","").split("\\s+")));
		//System.out.println("\n\t single token array"+redundantTerm);
		if(redundantTerm.size() == 1){
			if((redundantTerm.get(0).matches(".*"))){
				featureString = featureString.replaceFirst("\\(".concat(redundantTerm.get(0)), "").trim();
				featureString = featureString.substring(0, featureString.length()-1);
			}
		}else{
			if((redundantTerm.get(0).equalsIgnoreCase("S"))
					|| (redundantTerm.get(0).equalsIgnoreCase("SBAR"))
					|| (redundantTerm.get(0).equalsIgnoreCase("FRAG"))
					|| (redundantTerm.get(0).equalsIgnoreCase("X"))
					//|| (redundantTerm.get(0).matches(redundantTerm.get(1)))
					){
				featureString = featureString.replaceFirst("\\(".concat(redundantTerm.get(0)), "").trim();
				featureString = featureString.substring(0, featureString.length()-1);
			}
		}
		
		//System.out.println("updated token>>"+featureString);
		if(featureString.equals("")){
			return true;
		}
		return false;
	}

	private String removeRedundancyTerm(String collapsedFeature) {

		ArrayList<String> redundantTerm = new ArrayList<>(Arrays.asList(collapsedFeature
				.replaceAll("\\(|\\)","").split("\\s+")));
		//System.out.println("\n\t redundant terms"+redundantTerm);
		if(redundantTerm.size() > 2){
			//System.out.println("\n\t 1."+redundantTerm.get(0)+"\t 2."+redundantTerm.get(1));
			if((redundantTerm.get(0).equalsIgnoreCase("S"))
					|| (redundantTerm.get(0).equalsIgnoreCase("SBAR"))
					|| (redundantTerm.get(0).equalsIgnoreCase("FRAG"))
					|| (redundantTerm.get(0).equalsIgnoreCase("X"))
					|| (redundantTerm.get(0).matches(new CorpusDictionary().patternBuilder(redundantTerm.get(1))))){
				collapsedFeature = collapsedFeature.replaceFirst("\\(".concat(redundantTerm.get(0)), "").trim();
				collapsedFeature = collapsedFeature.substring(0, collapsedFeature.length()-1);
			}
		}
		
		if(checkBracketConsistency(collapsedFeature)){
			System.err.println("\n\t Bracket Inconsistency >>"+collapsedFeature);
		}
		
		return(collapsedFeature.trim());
	}
	
	private ArrayList<LinkedHashMap<String, Integer>> processPrunedTreeForFeatureVector(String featureString, 
			LinkedHashMap<String, Integer> decoyFeatureMap, int terminalFlag) {
		
		ArrayList<LinkedHashMap<String, Integer>> reformedFeatureArray = new ArrayList<>();
		LinkedHashMap<String, Integer> collapsedMap = new LinkedHashMap<>();
		LearningFeatureExtractor featureExtractorInstance = new LearningFeatureExtractor();
		//System.out.println("\n\t featureString>>>"+featureString);
		Matcher featureEntityIdentifer;
		if(terminalFlag == 1){
			featureEntityIdentifer = Pattern.compile("CHEMICALPRI|GENEPROPRI|RELATION")
					.matcher(featureString.trim());
			Set<String> primeEntitySet = new HashSet<>();
			while(featureEntityIdentifer.find()){
				if(featureEntityIdentifer.group(0).matches("CHEMICALPRI|GENEPROPRI|RELATION")){
					primeEntitySet.add(featureEntityIdentifer.group(0));
				}
				//System.out.println("\n\t 1. >>>"+featureEntityIdentifer.group(0));
			}
			decoyFeatureMap.put("Z", primeEntitySet.size());
			collapsedMap.put(featureString.trim(), 1);
			reformedFeatureArray.add(decoyFeatureMap);
			reformedFeatureArray.add(collapsedMap);
			
		}else{
			featureEntityIdentifer = Pattern.compile("CHEMICALPRI|GENEPROPRI|RELATION")
					.matcher(featureString.trim());
			ArrayList<String> decoyFeatureArray = new ArrayList<>(Arrays.asList(featureString.split("(\\s)+")));
			StringBuilder featureBuilder = new StringBuilder();
			if(featureEntityIdentifer.find()){
				for(String splitFeature : decoyFeatureArray){
					if(splitFeature.matches("\\(CHEMICALPRI(\\W)*|\\(GENEPROPRI(\\W)*|\\(RELATION(\\W)*")){
						String retString = recursiveTerminalSymbolCheck(splitFeature,splitFeature.length()-1);
						if(null == retString){
							retString = splitFeature;
						}
						featureBuilder.append(retString.concat(" "));
					}else{
						//splitFeature = splitFeature.replaceAll("\\w+", "");
						featureBuilder.append(splitFeature.concat(" "));
					}
				}
			}else{
				featureBuilder = new StringBuilder(featureString);
			}
			String collapsedFeature = featureBuilder.toString().trim();
			collapsedFeature = removeRedundancyTerm(collapsedFeature);
			/**
			int preCheckLength = 0;
			do {
				preCheckLength = collapsedFeature.length();
				collapsedFeature = removeRedundancyTerm(collapsedFeature);
				System.out.println("\n\t not herer>>>"+collapsedFeature);
				if(collapsedFeature.startsWith("(X")){
					System.exit(0);
				}
			} while(preCheckLength > collapsedFeature.length());**/
			
			//System.out.println("\n\t redRM>>>"+collapsedFeature);
			collapsedMap.put(collapsedFeature, 1);
			decoyFeatureMap = featureExtractorInstance.
					populateHashMapWithIntegers(decoyFeatureMap, collapsedFeature);
			reformedFeatureArray.add(decoyFeatureMap);
			reformedFeatureArray.add(collapsedMap);
		}
		return(reformedFeatureArray);
	}
	
	private ArrayList<String> generatePositiveSPTKFeatures(String prunedTree, 
			Integer instanceType, TreeMap<Integer, String> cprClassType) throws IOException {
		
		//System.out.println("\n\t>>"+prunedTree);
		prunedTree = prunedTree.replaceAll("(\\s)*\\(\\. \\.\\)", "");
		//System.out.println("\n\t updated >>"+prunedTree);
		Matcher bracketMatcher;
		ArrayList<LinkedHashMap<String, Integer>> reformedFeatureArray = new ArrayList<>();
		ArrayList<Integer> bracketStartIndex = new ArrayList<>();
		ArrayList<Integer> bracketEndIndex = new ArrayList<>();
		int prevEndIndex=-1,startIndex=0,endIndex=0,count=0;
		String featureString="",featureTree="";
		HashMap< Integer, Integer> featureIdMap = new LinkedHashMap<>();
		LinkedHashMap<String, Integer> decoyFeatureMap = new LinkedHashMap<>();
		int bracketCount=0;
		bracketMatcher = Pattern.compile("\\(|\\)").matcher(prunedTree);
		while(bracketMatcher.find()){
			bracketCount++;
			if(bracketCount > 2){
				if(bracketMatcher.group(0).matches("\\(")){
					bracketStartIndex.add(bracketMatcher.start());
				}else if(bracketMatcher.group(0).matches("\\)")){
					bracketEndIndex.add(bracketMatcher.end()-1);
				}
				//System.out.println("\n\tStart index>>"+bracketStartIndex+"\n\tEnd index>>"+bracketEndIndex+"\t>>"+prevEndIndex);
				// for reading a phrase structure
				if((bracketEndIndex.size() == 1) && (!bracketStartIndex.isEmpty())){
					int currentStartIndex = bracketStartIndex.size()-1;
					/**
					System.out.println("\n\t updated>>"+prunedTree);
					System.out.println("\n startIndex>>"+ prunedTree.charAt(bracketStartIndex.get(currentStartIndex)+1)+
							"\n\tend index>>"+prunedTree.charAt(bracketEndIndex.get(0)));**/

					//reset values
					if(-1 != prevEndIndex){
						int previousRelativePos = 
								(prevEndIndex-bracketStartIndex.get(currentStartIndex));
						int currentRelativePos = 
								(bracketEndIndex.get(0)-bracketStartIndex.get(currentStartIndex));
						//System.out.println("\n\tcurrent est:::"+(currentRelativePos*previousRelativePos));
						if((currentRelativePos*previousRelativePos) > 0){
							// add as a feature
							featureString = prunedTree.
									substring(bracketStartIndex.get(currentStartIndex),
											(bracketEndIndex.get(0)+1)).trim();
							//System.out.println("\n feature b4>>"+ featureString);
							reformedFeatureArray = processPrunedTreeForFeatureVector(
									featureString, decoyFeatureMap, 0);
							if(!reformedFeatureArray.isEmpty()){
								decoyFeatureMap = reformedFeatureArray.get(0);
								String swapFeature = reformedFeatureArray.get(1).keySet().iterator().next();
								//System.out.println("\n\t curr>>"+featureString+"\n\t lastFeat>>"+swapFeature);
								if((!featureString.contentEquals(swapFeature))){
									String redundantNode = new CorpusDictionary().
											patternBuilder(featureString);
									prunedTree = prunedTree.replaceFirst(redundantNode, 
											Matcher.quoteReplacement(swapFeature));
									if(checkBracketConsistency(prunedTree)){
										System.err.println("\n\t Bracket Inconsistency >>"+prunedTree);
									}
									bracketCount=0;
									bracketStartIndex = new ArrayList<>();
									bracketEndIndex = new ArrayList<>();
									prevEndIndex=-1;
									reformedFeatureArray= new ArrayList<>();
									decoyFeatureMap = new LinkedHashMap<>();
									//bracketMatcher.reset();
									bracketMatcher = Pattern.compile("\\(|\\)").matcher(prunedTree);
									count++;
									/**
									if(count == 5){
										System.out.println("\n\t state>>"+prunedTree);
										System.exit(0);
									}else{
										System.out.println("\n\t restart>>"+prunedTree+"\n\t>>"+count);
									}**/
									continue;
								}else{
									//System.out.println("\n\t no change >>"+swapFeature);
								}
							}
						}else{
							// read as individual token
							featureString = prunedTree.
									substring(bracketStartIndex.get(currentStartIndex),
											(bracketEndIndex.get(0)+1)).trim();
							//System.out.println("single token>>"+featureString);
							if(checkRedundantSingleToken(featureString)){
								String redundantNode = new CorpusDictionary().
										patternBuilder(featureString);
								//System.out.println("prior redundancy>>"+prunedTree);
								prunedTree = prunedTree.replaceFirst(redundantNode.concat("\\s*"), "");
								//System.out.println("redundancy identified>>"+prunedTree);
								if(checkBracketConsistency(prunedTree)){
									System.err.println("\n\t Bracket Inconsistency >>"+prunedTree);
								}
								bracketCount=0;
								bracketStartIndex = new ArrayList<>();
								bracketEndIndex = new ArrayList<>();
								prevEndIndex=-1;
								reformedFeatureArray= new ArrayList<>();
								decoyFeatureMap = new LinkedHashMap<>();
								//bracketMatcher.reset();
								bracketMatcher = Pattern.compile("\\(|\\)").matcher(prunedTree);
								count++;
								/**
								if(count == 5){
									System.out.println("\n\t state>>"+prunedTree);
									System.exit(0);
								}else{
									System.out.println("\n\t restart>>"+prunedTree+"\n\t>>"+count);
								}**/
								continue;
							}
						}
					}else{
						// check for redundancies in individual token
						featureString = prunedTree.
								substring(bracketStartIndex.get(currentStartIndex),
										(bracketEndIndex.get(0)+1)).trim();
						//System.out.println("single token>>"+featureString);
						if(checkRedundantSingleToken(featureString)){
							String redundantNode = new CorpusDictionary().
									patternBuilder(featureString);
							//System.out.println("prior redundancy>>"+prunedTree);
							prunedTree = prunedTree.replaceFirst(redundantNode.concat("\\s*"), "");
							//System.out.println("redundancy identified>>"+prunedTree);
							if(checkBracketConsistency(prunedTree)){
								System.err.println("\n\t Bracket Inconsistency >>"+prunedTree);
							}
							bracketCount=0;
							bracketStartIndex = new ArrayList<>();
							bracketEndIndex = new ArrayList<>();
							prevEndIndex=-1;
							reformedFeatureArray= new ArrayList<>();
							decoyFeatureMap = new LinkedHashMap<>();
							//bracketMatcher.reset();
							bracketMatcher = Pattern.compile("\\(|\\)").matcher(prunedTree);
							count++;
							/**
							if(count == 5){
								System.out.println("\n\t state>>"+prunedTree);
								System.exit(0);
							}else{
								System.out.println("\n\t restart>>"+prunedTree+"\n\t>>"+count);
							}**/
							continue;
						}
					}
					/**
					System.out.println("\n\t individual token>>"+prunedTree.
							substring(bracketStartIndex.get(currentStartIndex),
									(bracketEndIndex.get(0)+1)).trim());**/
					prevEndIndex = bracketEndIndex.get(0);
					bracketEndIndex.remove(0);
					bracketStartIndex.remove(currentStartIndex);
				}
				// to start a new phrase structure
				if(bracketStartIndex.isEmpty()){
					//System.out.println("\n\t new token>>"+featureString);
					prevEndIndex = -1;
					if(!bracketEndIndex.isEmpty()){
						// for terminal bracket case
						endIndex = bracketEndIndex.get(0);
					}
					if(checkBracketConsistency(prunedTree)){
						System.err.println("\n\t Bracket Inconsistency >>"+prunedTree);
					}
				}
			}else if(bracketCount == 2){
				if(bracketMatcher.group(0).matches("\\(")){
					// for start bracket case
					startIndex = bracketMatcher.start();
					//System.out.println("\n>>>>>>>here"+bracketMatcher.group(0)+"\t::::"+startIndex);
				}
			}
		}
		// addition of the feature tree, prior to forming compound tree
		featureTree = prunedTree.substring(startIndex,endIndex+1).trim();
		//System.out.println("\n\t prior>>"+featureTree);
		featureTree = featureTree.replaceFirst("[\\(\\w&&[^\\s]]+", "");
		featureTree = featureTree.substring(0, featureTree.length()-1).trim();
		if(checkBracketConsistency(prunedTree)){
			System.err.println("\n\t Bracket Inconsistency >>"+prunedTree);
		}
		reformedFeatureArray = processPrunedTreeForFeatureVector(featureTree, decoyFeatureMap, 0);
		decoyFeatureMap = reformedFeatureArray.get(0);
		featureTree = reformedFeatureArray.get(1).keySet().iterator().next();
		
		/**
		if(!decoyTreeMap.isEmpty()){
			for(Integer keyVal : decoyTreeMap.descendingKeySet()){
				Iterator<Map.Entry<String, String>> tier3Itr = 
						decoyTreeMap.get(keyVal).entrySet().iterator();
				while(tier3Itr.hasNext()){
					Map.Entry<String, String> tier3MapValue = tier3Itr.next();
					String redundantNode = new CorpusDictionary().patternBuilder(tier3MapValue.getKey());
					featureTree = featureTree.replaceAll(redundantNode, 
							Matcher.quoteReplacement(tier3MapValue.getValue()));
					//System.out.println("\n\t update>>"+featureTree);
				}
			}
		}**/
		//System.out.println("\n\t later>>"+featureTree);
		/**
		StringBuilder entityFeature = new StringBuilder();
		entityFeature.append("(ENTITY");
		Matcher featureGenerator = Pattern.compile("CHEMICALPRI|DISEASEPRI|RELATION")
				.matcher(featureTree);
		while(featureGenerator.find()){
			String entityType = featureGenerator.group(0).trim();
			switch (entityType) {
			case "CHEMICALPRI":
				if(!entityFeature.toString().trim().contains("ChemEntity")){
					entityFeature.append(" (ChemEntity ".concat(entityType).concat(")"));
				}
				continue;
			case "DISEASEPRI":
				if(!entityFeature.toString().trim().contains("DiseaseEntity")){
					entityFeature.append(" (DiseaseEntity ".concat(entityType).concat(")"));
				}
				continue;
			case "RELATION":
				if(!entityFeature.toString().trim().contains("Induced")){
					entityFeature.append(" (Induced ".concat(entityType).concat(")"));
				}
				continue;
			default:
				break;
			}
		}
		entityFeature.append(")");
		String entityFeatureTree = entityFeature.toString().trim();
		if(checkBracketConsistency(entityFeatureTree)){
			System.err.println("\n\t Bracket Inconsistency >>"+entityFeatureTree);
		}
		//System.out.println("\n\tShow me>>"+entityFeatureTree);
		reformedFeatureArray = processPrunedTreeForFeatureVector(
				entityFeatureTree, decoyFeatureMap, 0);
		decoyFeatureMap = reformedFeatureArray.get(0);
		entityFeatureTree = reformedFeatureArray.get(1).keySet().iterator().next();**/
		
		String decorateTree ="";
		if(instanceType > 0){
			/**
			ArrayList<String> classDecoration = new ArrayList<>();
			if(cprClassType.containsKey(instanceType)){
				classDecoration = new ArrayList<>(
						Arrays.asList(cprClassType.get(instanceType).split("\\|")));
			}
			if((instanceType == 3)||(instanceType == 4)||instanceType == 5){
				classDecoration.remove(0);
			}
			for(String classType : classDecoration){
				String subdecorateTree = "(".concat(classType+" ").concat("cpr)");
				reformedFeatureArray = processPrunedTreeForFeatureVector(subdecorateTree, decoyFeatureMap, 0);
				decoyFeatureMap = reformedFeatureArray.get(0);
				decorateTree = decorateTree.concat(
						reformedFeatureArray.get(1).keySet().iterator().next()+" ");
			}**/			
			decorateTree = "(CLASS CPR".concat(String.valueOf(instanceType)+")");
			reformedFeatureArray = processPrunedTreeForFeatureVector(decorateTree, decoyFeatureMap, 0);
			decoyFeatureMap = reformedFeatureArray.get(0);
			decorateTree = reformedFeatureArray.get(1).keySet().iterator().next();
			featureTree = "(ROOT ".concat(decorateTree).concat(" ")
					.concat(featureTree).concat(")");	
		}else{
			featureTree = "(ROOT ".concat(featureTree).concat(")");
		}
		
		//System.out.println("\n\t b4>>"+featureTree);
		reformedFeatureArray = processPrunedTreeForFeatureVector(featureTree, decoyFeatureMap, 0);
		decoyFeatureMap = reformedFeatureArray.get(0);
		featureTree = reformedFeatureArray.get(1).keySet().iterator().next();
		ArrayList<String> featureSet = new ArrayList<>(decoyFeatureMap.keySet());
		//System.out.println("\n\t>>"+featureSet);
		int index=0;
		while(index < featureSet.size()){
			if(featureSet.contains(featureTree)){
				break;
			}else{
				System.err.println("\n\t Feature Reduction - Error");
				featureTree = featureTree.replaceFirst("[\\(\\w&&[^\\s]]+", "");
				featureTree = featureTree.substring(0, featureTree.length()-1).trim();
				index++;
			}
		}
		
		if(checkBracketConsistency(featureTree)){
			System.err.println("\n\t Bracket Inconsistency >>"+featureTree);
		}
		reformedFeatureArray = processPrunedTreeForFeatureVector(featureTree, decoyFeatureMap, 1);
		decoyFeatureMap = reformedFeatureArray.get(0);
		featureTree = reformedFeatureArray.get(1).keySet().iterator().next();
		//System.out.println("\n\t aftr>>"+featureTree);
		/**
		featureTree = "(G ".concat(featureTree).concat(")");
		decoyFeatureMap = processPrunedTreeForFeatureVector(featureTree, decoyFeatureMap);
		featureTree = "(CS ".concat(featureTree).concat(")");
		decoyFeatureMap = processPrunedTreeForFeatureVector(featureTree, decoyFeatureMap);**/
		//System.out.println("\n\t4>>>"+decoyFeatureMap);
		//arrange in ascending order
		List<Map.Entry<String, Integer>> featureCompareList = new LinkedList<>(decoyFeatureMap.entrySet());
		Collections.sort(featureCompareList, new Comparator<Map.Entry<String, Integer>>() {

			@Override
			public int compare(Map.Entry<String, Integer> currItem, Map.Entry<String, Integer> nextItem) {
				return (Integer.valueOf(currItem.getKey().length()).
						compareTo(Integer.valueOf(nextItem.getKey().length())));
			}
		});
		
		int featureId=1;
		Iterator<Map.Entry<String, Integer>> tier1Itr = featureCompareList.iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<String, Integer> tier1MapValue = tier1Itr.next();
			if(!sptkFeatures.isEmpty()){
				// check for feature pattern or else increment values every subsequent addition
				if(sptkFeatures.containsKey(tier1MapValue.getKey())){
					featureId = sptkFeatures.get(tier1MapValue.getKey());
				}else{
					ArrayList<Integer> decoyValues = 
							new ArrayList<>(sptkFeatures.values());
					featureId = decoyValues.get(decoyValues.size()-1)+1;
				}
			}
			sptkFeatures.put(tier1MapValue.getKey(),featureId);
			featureIdMap.put(featureId, tier1MapValue.getValue());
			//System.out.println("\nkey>>"+sptkFeatures.get(tier1MapValue.getKey()));
		}
		
		//arrange feature vector in ascending order
		List<Map.Entry<Integer, Integer>> featureIdCompareList = new LinkedList<>(featureIdMap.entrySet());
		Collections.sort(featureIdCompareList, new Comparator<Map.Entry<Integer, Integer>>() {

			@Override
			public int compare(Map.Entry<Integer, Integer> currItem, Map.Entry<Integer, Integer> nextItem) {
				return (currItem.getKey().compareTo(nextItem.getKey()));
			}
		});
		Iterator<Map.Entry<Integer, Integer>> tier2Itr = featureIdCompareList.iterator();
		StringBuilder featureVector = new StringBuilder();
		while(tier2Itr.hasNext()){
			Map.Entry<Integer, Integer> tier2MapValue = tier2Itr.next();
			featureVector.append(String.valueOf(tier2MapValue.getKey()).
					concat(":").concat(String.valueOf(tier2MapValue.getValue())).concat(" "));
		}
		
		ArrayList<String> featureMap = new ArrayList<>();
		featureMap.add(String.valueOf(instanceType));
		featureMap.add(featureTree);
		featureMap.add(featureVector.toString().trim());
		
		/**
		System.out.println("\n\tfinal feature tree>>"+featureTree + 
				"\n\tfeatureVector>>"+featureVector.toString().trim()
				+"\n\tinstanceType>>"+instanceType);**/
		return(featureMap);
		
	}

	private LinkedHashMap<Integer, HashMap<String, HashMap<String, ArrayList<String>>>> 
	treeGenerationAndRestructuring(
			ArrayList<String> posTaggedSentenceSplit, 
			ArrayList<String> orgSentenceSplit,
			TreeMap<Integer, String> cprClassType,
			LinkedHashMap<String, HashMap<Integer, HashMap<String, 
			HashMap<Double, ArrayList<Integer>>>>> contextMatchedIndices) throws IOException {
		
		/**
		System.out.println("\n\t"+posTaggedSentenceSplit+"\t"+orgSentenceSplit);
		System.out.println("\n\t"+posTaggedSentenceSplit+"\t"+orgSentenceSplit);
		if(orgSentenceSplit.size() == posTaggedSentenceSplit.size()){		
			System.out.println("\n\t yyes"+instanceType);		
		}else{		
			System.out.println("\n\t noo"+instanceType);		
		}**/
		/**
		 * Stanford Parser
		 */
		String leafNodeValue ="",leafNodeTag="";
		List<CoreLabel> parseTreeLabelList = new ArrayList<CoreLabel>();
	    for (int i=0;i<posTaggedSentenceSplit.size();i++) {
	      CoreLabel parseTreeLabel = new CoreLabel();
	      parseTreeLabel.setTag(posTaggedSentenceSplit.get(i));
	   	  
    	  if(posTaggedSentenceSplit.get(i).matches("RELATION")){
    		  leafNodeTag = posTaggedSentenceSplit.get(i).trim().replaceAll("\\d+", "");
			  leafNodeValue = orgSentenceSplit.get(i).replaceAll("\\,|\\;|\\!|\\?", "");
    		  if(leafNodeValue.contains("#")){
    			  leafNodeValue = leafNodeValue.split("#")[0];
    		  }
    	  }else if(posTaggedSentenceSplit.get(i).matches("[\\W&&[^\\s]]*CHEMICALPRI[\\W&&[^\\s]]*")){
    		  leafNodeTag = posTaggedSentenceSplit.get(i).trim();
    		//leafNodeTag = "NNP";
    		//leafNodeValue = posTaggedSentenceSplit.get(i).replaceAll("PRI", "0");
    		  leafNodeValue = "CEM";
    	  }else if(posTaggedSentenceSplit.get(i).matches("[\\W&&[^\\s]]*GENEPROPRI[\\W&&[^\\s]]*")){
    		  leafNodeTag = posTaggedSentenceSplit.get(i).trim();
    		//leafNodeTag = "NNP";
    		//leafNodeValue = posTaggedSentenceSplit.get(i).replaceAll("PRI", "0");
    		  leafNodeValue = "GPRO";
	      }else{
	    	  if(posTaggedSentenceSplit.get(i).matches("GENEPRO")){
	    		  orgSentenceSplit.set(i,"genepro");
	    		  posTaggedSentenceSplit.set(i,"NNP");
	    	  }else if(posTaggedSentenceSplit.get(i).matches("CHEMICAL")){
	    		  orgSentenceSplit.set(i,"chemical");
	    		  posTaggedSentenceSplit.set(i,"NNP");
	    	  }
	    	  leafNodeTag = posTaggedSentenceSplit.get(i);
	    	  leafNodeValue = orgSentenceSplit.get(i).replaceAll("\\,|\\;|\\!|\\?", "");
	    	  if((posTaggedSentenceSplit.get(i).matches("VB.{0,1}")) || 
	    			  (posTaggedSentenceSplit.get(i).matches("NEGVB.{0,1}"))){
	    		  if(leafNodeValue.contains("#")){
	    			  leafNodeValue = leafNodeValue.split("#")[0];
	    			  leafNodeTag = posTaggedSentenceSplit.get(i);
	    		  }
	    	  }
	      }
	      parseTreeLabel.setWord(leafNodeValue);
    	  parseTreeLabel.setTag(leafNodeTag);
    	  parseTreeLabel.setValue(leafNodeValue);
	      parseTreeLabelList.add(parseTreeLabel);
	    }
	    Tree parseTree = stanfordLexParser.apply(parseTreeLabelList);
	    //System.out.println("\n\t"+parseTree.deepCopy().toString());
	    LinkedHashMap<Integer, HashMap<String,HashMap<String, ArrayList<String>>>> prunedTreeHashMap = 
	    		new LinkedHashMap<>();
	    Iterator<Map.Entry<String, HashMap<Integer, HashMap<String,HashMap<Double, 
	    ArrayList<Integer>>>>>> tier1Itr = contextMatchedIndices.entrySet().iterator();
	    while(tier1Itr.hasNext()){
	    	Map.Entry<String, HashMap<Integer, HashMap<String,HashMap<Double, 
	    	ArrayList<Integer>>>>> tier1MapValue = tier1Itr.next();
	    	Iterator<Map.Entry<Integer, HashMap<String, HashMap<Double, ArrayList<Integer>>>>> tier2Itr = 
	    			tier1MapValue.getValue().entrySet().iterator();
	    	while(tier2Itr.hasNext()){
	    		Map.Entry<Integer, HashMap<String, HashMap<Double, ArrayList<Integer>>>> tier2MapValue = 
	    				tier2Itr.next();
	    		Iterator<Map.Entry<String, HashMap<Double, ArrayList<Integer>>>> tier3Itr = 
	    				tier2MapValue.getValue().entrySet().iterator();
	    		while(tier3Itr.hasNext()){
	    			Map.Entry<String, HashMap<Double, ArrayList<Integer>>> tier3MapValue = tier3Itr.next();
	    			Set<Integer> currentMatchedIndices = new HashSet<>(
	    					tier3MapValue.getValue().values().iterator().next());
			    	//System.out.println("\n\t>>"+tier1ItrMapValue.getKey()+":"+currentMatchedIndices);
		    		//recursive function for tree traversal and pruning
		    		Tree decoyTree = parseTree.deepCopy();
		    		TreeTraversalAttributes treeInstance = 
		    				new TreeTraversalAttributes(currentMatchedIndices); 
		    		decoyTree = treeTraversalAndPrunning(decoyTree,treeInstance);
		    		String prunedTree = decoyTree.deepCopy().toString();
		    		//System.out.println("\n\t&&&&&"+prunedTree);
		    		prunedTree = prunedTree.replaceAll("null", "*");
		    		//System.out.println("\n\t>>"+prunedTree);
		    		HashMap<String, ArrayList<String>> subPatternFeatureMap = new LinkedHashMap<>();
		    		subPatternFeatureMap.put(tier3MapValue.getKey(), generatePositiveSPTKFeatures(
							prunedTree, tier2MapValue.getKey(), cprClassType));
		    		HashMap<String, HashMap<String, ArrayList<String>>> subInstanceFeatureMap = 
		    				new LinkedHashMap<>();
		    		if(prunedTreeHashMap.containsKey(tier2MapValue.getKey())){
		    			subInstanceFeatureMap = prunedTreeHashMap.get(tier2MapValue.getKey());
		    		}
		    		subInstanceFeatureMap.put(tier1MapValue.getKey(), subPatternFeatureMap);
		    		prunedTreeHashMap.put(tier2MapValue.getKey(),subInstanceFeatureMap);
	    		}
	    	}
	    }
	    return(prunedTreeHashMap);
	}
	
	private int compareContextScores(HashMap<Integer, ArrayList<Double>> patternScoreMap1,
			HashMap<Integer, ArrayList<Double>> patternScoreMap2) {
		
		ArrayList<Double> alphaDecoy = new ArrayList<>(
				patternScoreMap1.entrySet().iterator().next().getValue());
		ArrayList<Double> betaDecoy = new ArrayList<>(
				patternScoreMap2.entrySet().iterator().next().getValue());
		Set<Integer> returnSet = new LinkedHashSet<>();
		for(int l=0;l<alphaDecoy.size();l++){
			// return the value of the instance type of lesser size
			// both in pattern and score value
			if(alphaDecoy.get(l) > betaDecoy.get(l)){
				//System.out.println("\n\t1.>>"+patternScoreMap2.entrySet().iterator().next().getKey());
				returnSet.add(patternScoreMap2.entrySet().iterator().next().getKey());
			}else{
				//System.out.println("\n\t2.>>"+patternScoreMap1.entrySet().iterator().next().getKey());
				returnSet.add(patternScoreMap1.entrySet().iterator().next().getKey());
			}
		}
		//System.out.println("\n\t returnSet>>"+returnSet);
		if(returnSet.size() == 1){
			return (returnSet.iterator().next().intValue());
		}else if(returnSet.size() > 1){
			System.err.println("\n\t compareContextScores() ~ Discrepency in pattern selection >>"+returnSet);
			return (returnSet.iterator().next().intValue());
		}
		return(-1);
	}
	
	private HashMap<String, HashMap<Double, ArrayList<Integer>>> manageSVMFeatureSize(
			HashMap<String, HashMap<Double, ArrayList<Integer>>> taskFeatures) {

		HashMap<String, HashMap<Double, ArrayList<Integer>>> returnFeatureSize = 
				new LinkedHashMap<>();
		Iterator<Entry<String, HashMap<Double, ArrayList<Integer>>>> tier1Itr = 
				taskFeatures.entrySet().iterator();
		int index=0;
		if(taskFeatures.size() >= 1){
			while((index < 1) && (tier1Itr.hasNext())){
				Entry<String, HashMap<Double, ArrayList<Integer>>> tier1MapValue = 
						tier1Itr.next();
				returnFeatureSize.put(tier1MapValue.getKey(), tier1MapValue.getValue());
				index++;
			}
		}else if ((taskFeatures.size() > 0) && (taskFeatures.size() < 3)){
			HashMap<String,HashMap<Double, ArrayList<Integer>>> lastIndex = new HashMap<>();
			while(tier1Itr.hasNext()){
				Entry<String, HashMap<Double, ArrayList<Integer>>> tier1MapValue = 
						tier1Itr.next();
				returnFeatureSize.put(tier1MapValue.getKey(), tier1MapValue.getValue());
				lastIndex.clear();
				lastIndex.put(tier1MapValue.getKey(), tier1MapValue.getValue());
				index++;
			}
			Map.Entry<String,HashMap<Double, ArrayList<Integer>>> lastEntry = 
					lastIndex.entrySet().iterator().next();
			while(index < 3){
				returnFeatureSize.put(lastEntry.getKey().
						concat(String.valueOf(index)),lastEntry.getValue());
				index++;
			}
		}
		//System.out.println("\n\t final feature>>"+returnFeatureSize);
		return(returnFeatureSize);
	}
	
	private InstanceTypeIdentification_FV identifyInstanceType(
			ArrayList<InstanceTypeIdentification_FV> instanceSelectorList) {

		InstanceTypeIdentification_FV returnInstance = new InstanceTypeIdentification_FV();
		if(instanceSelectorList.size() > 1){
			for(int i=0;i<instanceSelectorList.size()-1;i++){
				for(int j=(i+1);j<instanceSelectorList.size();j++){
					int scoreSize = instanceSelectorList.get(i).taskFeatures.size();
					if(instanceSelectorList.get(i).taskFeatures.size() 
							> instanceSelectorList.get(j).taskFeatures.size()){
						scoreSize = instanceSelectorList.get(j).taskFeatures.size();
					}
					//System.out.println("\n\t scoreSIze>>>"+scoreSize);
					HashMap<Integer,ArrayList<Double>> patternScoreMap1 = new LinkedHashMap<>();
					HashMap<Integer,ArrayList<Double>> patternScoreMap2 = new LinkedHashMap<>();
					ArrayList<String> patternKeySet = 
							new ArrayList<>(instanceSelectorList.get(i).taskFeatures.keySet());
					ArrayList<Double> tempList = new ArrayList<>(); 
					double scoreSum = 0.0;
					scoreSize = 1;
					for(int k=0;k<scoreSize;k++){
						scoreSum = scoreSum + instanceSelectorList.get(i).
								taskFeatures.get(patternKeySet.get(k)).keySet().iterator().next();
					}
					tempList.add(scoreSum);
					tempList.add(new Integer(instanceSelectorList.get(i).taskFeatures.size()).doubleValue());
					patternScoreMap1.put(i, tempList);
					
					patternKeySet = 
							new ArrayList<>(instanceSelectorList.get(j).taskFeatures.keySet());
					tempList = new ArrayList<>();
					scoreSum = 0.0;
					for(int k=0;k<scoreSize;k++){
						scoreSum = scoreSum + instanceSelectorList.get(j).
								taskFeatures.get(patternKeySet.get(k)).keySet().iterator().next();
					}
					tempList.add(scoreSum);
					tempList.add(new Integer(instanceSelectorList.get(j).taskFeatures.size()).doubleValue());
					patternScoreMap2.put(j, tempList);
					//System.out.println("\n\t patterns>>"+patternScoreMap1 + "\t>>"+patternScoreMap2);
					int retVal = compareContextScores(patternScoreMap1,patternScoreMap2);
					if(retVal != -1){
						//System.out.println("\n\t bef4 >>"+instanceSelectorList.size());
						instanceSelectorList.remove(retVal);
						//System.out.println("\n\tupdated >>"+instanceSelectorList.size());
						i=0;
						break;
					}
				}
			}
		}else{
			
		}
		//System.out.println("\n\t final val>>"+instanceSelectorList.size()+"\t>>"+instanceSelectorList.get(0).taskFeatures);
		returnInstance.setOrgSentenceSplit(instanceSelectorList.get(0).orgSentenceSplit);
		returnInstance.setPosTaggedSentenceSplit(instanceSelectorList.get(0).posTaggedSentenceSplit);
		returnInstance.setTaskFeatures(manageSVMFeatureSize(instanceSelectorList.get(0).taskFeatures));
		returnInstance.setInstanceType(instanceSelectorList.get(0).instanceType);
		return(returnInstance);
	}

	private double compareContextPatterns(ArrayList<String> reframeTokens, ArrayList<String> patternArray) {

		// change the match criteria for test data especially for verbs
		//System.out.println("\n\treframeTokens>>"+reframeTokens+"\n\tpatternArray>>"+patternArray+"\n");
		int matchCount=0;
		double matchSum = 0.0;
		for(int index=0;index<patternArray.size();index++){
			if((!patternArray.get(index).equals("."))
					&& (patternArray.get(index).contentEquals(reframeTokens.get(index)))){
				//match
				matchSum = matchSum + 1.0;
				//System.out.print("\t1.0>>"+matchSum);
				matchCount++;
			}else if(((patternArray.get(index).equals("."))
					&& (!reframeTokens.get(index).equals(".")))
					|| ((!patternArray.get(index).equals("."))
							&& (reframeTokens.get(index).equals(".")))){
				// quasi token match
				matchSum = matchSum + 0.2;
				//System.out.print("\t0.2>>"+matchSum);
				matchCount++;
			}else if((patternArray.get(index).equals("."))
					&& (reframeTokens.get(index).equals("."))){
				// free match
				matchSum = matchSum + 0.01;
				//System.out.print("\t0.01>>"+matchSum);
				matchCount++;
			}else if((patternArray.get(index).matches("VB.{0,1}"))
					&& (reframeTokens.get(index)).matches("VB.{0,1}")){
				//+ve instance verb match
				matchSum = matchSum + 0.5;
				//System.out.print("\t0.5>>"+matchSum);
				matchCount++;
			}else if((patternArray.get(index).matches("NEGVB.{0,1}"))
					&& (reframeTokens.get(index)).matches("NEGVB.{0,1}")){
				//-ve instance verb match
				matchSum = matchSum + 0.5;
				//System.out.print("\t0.5>>"+matchSum);
				matchCount++;
			}else{
				matchSum = matchSum - 0.5;
				//System.out.print("\t-0.5>>"+matchSum);
				// no match
			}
		}
		//System.out.println("\n\tmatchSum>>"+matchSum+"\t>>"+patternArray);
		if(matchCount >= (patternArray.size()*0.85)){
			return(matchSum);
		}else{
			return(-100.0);
		}
	}
	
	public ArrayList<String> updateRelationTerm(ArrayList<String> tokenArray) {
		
		ArrayList<String> returnList = new ArrayList<>();
		for(String token : tokenArray){
			if(token.matches("(\\W)*RELATION\\d+(\\W)*")){
				token = token.replaceAll("\\d+", "");
			}
			returnList.add(token);
		}
		return(returnList);
	}
	
	private HashMap<Double, ArrayList<Integer>> indexingAndMatching(
			ArrayList<Integer> patternIndices, 
			ArrayList<String> patternArray, int compareIndex,
			ArrayList<String> compareTokenArray) {

		//System.out.println("\n\t compareTokenArray>>"+compareTokenArray+"\n\tpatternArray>>"+patternArray);
		for(Integer pivotIndex : patternIndices){
			//System.out.println("\n\t pivot>>>"+pivotIndex+"\t\tcompareIndex>>"+compareIndex);
			int decoyCompareIndex = compareIndex;
			ArrayList<Integer> reframeIndices = new ArrayList<>();
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
				reframeIndices.add(prefixFiller);
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
				reframeIndices.add(decoyCompareIndex);
				if(decoyCompareIndex == compareIndex){
					//replace Entity named with pattern entity
					reframeTokens.add(patternArray.get(pivotIndex));
				}else{
					// add other terms
					reframeTokens.add(compareTokenArray.get(decoyCompareIndex));
				}
				decoyCompareIndex++;
			}
			//System.out.println("\n\treframeIndices>>>"+reframeIndices+"\t>>>"+reframeTokens);
			while(additionalIndex > 0){
				reframeTokens.add(".");
				additionalIndex--;
			}
			//System.out.println("\n\t final reframeIndices>>>"+reframeIndices+"\t>>>"+reframeTokens);
			// SIZE INTEGRITY CHECK
			if(reframeTokens.size() == patternArray.size()){
				double retVal = compareContextPatterns(reframeTokens,patternArray);
				//return after first index match
				if(-100.0 != retVal){
					Collections.sort(reframeIndices);
					//System.out.println("\n\t string>>"+reframeTokens+"\n\t pattern>>"+
							//patternArray+"\n\t reframeIndices>>>>"+reframeIndices);
					HashMap<Double, ArrayList<Integer>> returnHash = new LinkedHashMap<>();
					returnHash.put(retVal,reframeIndices);
					return(returnHash);
				}
			}else{
				//System.out.println("\n\t string>>"+reframeTokens+"\n\t pattern>>"+patternArray);
				System.out.println(" indexingAndMatching() ~ SIZE CONSISTENCY CHECK ALERT");
			}
		}
		return(null);
	}
	
	private HashMap<String, HashMap<Double, ArrayList<Integer>>> orderPatterns(
			HashMap<String, HashMap<Double, ArrayList<Integer>>> matchedPattern) {
		
		HashMap<String, Double> decoyHash = new LinkedHashMap<>();
		Iterator<Map.Entry<String, HashMap<Double, ArrayList<Integer>>>> tier1Itr = 
				matchedPattern.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<String, HashMap<Double, ArrayList<Integer>>> tier1Value = tier1Itr.next();
			decoyHash.put(tier1Value.getKey(), tier1Value.getValue().entrySet().iterator().next().getKey());
		}
		
		List<Map.Entry<String, Double>> relationCompareList = new LinkedList<>(decoyHash.entrySet());
		Collections.sort(relationCompareList,
				new Comparator<Map.Entry<String, Double>>() {

			@Override
			public int compare(Map.Entry<String, Double> currItem,
					Map.Entry<String, Double> nextItem) {
				return (nextItem.getValue().compareTo(currItem.getValue()));
			}
		});
		
		HashMap<String, HashMap<Double, ArrayList<Integer>>> returnHash = new LinkedHashMap<>();
		for(int i=0;i<relationCompareList.size();i++){
			String keyValue = relationCompareList.get(i).getKey();
			returnHash.put(keyValue, matchedPattern.get(keyValue));
		}
		return(returnHash);
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
	
	@Override
	public HashMap<String,HashMap<Double, ArrayList<Integer>>> call() throws Exception {
		
		Matcher patternMatcher;
		String patternType="";
		HashMap<String,HashMap<Double, ArrayList<Integer>>> matchedPattern = 
				new LinkedHashMap<>();
		for(String patternId : aiContextPatterns.keySet()){
			//if(patternId.equals("pattern202")){
			//System.out.println("\n\t>>"+patternId);
			Iterator<Map.Entry<String, HashMap<String, ArrayList<Integer>>>> tier1Itr = 
					aiContextPatterns.get(patternId).entrySet().iterator();
			BitSet matchBit = new BitSet();
			int currIndex=0;
			HashMap<Double, ArrayList<Integer>> decoyTier1List = new LinkedHashMap<>();
			while(tier1Itr.hasNext()){
				Map.Entry<String, HashMap<String, ArrayList<Integer>>> tier1MapValue = 
						tier1Itr.next();
				patternType = tier1MapValue.getKey().trim();
				//System.out.println("\n\t key>>"+tier1MapValue.getKey().trim());
				if(!tier1MapValue.getKey().trim().equalsIgnoreCase("RELATION")){
					patternType = 
							"[\\W&&[^\\s]]*"+tier1MapValue.getKey().trim()+"PRI[\\W&&[^\\s]]*";
				}
				HashMap<Double, ArrayList<Integer>> decoyTier2List = new LinkedHashMap<>();
				// updating entity terms in accordance with AI-pattern
				ArrayList<String> decoyPOSTagger = new ArrayList<>(posTaggedTokens);
				decoyPOSTagger = updateRelationTerm(decoyPOSTagger);
				for(int index=0;index<decoyPOSTagger.size();index++){
					String token = decoyPOSTagger.get(index);
					//System.out.println("\n\tpatternType>>"+patternType+"\n\t>>"+token);
					patternMatcher = Pattern.compile(patternType).matcher(token);
					if(patternMatcher.matches()){
						//System.out.println("\n\tpatternMatchType>>>"+patternMatcher.group(0)+"\n\t>>"+posTaggedTokens);
						Map.Entry<String, ArrayList<Integer>> tier2MapValue =
								tier1MapValue.getValue().entrySet().iterator().next();
						//removing the period notation from the original construct
						//decoyPOSTagger.remove(decoyPOSTagger.size()-1);
						HashMap<Double, ArrayList<Integer>> tempDecoy = 
								indexingAndMatching(tier2MapValue.getValue(),
								new ArrayList<>(Arrays.asList(tier2MapValue.getKey().split(" "))),
								index, decoyPOSTagger);
						if(null != tempDecoy){
							decoyTier2List.putAll(tempDecoy);
						}
						// can add break after one match for improving performance
					}
				}
				// among multiple matches add the ones with the highest value
				if(decoyTier2List.size() > 1){
					List<Map.Entry<Double, ArrayList<Integer>>> relationCompareList = 
							new LinkedList<>(decoyTier2List.entrySet());
					
					//descending order
					Collections.sort(relationCompareList,
							new Comparator<Map.Entry<Double, ArrayList<Integer>>>() {

						@Override
						public int compare(Map.Entry<Double, ArrayList<Integer>> currItem,
								Map.Entry<Double, ArrayList<Integer>> nextItem) {
							return (nextItem.getKey().compareTo(currItem.getKey()));
						}
					});
					decoyTier2List.clear();
					decoyTier2List.put(relationCompareList.get(0).getKey(),relationCompareList.get(0).getValue());
				}
				//System.out.println("\n\tteir2>>"+decoyTier2List+"\n\t>>"+patternType);
				if(decoyTier2List.size() > 0){
					if(decoyTier1List.isEmpty()){
						matchBit.set(currIndex);
						if(!patternType.contains("RELATION")){
							decoyTier1List.putAll(decoyTier2List);
						}else{
							decoyTier1List.put(0.0,decoyTier2List.values().iterator().next());
						}
					}else{
						matchBit.set(currIndex);
						Map.Entry<Double, ArrayList<Integer>> tempDecoyTier1 = 
								decoyTier1List.entrySet().iterator().next();
						Map.Entry<Double, ArrayList<Integer>> tempDecoyTier2 = 
								decoyTier2List.entrySet().iterator().next();
						double keyValue = 0.0;
						if(!patternType.contains("RELATION")){
							keyValue = tempDecoyTier1.getKey() + tempDecoyTier2.getKey();
						}else{
							keyValue = tempDecoyTier1.getKey();
						}
						Set<Integer> tempDecoy = new HashSet<>(tempDecoyTier1.getValue());
						tempDecoy.addAll(tempDecoyTier2.getValue());
						ArrayList<Integer> tempList = new ArrayList(tempDecoy);
						Collections.sort(tempList);
						decoyTier1List.clear();
						decoyTier1List.put(keyValue, tempList);
					}
				}
				currIndex++;
			}
			if(matchBit.cardinality() == aiContextPatterns.get(patternId).size()){
				matchedPattern.put(patternId,decoyTier1List);
			}
			//System.out.println("\n\t>>"+decoyTier1List+"\tmatchit>>"+matchBit);
			//System.out.println("\n\t matchBit>>"+matchBit+"\t\t>>>"+matchedPattern+"\t\t>>"+instanceAIContextPatterns.get(patternId).size()+"\t\t>>"+matchBit.cardinality());
			//}
		}
		matchedPattern = orderPatterns(matchedPattern);
		//System.out.println("\n\tmatchedPattern>>"+matchedPattern);
		haltThreadProcess();
		return matchedPattern;
	}
	
	public void createAIPatternBasedParseTree(SVM_AIFeatureKernel featureInstance)
			throws InterruptedException,
	ExecutionException, IOException {
		
		ArrayList<String> orgSentenceSplit,posTaggedSentenceSplit;
		String originalSentence="",posTaggedSentence="",documentId="";
		//Random poolSizeGenerator = new Random();
		Integer threadPoolSize=1;
		//load the class type hash
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
		
		ArrayList<String> posTaggedValueList = new ArrayList<>();
		ArrayList<String> orgValueList = new ArrayList<>();
		ArrayList<String> posTaggedDocList = new ArrayList<>();
		ArrayList<Integer> posTaggedInstanceList = new ArrayList<>();	
		for(Map.Entry<Integer, HashMap<String, String>> mapEntry1 : featureInstance.
				posTaggedRelationSentence.entrySet() ){
			for(Map.Entry<String, String> mapEntry2 : mapEntry1.getValue().entrySet()){
				posTaggedDocList.add(mapEntry2.getKey());
				posTaggedValueList.add(mapEntry2.getValue());
				posTaggedInstanceList.add(mapEntry1.getKey());
				orgValueList.add(
						featureInstance.originalRelationSentence.get(mapEntry1.getKey()).get(mapEntry2.getKey()));
			}
		}
		
		long beginSysTime = System.currentTimeMillis();
		int universalCounter = 0;
		CorpusDictionary corpusInstance = new CorpusDictionary();
		for(int posIndex=0;posIndex < posTaggedDocList.size();posIndex++){
			//if(posTaggedDocList.get(posIndex).contains("23146838@6R16")){
			//if(universalCounter < 25){
			//System.out.println("\n\t1.>>"+posTaggedDocList.size());
			documentId = posTaggedDocList.get(posIndex);
			posTaggedSentence = posTaggedValueList.get(posIndex);
			originalSentence = orgValueList.get(posIndex);
			documentId = documentId.replaceAll("R\\d+", "R");
			posTaggedSentence = posTaggedSentence.replaceAll("RELATION\\d+", "RELATION");
			//ArrayList<String> posTaggedSentenceList = new ArrayList<>();
			//posTaggedSentence = corpusInstance.patternBuilder(posTaggedSentence);
			//posTaggedSentenceList.add(posTaggedSentence);
			//posTaggedSentence = posTaggedSentence.replaceAll("RELATION", "VB.{0,1}");
			//posTaggedSentenceList.add(posTaggedSentence);
			//System.out.println("\n\t1.>>"+posTaggedSentenceList);
			HashMap<String, Integer> instanceHashMap = new HashMap<>();
			int flag = 0;
			for(int subPosIndex=0;subPosIndex < posTaggedDocList.size();subPosIndex++){
				String compareDocumentId = posTaggedDocList.get(subPosIndex);
				String comparePOSTaggedSentence = posTaggedValueList.get(subPosIndex);
				compareDocumentId = compareDocumentId.replaceAll("R\\d+", "R");
				comparePOSTaggedSentence = comparePOSTaggedSentence.replaceAll("RELATION\\d+", "RELATION");
				if((documentId.contentEquals(compareDocumentId)) 
						&& (posTaggedSentence.contentEquals(comparePOSTaggedSentence))){
					//System.out.println("\n\t2.>>"+posTaggedDocList.size());
					//System.out.println("\n\t2.>>"+comparePOSTaggedSentence);
					instanceHashMap.put(posTaggedDocList.get(subPosIndex),posTaggedInstanceList.get(subPosIndex));
					//removalIndex.add(subPosIndex);
					posTaggedDocList.remove(subPosIndex);
					posTaggedValueList.remove(subPosIndex);
					orgValueList.remove(subPosIndex);
					posTaggedInstanceList.remove(subPosIndex);
					subPosIndex--;
					flag = 1;
				}
			}
			if(flag == 1){
				posIndex--;
			}
			if(!instanceHashMap.isEmpty()){
				if(instanceHashMap.size() == 1){
					threadPoolSize = 1;
				}else{
					threadPoolSize = (instanceHashMap.size()/2);
				}
				System.out.println("\n"+threadPoolSize);
				Matcher relationMatcher;
				for(String pattern : new ArrayList<>(Arrays.asList(
						"[\\W&&[^\\s]]*CHEMICAL(R\\d+T\\d+)+[\\W&&[^\\s]]*",
						"[\\W&&[^\\s]]*GENEPRO(R\\d+T\\d+)+[\\W&&[^\\s]]*"))){
					relationMatcher = Pattern.compile(pattern).matcher(posTaggedSentence);
					while(relationMatcher.find()){
						String entityGroup = relationMatcher.group(0).split("R")[0];
						posTaggedSentence = posTaggedSentence.
								replaceAll(relationMatcher.group(0), entityGroup.concat("PRI"));
					}
				}
				//System.out.println("\n\t "+posTaggedSentence+"\n\t>>"+instanceHashMap);
				
				orgSentenceSplit = new ArrayList<>(Arrays.asList(originalSentence.split("\\s")));
				posTaggedSentenceSplit = new ArrayList<>(Arrays.asList(posTaggedSentence.split("\\s")));
				// ARRAY SIZE INTEGRITY TEST
				if(orgSentenceSplit.size() != posTaggedSentenceSplit.size()){
					System.err.println("\n>>>"+originalSentence+"\n\t>>"+posTaggedSentence+
							"\t\t>>"+instanceHashMap.keySet());
				}
				
				if(posTaggedSentenceSplit.size() >= 70){
					int beginCounter = universalCounter;
					universalCounter = universalCounter+ (instanceHashMap.size()-1);
					System.err.println("\n\t Index>>"+beginCounter+"~"+universalCounter+
							" Excess for parser "+ instanceHashMap.keySet());
				}else{
					// add reference to multiple instances
					List<Map.Entry<String, Integer>> instanceList = new LinkedList<>(
							instanceHashMap.entrySet());
					Collections.sort(instanceList, new Comparator<Map.Entry<String, Integer>>() {
						// descending order
						@Override
						public int compare(Map.Entry<String, Integer> currItem, 
								Map.Entry<String, Integer> nextItem) {
							return (nextItem.getValue().compareTo(currItem.getValue()));
						}
					});
					ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(threadPoolSize);
					LinkedList<String> docList = new LinkedList<>();
					LinkedHashMap<String, HashMap<Integer, HashMap<String, 
					HashMap<Double, ArrayList<Integer>>>>> patternList = new LinkedHashMap<>();
					for(int i=0;i<instanceList.size();i++){
						docList.add(instanceList.get(i).getKey());
						String docId = instanceList.get(i).getKey();
						Integer instanceType = instanceList.get(i).getValue();
						System.out.println("\n\t index>>"+universalCounter+"\t>>"+docId+
								"\t>>"+instanceType);
						HashMap<String, HashMap<String, HashMap<String, ArrayList<Integer>>>> contextPatterns = 
								new LinkedHashMap<>();
						contextPatterns = featureInstance.instanceAIContextPatterns.get(instanceType);
						SVM_ParseTreeFeature_FeatureVectorSplitInstance workerThread = 
								new SVM_ParseTreeFeature_FeatureVectorSplitInstance(
								orgSentenceSplit,
								posTaggedSentenceSplit, contextPatterns);
						Future<HashMap<String,HashMap<Double, ArrayList<Integer>>>> taskCollector = 
								threadPoolExecutor.submit(workerThread);
						if((!taskCollector.get().isEmpty())){
							HashMap<Integer, HashMap<String, HashMap<Double, ArrayList<Integer>>>> subPattern = 
									new LinkedHashMap<>();
							subPattern.put(instanceType,manageSVMFeatureSize(taskCollector.get()));
							if(!patternList.containsKey(docId)){
								patternList.put(docId, subPattern);
							}else{
								System.err.println("\n\t Duplicate instance added>>"+docId);
								System.exit(0);
							}
						}else{
							System.err.println("\n\t faulty pattern>>"+instanceList.get(i).getKey()+
									"\n\t selected patterns>>"+taskCollector.get());
						}
						universalCounter++;
					}
					threadPoolExecutor.shutdown();
					
					if(!patternList.isEmpty()){
						LinkedHashMap<Integer, HashMap<String,HashMap<String, ArrayList<String>>>> prunedTreeHashMap = 
								treeGenerationAndRestructuring(posTaggedSentenceSplit,
										orgSentenceSplit, cprClassType, patternList);
						Iterator<Map.Entry<Integer, HashMap<String,HashMap<String, ArrayList<String>>>>> tier1Itr = 
								prunedTreeHashMap.entrySet().iterator();
						while(tier1Itr.hasNext()){
							Map.Entry<Integer, HashMap<String,HashMap<String, ArrayList<String>>>> tier1MapValue = 
									tier1Itr.next();
							Iterator<Map.Entry<String,HashMap<String, ArrayList<String>>>> tier2Itr = 
									tier1MapValue.getValue().entrySet().iterator();
							while(tier2Itr.hasNext()){
								Map.Entry<String,HashMap<String, ArrayList<String>>> tier2MapValue = 
										tier2Itr.next();
								HashMap<String, HashMap<String, ArrayList<String>>> decoyHash = new LinkedHashMap<>();
								if(sptkPatterns.containsKey(tier1MapValue.getKey())){
									decoyHash = sptkPatterns.get(tier1MapValue.getKey());
								}
								if(decoyHash.containsKey(tier2MapValue.getKey())){
									System.err.println("\n\t Duplicate instance added>>"+tier2MapValue.getKey());
									System.exit(0);
								}
								decoyHash.put(tier2MapValue.getKey(),tier2MapValue.getValue());
								sptkPatterns.put(tier1MapValue.getKey(), decoyHash);
							}
						}
					}else{
						System.err.println("\n\t Document Missing>>"+docList);
					}
				}
			}else{
				System.err.println("\n\t no more values");
			}
			/**
			}else{
				break;
			}**/
			//}
		}
		System.out.println("\n Total Execution Time:-"+(System.currentTimeMillis()-beginSysTime)/1000);
		
		/**
		for(Integer instanceType : featureInstance.posTaggedRelationSentence.keySet()){
			
			System.out.print("\t instanceType>>"+instanceType+
					"\t>>"+featureInstance.posTaggedRelationSentence.get(instanceType).size());
			//if(instanceType == 9){
				
			ArrayList<String> keySet = new ArrayList<>(
					featureInstance.posTaggedRelationSentence.get(instanceType).keySet());
			if(!keySet.isEmpty() & keySet.size() > 1){
				threadPoolSize = (keySet.size()/2);
			}
			System.out.println("\n"+threadPoolSize);
			ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(threadPoolSize);
			for(int index=0;index<keySet.size();index++){
				
				String docId = keySet.get(index);
				//if(docId.contains("10082498@5")){//11752354@2
				System.out.println("\n\t>>"+docId+"\t index>>"+index);
				ArrayList<InstanceTypeIdentification_FV>  instanceSelectorList = 
						new ArrayList<>(); 
								
				originalSentence = featureInstance.originalRelationSentence.get(instanceType).get(docId);
				posTaggedSentence = featureInstance.posTaggedRelationSentence.get(instanceType).get(docId);
				//System.out.println("\n\t originalSentence>>"+originalSentence+"\t posTaggedSentence>>"+posTaggedSentence);
				Matcher relationMatcher;
				for(String pattern : new ArrayList<>(Arrays.asList(
						"[\\W&&[^\\s]]*CHEMICAL(R\\d+T\\d+)+[\\W&&[^\\s]]*",
						"[\\W&&[^\\s]]*GENEPRO(R\\d+T\\d+)+[\\W&&[^\\s]]*"))){
					relationMatcher = Pattern.compile(pattern).matcher(posTaggedSentence);
					while(relationMatcher.find()){
						String entityGroup = relationMatcher.group(0).split("R")[0];
						posTaggedSentence = posTaggedSentence.
								replaceAll(relationMatcher.group(0), entityGroup.concat("PRI"));
					}
				}
						
				orgSentenceSplit = new ArrayList<>(Arrays.asList(originalSentence.split("\\s")));
				posTaggedSentenceSplit = new ArrayList<>(Arrays.asList(posTaggedSentence.split("\\s")));
				// ARRAY SIZE INTEGRITY TEST
				if(orgSentenceSplit.size() != posTaggedSentenceSplit.size()){
					System.err.println("\n>>>"+originalSentence+"\n\t>>"+posTaggedSentence+"\t\t>>"+docId);
				}
				
				if(posTaggedSentenceSplit.size() >= 70){
					System.err.println("\n\t Excess for parser "+ docId);
					continue;
				}

				// add reference to multiple instances
				HashMap<String, HashMap<String, HashMap<String, ArrayList<Integer>>>> contextPatterns = 
						new LinkedHashMap<>();
				contextPatterns = featureInstance.instanceAIContextPatterns.get(instanceType);
				
				SVM_ParseTreeFeature_FeatureVectorSplitInstance workerThread = new SVM_ParseTreeFeature_FeatureVectorSplitInstance(
						orgSentenceSplit,
						posTaggedSentenceSplit, contextPatterns);
				Future<HashMap<String,HashMap<Double, ArrayList<Integer>>>> taskCollector = 
						threadPoolExecutor.submit(workerThread);
				if((!taskCollector.get().isEmpty())){
					instanceSelectorList.add(new InstanceTypeIdentification_FV(orgSentenceSplit,
							posTaggedSentenceSplit, manageSVMFeatureSize(taskCollector.get()), instanceType));
				}else{
					//System.err.println("\n\t faulty pattern>>"+docId+"\n\t selected patterns>>"+taskCollector.get());
				}
				//select task and send for tree generation
				if((!instanceSelectorList.isEmpty())){
					//System.out.println("\n\t selector size>>"+instanceSelectorList.size());
					
					for(InstanceTypeIdentification_FV currentInstanceIdentifier : instanceSelectorList){
						HashMap<String, HashMap<String, ArrayList<String>>> decoyHash = new LinkedHashMap<>();
						if(sptkPatterns.containsKey(currentInstanceIdentifier.getInstanceType())){
							decoyHash = sptkPatterns.get(currentInstanceIdentifier.getInstanceType());
						}
						HashMap<String, ArrayList<String>> prunedTreeHashMap = 
								treeGenerationAndRestructuring(currentInstanceIdentifier.getPosTaggedSentenceSplit(),
										currentInstanceIdentifier.getOrgSentenceSplit(),cprClassType,
										currentInstanceIdentifier.getTaskFeatures(),
										currentInstanceIdentifier.getInstanceType());
						if(decoyHash.containsKey(docId)){
							System.err.println("\n\t Duplicate instance added>>"+docId);
							System.exit(0);
						}
						decoyHash.put(docId, prunedTreeHashMap);
						sptkPatterns.put(currentInstanceIdentifier.getInstanceType(), decoyHash);
					}
					
				}else{
					System.err.println("\n\t Document Missing>>"+docId);
				}// end if-else
			}// end for
			threadPoolExecutor.shutdown();
			//}
		}
		System.out.println("\n Total Execution Time:-"+(System.currentTimeMillis()-beginSysTime)/1000); **/
		
		Properties systemProperties = new Properties();
        InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
        FileWriter fileWS = new FileWriter(systemProperties.getProperty("svmTestingFeature"));
		BufferedWriter buffWS = new BufferedWriter(fileWS);
		FileWriter fileIdWS = new FileWriter(systemProperties.getProperty("svmTestingFeatureInstances"));
		BufferedWriter buffIdWS = new BufferedWriter(fileIdWS);
		for(Integer instance : sptkPatterns.keySet()){
			Iterator<Map.Entry<String, HashMap<String, ArrayList<String>>>> tier2Itr = 
					sptkPatterns.get(instance).entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<String, HashMap<String, ArrayList<String>>> tier2Value = 
						tier2Itr.next();
				String currentValue = generateFeatureString(tier2Value.getValue());
				buffWS.write(currentValue);
				buffWS.newLine();
				buffIdWS.write(tier2Value.getKey().concat("\t:").
						concat(String.valueOf(instance)));
				buffIdWS.newLine();
			}
		}
		buffWS.flush();
		buffIdWS.close();
		
		fileWS = new FileWriter(systemProperties.getProperty("svmTestingFeatureId"));
		buffWS = new BufferedWriter(fileWS);
		Iterator<Map.Entry<String, Integer>> tier3Itr = sptkFeatures.entrySet().iterator();
		while(tier3Itr.hasNext()){
			Map.Entry<String, Integer> tier3Val = tier3Itr.next();
			buffWS.write(tier3Val.getKey().concat("@"));
			buffWS.write(String.valueOf(tier3Val.getValue()));
			buffWS.newLine();
		}
		buffWS.close();
		
	}
}