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
    private JTable variablesTable, requestTable;
    private DefaultTableModel variablesTableModel, requestTableModel;
    private JPanel panel;
    private Postman2BurpActions actions;

    public Postman2BurpUI(MontoyaApi api) {
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

        sendToActiveScan.addActionListener(e -> actions.sendSelectedRequestsToActiveScan());
        sendToPassiveScan.addActionListener(e -> actions.sendSelectedRequestsToPassiveScan());

        // Create panel layout
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(repeaterButton);
        buttonPanel.add(activeScanButton);
        buttonPanel.add(passiveScanButton);
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

    public JTable getRequestTable() {
        return requestTable;
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