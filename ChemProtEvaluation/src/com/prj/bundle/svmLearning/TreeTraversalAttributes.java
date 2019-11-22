/**
 * 
 */
package com.prj.bundle.svmLearning;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author neha
 *
 */
public class TreeTraversalAttributes {

	protected Set<Integer> currentMatchedIndices;
	protected Integer indexCount;
	protected Integer pruneFlag;
	
	/**
	 * Constructors
	 */
	public TreeTraversalAttributes() {

	}

	public TreeTraversalAttributes(Set<Integer> currentMatchedIndices) {
		this.indexCount = 0;
		this.pruneFlag = 0;
		this.currentMatchedIndices = currentMatchedIndices;
	}

	protected Set<Integer> getCurrentMatchedIndices() {
		return currentMatchedIndices;
	}

	protected void setCurrentMatchedIndices(Set<Integer> currentMatchedIndices) {
		this.currentMatchedIndices = currentMatchedIndices;
	}

	protected Integer getIndexCount() {
		return indexCount;
	}

	protected void setIndexCount(Integer indexCount) {
		this.indexCount = indexCount;
	}

	public Integer getPruneFlag() {
		return pruneFlag;
	}

	public void setPruneFlag(Integer pruneFlag) {
		this.pruneFlag = pruneFlag;
	}
	
}
