package org.escalator.common;

import org.escalator.exception.DataException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public class DomUtils {
    public static Document marshal(InputStream inputStream) {
        return marshal(inputStream, "UTF-8");
    }

    public static Document marshal(InputStream inputStream, String encoding) {
        try {
            DocumentBuilder builder = createDocumentBuilder();
            return builder.parse(new InputSource(new InputStreamReader(inputStream, encoding)));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new DataException("Cannot marshal resource. ", e);
        }
    }

    public static Document createDocument() throws ParserConfigurationException {
        return createDocumentBuilder().newDocument();
    }

    private static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder();
    }

    public static Document marshal(String input) {
        return marshal(new ByteArrayInputStream(input.getBytes()));
    }

    public static String unmarshal(Node document) {
        TransformerFactory tf = TransformerFactory.newInstance();
        StringWriter writer = new StringWriter();

        Transformer transformer = null;
        try {
            transformer = tf.newTransformer();

            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource xmlSource = new DOMSource(document);

            transformer.transform(xmlSource, new StreamResult(writer));
        } catch (Exception e) {
            throw new DataException(e);
        }
        return writer.toString();
    }

    public static Document clone(Document doc) {
        return processNode(doc);
    }

    public static Document createRootFromNode(Node doc) {
        return processNode(doc);
    }

    public static Document processNode(Node doc) {
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer tx = tFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            DOMResult result = new DOMResult();
            tx.transform(source, result);
            return (Document) result.getNode();
        } catch (Exception e) {
            throw new DataException(e);
        }
    }

    public static Node getElementByTagNameAndAttribute(Document document, String tagName, String attribute) {
        NodeList list = document.getElementsByTagName(tagName);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            Node att = node.getAttributes().getNamedItem(attribute);

            if (att != null && att.getNodeValue() != null) {
                return node;
            }
        }

        return null;
    }
}
