package tv.freewheel.vi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;


public class XMLElement {
	private static final String CLASSTAG = "XMLElement";
	private String name;
	private String value;
	private boolean isCDATASection = false;
	private ArrayList<XMLElement> children;
	private TreeMap<String, String> attributes;
	
	public XMLElement(String name) {
		this.name = name;
		this.attributes = new TreeMap<String, String>();
		this.children = new ArrayList<XMLElement>();
	}
	
	
	public XMLElement(Element ele){
		this(ele.getNodeName());
		NamedNodeMap attrMap = ele.getAttributes();
		for(int i=0;i<attrMap.getLength();i++){
			 this.setAttribute(attrMap.item(i).getNodeName(),attrMap.item(i).getNodeValue());
		}
		NodeList children = ele.getChildNodes();
		for(int j=0;j <children.getLength();j++){
			Node node = children.item(j);
			if (node.getNodeType() == Node.ELEMENT_NODE)
				this.appendChild(new XMLElement((Element)node));
			else if(node.getNodeType() == Node.TEXT_NODE)
				this.setText(node.getNodeValue());
			else if(node.getNodeType() == Node.CDATA_SECTION_NODE){
				this.setCDATAContent(node.getNodeValue());
			}else{
				if(node.getNodeType() != Node.ATTRIBUTE_NODE)
					Log.v(CLASSTAG, "A kind of not supported node :" + node );
			}
		}
	}

	public void setAttribute(String name, String value) {
		if (name == null || value == null) {
			return;
		}
		if (name.trim().length() == 0) {
			return;
		}
		this.attributes.put(name, value);
	}
	
	public void setAttribute(String name, int value) {
		this.setAttribute(name, String.valueOf(value));
	}
	
	public void setAttribute(String name, int value, boolean validate) {
		if (validate && value <= 0) {
			return;
		}
		this.setAttribute(name, value);
	}
	
	public void setAttribute(String name, boolean value) {
		this.setAttribute(name, String.valueOf(value));
	}
	
	public void setAttribute(String name, double value) {
		this.setAttribute(name, String.valueOf(value));
	}
	
	public void setAttribute(String name, double value, boolean validate) {
		if (validate && value <= 0) {
			return;
		}
		this.setAttribute(name, value);
	}
	
	public void setText(String value) {
		this.value = value;
	}
	
	public void setCDATAContent(String value){
		this.isCDATASection = true;
		this.setText(value);
	}
	
	public void appendChild(XMLElement child) {
		if (child != null) {
			this.children.add(child);
		}
	}
	
	public void toXML(XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag("", this.name);
		Iterator<String> s_iter = this.attributes.keySet().iterator();
		while (s_iter.hasNext()) {
			String key = s_iter.next();
			serializer.attribute("", key, this.attributes.get(key));
		}
		
		if (this.value != null) {
			if (this.isCDATASection) {
				serializer.cdsect(this.value);
			}else {
				serializer.text(this.value);
			}
		}
		
		Iterator<XMLElement> o_iter = this.children.iterator();
		while (o_iter.hasNext()) {
			o_iter.next().toXML(serializer);
		}
    	serializer.endTag("", this.name);
	}
}
