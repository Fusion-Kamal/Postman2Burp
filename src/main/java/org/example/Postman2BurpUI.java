package org.example;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Map;


public class Postman2BurpUI {
    private JTable variablesTable;
    private DefaultTableModel variablesTableModel;
    private PostmanProcessor processor;
    private JTable requestTable;
    private DefaultTableModel requestTableModel;
    private MontoyaApi api;
    private JPanel panel;

    public Postman2BurpUI(MontoyaApi api) {
        this.api = api;
        panel = new JPanel(new BorderLayout());
        JButton importButton = new JButton("Import Postman Collection");
        importButton.setPreferredSize(new Dimension(200, 30));

        // Create table model and table for variables
        variablesTableModel = new DefaultTableModel(new Object[]{"Variable", "Value"}, 0);
        variablesTable = new JTable(variablesTableModel);
        JScrollPane variablesScrollPane = new JScrollPane(variablesTable);

        // Create table model and table for requests
        requestTableModel = new DefaultTableModel(new Object[]{"No.", "Method", "URL"}, 0);
        requestTable = new JTable(requestTableModel);
        requestTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane requestScrollPane = new JScrollPane(requestTable);

        // Create buttons for Repeater, Intruder, Process, and Clear
        JButton repeaterButton = new JButton("Send to Repeater");
        JButton intruderButton = new JButton("Send to Intruder");
        JButton processButton = new JButton("Process Collection");
        repeaterButton.setEnabled(false);
        intruderButton.setEnabled(false);

        requestTable.getSelectionModel().addListSelectionListener(e -> {
            boolean isSelected = requestTable.getSelectedRow() != -1;
            repeaterButton.setEnabled(isSelected);
            intruderButton.setEnabled(isSelected);
        });

        repeaterButton.addActionListener(e -> sendSelectedRequestsToRepeater());
        intruderButton.addActionListener(e -> sendSelectedRequestsToIntruder());
        processButton.addActionListener(e -> processRequests()); ;

        // Add right-click context menu
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem sendToRepeater = new JMenuItem("Send to Repeater");
        JMenuItem sendToIntruder = new JMenuItem("Send to Intruder");
        contextMenu.add(sendToRepeater);
        contextMenu.add(sendToIntruder);

        requestTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            private void showContextMenu(MouseEvent e) {
                int row = requestTable.rowAtPoint(e.getPoint());
                if (!requestTable.isRowSelected(row)) {
                }
                contextMenu.show(requestTable, e.getX(), e.getY());
            }
        });

        sendToRepeater.addActionListener(e -> sendSelectedRequestsToRepeater());
        sendToIntruder.addActionListener(e -> sendSelectedRequestsToIntruder());

        // Create panel layout
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(repeaterButton);
        buttonPanel.add(intruderButton);
        buttonPanel.add(processButton);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, variablesScrollPane, requestScrollPane);
        splitPane.setDividerLocation(300);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        importButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                Postman2Burp.postmanpath = selectedFile.getAbsolutePath();
                api.logging().logToOutput("Selected file: " + Postman2Burp.postmanpath);
                processor = new PostmanProcessor(api, Postman2Burp.postmanpath, Postman2BurpUI.this);
                processor.identifyVariables();

                SwingUtilities.invokeLater(() -> {
                    variablesTableModel.setRowCount(0);
                    for (Map.Entry<String, String> entry : processor.getVariablesMap().entrySet()) {
                        variablesTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
                    }
                    updateUndefinedVariablesTable();
                });
            }
        });

        panel.add(importButton, BorderLayout.NORTH);
        panel.add(mainPanel, BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void addRequestToTable(int number, String method, String url) {
        requestTableModel.addRow(new Object[]{number, method, url});
    }

    private void sendSelectedRequestsToRepeater() {
        int[] selectedRows = requestTable.getSelectedRows();
        for (int row : selectedRows) {
            Object[] requestData = processor.getHttpRequestList().get(row);
            HttpRequest httpRequest = (HttpRequest) requestData[0];
            String requestName = (String) requestData[1];
            api.repeater().sendToRepeater(httpRequest, requestName);
        }
    }

    private void sendSelectedRequestsToIntruder() {
        int[] selectedRows = requestTable.getSelectedRows();
        for (int row : selectedRows) {
            Object[] requestData = processor.getHttpRequestList().get(row);
            HttpRequest httpRequest = (HttpRequest) requestData[0];
            String requestName = (String) requestData[1];
            api.intruder().sendToIntruder(httpRequest, requestName);
        }
    }

    private void processRequests() {
        clearRequests();
        for (int i = 0; i < variablesTableModel.getRowCount(); i++) {
            String key = (String) variablesTableModel.getValueAt(i, 0);
            String value = (String) variablesTableModel.getValueAt(i, 1);
            processor.getVariablesMap().put(key, value);
        }
        processor.processPostman();
        SwingUtilities.invokeLater(() -> {
            requestTableModel.setRowCount(0);
            for (Object[] requestData : processor.getHttpRequestList()) {
                HttpRequest httpRequest = (HttpRequest) requestData[0];
                String requestName = (String) requestData[1];
                requestTableModel.addRow(new Object[]{requestTableModel.getRowCount() + 1, httpRequest.method(), httpRequest.url()});
            }
        });
    }

    private void clearRequests() {
        requestTableModel.setRowCount(0);
        processor.clearHttpRequestList();
    }

    public void updateUndefinedVariablesTable() {
        for (String key : processor.getUndefinedVariables()) {
            variablesTableModel.addRow(new Object[]{key, ""});
        }
    }
}