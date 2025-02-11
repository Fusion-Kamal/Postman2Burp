package org.example;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public class Postman2Burp implements BurpExtension {

    private MontoyaApi api;
    public static String postmanpath;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Postman2Burp");

        Postman2BurpUI ui = new Postman2BurpUI(api);

        api.userInterface().registerSuiteTab("Import Postman Collection", ui.getPanel());
    }


}