package com.looksee.journeyExpander.models;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.looksee.journeyExpander.gcp.ImageSafeSearchAnnotation;
import com.looksee.journeyExpander.models.enums.ElementClassification;

@Node
public class ImageElementState extends ElementState {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ImageElementState.class);
	
	@Relationship(type="HAS")
	private Set<Logo> logos;
	
	@Relationship(type="HAS")
	private Set<Label> labels;
	
	@Relationship(type="HAS")
	private Set<ImageLandmarkInfo> landmarkInfoSet;
	
	@Relationship(type="HAS")
	private Set<ImageFaceAnnotation> faces;
	
	@Relationship(type="HAS")
	private ImageSearchAnnotation imageSearchSet;
	
	private String adultContent;
	private String racyContent;
	private String violenceContent;
	
	public ImageElementState() {
		super();
		this.logos = new HashSet<>();
		this.labels = new HashSet<>();
		this.landmarkInfoSet = new HashSet<>();
		this.faces = new HashSet<>();
		setImageFlagged(false);
	}
	
	public ImageElementState(String owned_text, 
							 String all_text, 
							 String xpath, 
							 String tagName, 
							 Map<String, String> attributes,
							 Map<String, String> rendered_css_values, 
							 String screenshot_url, 
							 int x, 
							 int y, 
							 int width, 
							 int height,
							 ElementClassification classification, 
							 String outer_html, 
							 String css_selector,
							 String foreground_color, 
							 String background_color, 
							 Set<ImageLandmarkInfo> landmark_info_set,
							 Set<ImageFaceAnnotation> faces, 
							 ImageSearchAnnotation image_search, 
							 Set<Logo> logos,
							 Set<Label> labels, 
							 ImageSafeSearchAnnotation safe_search_annotation
	) {
		super(owned_text,
				all_text,
				xpath,
				tagName,
				attributes,
				rendered_css_values,
				screenshot_url,
				x,
				y,
				width,
				height,
				classification,
				outer_html,
				css_selector,
				foreground_color,
				background_color,
				!image_search.getFullMatchingImages().isEmpty());
		setLandmarkInfoSet(landmark_info_set);
		setFaces(faces);
		setImageSearchSet(imageSearchSet);
		setLogos(logos);
		setLabels(labels);
		setAdult(safe_search_annotation.getAdult());
		setRacy(safe_search_annotation.getRacy());
		setViolence(safe_search_annotation.getViolence());
	}

	public Set<Logo> getLogos() {
		return logos;
	}
	public void setLogos(Set<Logo> logos) {
		this.logos = logos;
	}
	public Set<Label> getLabels() {
		return labels;
	}
	public void setLabels(Set<Label> labels) {
		this.labels = labels;
	}
	public Set<ImageLandmarkInfo> getLandmarkInfoSet() {
		return landmarkInfoSet;
	}
	public void setLandmarkInfoSet(Set<ImageLandmarkInfo> landmark_info_set) {
		this.landmarkInfoSet = landmark_info_set;
	}
	public Set<ImageFaceAnnotation> getFaces() {
		return faces;
	}
	public void setFaces(Set<ImageFaceAnnotation> faces) {
		this.faces = faces;
	}
	public ImageSearchAnnotation getImageSearchSet() {
		return imageSearchSet;
	}
	public void setImageSearchSet(ImageSearchAnnotation image_search_set) {
		this.imageSearchSet = image_search_set;
	}

	@JsonIgnore
	public boolean isAdultContent() {
		if(getAdult() == null || getRacy() == null) {
			return false;
		}
		return getAdult().contains("LIKELY")
				|| getRacy().contains("LIKELY");					
	}
	
	@JsonIgnore
	public boolean isViolentContent() {
		return getViolence().contains("LIKELY");
					
	}

	public String getAdult() {
		return adultContent;
	}

	public void setAdult(String adult) {
		this.adultContent = adult;
	}

	public String getRacy() {
		return racyContent;
	}

	public void setRacy(String racy) {
		this.racyContent = racy;
	}

	public String getViolence() {
		return violenceContent;
	}

	public void setViolence(String violence) {
		this.violenceContent = violence;
	}
}
