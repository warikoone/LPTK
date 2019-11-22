/**
 * 
 */
package com.prj.bundle.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author neha
 *
 */
public class ContextFrameFeatureMap {

	private HashMap<Integer,HashMap<Integer,HashMap<String, Set<String>>>> chemicalFrameMap;
	private HashMap<Integer,HashMap<Integer,HashMap<String, Set<String>>>> geneFrameMap;
	private HashMap<Integer,HashMap<Integer,HashMap<String, Set<String>>>> relationFrameMap;
	private LinkedHashMap<Integer,LinkedHashMap<String, Set<String>>> contextFrameMap;
	/**
	 * Constructors 
	 */
	
	public ContextFrameFeatureMap() {
		
	}
	
	public ContextFrameFeatureMap(HashMap<Integer,HashMap<Integer, HashMap<String, Set<String>>>> chemicalFrameMap,
			HashMap<Integer,HashMap<Integer, HashMap<String, Set<String>>>> geneFrameMap,
			HashMap<Integer,HashMap<Integer, HashMap<String, Set<String>>>> relationFrameMap,
			LinkedHashMap<Integer,LinkedHashMap<String, Set<String>>> contextFrameMap) {
		setChemicalFrameMap(chemicalFrameMap);
		setGeneFrameMap(geneFrameMap);
		setRelationFrameMap(relationFrameMap);
		setContextFrameMap(contextFrameMap);
	}

	public HashMap<Integer, HashMap<Integer, HashMap<String, Set<String>>>> getChemicalFrameMap() {
		return chemicalFrameMap;
	}

	public void setChemicalFrameMap(HashMap<Integer, HashMap<Integer, HashMap<String, Set<String>>>> chemicalFrameMap) {
		this.chemicalFrameMap = chemicalFrameMap;
	}

	public HashMap<Integer, HashMap<Integer, HashMap<String, Set<String>>>> getGeneFrameMap() {
		return geneFrameMap;
	}

	public void setGeneFrameMap(HashMap<Integer, HashMap<Integer, HashMap<String, Set<String>>>> geneFrameMap) {
		this.geneFrameMap = geneFrameMap;
	}

	public HashMap<Integer, HashMap<Integer, HashMap<String, Set<String>>>> getRelationFrameMap() {
		return relationFrameMap;
	}

	public void setRelationFrameMap(HashMap<Integer, HashMap<Integer, HashMap<String, Set<String>>>> relationFrameMap) {
		this.relationFrameMap = relationFrameMap;
	}

	public LinkedHashMap<Integer, LinkedHashMap<String, Set<String>>> getContextFrameMap() {
		return contextFrameMap;
	}

	public void setContextFrameMap(LinkedHashMap<Integer, LinkedHashMap<String, Set<String>>> contextFrameMap) {
		this.contextFrameMap = contextFrameMap;
	}
	

}
