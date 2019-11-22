/**
 * 
 */
package com.prj.bundle.preprocessing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Vector;

/**
 * @author neha
 *
 */
public class PopulateResources {

	protected LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>> chemicalEntities;
	protected LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>> geneEntities;
	protected LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>> abstractCollection;
	protected LinkedHashMap<Long,TreeMap<Integer, ArrayList<String>>> relationCollection;
	
	/**
	 * Generate getter/setter methods for initializing
	 * @return
	 */
	
	public LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>> getChemicalEntities() {
		return chemicalEntities;
	}
	public void setChemicalEntities(LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>> chemicalEntities) {
		this.chemicalEntities = chemicalEntities;
	}
	public LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>> getGeneEntities() {
		return geneEntities;
	}
	public void setGeneEntities(LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>> geneEntities) {
		this.geneEntities = geneEntities;
	}
	public LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>> getAbstractCollection() {
		return abstractCollection;
	}
	public void setAbstractCollection(LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>> abstractCollection) {
		this.abstractCollection = abstractCollection;
	}
	public LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>> getRelationCollection() {
		return relationCollection;
	}
	public void setRelationCollection(LinkedHashMap<Long, TreeMap<Integer, ArrayList<String>>> relationCollection) {
		this.relationCollection = relationCollection;
	}
}
