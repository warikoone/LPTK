/**
 * 
 */
package com.prj.bundle.svmLearning;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author neha
 *
 */
public class InstanceTypeIdentification_FV {
	
		public ArrayList<String> orgSentenceSplit;
		public ArrayList<String> posTaggedSentenceSplit;
		public HashMap<String,HashMap<Double, ArrayList<Integer>>> taskFeatures;
		public Integer instanceType;
		
		/**
		 * Constructors
		 */
		public InstanceTypeIdentification_FV() {
			
		}
		
		public InstanceTypeIdentification_FV(ArrayList<String> orgSentenceSplit, ArrayList<String> posTaggedSentenceSplit,
				HashMap<String,HashMap<Double, ArrayList<Integer>>> taskCollector, 
				Integer instanceType) {
			this.orgSentenceSplit = orgSentenceSplit;
			this.posTaggedSentenceSplit = posTaggedSentenceSplit;
			this.taskFeatures = taskCollector;
			this.instanceType = instanceType;
		}



		public ArrayList<String> getOrgSentenceSplit() {
			return orgSentenceSplit;
		}

		public void setOrgSentenceSplit(ArrayList<String> orgSentenceSplit) {
			this.orgSentenceSplit = orgSentenceSplit;
		}

		public ArrayList<String> getPosTaggedSentenceSplit() {
			return posTaggedSentenceSplit;
		}

		public void setPosTaggedSentenceSplit(ArrayList<String> posTaggedSentenceSplit) {
			this.posTaggedSentenceSplit = posTaggedSentenceSplit;
		}

		public HashMap<String,HashMap<Double, ArrayList<Integer>>> getTaskFeatures() {
			return taskFeatures;
		}

		public void setTaskFeatures(
				HashMap<String,HashMap<Double, ArrayList<Integer>>> taskFeatures) {
			this.taskFeatures = taskFeatures;
		}

		public Integer getInstanceType() {
			return instanceType;
		}

		public void setInstanceType(Integer instanceType) {
			this.instanceType = instanceType;
		}

}

