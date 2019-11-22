/**
 * 
 */
package com.prj.bundle.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author neha
 *
 */
public class BoundaryReturn {

	protected TreeMap<Integer,HashMap<String,ArrayList<String>>> posTaggedSentenceBundle;
	
	protected TreeMap<Integer,HashMap<String,ArrayList<String>>> orgSentenceBundle;
	
	protected LinkedHashMap<String, Set<String>> entityRelationIdentifierTable;
	
	protected LinkedHashMap<String, ArrayList<String>> mainRelationVerbList;
	
	protected HashMap<String, Set<HashMap<String, String>>> negativeInstances;
	
	/**
	 * Constructor
	 */
	public BoundaryReturn() {
		
		this.posTaggedSentenceBundle = new TreeMap<>();
		this.orgSentenceBundle = new TreeMap<>();
		this.entityRelationIdentifierTable = new LinkedHashMap<>();
		this.mainRelationVerbList = new LinkedHashMap<>();
		this.negativeInstances = new LinkedHashMap<>();
	}
	
	public BoundaryReturn(TreeMap<Integer,HashMap<String,ArrayList<String>>> posTaggedSentenceAssembler,
			TreeMap<Integer,HashMap<String,ArrayList<String>>> originalSentenceAssembler,
			LinkedHashMap<String, Set<String>> entityRelationIdentifierTable, 
			LinkedHashMap<String, ArrayList<String>> mainRelationVerbList, 
			HashMap<String, Set<HashMap<String, String>>> negativeInstances) {
		//super();
		this.posTaggedSentenceBundle = posTaggedSentenceAssembler;
		this.orgSentenceBundle = originalSentenceAssembler;
		this.entityRelationIdentifierTable = entityRelationIdentifierTable;
		this.mainRelationVerbList = mainRelationVerbList;
		this.negativeInstances = negativeInstances;
	}

}
