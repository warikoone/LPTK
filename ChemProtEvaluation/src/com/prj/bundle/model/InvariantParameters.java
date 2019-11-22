/**
 * 
 */
package com.prj.bundle.model;

import java.util.HashMap;

/**
 * @author neha
 *
 */
public class InvariantParameters {
	
	private Integer relevantFrameIndex;
	private double invariantPolynomialValue;
	private HashMap<String, String> contextualFrames;

	/**
	 * Constructors 
	 */
	public InvariantParameters() {
	}

	public InvariantParameters( Integer relevantFrameIndex, double invariantPolynomialValue,
			HashMap<String, String> contextualFrames) {
		
		setRelevantFrameIndex(relevantFrameIndex);
		setInvariantPolynomialValue(invariantPolynomialValue);
		setContextualFrames(contextualFrames);
	}

	public Integer getRelevantFrameIndex() {
		return relevantFrameIndex;
	}

	public void setRelevantFrameIndex(Integer relevantFrameIndex) {
		this.relevantFrameIndex = relevantFrameIndex;
	}

	public double getInvariantPolynomialValue() {
		return invariantPolynomialValue;
	}

	public void setInvariantPolynomialValue(double invariantPolynomialValue) {
		this.invariantPolynomialValue = invariantPolynomialValue;
	}

	public HashMap<String, String> getContextualFrames() {
		return contextualFrames;
	}

	public void setContextualFrames(HashMap<String, String> contextualFrames) {
		this.contextualFrames = contextualFrames;
	}

}
