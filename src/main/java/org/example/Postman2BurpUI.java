// src/main/java/org/example/Postman2BurpUI.java
package org.example;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;

public class Postman2BurpUI {
    private JPanel panel;
    private MontoyaApi api;
    private JTable variablesTable;
    private DefaultTableModel tableModel;
    private DefaultTableModel requestTableModel;
    private PostmanProcessor processor;

    public Postman2BurpUI(MontoyaApi api) {
        this.api = api;
        panel = new JPanel(new BorderLayout());
        JButton importButton = new JButton("Import Postman Collection");
        importButton.setPreferredSize(new Dimension(200, 30)); // Set the size of the button

        // Create table model and table for variables
        tableModel = new DefaultTableModel(new Object[]{"Variable", "Value"}, 0);
        variablesTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(variablesTable);

        // Create table model and table for requests
        requestTableModel = new DefaultTableModel(new Object[]{"No.","Method","URL"}, 0);
        JTable requestTable = new JTable(requestTableModel);
        JScrollPane requestScrollPane = new JScrollPane(requestTable);

        // Create buttons for Repeater and Intruder
        JButton repeaterButton = new JButton("Send to Repeater");
        JButton intruderButton = new JButton("Send to Intruder");

        // Add action listeners for buttons
        repeaterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<Object[]> httpRequestList = processor.getHttpRequestList();
                for (Object[] request : httpRequestList) {
                    HttpRequest httpRequest = (HttpRequest) request[0];
                    String requestName = (String) request[1];
                    api.repeater().sendToRepeater(httpRequest, requestName);
                }
            }
        });

        intruderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<Object[]> httpRequestList = processor.getHttpRequestList();
                for (Object[] request : httpRequestList) {
                    HttpRequest httpRequest = (HttpRequest) request[0];
                    String requestName = (String) request[1];
                    api.intruder().sendToIntruder(httpRequest, requestName);
                }
            }
        });

        // Create right panel and add components
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(requestScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(repeaterButton);
        buttonPanel.add(intruderButton);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Create left panel and add components
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(300); // Set initial divider location

        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    Postman2Burp.postmanpath = selectedFile.getAbsolutePath();
                    api.logging().logToOutput("Selected file: " + Postman2Burp.postmanpath);

                    processor = new PostmanProcessor(api, Postman2Burp.postmanpath, Postman2BurpUI.this);
                    processor.processPostman();

                    try {
                        Thread.sleep(2000); // Wait for processing to complete
                        Map<String, String> variablesMap = processor.getVariablesMap();
                        SwingUtilities.invokeLater(() -> {
                            tableModel.setRowCount(0); // Clear existing rows
                            for (Map.Entry<String, String> entry : variablesMap.entrySet()) {
                                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
                            }
                        });
                    } catch (InterruptedException ex) {
                        api.logging().logToOutput("Error updating UI: " + ex.getMessage());
                    }
                }
            }
        });

        panel.add(importButton, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void addRequestToTable(int number, String method , String url) {
        requestTableModel.addRow(new Object[]{number,method, url});
    }
}