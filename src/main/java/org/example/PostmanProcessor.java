package org.example;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PostmanProcessor {
    private final MontoyaApi api;
    private final String postmanPath;
    private final Map<String, String> variablesMap = new HashMap<>();
    private final Set<String> undefinedVariables = new HashSet<>();
    private final Postman2BurpUI ui;
    private final List<Object[]> httpRequestList = new ArrayList<>();
    private int requestCounter = 0;
    private Map<String, String> headersList = new LinkedHashMap<>();


    public PostmanProcessor(MontoyaApi api, String postmanPath, Postman2BurpUI ui) {
        this.api = api;
        this.postmanPath = postmanPath;
        this.ui = ui;
    }

    public void identifyVariables() {
        try {
            JsonNode postmanCollection = new ObjectMapper().readTree(new File(postmanPath));
            JsonNode variables = postmanCollection.get("variable");
            if (variables != null) {
                variables.forEach(variable -> variablesMap.put(variable.get("key").asText(), variable.get("value").asText()));
            }
            extractVariablesFromItems(postmanCollection.get("item"));
        } catch (IOException e) {
            api.logging().logToOutput("Error loading Postman collection: " + e.getMessage());
        }
    }

    private void extractVariablesFromItems(JsonNode items) {
        items.forEach(item -> {
            if (item.has("item")) {
                extractVariablesFromItems(item.get("item"));
            } else {
                extractVariablesFromRequest(item);
            }
        });
    }

    private void extractVariablesFromRequest(JsonNode item) {
        extractVariables(item.get("request").get("url").get("raw").asText());
        JsonNode headersNode = item.get("request").get("header");
        if (headersNode != null) {
            headersNode.forEach(header -> extractVariables(header.get("value").asText()));
        }

        JsonNode auth = item.get("request").get("auth");
        if (auth != null && !auth.isNull()) {
            String authType = auth.get("type").asText();
            if ("bearer".equalsIgnoreCase(authType)) {
                extractVariables(auth.get("bearer").get(0).get("value").asText());
            } else if ("basic".equalsIgnoreCase(authType)) {
                extractVariables(auth.get("basic").get(0).get("value").asText());
                extractVariables(auth.get("basic").get(1).get("value").asText());
            }
        }


    }
    private void extractVariables(String input) {
        if (input == null) return;
        extractVariablesFromString(input).forEach(key -> {
            if (!variablesMap.containsKey(key)) {
                undefinedVariables.add(key);
            }
        });
    }

    private Set<String> extractVariablesFromString(String input) {
        Set<String> variables = new HashSet<>();
        int startIndex = input.indexOf("{{");
        while (startIndex != -1) {
            int endIndex = input.indexOf("}}", startIndex);
            if (endIndex != -1) {
                variables.add(input.substring(startIndex + 2, endIndex));
                startIndex = input.indexOf("{{", endIndex);
            } else {
                break;
            }
        }
        return variables;
    }
    public void processPostman() {
        try {
            JsonNode postmanCollection = new ObjectMapper().readTree(new File(postmanPath));
            processItems(postmanCollection.get("item"));
        } catch (IOException e) {
            api.logging().logToOutput("Error loading Postman collection: " + e.getMessage());
        }
    }

    private void processItems(JsonNode items) {
        items.forEach(item -> {
            if (item.has("item")) {
                processItems(item.get("item"));
            } else {
                processRequest(item);
            }
        });
    }

    private void processRequest(JsonNode item) {
        String requestName = replaceVariables(item.get("name").asText());
        String requestMethod = replaceVariables(item.get("request").get("method").asText());
        try {
            String rawUrl = replaceVariables(item.get("request").get("url").get("raw").asText());
            if (!rawUrl.startsWith("http")) {
                rawUrl = "https://" + rawUrl;
            }
            String protocol = rawUrl.split("://")[0];
            String requestUrl = rawUrl.split("://")[1].substring(rawUrl.split("://")[1].indexOf("/"));
            String host = rawUrl.split("://")[1].split("/")[0];
            int port = protocol.equalsIgnoreCase("https") ? 443 : 80;
            if (host.contains(":")) {
                port = Integer.parseInt(host.split(":")[1]);
            }

            StringBuilder headers = new StringBuilder();
            JsonNode headersNode = item.get("request").get("header");
            if (headersNode != null) {
                headersNode.forEach(header -> {
                    String headerKey = header.get("key").asText();
                    String headerValue = replaceVariables(header.get("value").asText());
                    headers.append(headerKey).append(": ").append(headerValue).append("\r\n");
                 });            }


            headers.append("User-Agent: PostmanRuntime/7.43.0\r\n")
                    .append("Accept: */*\r\n")
                    .append("Accept-Encoding: gzip, deflate, br\r\n")
                    .append("Cache-Control: no-cache\r\n")
                    .append("Postman-Token: 07dd37bc-a093-4ca0-a89f-0b566958ba9e\r\n")
                    .append("Connection: keep-alive\r\n")
                    .append("Host: ").append(host).append("\r\n");



            JsonNode auth = item.get("request").get("auth");
            if (auth != null && !auth.isNull()) {
                String authType = auth.get("type").asText();
                if ("bearer".equalsIgnoreCase(authType)) {
                    headers.append("Authorization: Bearer ").append(auth.get("bearer").get(0).get("value").asText()).append("\r\n");
                } else if ("basic".equalsIgnoreCase(authType)) {
                    String username = auth.get("basic").get(0).get("value").asText();
                    String password = auth.get("basic").get(1).get("value").asText();
                    String basicAuth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                    headers.append("Authorization: Basic ").append(basicAuth).append("\r\n");
                }
            }

            String requestBody = "";
            JsonNode body = item.get("request").get("body");
            if (body != null && !body.isNull()) {
                String contentType = body.get("mode").asText();
                if ("urlencoded".equalsIgnoreCase(contentType)) {
                    StringBuilder urlEncodedBody = new StringBuilder();
                    body.get("urlencoded").forEach(param -> {
                        if (urlEncodedBody.length() > 0) {
                            urlEncodedBody.append("&");
                        }
                        urlEncodedBody.append(param.get("key").asText()).append("=").append(param.get("value").asText());
                    });
                    requestBody = urlEncodedBody.toString();
                    headers.append("Content-Type: application/x-www-form-urlencoded\r\n");
                } else if ("raw".equalsIgnoreCase(contentType)) {
                    requestBody = body.get("raw").asText();
                    headers.append("Content-Type: application/json\r\n");
                }
            }

            HttpService service = HttpService.httpService(host.split(":")[0], port, protocol.equalsIgnoreCase("https"));
            HttpRequest httpRequest = HttpRequest.httpRequest(service, requestMethod + " " + requestUrl + " HTTP/1.1\r\n" + headers + "\r\n" + requestBody);


            List<HttpHeader> headerss = httpRequest.headers();
            for (HttpHeader header : headerss) {
                headersList.put(header.name(), header.value());
            }

            httpRequestList.add(new Object[]{httpRequest, requestName});
            ui.addRequestToTable(++requestCounter, requestMethod, requestUrl);
        } catch (Exception e) {
            api.logging().logToOutput("ERROR : " + requestMethod + " " + requestName + " - " + e.getMessage());
        }
    }

    private String replaceVariables(String input) {
        if (input == null) return "";
        for (Map.Entry<String, String> entry : variablesMap.entrySet()) {
            input = input.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        extractVariablesFromString(input).forEach(key -> {
            if (!variablesMap.containsKey(key)) {
                undefinedVariables.add(key);
            }
        });
        return input;
    }

    public Map<String, String> getVariablesMap() {
        return variablesMap;
    }

    public Set<String> getUndefinedVariables() {
        return undefinedVariables;
    }

    public List<Object[]> getHttpRequestList() {
        return httpRequestList;
    }

    public Map<String, String> getHeadersList() {
        return headersList;
    }

    public void clearHttpRequestList() {
        httpRequestList.clear();
        requestCounter = 0;
    }
}