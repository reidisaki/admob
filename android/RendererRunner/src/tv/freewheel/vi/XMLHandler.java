package tv.freewheel.vi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;

public class XMLHandler {
private static final String CLASSTAG = "XMLHandler";
	
	public static String createXMLDocument(XMLElement root) 
		throws IllegalArgumentException, 
				IllegalStateException, 
				IOException {
		
		Log.d(CLASSTAG, "Create xml document");
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        
    	serializer.setOutput(writer);
    	serializer.startDocument("UTF-8", true);
    	root.toXML(serializer);	
    	serializer.endDocument();
        
    	root = null;
        return writer.toString();
	}
	
	public static final Element getXMLElementFromString(String data, String rootName) throws Exception {
		ByteArrayInputStream byteArrayInputStream;
		try {
			byteArrayInputStream = new ByteArrayInputStream(data.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			byteArrayInputStream = new ByteArrayInputStream(data.getBytes());
		}
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;

		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new Exception("new DocumentBuilder failed");
		}

		Document document = null;
		try {
			document = builder.parse(byteArrayInputStream);
		} catch (SAXException e) {
			throw new Exception("parse xml failed");
		} catch (IOException e) {
			throw new Exception("IO Error occured");
		}
		
		NodeList rootList = document.getElementsByTagName(rootName);
		if(rootList.getLength() == 0) {
			throw new Exception("no root node" + rootName
					+ " found in document");
		}

		Element adResponseElement = (Element)rootList.item(0);
		
		return adResponseElement;
	}
}
