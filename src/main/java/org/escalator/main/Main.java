package org.escalator.main;

import org.escalator.TextService;

import java.io.FileInputStream;

/**
 * Created by lfoppiano on 18/03/17.
 */
public class Main {

    public static void main(String... args) throws Exception {
        TextService textService = new TextService();
        if(args.length != 1) {
            System.out.println("please add the path to a pdf file");
        }

        System.out.println(textService.process(new FileInputStream(args[0])).toString());


    }
}
