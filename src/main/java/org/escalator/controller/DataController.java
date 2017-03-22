package org.escalator.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.escalator.TextService;
import org.escalator.exception.DataException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Created by lfoppiano on 19/03/17.
 */

@RestController
@EnableAutoConfiguration
@SpringBootApplication
@RequestMapping("/service")
public class DataController {

    @RequestMapping(value = "/data", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public String processPdf(@RequestParam(value = "file") MultipartFile pdf) {
        TextService textService = new TextService();
        ObjectNode process = null;
        try {
            process = textService.process(pdf.getInputStream());
        } catch (IOException e) {
            throw new DataException("Cannot read input pdf.", e);
        }

        return process.toString();
    }


    public static void main(String[] args) throws Exception {
        SpringApplication.run(DataController.class, args);
    }
}
