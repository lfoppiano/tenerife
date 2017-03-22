package org.escalator.service;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.escalator.exception.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;

/**
 * Created by lfoppiano on 17/08/16.
 */
public class GrobidClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidClient.class);

    private String prefix = "http://localhost:8080";

    public String getFulltext(InputStream inputStream) {

        String tei = null;
        try {
            URL url = new URL(prefix + "/processFulltextDocument");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            InputStreamBody inputStreamBody = new InputStreamBody(inputStream, "input");
            HttpEntity entity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT)
                    .addPart("input", inputStreamBody).build();
            conn.setRequestProperty("Content-Type", entity.getContentType().getValue());
            try (OutputStream out = conn.getOutputStream()) {
                entity.writeTo(out);
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_UNAVAILABLE) {
                throw new HttpRetryException("Failed : HTTP error code : "
                        + conn.getResponseCode(), conn.getResponseCode());
            }

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode() + " " + IOUtils.toString(conn.getErrorStream(), "UTF-8"));
            }

            InputStream in = conn.getInputStream();
            tei = IOUtils.toString(in, "UTF-8");
            IOUtils.closeQuietly(in);

            conn.disconnect();

        } catch (ConnectException | HttpRetryException e) {
            LOGGER.error(e.getMessage(), e.getCause());
            try {
                Thread.sleep(20000);
                getFulltext(inputStream);
            } catch (InterruptedException ex) {
            }
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Grobid processing timed out.", e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e.getCause());
        }
        return tei;
    }

    public String getFulltext(String filepath) {

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filepath);
        } catch (FileNotFoundException e) {
            throw new DataException("File " + filepath + " not found ", e);
        }
        return getFulltext(inputStream);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
