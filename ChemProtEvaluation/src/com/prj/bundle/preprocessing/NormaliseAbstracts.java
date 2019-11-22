/**
 * 
 */
package com.prj.bundle.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.jmcejuela.bio.jenia.Bidir;
import com.jmcejuela.bio.jenia.Chunking;
import com.jmcejuela.bio.jenia.JeniaTagger;
import com.jmcejuela.bio.jenia.MorphDic;
import com.jmcejuela.bio.jenia.common.Sentence;
import com.jmcejuela.bio.jenia.common.Token;

import edu.stanford.nlp.ling.CoreAnnotations.NeighborsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.StemAnnotation;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.DependencyPrinter;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.WordStemmer;
import edu.stanford.nlp.util.Sets;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.logging.StanfordRedwoodConfiguration;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import edu.stanford.nlp.ling.Word;

/**
 * @author neha
 *
 */

public class NormaliseAbstracts extends PopulateResources 
implements Callable<ArrayList<ArrayList<ArrayList<String>>>>{
	
	private ArrayList<String> bundle;
	private ArrayList<LinkedHashSet<String>> relationPairs;
	private TreeMap<Integer, ArrayList<String>> chemicalPatternList;
	private TreeMap<Integer, ArrayList<String>> genePatternList;
	private Properties systemProperties;
	
	//constructors
	public NormaliseAbstracts() throws IOException {
		this.systemProperties = new Properties();
        InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
	}

	public NormaliseAbstracts(ArrayList<String> abstractBundle, 
			ArrayList<LinkedHashSet<String>> relationList, 
			TreeMap<Integer, ArrayList<String>> parsedChemicalPatternList, 
			TreeMap<Integer, ArrayList<String>> parsedGenePatternList, 
			Properties systemProperties){
		this.bundle = abstractBundle;
		this.relationPairs = relationList;
		this.chemicalPatternList = parsedChemicalPatternList;
		this.genePatternList = parsedGenePatternList;
		this.systemProperties = systemProperties;
	}
	
	private HashMap<String, String> populateStringHashWithAppendedString(HashMap<String, String> decoyHash, 
			String tokenKey, String suffixTag) {
		
		if(!decoyHash.isEmpty()){
			if(decoyHash.containsKey(tokenKey)){
				String appendTag = decoyHash.get(tokenKey);
				//System.out.println("\n\t appendTag outside>>"+appendTag+"\tsuffixTag>>"+suffixTag);
				suffixTag = appendTag.concat(suffixTag);
			}
		}
		decoyHash.put(tokenKey,suffixTag);
		return(decoyHash);
	}

	/**
	 * Instantiate the values retrieved from corpus data into the corresponding dictionaries  
	 * @param resultHolder
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public void addCorpusResource(Hashtable<String, 
			LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>>> resultHolder)
			throws IOException, ParserConfigurationException, TransformerException, InterruptedException, ExecutionException {
		
		NormaliseAbstracts normaliseInstance = new NormaliseAbstracts();
		Enumeration<String> keySet = resultHolder.keys();
		while(keySet.hasMoreElements()){
			String keyData = keySet.nextElement();
			switch (keyData) {
			case "Chemical":
				normaliseInstance.setChemicalEntities(resultHolder.get(keyData));
				break;
			case "Gene":
				normaliseInstance.setGeneEntities(resultHolder.get(keyData));
				break;
			case "Abstract":
				normaliseInstance.setAbstractCollection(resultHolder.get(keyData));
				break;
			case "Relation":
				normaliseInstance.setRelationCollection(resultHolder.get(keyData));
				break;
			default:
				break;
			}
		}
		abstractEntitySwap(normaliseInstance);
	}
	
	/**
	 * Replace a dictionary matching token with corresponding NE
	 * @param targetString
	 * @param tokenString
	 * @param entityType 
	 * @param relationItem 
	 * @return 
	 */
	private String replaceTokenWithNE(String targetString, String tokenString, 
			String entityType, int relationItem) {
		
		/**
		 * First level of comparison to check if the pattern is present is any form
		 */
		Pattern patternString;
		Matcher patternMatcher;
		if((tokenString.length() >= 1) && (tokenString.length() <= 2)) {
			// camel case only capital
			patternString = Pattern.compile(tokenString);
		}else{
			if(relationItem == 1){
				patternString = Pattern.compile(tokenString);
			}else{
				patternString = Pattern.compile(tokenString, Pattern.CASE_INSENSITIVE);
			}
		}
		patternMatcher = patternString.matcher(targetString);
		StringBuilder recurringString = new StringBuilder();
		String lastSavepoint = targetString;
		int flag = 0;
		while(patternMatcher.find()){
			/**
			System.out.println("\n\t tokenString>"+tokenString+"\t entityType>"+entityType
					+"\t relationItem>"+relationItem);
			System.out.println("\n\tmatch found "+patternMatcher.group(0)+"\t>>"+
					patternString+"\t\t>>"+entityType+"\t flag>>"+flag
					+"\t tokenString>>"+tokenString+"\t>>"+targetString);**/
			if(!(targetString.contentEquals(lastSavepoint))){
				patternMatcher.reset();
				patternMatcher = patternString.matcher(targetString);
				if(patternMatcher.find()){
					flag = 0;
				}
			}
			if(flag == 0){
				lastSavepoint = targetString;
				int startIndex = patternMatcher.start();
				int endIndex = (patternMatcher.end()-1);
				//System.out.println("\n\t startIndex::"+startIndex+"\t endIndex::"+endIndex);
				StringBuilder tempBuilder = new StringBuilder();						
				if((startIndex != 0) && (endIndex != (targetString.length()-1))){
					//System.out.println("\n\t in middle: ");
					if((Pattern.compile("\\W").matcher(String.valueOf(targetString.charAt(startIndex-1))).find())
							&& (Pattern.compile("\\W").matcher(String.valueOf(targetString.charAt(endIndex+1))).find())){
						recurringString.append(String.valueOf(targetString.subSequence(0, (startIndex))).toString());
						String subSeqEnd = String.valueOf(targetString.subSequence((endIndex+1), targetString.length()));
						targetString = tempBuilder.append(entityType).append(subSeqEnd).toString();
						//System.out.println("\n\t in middle: "+targetString);
					}						
				}else if(startIndex == 0){
					//System.out.println("\n\t in start: ");
					if((Pattern.compile("\\W").matcher(String.valueOf(targetString.charAt(endIndex+1))).find())){
						String subSeqEnd = String.valueOf(targetString.subSequence((endIndex+1), targetString.length()));
						targetString = tempBuilder.append(entityType).append(subSeqEnd).toString();
						//System.out.println("\n\t in start: "+targetString);
					}
				}else if(endIndex == (targetString.length()-1)){
					//System.out.println("\n\t in end: ");
					if((Pattern.compile("\\W").matcher(String.valueOf(targetString.charAt(startIndex-1))).find())){
						recurringString.append(String.valueOf(targetString.subSequence(0, (startIndex))).toString());
						targetString = tempBuilder.append(entityType).toString();
						//System.out.println("\n\t in end: "+targetString);
					}
				}
				flag = 1;
			}
		}
		targetString = recurringString.append(targetString).toString();
		return(targetString);
	}

	/**
	 * Compare a matching pattern between dictionary and abstract 
	 * @param currentString
	 * @param chemicalPatternList
	 * @param string
	 * @return 
	 */
	private StringBuilder enforcePatternMatch(StringBuilder currentString, 
			TreeMap<Integer, ArrayList<String>> parsedTokenPatternList, String entityType) {
		
		//System.out.println("\n\t currentString>>"+currentString.toString());
		String targetString = currentString.toString();
		int relationItem=0;
		ArrayList<LinkedHashSet<String>> relationPairArray = this.relationPairs;
		//System.out.println("\n\t ETYPE>>>"+entityType+"\t>>"+relationPairArray);
		/**
		 * First tend to all the relation based entity names
		 * Match if the entity name from library matches the entity in relation pair group
		 */
		TreeMap<Integer,HashMap<String, String>> replaceMap = new TreeMap<>();
		if(!relationPairArray.isEmpty()){
			int index=0, flag = 0;
			Iterator<LinkedHashSet<String>> tier1Itr = relationPairArray.iterator();
			while(tier1Itr.hasNext()){
				LinkedHashSet<String> tier1Value = tier1Itr.next();
				//System.out.println("\n\t>>"+tier1Value);
				Iterator<String> tier2Itr = tier1Value.iterator();
				while(tier2Itr.hasNext()){
					String suffixTag="";
					String[] relationCategory = tier2Itr.next().split("\\#");
					// Check for corresponding entry type
					if(relationCategory[0].equalsIgnoreCase(entityType)){
						HashMap<String, String> decoyHash = new LinkedHashMap<>();
						String relationType = String.valueOf(index).concat("T"+relationCategory[1]);
						ArrayList<String> decoyParsedToken = 
								parsedTokenPatternList.get(relationCategory[2].length());
						if(replaceMap.containsKey(relationCategory[2].length())){
							decoyHash = replaceMap.get(relationCategory[2].length());
						}
						if(decoyParsedToken.contains(relationCategory[2])){
							// DIRECT MATCH
							suffixTag = suffixTag.concat("R").concat(relationType);
							decoyHash = populateStringHashWithAppendedString(decoyHash,relationCategory[2],suffixTag);
							if(!decoyHash.isEmpty()){
								replaceMap.put(relationCategory[2].length(),decoyHash);
							}
						}else{
							// CASE INSENSITIVE MATCH
							for(String decoyToken : decoyParsedToken){
								if(decoyToken.equalsIgnoreCase(relationCategory[2])){
									suffixTag = suffixTag.concat("R").concat(relationType);
									decoyHash = populateStringHashWithAppendedString(decoyHash,relationCategory[2],suffixTag);
									if(!decoyHash.isEmpty()){
										replaceMap.put(relationCategory[2].length(),decoyHash);
									}
									flag=1;
									break;
								}
							}
							if(flag == 0){
								System.err.println("\nenforcePatternMatch() \n ALERT! "+relationCategory[0]+
										"-NE NOT PRESENT in DICTIONARY >>"+relationCategory[2]);
							}
							flag = 0;
						}
						//System.out.println("\n\t index>>"+index+" previousIndex>>"+previousIndex+" flag"+flag);
						//System.out.println("\n\t inside run>>"+patternKeyHash+"suffixTag>>"+suffixTag);
					}
				}
				//System.out.println("\n\t patternKeyHash outside>>"+patternKeyHash);
				index++;
			}
		}
		//System.out.println("\n\t replaceMap>>"+replaceMap);
		// execute replacement of each relation entity
		Iterator<HashMap<String, String>> tier1Itr = replaceMap.descendingMap().values().iterator();
		while(tier1Itr.hasNext()){
			HashMap<String, String> tier1HashMap = tier1Itr.next();
			Iterator<Map.Entry<String, String>> tier2Itr = tier1HashMap.entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<String, String> tier2MapValue = tier2Itr.next();
				relationItem = 1;
				//System.out.println("\n\t curr key>>"+tier3MapValue.getKey());
				targetString = replaceTokenWithNE(targetString,tier2MapValue.getKey(),
						entityType.concat(tier2MapValue.getValue()),relationItem);
				//System.out.println("\n\t relation words>>>"+targetString);
			}
		}
		
		//System.out.println("\n\t enforcePatternMatch final >>>"+targetString);
		currentString = new StringBuilder(targetString);
		String checkString = currentString.toString().trim();
		if(!String.valueOf(checkString.charAt(checkString.length()-1)).matches("\\.|\\?|\\!")){
			System.err.println("ALERT! enforcePatternMatch() "+checkString);
		}
		return(currentString);
	}
	
	private StringBuilder enforceSecondaryPatternMatch(StringBuilder currentString,
			TreeMap<Integer, ArrayList<String>> parsedTokenPatternList, String entityType) {
		
		/**
		 * Replace other non relation based tokens; simple chemical or gene with respective NE's
		 */
		//System.out.println("\n\t currentString>>"+currentString.toString());
		String targetString = currentString.toString();
		//System.out.println("\n\t entityType>"+entityType);
		int relationItem = 0;
		String tokenString="";
		Iterator<Integer> sizeKeySet = parsedTokenPatternList.keySet().iterator();
		while(sizeKeySet.hasNext()){
			Iterator<String> tokenItr = parsedTokenPatternList.get(sizeKeySet.next()).iterator();
			while(tokenItr.hasNext()){
				tokenString = tokenItr.next();
				targetString = replaceTokenWithNE(targetString,tokenString,entityType,relationItem);
			}
		}
		//System.out.println("\n\t enforceSecondaryPatternMatch final >>>"+targetString);
		currentString = new StringBuilder(targetString);
		String checkString = currentString.toString().trim();
		if(!String.valueOf(checkString.charAt(checkString.length()-1)).matches("\\.|\\?|\\!")){
			System.err.println("ALERT! enforceSecondaryPatternMatch() "+checkString);
		}
		return(currentString);
	}

	/**
	 * Genia Tagger - Biomedical Domain
	 * GeniaTagger Based implementation
	 * @param abstractString
	 * @return 
	 */
	private List<ArrayList<String>> geniaTagger(String abstractString) {
		
		abstractString = abstractString.trim();
		String terminalIdentifier = String.valueOf(abstractString.charAt(abstractString.length()-1));
		//possibly minimize the number of terminal sentence identifiers
		if(terminalIdentifier.matches("[\\W&&[^\\s]]")){
			abstractString = abstractString.substring(0,abstractString.length()-1);
		}
		List<ArrayList<String>> complexSet = new ArrayList<>();
		ArrayList<String> wordList = new ArrayList<>();
		ArrayList<String> posTagList = new ArrayList<>();
		JeniaTagger.setModelsPath(systemProperties.getProperty("geniaModelFile"));
		Sentence baseForm = JeniaTagger.analyzeAll(abstractString, true);
		//System.out.println("\n abstractPosTagger>"+abstractString);
		Iterator<Token> tokenItr = baseForm.iterator();
		//System.out.print("\n");
		String previousToken = "";
		while(tokenItr.hasNext()){
			Token currentToken = tokenItr.next();
			
			//removing delimiters
			//System.out.print("\nbefore \t"+currentToken.baseForm+"\t"+currentToken.pos);
			currentToken.baseForm = currentToken.baseForm.replaceAll("\\(|\\)|\\{|\\}|\\[|\\]", "");
			if(currentToken.baseForm.equals("")){
				currentToken.pos="";
			}
			//System.out.print("\nafter::\t"+currentToken.baseForm+"\t"+currentToken.pos);
			
			/**
			//adding '.' period at the end of sentences
			if(Character.isUpperCase(currentToken.baseForm.charAt(0))
					&& (previousToken.endsWith("."))){
				int index = posTagList.size()-1;
				String prevTag = posTagList.get(index);
				posTagList.set(index, prevTag.concat("."));
				posTagList.add(currentToken.pos);
			}else{
				posTagList.add(currentToken.pos);
			}**/
			
			if(baseForm.get(baseForm.size()-1).baseForm.contentEquals(currentToken.baseForm)){
				//System.out.println("\t>>"+currentToken.pos);
				if(currentToken.pos.equals(".")){
					currentToken.pos = "CD";
					//System.out.println("\n\t>>"+terminalIdentifier+"\t>>"+currentToken.baseForm+"\t"+currentToken.pos);
				}
			}
			posTagList.add(currentToken.pos);
			wordList.add(currentToken.baseForm);
			previousToken = currentToken.baseForm;
			//System.out.print("\t"+currentToken.baseForm);
		}
		/**
		int index = posTagList.size()-1;
		String prevTag = posTagList.get(index);
		posTagList.set(index, prevTag.concat("."));**/
		
		if((terminalIdentifier.matches("[\\W&&[^\\s]]"))){
			posTagList.add(terminalIdentifier);
			wordList.add(terminalIdentifier);
		}
		//System.out.println("\n\t>>"+wordList+"\n\t>>"+posTagList);
		complexSet.add(wordList);
		complexSet.add(posTagList);
		return(complexSet);
	}
	
	private StringBuilder compositeTermTokeniser(String tokenString, String delimiter) {
		
		//System.out.println("\n\t compositeTermTokeniser() term::"+tokenString+"\t delimiter>>"+delimiter);
		StringTokenizer subTokenisation = new StringTokenizer(tokenString, delimiter);
		StringBuilder subTokens = new StringBuilder();
		int tokenSize =0;
		while(subTokenisation.hasMoreTokens()){
			String currToken = subTokenisation.nextToken();
			Matcher termMatcher = Pattern.compile("\\W+").matcher(currToken);
			if(termMatcher.find()){
				//System.out.println("\n\t>>>"+termMatcher.group());
				if(!currToken.matches("\\W+")){
					subTokens = compositeTermTokeniser(currToken, termMatcher.group(0));
				}else if(currToken.matches("\\W+")){
					subTokens.append(currToken.concat(" "));
					tokenSize++;
				}
			}else{
				//System.out.println("\n\t2>>>"+"\t>"+currToken);
				Matcher subTermMatcher = Pattern.compile("[\\W&&[^\\s]]*CHEMICAL(R\\d+T\\d+)*[\\W&&[^\\s]]*|"
							+ "[\\W&&[^\\s]]*GENEPRO(R\\d+T\\d+)*[\\W&&[^\\s]]*").matcher(currToken);
				if(subTermMatcher.find()){
					//System.out.println("\n\t start>>>"+subTermMatcher.start()+"\t>"+subTermMatcher.end());
					currToken = subTermMatcher.group(0).replaceAll(
							"[^(CHEMICAL(R\\d+T\\d+)*|GENEPRO(R\\d+T\\d+)*)]", "");
					//System.out.println("\n\t1>>>"+subTermMatcher.group(0)+"\t>"+currToken);
				}
				subTokens.append(currToken.concat(" "));
				tokenSize++;
			}
		}
		return(subTokens);
	}

	/**
	 * Tokenize the abstract sentence and frame new sentence based on POS-tags and NE  
	 * @param posTagList 
	 * @param wordList 
	 * @param taggedCurrentString
	 * @return 
	 * @throws IOException 
	 */
	private ArrayList<String> tokeniseAbstract(ArrayList<String> wordList, ArrayList<String> posTagList) throws IOException {
		
		//ArrayList<String> negationWordList = loadNegationWords();
		StringBuilder posNETaggedSentence = new StringBuilder();
		StringBuilder originalSentence = new StringBuilder();
		Matcher posTagMatcher;
		int index = 0;
		while(index < wordList.size()){
			//System.out.println("\n\t"+wordList.get(index)+"\t"+posTagList.get(index));
			posTagMatcher = Pattern.compile("CHEMICAL(R\\d+T\\d+)*|GENEPRO(R\\d+T\\d+)*",
					Pattern.CASE_INSENSITIVE).matcher(wordList.get(index));
			if(posTagMatcher.find()){
				//System.out.println("\n\t---> "+posTagMatcher.group(0)+" --> "+wordList.get(index));
				if(((posTagMatcher.start() == 0) && (posTagMatcher.end()-1 == wordList.get(index).length()-1))){
					/**Takes care of the words with just NE names only**/
					posNETaggedSentence.append(wordList.get(index).concat(" "));
					originalSentence.append(wordList.get(index).concat(" "));
					//System.out.println("\n\t 1");
				}else if((wordList.get(index).endsWith("s")) 
						&& ((wordList.get(index).length()-posTagMatcher.group(0).length())==1)){
					/**Takes care of the words with plural form of NE names only**/
					//System.out.println("\n\t Single visit with s");
					posNETaggedSentence.append(wordList.get(index).substring(0, wordList.get(index).length()-2).concat(" "));
					originalSentence.append(wordList.get(index).substring(0, wordList.get(index).length()-2).concat(" "));
				}else{
					/**Takes care of the words with NE names appended with identifiers like ,.|- etc and MWE**/
					StringBuilder subTokens = new StringBuilder();
					subTokens = compositeTermTokeniser(wordList.get(index),"-|/");
					//System.out.println("\n\t"+subTokens.toString()+"\t:"+"\t"+subTokens.toString().length());
					posTagMatcher = Pattern.compile("[\\W&&[^\\s]]*CHEMICAL(R\\d+T\\d+)*[\\W&&[^\\s]]*|"
							+ "[\\W&&[^\\s]]*GENEPRO(R\\d+T\\d+)*[\\W&&[^\\s]]*",Pattern.CASE_INSENSITIVE).
							matcher(subTokens.toString().trim());
					//doesn't take care of multiple matches problem
					if(posTagMatcher.matches()){
						/**Takes care of the words with just NE ending with only identifiers**/
						posNETaggedSentence.append(subTokens.toString().trim().concat(" "));
						originalSentence.append(subTokens.toString().trim().concat(" "));
						//System.out.println("\n\t in matches::"+subTokens.toString());
					}else{
						/**Takes care of the words with just NE appended with other words by a joining identifier**/
						List<ArrayList<String>> callGenia = geniaTagger(subTokens.toString().trim());
						ArrayList<String> geniaWord = new ArrayList<>();
						ArrayList<String> geniaPOSTag = new ArrayList<>();
						geniaWord = callGenia.get(0);
						geniaPOSTag = callGenia.get(1);
						int i =0;
						while(i < geniaWord.size()){
							posTagMatcher = Pattern.compile("CHEMICAL(R\\d+T\\d+)*|GENEPRO(R\\d+T\\d+)*",
									Pattern.CASE_INSENSITIVE).matcher(geniaWord.get(i));
							if(posTagMatcher.matches()){
								// second genia "chemical-gene" match
								posNETaggedSentence.append(geniaWord.get(i).concat(" "));
								originalSentence.append(geniaWord.get(i).concat(" "));
								//System.out.println("\n\t else matches::"+subTokens.toString());
							}else{
								// if not match to chemical-gene pattern,the check against
								// verb or negative verb to determine negative relations or other pos tags 
								//are directly appended
								if(geniaPOSTag.get(i).matches("VB\\w{1}")){
									// word match to verb
									posNETaggedSentence.append(
											geniaWord.get(i).concat("#".concat(geniaPOSTag.get(i).concat(" "))));
									originalSentence.append(
											geniaWord.get(i).concat("#".concat(geniaPOSTag.get(i).concat(" "))));
								}else{
									// non verb POS match
									posNETaggedSentence.append(geniaPOSTag.get(i).concat(" "));
									originalSentence.append(geniaWord.get(i).concat(" "));
								}
							}
							i++;
						}
					}
				}
			}else{
				int flag = 0;
				posTagMatcher = Pattern.compile("VB|VB.",Pattern.CASE_INSENSITIVE).matcher(posTagList.get(index));
				if(posTagMatcher.find()){
					/**
					Iterator<String> negationListItr = negationWordList.iterator();
					while(negationListItr.hasNext()){
						String negWord = negationListItr.next();
						if(index != wordList.size()-1){
							//next word as negative adverb, merge 2 as negative verb
							if(Pattern.compile("RB",Pattern.CASE_INSENSITIVE).matcher(posTagList.get(index+1)).find()
									&& Pattern.compile(negWord,Pattern.CASE_INSENSITIVE).matcher(wordList.get(index+1)).find()){
								posNETaggedSentence.append("NEG".concat(posTagList.get(index)).concat(" "));
								originalSentence.append(wordList.get(index).concat("n't "));
								flag = 1;
								index++;
								break;
							}
						}else if(Pattern.compile(negWord,Pattern.CASE_INSENSITIVE).matcher(wordList.get(index)).matches()){
							//last word as negative verb
							posNETaggedSentence.append("NEG".concat(posTagList.get(index)).concat(" "));
							originalSentence.append(wordList.get(index).concat("n't "));
							flag = 1;
						}
					}**/
					
					// if current word is a verb
					if(posTagList.get(index).matches("VB\\w{1}")){
						posNETaggedSentence.append(wordList.get(index).
								concat("#".concat(posTagList.get(index).concat(" "))));
						originalSentence.append(wordList.get(index).
								concat("#".concat(posTagList.get(index).concat(" "))));
						flag = 1;
					}
				}
				// if current word is not a verb
				if(flag == 0){
					posNETaggedSentence.append(posTagList.get(index).concat(" "));
					originalSentence.append(wordList.get(index).concat(" "));
				}
			}
			index++;
		}
		//System.out.println("\n\t tk>>>"+posNETaggedSentence.toString()+"\n\t>>"+originalSentence.toString());
		String checkString = posNETaggedSentence.toString().trim();
		if(!String.valueOf(checkString.charAt(checkString.length()-1)).matches("\\.|\\?|\\!")){
			System.err.println("ALERT! tokeniseAbstract() ~ posNETaggedSentence  "+checkString);
		}
		checkString = originalSentence.toString().trim();
		if(!String.valueOf(checkString.charAt(checkString.length()-1)).matches("\\.|\\?|\\!")){
			System.err.println("ALERT! tokeniseAbstract() ~ originalSentence "+checkString);
		}
		
		return(new ArrayList<>(Arrays.asList(posNETaggedSentence.toString().trim(),originalSentence.toString().trim())));
	}

	private boolean patternCheck(Set<String> alpha){
			
		Iterator<String> tempItr = alpha.iterator();
		boolean reactionFlag = true;
		while(tempItr.hasNext()){
			String pattern = tempItr.next().toString();
			if(!Pattern.compile("\\W",Pattern.CASE_INSENSITIVE)
					.matcher(pattern).matches()){
				reactionFlag = false;
				break;
			}
		}
		return(reactionFlag);
	}
	
	/**
	 * 
	 * @param paramToken
	 * @return
	 */
	public Set<String> populateSet(String paramToken){
		
		Matcher subMatcher;
		Set<String> characterSet =new HashSet<>();
		paramToken = paramToken.replaceAll("[A-Z&&[^R]]", "");
		for(String eachChar : paramToken.split("R")){
			subMatcher = Pattern.compile("\\W").matcher(eachChar);
			while(subMatcher.find()){
				characterSet.add(subMatcher.group(0));
				eachChar = eachChar.replace(subMatcher.group(0), "");
			}
			if(eachChar.length()!=0){
				characterSet.add(eachChar);
			}
		}
		return characterSet;
	}
	
	/**
	 * populate list ; current method doesn't hold for double digits
	 * @param paramToken
	 * @return
	 */
	public ArrayList<Character> populateList(String paramToken) {
			
		ArrayList<Character> tempCharArr = new ArrayList<>();
		for(char letter : paramToken.toCharArray()){
			tempCharArr.add(letter);
		}
		return(tempCharArr);
	}
	
	private ArrayList<Object> generateOffsetInfo(String currToken, String testPattern) {
		Matcher subMatcher;
		ArrayList<Object> ret = new ArrayList<>();
		ArrayList<Integer> startIndex = new ArrayList<>();
		ArrayList<Integer> endIndex = new ArrayList<>();
		ArrayList<String> patternIndex = new ArrayList<>();
		subMatcher = Pattern.compile(testPattern).matcher(currToken);
		while(subMatcher.find()){
			startIndex.add(subMatcher.start());
			endIndex.add(subMatcher.end());
			patternIndex.add(subMatcher.group(0).trim());
		}
		/**
		int i=0;
		while(i < startIndex.size()){
			System.out.println("\t"+startIndex.get(i)+"\t"+endIndex.get(i)+"\t"+patternIndex.get(i));
			i++;
		}*/
		ret.add(startIndex);
		ret.add(endIndex);
		ret.add(patternIndex);
		return(ret);
	}
	
	/**
	 * mergeDuplicates() merges the consecutive instances of the identical Entity words in a sentence
	 * @param taggedCurrentString
	 * @return 
	 */
	private ArrayList<String> mergeDuplicates(String taggedCurrentString, String originalCurrentString) {
		
		//taggedCurrentString = "DT JJ NN NN VBD RP CHEMICAL0 VBD CHEMICAL1,  CHEMICAL,  CHEMICAL,  CC CHEMICAL";
		//System.out.println("\n**BEGIN mergeDuplicates***");
		//System.out.println("\nSentence : "+taggedCurrentString+"\t\t"+originalCurrentString);
		long timebeg = System.currentTimeMillis();
		String terminalIdentifier = String.valueOf(taggedCurrentString.charAt(taggedCurrentString.length()-1)); 
		StringBuilder posAssembler = new StringBuilder();
		StringBuilder orgAssembler = new StringBuilder();
		ArrayList<Integer> posStartIndex = new ArrayList<>();
		ArrayList<Integer> posEndIndex = new ArrayList<>();
		ArrayList<String> posPatternIndex = new ArrayList<>();
		ArrayList<Object> posReturn = new ArrayList<>();
		ArrayList<Integer> orgStartIndex = new ArrayList<>();
		ArrayList<Integer> orgEndIndex = new ArrayList<>();
		ArrayList<Object> orgReturn = new ArrayList<>();
		String currentPOSToken = taggedCurrentString;
		String currentOriginalToken = originalCurrentString;
		String[] testPattern = {"[\\W&&[^\\s]]*CHEMICAL(R\\d+T\\d+)+[\\W&&[^\\s]]*",
				"[\\W&&[^\\s]]*GENEPRO(R\\d+T\\d+)+[\\W&&[^\\s]]*"};
		for(int patternCounter = 0;patternCounter < testPattern.length;patternCounter++){
			//for pos tagged sentence
			posReturn = generateOffsetInfo(currentPOSToken,testPattern[patternCounter]);
			posStartIndex = (ArrayList<Integer>) posReturn.get(0);
			posEndIndex = (ArrayList<Integer>) posReturn.get(1);
			posPatternIndex = (ArrayList<String>) posReturn.get(2);
			//System.out.println("\n\t>>"+posStartIndex+"\t"+posEndIndex+"\t"+posPatternIndex);
			//for original sentence
			orgReturn = generateOffsetInfo(currentOriginalToken,testPattern[patternCounter]);
			orgStartIndex = (ArrayList<Integer>) orgReturn.get(0);
			orgEndIndex = (ArrayList<Integer>) orgReturn.get(1);
			//System.out.println("\n\t>>"+orgStartIndex+"\t>>"+orgEndIndex);
			int index=0;
			String commonToken = null,originalToken=null;
			StringBuilder reframePOSTagSentence = new StringBuilder();
			StringBuilder reframeOriginalSentence = new StringBuilder();
			while(index < (posEndIndex.size()-1)){
				if(posEndIndex.get(index)+1 == posStartIndex.get(index+1)){
					Set<String> alpha = populateSet(posPatternIndex.get(index));
					Set<String> beta = populateSet(posPatternIndex.get(index+1));
					//System.out.println("\n\talpha>>"+alpha+"\tbeta>>"+beta);
					Set<String> alphaDecoy = new HashSet<>();
					if(alpha.size() >= beta.size()){
						alphaDecoy = Sets.symmetricDiff(alpha, beta);
						//System.out.println("\n\talphaDecoy>>"+alphaDecoy);
						if(!alphaDecoy.isEmpty()){
							if(patternCheck(alphaDecoy)){
								//delimiters only difference
								if(0 != posStartIndex.get(index)){
									reframePOSTagSentence.append(
											currentPOSToken.substring(0,posStartIndex.get(index)));
									reframeOriginalSentence.append(
											currentOriginalToken.substring(0,orgStartIndex.get(index)));
								}
								commonToken = currentPOSToken.substring(
										posStartIndex.get(index),posEndIndex.get(index));
								originalToken = currentOriginalToken.substring(
										orgStartIndex.get(index),orgEndIndex.get(index));
								//System.out.println("\n\t 1pos>>"+currentPOSToken.substring(0,posStartIndex.get(index)));
								//System.out.println("\n\t 1org>>"+currentOriginalToken.substring(0,orgStartIndex.get(index)));
							}else{
								//numbers difference so concatenate
								reframePOSTagSentence.append(
										currentPOSToken.substring(0,posEndIndex.get(index)+1));
								reframeOriginalSentence.append(
										currentOriginalToken.substring(0,orgEndIndex.get(index)+1));
								commonToken = currentPOSToken.substring(
										posStartIndex.get(index+1),posEndIndex.get(index+1));
								originalToken = currentOriginalToken.substring(
										orgStartIndex.get(index+1),orgEndIndex.get(index+1));
								//System.out.println("\n\t pos>>"+currentPOSToken.substring(0,posEndIndex.get(index)+1));
								//System.out.println("\n\t org>>"+currentOriginalToken.substring(0,orgEndIndex.get(index)+1));
							}
						}else{
							//both tags are same, so add just one
							if(0 != posStartIndex.get(index)){
								reframePOSTagSentence.append(
										currentPOSToken.substring(0,posStartIndex.get(index)));
								reframeOriginalSentence.append(
										currentOriginalToken.substring(0,orgStartIndex.get(index)));
							}
							commonToken = currentPOSToken.substring(
									posStartIndex.get(index+1),posEndIndex.get(index+1));
							originalToken = currentOriginalToken.substring(
									orgStartIndex.get(index+1),orgEndIndex.get(index+1));
							//System.out.println("\n\t 2pos>>"+currentPOSToken.substring(0,posStartIndex.get(index)));
							//System.out.println("\n\t 2org>>"+currentOriginalToken.substring(0,orgStartIndex.get(index)));
						}
					}else{
						alphaDecoy = Sets.symmetricDiff(beta, alpha);
						if(patternCheck(alphaDecoy)){
							//delimiters only difference
							if(0 != posStartIndex.get(index)){
								reframePOSTagSentence.append(
										currentPOSToken.substring(0,posStartIndex.get(index)));
								reframeOriginalSentence.append(
										currentOriginalToken.substring(0,orgStartIndex.get(index)));
							}
							commonToken = currentPOSToken.substring(
									posStartIndex.get(index+1),posEndIndex.get(index+1));
							originalToken = currentOriginalToken.substring(
									orgStartIndex.get(index+1),orgEndIndex.get(index+1));
						}else{
							//numbers difference so concatenate
							reframePOSTagSentence.append(
									currentPOSToken.substring(0,posEndIndex.get(index)+1));
							reframeOriginalSentence.append(
									currentOriginalToken.substring(0,orgEndIndex.get(index)+1));
							commonToken = currentPOSToken.substring(
									posStartIndex.get(index+1),posEndIndex.get(index+1));
							originalToken = currentOriginalToken.substring(
									orgStartIndex.get(index+1),orgEndIndex.get(index+1));
						}
					}
					
					if(posEndIndex.get(index+1) == currentPOSToken.length()){
						currentPOSToken = commonToken.concat("");
						currentOriginalToken = originalToken.concat("");
					}else{
						currentPOSToken = commonToken.concat(
								currentPOSToken.substring(posEndIndex.get(index+1),currentPOSToken.length()));
						currentOriginalToken = originalToken.concat(
								currentOriginalToken.substring(orgEndIndex.get(index+1),currentOriginalToken.length()));
					}
				}else{
					reframePOSTagSentence.append(currentPOSToken.substring(0,posEndIndex.get(index)));
					reframeOriginalSentence.append(currentOriginalToken.substring(0,orgEndIndex.get(index)));
					commonToken = currentPOSToken.substring(posEndIndex.get(index),currentPOSToken.length());
					currentPOSToken = commonToken;
					originalToken = currentOriginalToken.substring(
							orgEndIndex.get(index),currentOriginalToken.length());
					currentOriginalToken = originalToken;
				}
				index = 0;
				posReturn = generateOffsetInfo(currentPOSToken,testPattern[patternCounter]);
				posStartIndex = (ArrayList<Integer>) posReturn.get(0);
				posEndIndex = (ArrayList<Integer>) posReturn.get(1);
				posPatternIndex = (ArrayList<String>) posReturn.get(2);
				orgReturn = generateOffsetInfo(currentOriginalToken,testPattern[patternCounter]);
				orgStartIndex = (ArrayList<Integer>) orgReturn.get(0);
				orgEndIndex = (ArrayList<Integer>) orgReturn.get(1);
			}
			
			if(!currentPOSToken.endsWith(terminalIdentifier)){
				currentPOSToken = currentPOSToken.concat(" "+terminalIdentifier);
				currentOriginalToken = currentOriginalToken.concat(" "+terminalIdentifier);
			}
			reframePOSTagSentence.append(currentPOSToken);
			reframeOriginalSentence.append(currentOriginalToken);
			currentPOSToken = reframePOSTagSentence.toString().trim();
			currentOriginalToken = reframeOriginalSentence.toString().trim();
		}
		//System.out.println("\n merged Sentence : "+currentToken);
		posAssembler.append(currentPOSToken.toUpperCase());
		orgAssembler.append(currentOriginalToken);
		//System.out.println("\n**Tme Elapsed in duplicates***"+(System.currentTimeMillis()-timebeg)/1000);
		//System.out.println("\n\t**"+posAssembler.toString()+"\n\t>>"+orgAssembler.toString());
		
		String checkString = posAssembler.toString();
		if(!String.valueOf(checkString.charAt(checkString.length()-1)).matches("\\.|\\?|\\!")){
			System.err.println("ALERT! mergeDuplicates() ~ posAssembler  "+checkString);
		}
		checkString = orgAssembler.toString();
		if(!String.valueOf(checkString.charAt(checkString.length()-1)).matches("\\.|\\?|\\!")){
			System.err.println("ALERT! mergeDuplicates() ~ orgAssembler  "+checkString);
		}
		
		return(new ArrayList<>(Arrays.asList(posAssembler.toString(),orgAssembler.toString())));
	}
	
	private String handleBrackets(String abstractString) {

		CorpusDictionary corpusInstance = new CorpusDictionary();
		Matcher bracketMatcher = Pattern.compile("\\(|\\)|\\{|\\}|\\[|\\]").matcher(abstractString);
		String currChar="";
		while(bracketMatcher.find()){
			int flag=0;
			//System.out.println("\n\tmatch group>>"+bracketMatcher.group(0));			
			if(bracketMatcher.group(0).matches("\\(|\\{|\\[")){
				if(bracketMatcher.start() != 0){
					currChar = String.valueOf(abstractString.charAt(bracketMatcher.start()-1));
					flag = 1;
				}
				//System.out.println("\n (>>"+currChar);
			}else if(bracketMatcher.group(0).matches("\\)|\\}|\\]")){
				if(bracketMatcher.end() != abstractString.length()){
					currChar = String.valueOf(abstractString.charAt(bracketMatcher.end()));
					flag = 1;
				}
				//System.out.println("\n )>>"+currChar);
			}
			if(flag == 1){
				if(currChar.matches("\\s+")){
					// correct arrangement
					abstractString = abstractString.replaceFirst(
							corpusInstance.patternBuilder(bracketMatcher.group(0)), "");
				}else{
					// adjust string
					abstractString = abstractString.replaceFirst(
							corpusInstance.patternBuilder(bracketMatcher.group(0)), " ");
				}
			}else{
				// default arrangement
				abstractString = abstractString.replaceFirst(
						corpusInstance.patternBuilder(bracketMatcher.group(0)), "");
			}
			abstractString = abstractString.trim();
			//System.out.println("\n\t updated bracket>>"+abstractString);
			bracketMatcher = Pattern.compile("\\(|\\)|\\{|\\}|\\[|\\]").matcher(abstractString);
		}
		return(abstractString.trim());
	}

	/**
	 * This method calls the jar-library based methods for invoking POS tagging for each input sentences.
	 * @param string
	 * @return 
	 * @throws IOException 
	 */
	private ArrayList<String> abstractPosTagger(String abstractString) throws IOException {
		
		List<ArrayList<String>> complexHashSet = new ArrayList<>();
		ArrayList<String> wordList = new ArrayList<>();
		ArrayList<String> posTagList = new ArrayList<>();
		//System.out.println("\n\t before brackets >>"+abstractString);
		abstractString = handleBrackets(abstractString);
		complexHashSet = geniaTagger(abstractString);
		wordList = complexHashSet.get(0);
		posTagList = complexHashSet.get(1);
		
		/**
		if((!wordList.get(wordList.size()-1).contains("GENE"))
				||(!wordList.get(wordList.size()-1).contains("CHEMICAL"))){
			int index = posTagList.size()-1;
			String currTag = posTagList.get(index);
			posTagList.set(index,currTag.concat("."));
		}**/
		
		
		//tokenize the sentence and replace EN with their POS tags
		ArrayList<String> taggedCurrentString = tokeniseAbstract(wordList,posTagList);
		
		 //Sentence SIZE TEST
		if(taggedCurrentString.get(0).split("\\s").length != taggedCurrentString.get(1).split("\\s").length){
			System.err.println("\n\t unequal instances");
			System.err.println("\n\t tokeniseAbstract :- "+taggedCurrentString.get(0)+"\n\t>>"+taggedCurrentString.get(1));
		}
		
		//taggedCurrentString = mergeDuplicates(taggedCurrentString.get(0),taggedCurrentString.get(1));
		//merge the consecutive duplicate references to any NE in a sentence
		taggedCurrentString = mergeDuplicates(taggedCurrentString.get(0),taggedCurrentString.get(1));
	
		//Sentence SIZE TEST
		if(taggedCurrentString.get(0).split("\\s").length != taggedCurrentString.get(1).split("\\s").length){
			System.err.println("\n\t unequal instances");
			System.err.println("\n\t mergeDuplicates :- "+taggedCurrentString.get(0)+"\n\t>>"+taggedCurrentString.get(1));
		}
		//System.out.println("\n\tabstractPosTagger :- "+taggedCurrentString);
		//return(taggedString.toUpperCase());
		return(taggedCurrentString);
	}
	
	private boolean checkWordForLowerCase(String charString) {
		
		//screen the characters for presence of Lower case and rule it out as part of another sentence
		int charCount=0;
		if(charString.length() > 1){
			for(char character : charString.toCharArray()){
				if(Character.isLowerCase(character)){
					charCount++;
				}
			}
		}
		if(charCount == charString.length()){
			return true;
		}else{
			return false;
		}
	}	
	
	private String recursiveTerminalSymbolCheck(String sentence, int sentenceSize) {
		
		// iteratively remove non period operators from the rare of the sentence
		//System.out.println("\n\t>>"+String.valueOf(sentence.charAt(sentenceSize))+"\tindex>>"+sentenceSize);
		if(String.valueOf(sentence.charAt(sentenceSize)).matches("\\.|\\?|\\!")){
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

	/**
	 * 
	 * @param cacheSentence 
	 * @param string
	 * @return 
	 */
	private String checkDocumentStructure(String documentString) {
		
		Matcher docStructureMatcher;
		TreeSet<String> structureWords = new TreeSet<>(Arrays.asList("BACKGROUND:","METHODS:",
				"RESULTS:","CONCLUSIONS:","STUDY DESIGN AND METHODS:","FINDINGS:","INTERPRETATION:",
				"METHODS AND RESULTS:","OBJECTIVES:","CASE REPORT:","DISCUSSION:","CASE:",
				"DESIGN/METHODS:","BACKGROUND AND OBJECTIVES:","RELEVANCE TO CLINICAL PRACTICE",
				"SEARCH STRATEGY:","SELECTION CRITERIA:","DATA COLLECTION AND ANALYSIS:",
				"MAIN RESULTS:","AUTHORS' CONCLUSIONS:","PURPOSE:","RATIONALE:","OBJECTIVE:",
				"CONCLUSION:","MATERIALS AND METHODS:","INTRODUCTION:","AIM OF THE STUDY:","AIMS:",
				"ETHNOPHARMACOLOGICAL RELEVANCE:","SEARCH STRATEGY:","BACKGROUND AND AIMS:","AIM:",
				"BACKGROUND & AIMS:","STUDY DESIGN:","DESIGN:","REVIEW SUMMARY:","UNLABELLED:",
				"ABSTRACT CONTEXT:","PARTICIPANTS:","METHODOLOGY:"));
		TreeMap<Integer, TreeSet<String>> docStructureWords = new TreeMap<>();
		for(String structureToken : structureWords){
			TreeSet<String> tempSet = new TreeSet<>();
			int tokenSize = structureToken.length();
			if(docStructureWords.containsKey(tokenSize)){
				tempSet = docStructureWords.get(tokenSize);
			}
			tempSet.add(structureToken);
			docStructureWords.put(tokenSize, tempSet);
		}
		
		documentString = documentString.trim();
		for(TreeSet<String> tempSet : docStructureWords.descendingMap().values()){
			for(String structureToken : tempSet){
				docStructureMatcher = Pattern.compile(structureToken,Pattern.CASE_INSENSITIVE).matcher(documentString);
				while(docStructureMatcher.find()){
					if(docStructureMatcher.group(0).matches(structureToken)){
						// complete match
						if(docStructureMatcher.start() == 0){
							documentString = documentString.replaceAll(structureToken, "").trim();
						}else{
							documentString = documentString.replaceAll(structureToken, ". ");
						}
					}else{
						// incomplete match
					}
				}
			}
		}
		return documentString.trim();
	}

	/**
	 * Put the current thread to sleep 
	 */
	private void haltThreadProcess() {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public ArrayList<ArrayList<ArrayList<String>>> call() throws Exception {
		//System.out.println("\n\t inside call");
		ArrayList<ArrayList<ArrayList<String>>> taggedSentences = new ArrayList<>();
		Iterator<String> abstractIterator = this.bundle.iterator();
		TreeMap<Integer, ArrayList<String>> parsedChemicalPatternList = 
				this.chemicalPatternList;
		TreeMap<Integer, ArrayList<String>> parsedGenePatternList = 
				this.genePatternList;
		//System.out.println("\n\t parsedGenePatternList>>"+parsedGenePatternList);
		//InputStream sentenceDetectorModel = new FileInputStream("/home/neha/Disk_R/Bio_NLP/NLP_Parsers/OpenNLP_Models/en-sent.bin");
		while (abstractIterator.hasNext()) {
			InputStream sentenceDetectorModel = new FileInputStream(systemProperties.
					getProperty("openNLPSentenceSplitterModel"));
			SentenceModel loadedSentenceModel = new SentenceModel(sentenceDetectorModel);
			SentenceDetectorME sentenceDetector = new SentenceDetectorME(loadedSentenceModel);
			ArrayList<String> decoySentences = new ArrayList<>(Arrays.asList(sentenceDetector.
					sentDetect(abstractIterator.next())));
			sentenceDetectorModel.close();
			//System.out.println("\n\tbefore>>"+decoySentences);
			ArrayList<ArrayList<String>> subTaggedSentences = new ArrayList<>();
			ArrayList<String> sentences = new ArrayList<>();
			StringBuilder cacheSentence = new StringBuilder();
			int lastIndex = -1;
			//check if there are more than 2 sentences in the input sentences
			if(decoySentences.size() > 0){
				for(int index=0;index<decoySentences.size();index++){
					int terminalFlag = 0;
					//System.out.println("\n\t test>>"+ decoySentences.get(index));
					//distinction for sentence kept as first letter should be upper case , a number 
					//or first word should appear in toggle case like gene name preceded by period symbol
					if(checkWordForLowerCase(decoySentences.get(index).trim().split(" ")[0])){
						// check for the case of first character and word
						if(!sentences.isEmpty()){
							if(cacheSentence.length() == 0){
								lastIndex = sentences.size()-1;
								cacheSentence.append(sentences.get(lastIndex).concat(" "));
								sentences.remove(lastIndex);
							}
						}
					}
					String reformattedString = checkDocumentStructure(decoySentences.get(index));
					if((!String.valueOf
							(reformattedString.charAt(reformattedString.length()-1))
							.matches("\\.|\\?|\\!"))){
						// check if sentence ends with period or similar operators
						terminalFlag = 1;
						cacheSentence.append(reformattedString.concat(" "));
					}
					if(terminalFlag == 0){
						if(cacheSentence.length() == 0){
							sentences.add(reformattedString.trim());
						}else{
							cacheSentence.append(reformattedString.concat(" "));
							sentences.add(cacheSentence.toString().trim());
							cacheSentence = new StringBuilder();
						}
					}
				}
				if(cacheSentence.length() != 0){
					System.err.println("\n\t Illegal sentence terminator>>"+cacheSentence.toString().trim());
					String currentSentence = cacheSentence.toString().trim();
					int sentenceSize = currentSentence.length()-1;
					// Always ensure sentences with terminal symbol proceed to next phase
					currentSentence = recursiveTerminalSymbolCheck(currentSentence,sentenceSize);
					if(null != currentSentence){
						sentences.add(currentSentence);
					}else{
						sentences.add(cacheSentence.toString().trim().concat("."));
					}
				}
			}
			for(String sentence : sentences){
				//System.out.println(sentence);
				StringBuilder currentString = new StringBuilder(sentence);
				//NE Swap
				currentString = enforcePatternMatch(
						currentString, parsedGenePatternList,"GENEPRO");
				currentString = enforcePatternMatch(
						currentString, parsedChemicalPatternList,"CHEMICAL");
				currentString = enforceSecondaryPatternMatch(
						currentString, parsedGenePatternList,"GENEPRO");
				currentString = enforceSecondaryPatternMatch(
						currentString, parsedChemicalPatternList,"CHEMICAL");
				// Add relation type
				//System.out.println("\nenforcePatternMatch :- "+currentString.toString());
				
				//POS tagging
				subTaggedSentences.add(abstractPosTagger(currentString.toString()));
			}
			taggedSentences.add(subTaggedSentences);
			
			/**
			StringBuilder tempBuilder = new StringBuilder();
			for(String sentence : subTaggedSentences){
				tempBuilder.append(sentence.concat(" "));
			}
			taggedSentences.add(tempBuilder.toString().trim());
			**/
		}
		haltThreadProcess();
		return(taggedSentences);
	}

	/**
	 * Identify the MWE/SW from corpus abstracts and replace them with corresponding NE's
	 * @param normaliseInstance
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void abstractEntitySwap(NormaliseAbstracts normaliseInstance) 
			throws IOException, ParserConfigurationException, TransformerException, 
			InterruptedException, ExecutionException {
		
		/**
		 * Compare the patterns against the abstract text 
		 */
		TransformerFactory transformMethod = TransformerFactory.newInstance();
		Transformer transformer = transformMethod.newTransformer();
		DocumentBuilderFactory createXmlDocFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder createXmlDocBuilder = createXmlDocFactory.newDocumentBuilder();
		Document createPOSTagXmlDoc = createXmlDocBuilder.newDocument();
		Document createOrgTextXmlDoc = createXmlDocBuilder.newDocument();
		Element rootPOSTagElement = createPOSTagXmlDoc.createElement("taggedDocument");
		Element rootOrgTextElement = createOrgTextXmlDoc.createElement("taggedDocument");
		createPOSTagXmlDoc.appendChild(rootPOSTagElement);
		createOrgTextXmlDoc.appendChild(rootOrgTextElement);
		
		Iterator<Long> documentIdItr = 
				normaliseInstance.abstractCollection.keySet().iterator();
		
		//Random poolSizeGenerator = new Random();
		Integer threadPoolSize;
		if(normaliseInstance.abstractCollection.keySet().size() > 1){
			threadPoolSize = (normaliseInstance.abstractCollection.keySet().size()/2);
		}else{
			threadPoolSize = 1;
		}
		/**
		while(0 == threadPoolSize ){
			threadPoolSize = poolSizeGenerator.nextInt(normaliseInstance.abstractCollection.keySet().size());
		}**/
		
		System.out.println("\n"+threadPoolSize);
		long beginSysTime = System.currentTimeMillis();
		ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(threadPoolSize);
		int i=0;
		boolean relationExists = false;
		while(documentIdItr.hasNext()){
			i++;
			relationExists = false;
			Long docId = documentIdItr.next();
			//System.out.println("\t*"+docId);
			/**
			if(docId == 16789740){
				System.out.println("\t******************"+i);
				//System.exit(0);
			//}**/
			
			/**
			 * Gather chemical and gene pattern list
			 */
			TreeMap<Integer, ArrayList<String>> parsedChemicalPatternList = 
					new TreeMap<>();
			if(normaliseInstance.chemicalEntities.containsKey(docId)){
				parsedChemicalPatternList = normaliseInstance.chemicalEntities.get(docId);
			}
			//sort the list for ordered pattern matching
			//parsedChemicalPatternList = sortHashMap(tempParsedPatternList);
			TreeMap<Integer, ArrayList<String>> parsedGenePatternList = 
					new TreeMap<>();
			if(normaliseInstance.geneEntities.containsKey(docId)){
				parsedGenePatternList = normaliseInstance.geneEntities.get(docId);
			}
			//sort the list for ordered pattern matching
			//parsedGenePatternList = sortHashMap(tempParsedPatternList);
			ArrayList<LinkedHashSet<String>> relationList = new ArrayList<>();
			if(normaliseInstance.relationCollection.containsKey(docId)){
				TreeMap<Integer, ArrayList<String>> relationTree = 
						normaliseInstance.relationCollection.get(docId);
				for(ArrayList<String> tempList : relationTree.values()){
					relationList.add(new LinkedHashSet<>(tempList));
					relationExists = true;
				}
			}
			ArrayList<String> abstractBundle = new ArrayList<>();
			if(normaliseInstance.abstractCollection.containsKey(docId)){
				TreeMap<Integer, ArrayList<String>> tempMap = 
						normaliseInstance.abstractCollection.get(docId);
				for(ArrayList<String> tempList : tempMap.values()){
					abstractBundle.addAll(tempList);
				}
			}
			if(relationExists){
				Element childPOSTagNode = createPOSTagXmlDoc.createElement("document");
				Element childOrgTextNode = createOrgTextXmlDoc.createElement("document");
				rootPOSTagElement.appendChild(childPOSTagNode);
				rootOrgTextElement.appendChild(childOrgTextNode);
				Attr idPOSTagAttr = createPOSTagXmlDoc.createAttribute("docId");
				idPOSTagAttr.setValue(docId.toString());
				childPOSTagNode.setAttributeNode(idPOSTagAttr);
				Attr idOrgTextAttr = createOrgTextXmlDoc.createAttribute("docId");
				idOrgTextAttr.setValue(docId.toString());
				childOrgTextNode.setAttributeNode(idOrgTextAttr);
				
				NormaliseAbstracts workerThread = new NormaliseAbstracts(abstractBundle, 
						relationList, parsedChemicalPatternList, parsedGenePatternList, 
						systemProperties);
				Future<ArrayList<ArrayList<ArrayList<String>>>> taskCollector = 
						threadPoolExecutor.submit(workerThread);
				Iterator<ArrayList<ArrayList<String>>> tier1Itr = 
						taskCollector.get().iterator();
				while(tier1Itr.hasNext()){
					//int ind=0;
					Iterator<ArrayList<String>> tier2Itr = tier1Itr.next().iterator();
					while(tier2Itr.hasNext()){
						ArrayList<String> sentenceBundle = tier2Itr.next();
						/**
						if((docId==8919272)&&(ind==0)){
							System.out.println("\n\t1.>>>"+posTaggedSentenceBundle.get(0).split("\\s").length+">>"+posTaggedSentenceBundle.get(0));
							System.out.println("\n\t2.>>>"+posTaggedSentenceBundle.get(1).split("\\s").length+">>"+posTaggedSentenceBundle.get(1));
						}**/
						Element abstractPOSTagNode = createPOSTagXmlDoc.createElement("abstract");
						abstractPOSTagNode.appendChild(createPOSTagXmlDoc.createTextNode(sentenceBundle.get(0)));
						childPOSTagNode.appendChild(abstractPOSTagNode);
						Element abstractOrgTextNode = createOrgTextXmlDoc.createElement("abstract");
						abstractOrgTextNode.appendChild(createOrgTextXmlDoc.createTextNode(sentenceBundle.get(1)));
						childOrgTextNode.appendChild(abstractOrgTextNode);
						//ind++;
					}
				}
			//}
			}
		}
		threadPoolExecutor.shutdown();
		System.out.println("\n Total Execution Time:-"+(System.currentTimeMillis()-beginSysTime)/1000);
		DOMSource domPOSTagCreater = new DOMSource(createPOSTagXmlDoc);
		StreamResult createXmlStream = new StreamResult(new File(systemProperties.getProperty("processedPOSTaggedXmlFile")));
		transformer.transform(domPOSTagCreater, createXmlStream);
		DOMSource domOrgTextCreater = new DOMSource(createOrgTextXmlDoc);
		createXmlStream = new StreamResult(new File(systemProperties.getProperty("processedOriginalXMLFile")));
		transformer.transform(domOrgTextCreater, createXmlStream);
	}

}





