/**
 * 
 */
package com.prj.bundle.model;

import java.util.LinkedHashMap;

/**
 * @author neha
 *
 */
public class DensityFeatureMap {

	private LinkedHashMap<String, Integer> verbChemicalGeneAssociation;
	private LinkedHashMap<String, Integer> verbChemicalAssociation;
	private LinkedHashMap<String, Integer> verbGeneAssociation;
	private LinkedHashMap<String, Integer> chemicalTermFrequency;
	private LinkedHashMap<String, Integer> geneTermFrequency;
	private LinkedHashMap<String, Integer> relationTermFrequency;
	
	/**
	 * Constructor
	 */
	public DensityFeatureMap() {
		
	}
	
	public DensityFeatureMap(LinkedHashMap<String, Integer> verbChemicalGeneAssociation,
			LinkedHashMap<String, Integer> verbChemicalAssociation,
			LinkedHashMap<String, Integer> verbGeneAssociation, LinkedHashMap<String, Integer> chemicalTermFrequency,
			LinkedHashMap<String, Integer> geneTermFrequency, LinkedHashMap<String, Integer> relationTermFrequency) {
		
		setVerbChemicalGeneAssociation(verbChemicalGeneAssociation);
		setVerbChemicalAssociation(verbChemicalAssociation);
		setVerbGeneAssociation(verbGeneAssociation);
		setChemicalTermFrequency(chemicalTermFrequency);
		setGeneTermFrequency(geneTermFrequency);
		setRelationTermFrequency(relationTermFrequency);
	}

	public LinkedHashMap<String, Integer> getVerbChemicalGeneAssociation() {
		return verbChemicalGeneAssociation;
	}

	public void setVerbChemicalGeneAssociation(LinkedHashMap<String, Integer> verbChemicalGeneAssociation) {
		this.verbChemicalGeneAssociation = verbChemicalGeneAssociation;
	}

	public LinkedHashMap<String, Integer> getVerbChemicalAssociation() {
		return verbChemicalAssociation;
	}

	public void setVerbChemicalAssociation(LinkedHashMap<String, Integer> verbChemicalAssociation) {
		this.verbChemicalAssociation = verbChemicalAssociation;
	}

	public LinkedHashMap<String, Integer> getVerbGeneAssociation() {
		return verbGeneAssociation;
	}

	public void setVerbGeneAssociation(LinkedHashMap<String, Integer> verbGeneAssociation) {
		this.verbGeneAssociation = verbGeneAssociation;
	}

	public LinkedHashMap<String, Integer> getChemicalTermFrequency() {
		return chemicalTermFrequency;
	}

	public void setChemicalTermFrequency(LinkedHashMap<String, Integer> chemicalTermFrequency) {
		this.chemicalTermFrequency = chemicalTermFrequency;
	}

	public LinkedHashMap<String, Integer> getGeneTermFrequency() {
		return geneTermFrequency;
	}

	public void setGeneTermFrequency(LinkedHashMap<String, Integer> geneTermFrequency) {
		this.geneTermFrequency = geneTermFrequency;
	}

	public LinkedHashMap<String, Integer> getRelationTermFrequency() {
		return relationTermFrequency;
	}

	public void setRelationTermFrequency(LinkedHashMap<String, Integer> relationTermFrequency) {
		this.relationTermFrequency = relationTermFrequency;
	}
	
}
