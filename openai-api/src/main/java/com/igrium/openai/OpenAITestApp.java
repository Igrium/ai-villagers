package com.igrium.openai;

import org.slf4j.LoggerFactory;

public class OpenAITestApp {
    public static void main(String[] args) {
        LoggerFactory.getLogger(OpenAITestApp.class).info(OpenAI.getTestMessage());
    }
}
