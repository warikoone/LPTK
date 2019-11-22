package com.prj.bundle.preprocessing;

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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DataStreaming {
	
	private Properties systemProperties;
	
	// Constructors
	public DataStreaming() throws IOException {
		this.systemProperties = new Properties();
		InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
	}

	
	private ArrayList<String> loadCorpusSpecifics(String fileName) throws IOException {

		ArrayList<String> decoyList = new ArrayList<>();
		FileReader fileRS = new FileReader(fileName);
		BufferedReader buffRS = new BufferedReader(fileRS);
		String readLine = buffRS.readLine();
		while(null != readLine){
			decoyList.add(readLine);
			readLine = buffRS.readLine();
		}
		buffRS.close();
		return(decoyList);
	}

	/**
	 * 
	 * @param resultHolder
	 * @return 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private Hashtable<String, LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>>> 
		processDataStream(Hashtable<String, 
				LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>>> resultHolder) 
						throws InterruptedException, ExecutionException, IOException{
		
		LinkedHashMap<String, ArrayList<String>> corpusMap = new LinkedHashMap<>();
		String fileName = systemProperties.getProperty("entitiesTrainingFile");
		ArrayList<String> corpusList = loadCorpusSpecifics(fileName);
		corpusMap.put("Entities", corpusList);
		fileName = systemProperties.getProperty("relationTrainingFile");
		corpusList = loadCorpusSpecifics(fileName);
		corpusMap.put("Relation", corpusList);
		fileName = systemProperties.getProperty("abstractTrainingFile");
		corpusList = loadCorpusSpecifics(fileName);
		corpusMap.put("Abstract", corpusList);
		
		Integer threadPoolSize;
		if(corpusList.size() > 1){
			threadPoolSize = (corpusList.size()/2);
		}else{
			threadPoolSize = 1;
		}
		System.out.println("\n"+threadPoolSize);
		ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(threadPoolSize);
		LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>> entityType;
		resultHolder.put("Chemical",entityType = new LinkedHashMap<>());
		resultHolder.put("Gene",entityType = new LinkedHashMap<>());
		resultHolder.put("Abstract",entityType = new LinkedHashMap<>());
		resultHolder.put("Relation",entityType = new LinkedHashMap<>());
		long beginSysTime = System.currentTimeMillis();
		
		for(int index=0; index < corpusList.size(); index++){
			String currDocId = corpusList.get(index).split("\t")[0];
			//System.out.println("\n\t index>>"+index);
			//if(currDocId.equals("23415902")){
			CorpusDictionary workerThread = new CorpusDictionary(currDocId, corpusMap, resultHolder);
			//collect entities of various kinds and abstracts
			Future<Hashtable<String, 
			LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>>>> taskCollector =
					threadPoolExecutor.submit(workerThread);
			resultHolder = taskCollector.get();
			//}
		}
		threadPoolExecutor.shutdown();
		System.out.println("\n Total Execution Time:-"+(System.currentTimeMillis()-beginSysTime)/1000);
		return(resultHolder);
	}

	public static void main(String[] args) {

		Hashtable<String, LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>>> 
		resultHolder = new Hashtable<>();
		try {
			DataStreaming streamInstance = new DataStreaming();
			NormaliseAbstracts normaliseInstance = new NormaliseAbstracts();
			resultHolder = streamInstance.processDataStream(resultHolder);		
			System.out.println("\n\trelSize>>>"+resultHolder.get("Relation").size());
			//System.out.println("\n\t rel>>>"+resultHolder.get("Relation"));
			//System.out.println("\n\t gene>>>"+resultHolder.get("Gene"));
			//System.out.println("\n\t chemical>>>"+resultHolder.get("Chemical"));
			normaliseInstance.addCorpusResource(resultHolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			// TODO: handle finally clause
			System.out.println("Phase I - Preprocessing Analysis Completed");
		}
	}
	
}
