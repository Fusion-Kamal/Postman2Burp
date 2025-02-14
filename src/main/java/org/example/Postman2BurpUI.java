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
    private JPanel panel;
    private MontoyaApi api;
    private JTable variablesTable;
    private DefaultTableModel requestTableModel;
    private DefaultTableModel undefinedVariablesTableModel;
    private PostmanProcessor processor;
    private JTable requestTable;
    private JTable undefinedVariablesTable;
    private DefaultTableModel tableModel;

    this.api = api;
    panel = new JPanel(new BorderLayout());
    JButton importButton = new JButton("Import Postman Collection");
    importButton.setPreferredSize(new Dimension(200, 30));

    // Create table model and table for variables
    tableModel = new DefaultTableModel(new Object[]{"Variable", "Value"}, 0);
    variablesTable = new JTable(tableModel);
    JScrollPane scrollPane = new JScrollPane(variablesTable);

    // Create table model and table for requests
    requestTableModel = new DefaultTableModel(new Object[]{"No.", "Method", "URL"}, 0);
    requestTable = new JTable(requestTableModel);
        requestTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    JScrollPane requestScrollPane = new JScrollPane(requestTable);

    // Create table model and table for undefined variables
    undefinedVariablesTableModel = new DefaultTableModel(new Object[]{"Variable", "Value"}, 0);
    undefinedVariablesTable = new JTable(undefinedVariablesTableModel);
    JScrollPane undefinedVariablesScrollPane = new JScrollPane(undefinedVariablesTable);

    // Create buttons for Repeater, Intruder, Reprocess, and Clear
    JButton repeaterButton = new JButton("Send to Repeater");
    JButton intruderButton = new JButton("Send to Intruder");
    JButton reprocessButton = new JButton("Reprocess Requests");
    JButton clearButton = new JButton("Clear Requests");
        repeaterButton.setEnabled(false);
        intruderButton.setEnabled(false);

        requestTable.getSelectionModel().addListSelectionListener(e -> {
        boolean isSelected = requestTable.getSelectedRow() != -1;
        repeaterButton.setEnabled(isSelected);
        intruderButton.setEnabled(isSelected);
    });

        repeaterButton.addActionListener(e -> sendSelectedRequestsToRepeater());
        intruderButton.addActionListener(e -> sendSelectedRequestsToIntruder());
        reprocessButton.addActionListener(e -> reprocessRequests());
        clearButton.addActionListener(e -> clearRequests());

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
                requestTable.addRowSelectionInterval(row, row);
            }
            contextMenu.show(requestTable, e.getX(), e.getY());
        }
    }
});

        sendToRepeater.addActionListener(e -> sendSelectedRequestsToRepeater());
        sendToIntruder.addActionListener(e -> sendSelectedRequestsToIntruder());

// Create panel layout
JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(requestScrollPane, BorderLayout.CENTER);

JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(repeaterButton);
        buttonPanel.add(intruderButton);
        buttonPanel.add(reprocessButton);
        buttonPanel.add(clearButton);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        leftPanel.add(undefinedVariablesScrollPane, BorderLayout.SOUTH);

JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(300);

        importButton.addActionListener(e -> {
JFileChooser fileChooser = new JFileChooser();
int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
File selectedFile = fileChooser.getSelectedFile();
Postman2Burp.postmanpath = selectedFile.getAbsolutePath();
                api.logging().logToOutput("Selected file: " + Postman2Burp.postmanpath);
processor = new PostmanProcessor(api, Postman2Burp.postmanpath, Postman2BurpUI.this);
                processor.processPostman();

                SwingUtilities.invokeLater(() -> {
        tableModel.setRowCount(0);
                    for (Map.Entry<String, String> entry : processor.getVariablesMap().entrySet()) {
        tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
updateUndefinedVariablesTable();
                });
                        }
                        });

                        panel.add(importButton, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
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

private void reprocessRequests() {
    clearRequests();
    for (int i = 0; i < undefinedVariablesTableModel.getRowCount(); i++) {
        String key = (String) undefinedVariablesTableModel.getValueAt(i, 0);
        String value = (String) undefinedVariablesTableModel.getValueAt(i, 1);
        processor.getVariablesMap().put(key, value);
    }
    processor.processPostman();
    SwingUtilities.invokeLater(() -> {
        tableModel.setRowCount(0);
        for (Map.Entry<String, String> entry : processor.getVariablesMap().entrySet()) {
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
        updateUndefinedVariablesTable();
    });
}

private void clearRequests() {
    requestTableModel.setRowCount(0);
    processor.clearHttpRequestList();
}

public void updateUndefinedVariablesTable() {
    undefinedVariablesTableModel.setRowCount(0);
    for (String key : processor.getUndefinedVariables()) {
        undefinedVariablesTableModel.addRow(new Object[]{key, ""});
    }
}
}