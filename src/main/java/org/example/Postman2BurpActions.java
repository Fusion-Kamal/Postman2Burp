package org.example;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.AuditConfiguration;
import burp.api.montoya.scanner.BuiltInAuditConfiguration;

import javax.swing.*;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Postman2BurpActions {
    private MontoyaApi api;
    private Postman2BurpUI ui;
    private PostmanProcessor processor;

    public Postman2BurpActions(MontoyaApi api, Postman2BurpUI ui) {
        this.api = api;
        this.ui = ui;
    }

    public void initializeProcessor(String postmanPath) {
        processor = new PostmanProcessor(api, postmanPath, ui);
        processor.identifyVariables();

        SwingUtilities.invokeLater(() -> {
            ui.getVariablesTableModel().setRowCount(0);
            for (Map.Entry<String, String> entry : processor.getVariablesMap().entrySet()) {
                ui.getVariablesTableModel().addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
            ui.updateUndefinedVariablesTable();
        });
    }

    public PostmanProcessor getProcessor() {
        return processor;
    }

    public void sendSelectedRequestsToRepeater() {
        int[] selectedRows = ui.getRequestTable().getSelectedRows();
        for (int row : selectedRows) {
            Object[] requestData = processor.getHttpRequestList().get(row);
            HttpRequest httpRequest = (HttpRequest) requestData[0];
            String requestName = (String) requestData[1];
            api.repeater().sendToRepeater(httpRequest, requestName);
        }
    }

    public void sendSelectedRequestsToActiveScan() {
        int[] selectedRows = ui.getRequestTable().getSelectedRows();
        for (int row : selectedRows) {
            Object[] requestData = processor.getHttpRequestList().get(row);
            HttpRequest httpRequest = (HttpRequest) requestData[0];
            api.scanner()
                    .startAudit(AuditConfiguration.auditConfiguration(BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS))
                    .addRequest(httpRequest);
        }
    }

    public void sendSelectedRequestsToPassiveScan() {
        int[] selectedRows = ui.getRequestTable().getSelectedRows();
        for (int row : selectedRows) {
            Object[] requestData = processor.getHttpRequestList().get(row);
            HttpRequest httpRequest = (HttpRequest) requestData[0];
            api.scanner()
                    .startAudit(AuditConfiguration.auditConfiguration(BuiltInAuditConfiguration.LEGACY_PASSIVE_AUDIT_CHECKS))
                    .addRequest(httpRequest);
        }
    }

    public void processRequests() {
        clearRequests();
        for (int i = 0; i < ui.getVariablesTableModel().getRowCount(); i++) {
            String key = (String) ui.getVariablesTableModel().getValueAt(i, 0);
            String value = (String) ui.getVariablesTableModel().getValueAt(i, 1);
            processor.getVariablesMap().put(key, value);
        }
        processor.processPostman();
        SwingUtilities.invokeLater(() -> {
            ui.getRequestTableModel().setRowCount(0);
            for (Object[] requestData : processor.getHttpRequestList()) {
                HttpRequest httpRequest = (HttpRequest) requestData[0];
                String requestName = (String) requestData[1];
                ui.getRequestTableModel().addRow(new Object[]{ui.getRequestTableModel().getRowCount() + 1, httpRequest.method(), httpRequest.url()});
            }
            logSummary();
        });
    }

    private void logSummary() {
        int totalRequests = processor.getHttpRequestList().size();
        Map<String, Long> methodCounts = processor.getHttpRequestList().stream()
                .map(req -> ((HttpRequest) req[0]).method().toUpperCase())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        api.logging().logToOutput("***      Summary    ***");
        api.logging().logToOutput("Total requests: " + totalRequests);
        methodCounts.forEach((method, count) -> api.logging().logToOutput(method + " requests: " + count));
        api.logging().logToOutput("-----------------------------------------------------\n");
    }

    private void clearRequests() {
        ui.getRequestTableModel().setRowCount(0);
        processor.clearHttpRequestList();
    }
}