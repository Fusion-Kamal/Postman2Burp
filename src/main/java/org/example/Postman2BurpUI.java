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
    private DefaultTableModel tableModel;
    private DefaultTableModel requestTableModel;
    private PostmanProcessor processor;
    private JTable requestTable;

    public Postman2BurpUI(MontoyaApi api) {
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

        // Create buttons for Repeater and Intruder
        JButton repeaterButton = new JButton("Send to Repeater");
        JButton intruderButton = new JButton("Send to Intruder");
        repeaterButton.setEnabled(false);
        intruderButton.setEnabled(false);

        requestTable.getSelectionModel().addListSelectionListener(e -> {
            boolean isSelected = requestTable.getSelectedRow() != -1;
            repeaterButton.setEnabled(isSelected);
            intruderButton.setEnabled(isSelected);
        });

        repeaterButton.addActionListener(e -> sendSelectedRequestsToRepeater());
        intruderButton.addActionListener(e -> sendSelectedRequestsToIntruder());

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
                if (row != -1) {
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
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(scrollPane, BorderLayout.CENTER);

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
            HttpRequest httpRequest = (HttpRequest) processor.getHttpRequestList().get(row)[0];
            String requestName = (String) processor.getHttpRequestList().get(row)[1];
            api.repeater().sendToRepeater(httpRequest, requestName);
        }
    }

    private void sendSelectedRequestsToIntruder() {
        int[] selectedRows = requestTable.getSelectedRows();
        for (int row : selectedRows) {
            HttpRequest httpRequest = (HttpRequest) processor.getHttpRequestList().get(row)[0];
            String requestName = (String) processor.getHttpRequestList().get(row)[1];
            api.intruder().sendToIntruder(httpRequest, requestName);
        }
    }
}