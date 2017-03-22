package org.escalator.service;

import org.escalator.common.DomUtils;
import org.escalator.exception.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by lfoppiano on 18/08/16.
 */
public class GrobidResponseParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidResponseParser.class);


    public List<String> getParagraphs(String tei) throws DataException {
        XPath xPath = XPathFactory.newInstance().newXPath();

        List<String> resultParagraphs = new ArrayList<>();

        Document domDocument = DomUtils.marshal(tei);

        NodeList divs = null;
        try {
            divs = (NodeList) xPath
                    .compile("//TEI/text/body/div")
                    .evaluate(domDocument, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DataException("Cannot parse xml", e);
        }


        for (int i = 0; i < divs.getLength(); i++) {
            Node div = divs.item(i);

            try {

                final Document divAsRootNode = DomUtils.createRootFromNode(div);
                Node head = (Node) xPath
                        .compile("//*[local-name() = 'div']/*[local-name() = 'head']")
                        .evaluate(divAsRootNode, XPathConstants.NODE);

                if (head != null) {
                    resultParagraphs.add(head.getTextContent());
                }

                NodeList paragraphs = (NodeList) xPath
                        .compile("//*[local-name() = 'div']/*[local-name() = 'p']")
                        .evaluate(divAsRootNode, XPathConstants.NODESET);

                for (int j = 0; j < paragraphs.getLength(); j++) {
                    resultParagraphs.add(paragraphs.item(j).getTextContent());
                }

            } catch (XPathExpressionException e) {
                throw new DataException("Cannot parse xml", e);
            }


        }


        return resultParagraphs;
    }

    private List<Node> getAffiliations(Element authorNode) {
        List<Node> affiliations = new ArrayList<>();

        for (int i = 0; i < authorNode.getChildNodes().getLength(); i++) {
            Node affiliationNode = authorNode.getChildNodes().item(i);
            if ("affiliation".equals(affiliationNode.getNodeName())) {
                affiliations.add(affiliationNode);
            }
        }
        return affiliations;
    }

    private Node getFirstAffiliation(Element authorNode) {
        for (int i = 0; i < authorNode.getChildNodes().getLength(); i++) {
            Node affiliationNode = authorNode.getChildNodes().item(i);
            if ("affiliation".equals(affiliationNode.getNodeName())) {
                return affiliationNode;
            }
        }
        return null;
    }

    public Node getPersName(Node authorNode) {
        for (int i = 0; i < authorNode.getChildNodes().getLength(); i++) {
            Node persNode = authorNode.getChildNodes().item(i);
            if ("persName".equals(persNode.getNodeName())) {
                return persNode;
            }
        }
        return null;
    }

    /*public List<List<Entity>> parseAffiliation(String tei) throws DataException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        List<List<Entity>> entities = new ArrayList<>();
        Document doc = DomUtils.marshal(tei);

        try {
            NodeList listAffiliations = (NodeList) xPath.compile("//TEI/teiHeader/fileDesc/sourceDesc/biblStruct/analytic/author/affiliation")
                    .evaluate(doc, XPathConstants.NODESET);

            for (int i = 0; i < listAffiliations.getLength(); i++) {
                Node affiliation = listAffiliations.item(i);
                if (affiliation == null) {
                    continue;
                }
                List<Entity> populatedEntities = populateAffiliationEntity(affiliation);

                buildRelations(populatedEntities);
                entities.add(populatedEntities);

            }
        } catch (XPathExpressionException e) {
            throw new DataException(e);
        }


        return entities;
    }*/


    private Optional<String> extractAndApply(Node affiliation, String xPathString, Consumer<String> closure) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        Optional<String> o = Optional.empty();

        try {
            final Node node = (Node) xPath
                    .compile(xPathString)
                    .evaluate(DomUtils.createRootFromNode(affiliation), XPathConstants.NODE);
            if (node != null) {
                closure.accept(node.getTextContent());
            }
        } catch (XPathExpressionException e) {
            throw new DataException(e);
        }

        return o;
    }

    private void removeAttribute(NodeList persNames, String attributeName) {
        for (int i = 0; i < persNames.getLength(); i++) {
            Node persName = persNames.item(i);
            try {
                persName.getAttributes().removeNamedItem(attributeName);
            } catch (DOMException e) {
                LOGGER.warn("Cannot remove " + attributeName + " from\n " + DomUtils.unmarshal(persName) + ".\n" +
                        "Perhaps the attribute is not there?");
            }
        }
    }

}
