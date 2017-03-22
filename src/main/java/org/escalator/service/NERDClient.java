package org.escalator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.escalator.exception.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

/**
 * Created by lfoppiano on 17/08/16.
 */
public class NERDClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NERDClient.class);

    private String prefix = "http://localhost:8090";

    public String getNERDAnnotations(String text) {
        String json = null;
        try {
            URL url = new URL(prefix + "/processNERDQuery");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = mapper.createObjectNode();
            node.put("text", text);
            ObjectNode dataNode = mapper.createObjectNode();
            dataNode.put("lang", "en");
            node.set("language", dataNode);


            byte[] postDataBytes = node.toString().getBytes("UTF-8");

            try (OutputStream out = conn.getOutputStream()) {
                out.write(postDataBytes);
                out.flush();
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_UNAVAILABLE) {
                throw new HttpRetryException("Failed : HTTP error code: "
                        + conn.getResponseCode(), conn.getResponseCode());
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw new DataException("The input: " + text + ", generate a BAD REQUEST (400)");
            } else if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode() + " " + IOUtils.toString(conn.getErrorStream(), "UTF-8"));
            }

            InputStream in = conn.getInputStream();
            json = IOUtils.toString(in, "UTF-8");
            IOUtils.closeQuietly(in);

            conn.disconnect();

        } catch (ConnectException | HttpRetryException e) {
            LOGGER.error(e.getMessage(), e.getCause());
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("NERD processing timed out.", e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e.getCause());
        }

        return json;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
