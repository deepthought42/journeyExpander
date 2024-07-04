package com.looksee.journeyExpander.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.looksee.journeyExpander.gcp.ImageSafeSearchAnnotation;
import com.looksee.journeyExpander.models.Browser;
import com.looksee.journeyExpander.models.BrowserConnectionHelper;
import com.looksee.journeyExpander.models.ElementState;
import com.looksee.journeyExpander.models.ImageElementState;
import com.looksee.journeyExpander.models.ImageFaceAnnotation;
import com.looksee.journeyExpander.models.ImageLandmarkInfo;
import com.looksee.journeyExpander.models.ImageSearchAnnotation;
import com.looksee.journeyExpander.models.Label;
import com.looksee.journeyExpander.models.Logo;
import com.looksee.journeyExpander.models.enums.BrowserEnvironment;
import com.looksee.journeyExpander.models.enums.BrowserType;
import com.looksee.journeyExpander.models.enums.ElementClassification;

/**
 * A collection of methods for interacting with the {@link Browser} session object
 *
 */
@Component
public class BrowserService {
	private static Logger log = LoggerFactory.getLogger(BrowserService.class);
	
	private static String[] valid_xpath_attributes = {"class", "id", "name", "title"};

	/**
	 * retrieves a new browser connection
	 *
	 * @param browser_name name of the browser (ie. firefox, chrome)
	 *
	 * @return new {@link Browser} instance
	 * @throws MalformedURLException
	 *
	 * @pre browser_name != null;
	 * @pre !browser_name.isEmpty();
	 */
	public Browser getConnection(BrowserType browser, BrowserEnvironment browser_env) throws MalformedURLException {
		assert browser != null;

		return BrowserConnectionHelper.getConnection(browser, browser_env);
	}

	/**
 	 * Constructs an {@link Element} from a JSOUP {@link Element element}
 	 * 
	 * @param xpath
	 * @param attributes
	 * @param element
	 * @param web_elem
	 * @param classification
	 * @param rendered_css_values
	 * @param screenshot_url TODO
	 * @param css_selector TODO
	 * @pre xpath != null && !xpath.isEmpty()
	 * @pre attributes != null
	 * @pre element != null
	 * @pre classification != null
	 * @pre rendered_css_values != null
	 * @pre css_values != null
	 * @pre screenshot != null
	 * 
	 * @return {@link ElementState} based on {@link WebElement} and other params
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public static ElementState buildElementState(
			String xpath, 
			Map<String, String> attributes, 
			Element element,
			WebElement web_elem,
			ElementClassification classification, 
			Map<String, String> rendered_css_values, 
			String screenshot_url,
			String css_selector
	) throws IOException{
		assert xpath != null && !xpath.isEmpty();
		assert attributes != null;
		assert element != null;
		assert classification != null;
		assert rendered_css_values != null;
		assert screenshot_url != null;
		
		Point location = web_elem.getLocation();
		Dimension dimension = web_elem.getSize();
		
		String foreground_color = rendered_css_values.get("color");
		if(foreground_color == null || foreground_color.trim().isEmpty()) {
			foreground_color = "rgb(0,0,0)";
		}
		
		ElementState element_state = new ElementState(
											element.ownText().trim(),
											element.text(),
											xpath, 
											element.tagName(), 
											attributes, 
											rendered_css_values, 
											screenshot_url, 
											location.getX(), 
											location.getY(), 
											dimension.getWidth(), 
											dimension.getHeight(), 
											classification,
											element.outerHtml(),
											css_selector, 
											foreground_color,
											rendered_css_values.get("background-color"),
											false);
		
		return element_state;
	}
	
	/**
 	 * Constructs an {@link Element} from a JSOUP {@link Element element}
 	 * 
	 * @param xpath
	 * @param attributes
	 * @param element
	 * @param web_elem
	 * @param classification
	 * @param rendered_css_values
	 * @param screenshot_url TODO
	 * @param css_selector TODO
	 * @pre xpath != null && !xpath.isEmpty()
	 * @pre attributes != null
	 * @pre element != null
	 * @pre classification != null
	 * @pre rendered_css_values != null
	 * @pre css_values != null
	 * @pre screenshot != null
	 * 
	 * @return {@link ElementState} based on {@link WebElement} and other params
	 * @throws IOException 
	 */
	public static ElementState buildImageElementState(
			String xpath, 
			Map<String, String> attributes, 
			Element element,
			WebElement web_elem,
			ElementClassification classification, 
			Map<String, String> rendered_css_values, 
			String screenshot_url,
			String css_selector,
			Set<ImageLandmarkInfo> landmark_info_set,
			Set<ImageFaceAnnotation> faces,
			ImageSearchAnnotation image_search_set,
			Set<Logo> logos,
			Set<Label> labels,
			ImageSafeSearchAnnotation safe_search_annotation
	) throws IOException{
		assert xpath != null && !xpath.isEmpty();
		assert attributes != null;
		assert element != null;
		assert classification != null;
		assert rendered_css_values != null;
		assert screenshot_url != null;
		
		Point location = web_elem.getLocation();
		Dimension dimension = web_elem.getSize();
		
		String foreground_color = rendered_css_values.get("color");
		if(foreground_color == null || foreground_color.trim().isEmpty()) {
			foreground_color = "rgb(0,0,0)";
		}
		
		String background_color = rendered_css_values.get("background-color");
		if(background_color == null) {
			background_color = "rgb(255,255,255)";
		}
		
		ElementState element_state = new ImageElementState(
													element.ownText().trim(),
													element.text(),
													xpath, 
													element.tagName(), 
													attributes, 
													rendered_css_values, 
													screenshot_url, 
													location.getX(), 
													location.getY(), 
													dimension.getWidth(), 
													dimension.getHeight(), 
													classification,
													element.outerHtml(),
													css_selector, 
													foreground_color,
													background_color,
													landmark_info_set,
													faces,
													image_search_set,
													logos,
													labels,
													safe_search_annotation);
		
		return element_state;
	}

	public static String generalizeSrc(String src) {
		assert src != null;
		Document html_doc = Jsoup.parse(src);
		html_doc.select("script").remove();
		html_doc.select("link").remove();
		html_doc.select("style").remove();
		html_doc.select("iframe").remove();
		
		//html_doc.attr("id","");
		for(Element element : html_doc.getAllElements()) {
			/*
			element.removeAttr("id")
				   .removeAttr("name")
				   .removeAttr("style")
				   .removeAttr("data-id");
			*/
		    List<String>  attToRemove = new ArrayList<>();
			for (Attribute a : element.attributes()) {
				if(element.tagName().contentEquals("img") && a.getKey().contentEquals("src")) {
					continue;
				}
		        // transfer it into a list -
		        // to be sure ALL data-attributes will be removed!!!
		        attToRemove.add(a.getKey());
		    }

		    for(String att : attToRemove) {
		        element.removeAttr(att);
		   }
		}
		
		return removeComments(html_doc.html());
	}
	
	/**
	 * Removes HTML comments from html string
	 * 
	 * @param html
	 * 
	 * @return html string without comments
	 */
	public static String removeComments(String html) {
		return Pattern.compile("<!--.*?-->").matcher(html).replaceAll("");
    }

	/** MESSAGE GENERATION METHODS **/
	static String[] data_extraction_messages = {
			"Locating elements",
			"Create an account to get results faster",
			"Looking for content",
			"Having a look-see",
			"Extracting colors",
			"Checking fonts",
			"Pssst. Get results faster by logging in",
			"Mapping page structure",
			"Locating links",
			"Extracting navigation",
			"Pssst. Get results faster by logging in",
			"Create an account to get results faster",
			"Mapping CSS styles",
			"Generating unique CSS selector",
			"Mapping forms",
			"Measuring whitespace",
			"Pssst. Get results faster by logging in",
			"Create an account to get results faster",
			"Mapping attributes",
			"Mapping attributes",
			"Mapping attributes",
			"Mapping attributes",
			"Mapping attributes",
			"Mapping attributes",
			"Extracting color palette",
			"Looking for headers",
			"Mapping content structure",
			"Create an account to get results faster",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Crunching the numbers",
			"Pssst. Get results faster by logging in",
			"Create an account to get results faster",
			"Searching for areas of interest",
			"Evaluating purpose of webpage",
			"Just a single page audit? Login to audit a domain",
			"Labeling icons",
			"Labeling images",
			"Labeling logos",
			"Applying customizations",
			"Checking for overfancification",
			"Grouping by proximity",
			"Almost there!",
			"Create an account to get results faster",
			"Labeling text elements",
			"Labeling links",
			"Pssst. Get results faster by logging in",
			"Labeling images",
			"Mapping form fields",
			"Extracting templates",
			"Contemplating the meaning of the universe",
			"Checking template structure"
			};

	/** MESSAGE GENERATION METHODS **/
	

	public static boolean doesElementHaveNegativePosition(Point location) {
		return location.getX() < 0 || location.getY() < 0;
	}

	public static boolean hasWidthAndHeight(Dimension dimension) {
		return dimension.getHeight() > 1 && dimension.getWidth() > 1;
	}

	/**
	 * Filters out html, body, link, title, script, meta, head, iframe, or noscript tags
	 *
	 * @param tag_name
	 *
	 * @pre tag_name != null
	 *
	 * @return true if tag name is html, body, link, title, script, meta, head, iframe, or noscript
	 */
	public static boolean isStructureTag(String tag_name) {
		assert tag_name != null;

		return "head".contentEquals(tag_name) || "link".contentEquals(tag_name) 
				|| "script".contentEquals(tag_name) || "g".contentEquals(tag_name) 
				|| "path".contentEquals(tag_name) || "svg".contentEquals(tag_name) 
				|| "polygon".contentEquals(tag_name) || "br".contentEquals(tag_name) 
				|| "style".contentEquals(tag_name) || "polyline".contentEquals(tag_name) 
				|| "use".contentEquals(tag_name) || "template".contentEquals(tag_name) 
				|| "audio".contentEquals(tag_name)  || "iframe".contentEquals(tag_name)
				|| "noscript".contentEquals(tag_name) || "meta".contentEquals(tag_name) 
				|| "base".contentEquals(tag_name) || "em".contentEquals(tag_name);
	}

	public static List<WebElement> filterNoWidthOrHeight(List<WebElement> web_elements) {
		List<WebElement> elements = new ArrayList<WebElement>(web_elements.size());
		for(WebElement element : web_elements){
			Dimension dimension = element.getSize();
			if(dimension.getHeight() > 1 && dimension.getWidth() > 1){
				elements.add(element);
			}
		}

		return elements;
	}

	public static List<ElementState> filterNoWidthOrHeight(List<ElementState> web_elements, boolean is_element_state) {
		List<ElementState> elements = new ArrayList<>(web_elements.size());
		for(ElementState element : web_elements){
			if(hasWidthAndHeight(new Dimension(element.getWidth(), element.getHeight()))) {
				elements.add(element);
			}
		}

		return elements;
	}

	/**
	 * Checks if {@link WebElement element} is visible in the current viewport window or not
	 * 
	 * @param browser {@link Browser browser} connection to use 
	 * @param location {@link Point point} where the element top left corner is located
	 * @param size {@link Dimension size} of the element
	 * 
	 * @return true if element is rendered within viewport, otherwise false
	 */
	public static boolean isElementVisibleInPane(Browser browser, Point location, Dimension size){
		assert browser != null;
		assert location != null;
		assert size != null;

		Point offsets = browser.getViewportScrollOffset();
		browser.setXScrollOffset(offsets.getX());
		browser.setYScrollOffset(offsets.getY());
		
		long y_offset = browser.getYScrollOffset();
		long x_offset = browser.getXScrollOffset();

		int x = location.getX();
		int y = location.getY();

		int height = size.getHeight();
		int width = size.getWidth();

		return x >= x_offset 
				&& y >= y_offset 
				&& ((x-x_offset)+width) <= (browser.getViewportSize().getWidth())
				&& ((y-y_offset)+height) <= (browser.getViewportSize().getHeight());
	}

	/**
	 * Checks if {@link WebElement element} is visible in the current viewport window or not
	 * 
	 * @param browser {@link Browser browser} connection to use 
	 * @param size {@link Dimension size} of the element
	 * 
	 * @return true if element is rendered within viewport, otherwise false
	 */
	public static boolean doesElementFitInViewport(Browser browser, Point position, Dimension size){
		assert browser != null;
		assert size != null;

		int height = size.getHeight();
		int width = size.getWidth();

		return width <= (browser.getViewportSize().getWidth())
				&& height <= (browser.getViewportSize().getHeight())
				&& position.getX() < browser.getViewportSize().getWidth()
				&& position.getX() >= 0
				&& position.getY() >= 0;
	}
	
	/**
	 * Get immediate child elements for a given element
	 *
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public static List<WebElement> getChildElements(WebElement elem) throws WebDriverException{
		return elem.findElements(By.xpath("./*"));
	}

	/**
	 * Get immediate child elements for a given element
	 *
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public static List<WebElement> getNestedElements(WebElement elem) throws WebDriverException{
		return elem.findElements(By.xpath(".//*"));
	}

	/**
	 * Get immediate parent elements for a given element
	 *
	 * @param elem	{@linkplain WebElement) to get parent of
	 * @return parent {@linkplain WebElement)
	 */
	public WebElement getParentElement(WebElement elem) throws WebDriverException{
		return elem.findElement(By.xpath(".."));
	}

	public static String cleanAttributeValues(String attribute_values_string) {
		String escaped = attribute_values_string.replaceAll("[\\t\\n\\r]+"," ");
		escaped = escaped.trim().replaceAll("\\s+", " ");
		escaped = escaped.replace("\"", "\\\"");
		return escaped.replace("\'", "'");
	}

	
	/**
	 * Extracts template for element by using outer html and removing inner text
	 * @param element {@link Element}
	 * @return templated version of element html
	 */
	public static String extractTemplate(String outerHtml){
		assert outerHtml != null;
		assert !outerHtml.isEmpty();
		
		Document html_doc = Jsoup.parseBodyFragment(outerHtml);

		Cleaner cleaner = new Cleaner(Whitelist.relaxed());
		html_doc = cleaner.clean(html_doc);
		
		html_doc.select("script").remove()
				.select("link").remove()
				.select("style").remove();

		for(Element element : html_doc.getAllElements()) {
			element.removeAttr("id");
			element.removeAttr("name");
			element.removeAttr("style");
		}
		
		return html_doc.html();
	}

	private boolean isTopLevelElement() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public static boolean testContainsElement(List<String> keys) {
		for(String key : keys) {
			if(key.contains("elementstate")) {
				return true;
			}
		}
		
		return false;
	}

	public String getPageSource(Browser browser, URL sanitized_url) throws MalformedURLException {
		assert browser != null;
		assert sanitized_url != null;
		
		return browser.getSource();
	}
}


@ResponseStatus(HttpStatus.SEE_OTHER)
class ServiceUnavailableException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 794045239226319408L;

	public ServiceUnavailableException(String msg) {
		super(msg);
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class FiveZeroThreeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 452417401491490882L;

	public FiveZeroThreeException(String msg) {
		super(msg);
	}
}
