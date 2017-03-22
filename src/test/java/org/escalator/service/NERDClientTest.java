package org.escalator.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by lfoppiano on 19/03/17.
 */
class NERDClientTest {

    @Test
    void myFirstTest() {
        final NERDClient nerdClient = new NERDClient();

        nerdClient.getNERDAnnotations("This is a new New York City annotator company.");

    }

}