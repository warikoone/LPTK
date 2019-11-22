package com.prj.bundle.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.stanford.nlp.util.Sets;

public class test {

	public static void main(String[] args) {

		TreeSet<String> structureWords = new TreeSet<>(Arrays.asList("BACKGROUND:","METHODS:",
				"RESULTS:","CONCLUSIONS:","STUDY DESIGN AND METHODS:","FINDINGS:","INTERPRETATION:",
				"METHODS AND RESULTS:","OBJECTIVES:","CASE REPORT:","DISCUSSION:","CASE:",
				"DESIGN/METHODS:","BACKGROUND AND OBJECTIVES:","RELEVANCE TO CLINICAL PRACTICE",
				"SEARCH STRATEGY:","SELECTION CRITERIA:","DATA COLLECTION AND ANALYSIS:",
				"MAIN RESULTS:","AUTHORS' CONCLUSIONS:","PURPOSE:","RATIONALE:","OBJECTIVE:",
				"CONCLUSION:","MATERIALS AND METHODS:","INTRODUCTION:","AIM OF THE STUDY:","AIMS:",
				"ETHNOPHARMACOLOGICAL RELEVANCE:","SEARCH STRATEGY:","BACKGROUND AND AIMS:","AIM:",
				"BACKGROUND & AIMS:","STUDY DESIGN:","DESIGN:","REVIEW SUMMARY:","UNLABELLED:",
				"ABSTRACT CONTEXT:"));
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
		NavigableMap<Integer, TreeSet<String>> temp = docStructureWords.descendingMap();
		System.out.println("\n\t>>"+temp.values());
		String a = "-1";
		if(a.matches("(|-)\\d+")){
			System.out.println("hi");
		}
	}
}
