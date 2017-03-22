package org.escalator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.escalator.exception.DataException;
import org.escalator.service.GrobidClient;
import org.escalator.service.GrobidResponseParser;
import org.escalator.service.NERDClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by lfoppiano on 18/03/17.
 */
public class TextService {
    GrobidClient grobidClient = new GrobidClient();
    NERDClient nerdClient = new NERDClient();
    GrobidResponseParser parser = new GrobidResponseParser();

    public TextService() {
    }

    public ObjectNode process(InputStream inputPdf) {
        String fulltext = grobidClient.getFulltext(inputPdf);

        List<String> paragraphs = parser.getParagraphs(fulltext);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
//        root.put("fulltext", fulltext);
        ArrayNode list = mapper.createArrayNode();

        paragraphs.forEach(p -> {
            System.out.println("PROCESSING: " + p);
            try {
                JsonNode actualObj = mapper.readTree(nerdClient.getNERDAnnotations(p));
                list.add(actualObj);
            } catch (DataException de) {
                de.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            root.set("paragraphs", list);
        });

        return root;
    }
}
