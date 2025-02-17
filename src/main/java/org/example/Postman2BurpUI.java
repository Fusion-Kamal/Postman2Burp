package org.example;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class Postman2BurpUI {
    private JTable variablesTable, requestTable, headersTable;
    private DefaultTableModel variablesTableModel, requestTableModel, headersTableModel;
    private JPanel panel;
    private Postman2BurpActions actions;

    public Postman2BurpUI(MontoyaApi api) {
        panel = new JPanel(new BorderLayout());
        JButton importButton = new JButton("Import Postman Collection");
        // Create table model and table for variables
        variablesTableModel = new DefaultTableModel(new Object[]{"Name", "Value"}, 0);
        variablesTable = new JTable(variablesTableModel);
        JScrollPane variablesScrollPane = new JScrollPane(variablesTable);

        // Create table model and table for requests
        requestTableModel = new DefaultTableModel(new Object[]{"No.", "Method", "URL"}, 0);
        requestTable = new JTable(requestTableModel);
        requestTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane requestScrollPane = new JScrollPane(requestTable);

        // Create table model and table for headers
        headersTableModel = new DefaultTableModel(new Object[]{"Header", "Value"}, 0);
        headersTable = new JTable(headersTableModel);
        JScrollPane headersScrollPane = new JScrollPane(headersTable);

        // Create buttons for Repeater, Active Scan, Passive Scan, Process, and Clear
        JButton repeaterButton = new JButton("Send to Repeater(Ctrl+R)");
        JButton activeScanButton = new JButton("Send to Active Scan(Ctrl+I)");
        JButton passiveScanButton = new JButton("Send to Passive Scan");
        JButton processButton = new JButton("Process Collection");

        repeaterButton.setEnabled(false);
        activeScanButton.setEnabled(false);
        passiveScanButton.setEnabled(false);

        requestTable.getSelectionModel().addListSelectionListener(e -> {
            boolean isSelected = requestTable.getSelectedRow() != -1;
            repeaterButton.setEnabled(isSelected);
            activeScanButton.setEnabled(isSelected);
            passiveScanButton.setEnabled(isSelected);
        });

        // Initialize actions
        actions = new Postman2BurpActions(api, this);

        repeaterButton.addActionListener(e -> actions.sendSelectedRequestsToRepeater());
        activeScanButton.addActionListener(e -> actions.sendSelectedRequestsToActiveScan());
        passiveScanButton.addActionListener(e -> actions.sendSelectedRequestsToPassiveScan());
        processButton.addActionListener(e -> actions.processRequests());

        // Add right-click context menu
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem sendToRepeater = new JMenuItem("Send to Repeater");
        JMenuItem sendToActiveScan = new JMenuItem("Send to Active Scan");
        JMenuItem sendToPassiveScan = new JMenuItem("Send to Passive Scan");
        contextMenu.add(sendToRepeater);
        contextMenu.add(sendToActiveScan);
        contextMenu.add(sendToPassiveScan);

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
        });

        sendToRepeater.addActionListener(e -> actions.sendSelectedRequestsToRepeater());
        sendToActiveScan.addActionListener(e -> actions.sendSelectedRequestsToActiveScan());
        sendToPassiveScan.addActionListener(e -> actions.sendSelectedRequestsToPassiveScan());

//        // Create labels for tables
//        JLabel variablesLabel = new JLabel("VARIABLES");
//        variablesLabel.setFont(variablesLabel.getFont().deriveFont(Font.BOLD));
//        JLabel headersLabel = new JLabel("HEADERS");
//        headersLabel.setFont(headersLabel.getFont().deriveFont(Font.BOLD));
//        JLabel requestsLabel = new JLabel("REQUESTS");
//        requestsLabel.setFont(requestsLabel.getFont().deriveFont(Font.BOLD));

        // Create panel layout
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(repeaterButton);
        buttonPanel.add(activeScanButton);
        buttonPanel.add(passiveScanButton);
        buttonPanel.add(processButton);

        JPanel variablesPanel = new JPanel(new BorderLayout());
//        variablesPanel.add(variablesLabel, BorderLayout.CENTER);
        variablesPanel.add(variablesScrollPane, BorderLayout.CENTER);

        JPanel headersPanel = new JPanel(new BorderLayout());
//        headersPanel.add(headersLabel, BorderLayout.NORTH);
        headersPanel.add(headersScrollPane, BorderLayout.CENTER);

        JPanel requestsPanel = new JPanel(new BorderLayout());
//        requestsPanel.add(requestsLabel, BorderLayout.NORTH);
        requestsPanel.add(requestScrollPane, BorderLayout.CENTER);

        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, variablesPanel, headersPanel);
        leftSplitPane.setDividerLocation(300);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, requestsPanel);
        mainSplitPane.setDividerLocation(300);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        importButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                Postman2Burp.postmanpath = selectedFile.getAbsolutePath();
                api.logging().logToOutput("Selected file: " + Postman2Burp.postmanpath);
                actions.initializeProcessor(selectedFile.getAbsolutePath());
            }
        });

        // Add keyboard shortcuts
        InputMap inputMap = requestTable.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = requestTable.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK), "sendToRepeater");
        actionMap.put("sendToRepeater", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actions.sendSelectedRequestsToRepeater();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK), "sendToActiveScan");
        actionMap.put("sendToActiveScan", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actions.sendSelectedRequestsToActiveScan();
            }
        });

        panel.add(importButton, BorderLayout.NORTH);
        panel.add(mainPanel, BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return panel;
    }

    public DefaultTableModel getVariablesTableModel() {
        return variablesTableModel;
    }

    public DefaultTableModel getRequestTableModel() {
        return requestTableModel;
    }

    public DefaultTableModel getHeadersTableModel() {
        return headersTableModel;
    }

    public JTable getRequestTable() {
        return requestTable;
    }

    public JTable getHeadersTable() {
        return headersTable;
    }

    public void addRequestToTable(int number, String method, String url) {
        requestTableModel.addRow(new Object[]{number, method, url});
    }

    public void updateUndefinedVariablesTable() {
        for (String key : actions.getProcessor().getUndefinedVariables()) {
            variablesTableModel.addRow(new Object[]{key, ""});
        }
    }
}