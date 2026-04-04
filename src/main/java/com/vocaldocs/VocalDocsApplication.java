package com.vocaldocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VocalDocsApplication {

    public static void main(String[] args) {
        // Enforce FreeTTS Voice Loading Fix before Spring Context launches
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        SpringApplication.run(VocalDocsApplication.class, args);
    }
}
