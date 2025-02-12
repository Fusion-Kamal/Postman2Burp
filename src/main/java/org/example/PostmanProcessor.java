package org.example;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostmanProcessor {
    private MontoyaApi api;
    private String postmanPath;
    private Map<String, String> variablesMap = new HashMap<>();
    private Postman2BurpUI ui;
    private int requestCounter = 0;
    private List<Object[]> httpRequestList = new ArrayList<>();

    public PostmanProcessor(MontoyaApi api, String postmanPath, Postman2BurpUI ui) {
        this.api = api;
        this.postmanPath = postmanPath;
        this.ui = ui;
    }

    public void processPostman() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode postmanCollection = mapper.readTree(new File(postmanPath));

            JsonNode variables = postmanCollection.get("variable");
            if (variables != null) {
                for (JsonNode variable : variables) {
                    String key = variable.get("key").asText();
                    String value = variable.get("value").asText();
//                    api.logging().logToOutput("Variable: " + key + " = " + value);
                    variablesMap.put(key, value);
                }
            }

            JsonNode items = postmanCollection.get("item");
            processItems(items);

        } catch (IOException e) {
            api.logging().logToOutput("Error loading Postman collection: " + e.getMessage());
        }
    }
    private void processItems(JsonNode items) {
        for (JsonNode item : items) {
            if (item.has("item")) {
                processItems(item.get("item"));
            } else {
                processRequest(item);
            }
        }
    }

    private void processRequest(JsonNode item) {
        String requestName = item.get("name").asText();
        String requestMethod = item.get("request").get("method").asText();

        try {
            String rawUrl = replaceVariables(item.get("request").get("url").get("raw").asText());

            if (!rawUrl.startsWith("http")) {
                rawUrl = "https://" + rawUrl;
            }


            String protocol =  rawUrl.split("://")[0] ;

            String requestUrl = rawUrl.split("://")[1].substring(rawUrl.split("://")[1].indexOf("/"));

//            api.logging().logToOutput(requestUrl);

            String host = rawUrl.split("://")[1].split("/")[0];
            int port = protocol.equalsIgnoreCase("https") ? 443 : 80;
            if (host.contains(":")) {
                port = Integer.parseInt((host.split(":")[1]));
            }


            JsonNode headersNode = item.get("request").get("header");
            StringBuilder headers = new StringBuilder();
            if (headersNode != null) {
                for (JsonNode header : headersNode) {
                    headers.append(header.get("key").asText()).append(": ").append(header.get("value").asText()).append("\r\n");
                }
            }
            String requestBody = "";
            String contentType = "";

            if (item.get("request").has("body") && !item.get("request").get("body").isNull()) {
                JsonNode body = item.get("request").get("body");
                contentType = body.get("mode").asText();

                if ("urlencoded".equalsIgnoreCase(contentType)) {
                    StringBuilder urlEncodedBody = new StringBuilder();
                    for (JsonNode param : body.get("urlencoded")) {
                        if (urlEncodedBody.length() > 0) {
                            urlEncodedBody.append("&");
                        }
                        urlEncodedBody.append(param.get("key").asText())
                                .append("=")
                                .append(param.get("value").asText());
                    }
                    requestBody = urlEncodedBody.toString();
                    headers.append("Content-Type: application/x-www-form-urlencoded\r\n");
                } else if ("raw".equalsIgnoreCase(contentType)) {
                    requestBody = body.get("raw").asText();
                    headers.append("Content-Type: application/json\r\n");
                }
            }


            requestName = replaceVariables(requestName);
            requestUrl = replaceVariables(requestUrl);
            requestMethod = replaceVariables(requestMethod);
            requestBody = replaceVariables(requestBody);


            headers.append("User-Agent: PostmanRuntime/7.43.0\r\n");
            headers.append("Accept: */*\r\n");
            headers.append("Accept-Encoding: gzip, deflate, br\r\n");
            headers.append("Cache-Control: no-cache\r\n");
            headers.append("Postman-Token: 07dd37bc-a093-4ca0-a89f-0b566958ba9e\r\n");
            headers.append("Connection: keep-alive\r\n");
            headers.append("Host: " + host + "\r\n");

            if (host.contains(":")) {
                host = host.split(":")[0];
            }

            HttpService service = HttpService.httpService(host, port, protocol.equalsIgnoreCase("https"));
            HttpRequest httpRequest = HttpRequest.httpRequest(service, requestMethod + " " + requestUrl + " HTTP/1.1\r\n" +
                    headers + "\r\n" +
                    (requestBody != null ? requestBody : ""));

            httpRequestList.add(new Object[]{httpRequest, requestName});
            requestCounter++;
            ui.addRequestToTable(requestCounter,requestMethod, requestUrl);
        } catch (Exception e) {
            api.logging().logToOutput("ERROR : " +requestMethod +" "+ requestName+ " - " + e.getMessage());
        }

    }

    private String replaceVariables(String input) {
        if (input == null) return "";
        for (Map.Entry<String, String> entry : variablesMap.entrySet()) {
            String variablePlaceholder = "{{" + entry.getKey() + "}}";
            input = input.replace(variablePlaceholder, entry.getValue());
        }
        return input;
    }

    public Map<String, String> getVariablesMap() {
        return variablesMap;
    }

    public List<Object[]> getHttpRequestList() {
        return httpRequestList;
    }
}