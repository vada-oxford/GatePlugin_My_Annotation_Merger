/*
 *  MyAnnotationMerger.java
 *
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the Apache License Version 2.0, January 2004.
 *
 * A copy of this licence is included in the distribution in the file
 * LICENSE.txt.
 *
 *  Ruslan Fayzrakhmanov, 24/7/2016
 *
 */

package uk.ac.ox.cs.nlp.gate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.GateRuntimeException;
import gate.util.InvalidOffsetException;


/**
 * This class is the implementation of the processing resource MY ANNOTATION MERGER.
 * 
 * @author Ruslan Fayzrakhmanov {@literal <ruslanrf@gmail.com>}
 * @version 0.0.1
 * 
 * Jul 24, 2016
 */
@CreoleResource(name = "My Annotation Merger",
        comment = "Gate processing resource, My Annotation Merger.")
public class MyAnnotationMerger  extends AbstractLanguageAnalyser {
	
	private static final long serialVersionUID = 4930542526491972781L;
	
	public final static String DEFAULT_OUTPUT_ANNOTATION_NAME = "MyAnnotationMerged";

	private static class AnnotationSpan {
		
		private final long start;
		public long getStart() {
			return start;
		}
		
		private final long end;
		public long getEnd() {
			return end;
		}
		
		private final String annotName;
		private final Map<Object, Object> annotFeatures;
		
		public AnnotationSpan(Annotation annot, Map<Object, Object> annotFeatures) {
			this(annot, annot.getType(), annotFeatures);
		}
		
		public AnnotationSpan(Annotation annot, String annotationNameOverride
				, Map<Object, Object> annotFeatures) {
			this(annot.getStartNode().getOffset(), annot.getEndNode().getOffset()
					, annotationNameOverride, annotFeatures);
		}
		
		public AnnotationSpan(long start, long end, String annotName, Map<Object, Object> annotFeatures) {
			this.start = start;
			this.end = end;
			this.annotName = annotName;
			this.annotFeatures = annotFeatures;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((annotFeatures == null) ? 0 : annotFeatures.hashCode());
			result = prime * result + ((annotName == null) ? 0 : annotName.hashCode());
			result = prime * result + (int) (end ^ (end >>> 32));
			result = prime * result + (int) (start ^ (start >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AnnotationSpan other = (AnnotationSpan) obj;
			if (annotFeatures == null) {
				if (other.annotFeatures != null)
					return false;
			} else if (!annotFeatures.equals(other.annotFeatures))
				return false;
			if (annotName == null) {
				if (other.annotName != null)
					return false;
			} else if (!annotName.equals(other.annotName))
				return false;
			if (end != other.end)
				return false;
			if (start != other.start)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "AnnotationSpan [start=" + start + ", end=" + end + ", annotName=" + annotName + ", annotFeatures="
					+ annotFeatures + "]";
		}
	}
	
	private Set<String> inputASNames;
	public Set<String> getInputASNames() {
		return inputASNames;
	}
	@RunTime @Optional @CreoleParameter(comment="Input annotation sets")
	public void setInputASNames(Set<String> inputASNames) {
		this.inputASNames = inputASNames==null? Collections.<String>emptySet(): inputASNames;
	}
	
	private Set<String> inputAnnotNames;
	public Set<String> getInputAnnotNames() {
	    return inputAnnotNames;
	  }
	  @RunTime @Optional
	  @CreoleParameter(comment="The annotation types to merge otherwise merge all")
	  public void setInputAnnotNames(Set<String> inputAnnotNames) {
		  this.inputAnnotNames = inputAnnotNames==null? Collections.<String>emptySet(): inputAnnotNames;
	  }
	  
		private Boolean bySameAnnotName;
	  	public Boolean getBySameAnnotName() {
			return bySameAnnotName;
		}
	  	@RunTime
		  @CreoleParameter(comment="Merge by the same annotation name.",
				  defaultValue="true")
		public void setBySameAnnotName(Boolean bySameAnnotName) {
			this.bySameAnnotName = (boolean)bySameAnnotName;
		}
		
	  	private Boolean byAllAnnotFeatures;
	  	public Boolean getByAllAnnotFeatures() {
			return byAllAnnotFeatures;
		}
	  	@RunTime
		  @CreoleParameter(comment="Merge if all features are the same",
		  	defaultValue="false")
		public void setByAllAnnotFeatures(Boolean byAllAnnotFeatures) {
			this.byAllAnnotFeatures = (boolean)byAllAnnotFeatures;
		}
	  	
		private Set<String> bySpecificAnnotFeatres;
		public Set<String> getBySpecificAnnotFeatres() {
			return bySpecificAnnotFeatres;
		}
		@RunTime @Optional
		  @CreoleParameter(comment="Merge by a specific annotation features. Empty set: do not consider features as a condition for merging.")
		public void setBySpecificAnnotFeatres(Set<String> bySpecificAnnotFeatres) {
			this.bySpecificAnnotFeatres = bySpecificAnnotFeatres==null? Collections.<String>emptySet(): bySpecificAnnotFeatres;
		}
		
		private EFeatureMergeMode featureMergeMode;
		public EFeatureMergeMode getFeatureMergeMode() {
			return featureMergeMode;
		}
		@RunTime
		  @CreoleParameter(comment="Annotation merging mode. \"SelectOne\" selects only one feature of the features to be merged."
		  		+ " \"Merge\" merge all features to be merged and represent them as a set."
		  		, defaultValue="MERGE"
		  		)
		public void setFeatureMergeMode(EFeatureMergeMode featureMergeMode) {
			this.featureMergeMode = featureMergeMode;
		}
		
		private String outputASName;
		public String getOutputASName() {
			return outputASName;
		}
		@RunTime @Optional @CreoleParameter(comment="Output annotation set")
		public void setOutputASName(String outputASName) {
			this.outputASName = outputASName==null?"":outputASName;
		}
		
		private String outputAnnotName;
		public String getOutputAnnotName() {
			return outputAnnotName;
		}
		@RunTime @CreoleParameter(comment="Output annotation name",
				defaultValue=DEFAULT_OUTPUT_ANNOTATION_NAME)
		public void setOutputAnnotName(String outputAnnotName) {
			this.outputAnnotName = outputAnnotName==null?"":outputAnnotName;
		}
		
	  /** Initialise this resource, and return it. */
	  @Override
	  public Resource init() throws ResourceInstantiationException
	  {
		  Resource r = super.init();
//		  this.inputASNames = Collections.<String>emptySet();
//		  this.inputAnnotNames = Collections.<String>emptySet();
//		  this.byAllAnnotName = false;
//		  this.bySpecificAnnotName = false;
//		  this.bySameAnnotFeatres = Collections.<String>emptySet();
//		  this.featureMergeMode = EFeatureMergeMode.MERGE;
//		  this.outputASName = "";
//		  this.outputAnnotName = DEFAULT_OUTPUT_ANNOTATION_NAME;
		  return r;
	  }

	  /**
	  * Reinitialises the processing resource. After calling this method the
	  * resource should be in the state it is after calling init.
	  * If the resource depends on external resources (such as rules files) then
	  * the resource will re-read those resources. If the data used to create
	  * the resource has changed since the resource has been created then the
	  * resource will change too after calling reInit().
	  */
	  @Override
	  public void reInit() throws ResourceInstantiationException
	  {
	    init();
	  }
	  
	  private final static String DEFAULT_ANNOTATION_COMPARE_NAME = "DEFAULT_ANNOTATION_COMPARE_NAME";
	  
	  private AnnotationSpan createAnnotationSpan(Annotation annot) {
		  Map<Object, Object> featuresTmp = new HashMap<>();
		  if (getByAllAnnotFeatures()) {
			  featuresTmp.putAll(annot.getFeatures());
		  } else {
			  if (getBySpecificAnnotFeatres() != null) {
				  for (String sameFeature : getBySpecificAnnotFeatres()) {
					  if (annot.getFeatures().containsKey(sameFeature)) {
						  featuresTmp.put(sameFeature, annot.getFeatures().get(sameFeature));
					  }
				  }
			  }
		  }
		  if (getBySameAnnotName()) {
			  return new AnnotationSpan(annot, featuresTmp);
		  } else {
			  return new AnnotationSpan(annot, DEFAULT_ANNOTATION_COMPARE_NAME, featuresTmp);
		  }
	  }
	  
	  private void addIntoInputAnnEqualGroups(
			  Annotation annot, Map<AnnotationSpan, Set<Annotation>> inputAnnEqualGroups) {
		  if (getInputAnnotNames().size() == 0
					|| getInputAnnotNames().contains(annot.getType())) {
				AnnotationSpan annotSpan = createAnnotationSpan(annot);
				Set<Annotation> annotationSetTmp = inputAnnEqualGroups.get(annotSpan);
				if (annotationSetTmp == null) {
					annotationSetTmp = new HashSet<Annotation>();
					inputAnnEqualGroups.put(annotSpan, annotationSetTmp);
				}
				annotationSetTmp.add(annot);
			}
	  }
	  
	  /** Run the resource. */
	  @Override
	  public void execute() throws ExecutionException {
		  Document document = getDocument();
		  
		  if(document == null)
			  throw new GateRuntimeException("No document to process!");
		  
		  HashMap<AnnotationSpan, Set<Annotation>> inputAnnEqualGroups = new HashMap<>();
		
		if (getInputASNames().size() > 0) {
			for (String inputASNames : getInputASNames()) {
				AnnotationSet asInputTmp = null;
				if (inputASNames == null) {
					asInputTmp = document.getAnnotations();
				} else {
					asInputTmp = document.getAnnotations(inputASNames);
				}
				for (Annotation annot : asInputTmp) {
					addIntoInputAnnEqualGroups(annot, inputAnnEqualGroups);
				}
			}
		} else {
			AnnotationSet asInputTmp = document.getAnnotations();
			for (Annotation annot : asInputTmp) {
				addIntoInputAnnEqualGroups(annot, inputAnnEqualGroups);
			}
		}
		
		AnnotationSet outputAnnSet = (getOutputASName().length() == 0)
			  ? document.getAnnotations()
			  : document.getAnnotations(getOutputASName());
		String outputAnnName = (getOutputAnnotName().length() == 0)
				? DEFAULT_OUTPUT_ANNOTATION_NAME
				: getOutputAnnotName();
		
		for (Entry<AnnotationSpan, Set<Annotation>> inputAnnEqualGroup : inputAnnEqualGroups.entrySet()) {
			final FeatureMap fm = Factory.newFeatureMap();
			switch (getFeatureMergeMode()) {
			case MERGE: {
				for (Annotation annot : inputAnnEqualGroup.getValue()) {
					for (Entry<Object, Object> f : annot.getFeatures().entrySet()) {
						if (getByAllAnnotFeatures() ||
								getBySpecificAnnotFeatres().contains(f.getKey())) {
							if (!fm.containsKey(f.getKey())) {
								fm.put(f.getKey(), f.getValue());
							}
						} else {
							if (fm.containsKey(f.getKey())) {
								@SuppressWarnings("unchecked")
								Collection<Object> col = (Collection<Object>)fm.get(f.getKey());
								col.add(f.getValue());
							} else {
								Set<Object> col = new HashSet<Object>();
								col.add(f.getValue());
								fm.put(f.getKey(), col);
							}
						}
					}
				}
				break;
			}
			case SELECT_ONE: {
				for (Annotation annot : inputAnnEqualGroup.getValue()) {
					for (Entry<Object, Object> f : annot.getFeatures().entrySet()) {
						if (!fm.containsKey(f.getKey())) {
							fm.put(f.getKey(), f.getValue());
						}
					}
				}
				break;
			}
			default:
				throw new ExecutionException("Unknown option "+getFeatureMergeMode()+" for FeatureMergeMode.");
			}
			
			try {
				outputAnnSet.add(
						inputAnnEqualGroup.getKey().getStart(),
						inputAnnEqualGroup.getKey().getEnd(),
						outputAnnName,
						fm);
			} catch (InvalidOffsetException e) {
				throw new ExecutionException(e);
			}
		}
		  
	  }
	  
} 
