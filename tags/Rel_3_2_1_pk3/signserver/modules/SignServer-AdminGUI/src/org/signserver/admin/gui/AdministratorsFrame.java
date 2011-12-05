/*************************************************************************
 *                                                                       *
 *  SignServer: The OpenSource Automated Signing Server                  *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.signserver.admin.gui;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import org.jdesktop.application.Action;
import org.jdesktop.application.Task;
import org.signserver.admin.gui.adminws.gen
        .AdminNotAuthorizedException_Exception;
import org.signserver.admin.gui.adminws.gen.WsGlobalConfiguration;
import  org.signserver.common.GlobalConfiguration;

/**
 * Frame for viewing and editing global configuration properties.
 *
 * @author Markus Kilås
 * @version $Id$
 */
@SuppressWarnings("PMD.UnusedFormalParameter")
public class AdministratorsFrame extends javax.swing.JFrame {

    private static final String[] COLUMN_NAMES = new String[] {
        "Certificate serial number",
        "Issuer DN"
    };

    private List<Entry> entries = Collections.emptyList();

    /** Creates new form GlobalConfigurationFrame */
    public AdministratorsFrame() {
        initComponents();

        adminsTable.setModel(new AbstractTableModel() {

            @Override
            public int getRowCount() {
                return entries.size();
            }

            @Override
            public int getColumnCount() {
                return COLUMN_NAMES.length;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                final Object result;
                if (columnIndex == 0) {
                    result = entries.get(rowIndex).getCertSerialNo();
                } else if (columnIndex == 1) {
                    result = entries.get(rowIndex).getIssuerDN();
                } else {
                    result = null;
                }
                return result;
            }

            @Override
            public String getColumnName(int column) {
                return COLUMN_NAMES[column];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

        });

        adminsTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {

            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    final boolean enable
                            = adminsTable.getSelectedRowCount() == 1;
                    editButton.setEnabled(enable);
                    removeButton.setEnabled(enable);
                }
            }
        });
        refreshButton.doClick();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        editPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        editCertSerialNoTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        editIssuerDNTextField = new javax.swing.JTextField();
        jScrollPane6 = new javax.swing.JScrollPane();
        adminsTable = new javax.swing.JTable();
        addButton = new javax.swing.JButton();
        editButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        refreshButton = new javax.swing.JButton();

        editPanel.setName("editPanel"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.signserver.admin.gui.SignServerAdminGUIApplication.class).getContext().getResourceMap(AdministratorsFrame.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        editCertSerialNoTextField.setEditable(false);
        editCertSerialNoTextField.setText(resourceMap.getString("editCertSerialNoTextField.text")); // NOI18N
        editCertSerialNoTextField.setName("editCertSerialNoTextField"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        editIssuerDNTextField.setText(resourceMap.getString("editIssuerDNTextField.text")); // NOI18N
        editIssuerDNTextField.setName("editIssuerDNTextField"); // NOI18N

        javax.swing.GroupLayout editPanelLayout = new javax.swing.GroupLayout(editPanel);
        editPanel.setLayout(editPanelLayout);
        editPanelLayout.setHorizontalGroup(
            editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(editCertSerialNoTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                    .addComponent(editIssuerDNTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE))
                .addContainerGap())
        );
        editPanelLayout.setVerticalGroup(
            editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editCertSerialNoTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editIssuerDNTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setLocationByPlatform(true);
        setName("Form"); // NOI18N

        jScrollPane6.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane6.setName("jScrollPane6"); // NOI18N

        adminsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Certificate serial number", "Issuer DN"
            }
        ));
        adminsTable.setName("adminsTable"); // NOI18N
        jScrollPane6.setViewportView(adminsTable);

        addButton.setText(resourceMap.getString("addButton.text")); // NOI18N
        addButton.setName("addButton"); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        editButton.setText(resourceMap.getString("editButton.text")); // NOI18N
        editButton.setEnabled(false);
        editButton.setName("editButton"); // NOI18N
        editButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editButtonActionPerformed(evt);
            }
        });

        removeButton.setText(resourceMap.getString("removeButton.text")); // NOI18N
        removeButton.setEnabled(false);
        removeButton.setName("removeButton"); // NOI18N
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(org.signserver.admin.gui.SignServerAdminGUIApplication.class).getContext().getActionMap(AdministratorsFrame.class, this);
        refreshButton.setAction(actionMap.get("reloadGlobalConfiguration")); // NOI18N
        refreshButton.setText(resourceMap.getString("refreshButton.text")); // NOI18N
        refreshButton.setFocusable(false);
        refreshButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        refreshButton.setName("refreshButton"); // NOI18N
        refreshButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(refreshButton);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 770, Short.MAX_VALUE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 660, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(addButton)
                            .addComponent(editButton)
                            .addComponent(removeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addButton, editButton, removeButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(addButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(editButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeButton))
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jButton1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        try {
            editCertSerialNoTextField.setText("");
            editCertSerialNoTextField.setEditable(true);
            editIssuerDNTextField.setText("");

            final int res = JOptionPane.showConfirmDialog(this, editPanel,
                    "Add property", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.OK_OPTION) {
                final String certSerialNo = editCertSerialNoTextField.getText();
                final String issuerDN = editIssuerDNTextField.getText();

                final List<Entry> admins = parseAdmins();
                final Entry newEntry = new Entry(certSerialNo, issuerDN);
                if (admins.contains(newEntry)) {
                    JOptionPane.showMessageDialog(this,
                            "The administrator already existed");
                } else {
                    admins.add(newEntry);

                    SignServerAdminGUIApplication.getAdminWS()
                            .setGlobalProperty(GlobalConfiguration.SCOPE_GLOBAL,
                            "WSADMINS",
                            serializeAdmins(admins));
                }
                refreshButton.doClick();
            }
        } catch (AdminNotAuthorizedException_Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Authorization denied", JOptionPane.ERROR_MESSAGE);
        }
}//GEN-LAST:event_addButtonActionPerformed

    private void editButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_editButtonActionPerformed
        try {
            final int row = adminsTable.getSelectedRow();

            if (row != -1) {
                final Entry oldEntry
                        = new Entry(entries.get(row).getCertSerialNo(),
                            entries.get(row).getIssuerDN());

                editCertSerialNoTextField.setText(oldEntry.getCertSerialNo());
                editCertSerialNoTextField.setEditable(true);
                editIssuerDNTextField.setText(oldEntry.getIssuerDN());

                final int res = JOptionPane.showConfirmDialog(this, editPanel,
                        "Edit administrator", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                if (res == JOptionPane.OK_OPTION) {
                    final Entry newEntry
                        = new Entry(editCertSerialNoTextField.getText(), 
                            editIssuerDNTextField.getText());

                    final List<Entry> admins = parseAdmins();

                    if (!admins.contains(oldEntry)) {
                        JOptionPane.showMessageDialog(this,
                                "No such administrator");
                    } else {
                        admins.remove(oldEntry);
                        
                        if (admins.contains(newEntry)) {
                            JOptionPane.showMessageDialog(this,
                            "The administrator already existed");
                        } else {
                            admins.add(newEntry);
                        }
                        SignServerAdminGUIApplication.getAdminWS()
                            .setGlobalProperty(GlobalConfiguration.SCOPE_GLOBAL,
                            "WSADMINS",
                            serializeAdmins(admins));
                    }
                    refreshButton.doClick();
                }
            }
        } catch (AdminNotAuthorizedException_Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Authorization denied", JOptionPane.ERROR_MESSAGE);
        }
}//GEN-LAST:event_editButtonActionPerformed

    private void removeButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        try {
            final int row = adminsTable.getSelectedRow();

            if (row != -1) {
                final int res = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to remove the administrator?",
                        "Remove administrator", JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (res == JOptionPane.YES_OPTION) {
                    final Entry oldEntry
                        = new Entry(entries.get(row).getCertSerialNo(),
                            entries.get(row).getIssuerDN());

                    final List<Entry> admins = parseAdmins();

                    if (!admins.contains(oldEntry)) {
                        JOptionPane.showMessageDialog(this,
                                "No such administrator");
                    } else {
                        admins.remove(oldEntry);

                        SignServerAdminGUIApplication.getAdminWS()
                            .setGlobalProperty(GlobalConfiguration.SCOPE_GLOBAL,
                            "WSADMINS",
                            serializeAdmins(admins));
                    }
                    refreshButton.doClick();
                }
            }
        } catch (AdminNotAuthorizedException_Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Authorization denied", JOptionPane.ERROR_MESSAGE);
        }
}//GEN-LAST:event_removeButtonActionPerformed

    private void jButton1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        dispose();
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AdministratorsFrame().setVisible(true);
            }
        });
    }

    @Action(block = Task.BlockingScope.WINDOW)
    public Task reloadGlobalConfiguration() {
        return new ReloadGlobalConfigurationTask(org.jdesktop.application.Application.getInstance(org.signserver.admin.gui.SignServerAdminGUIApplication.class));
    }

    private class ReloadGlobalConfigurationTask extends Task<List<Entry>, Void> {
        ReloadGlobalConfigurationTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to ReloadGlobalConfigurationTask fields, here.
            super(app);
        }
        @Override protected List<Entry> doInBackground() {
            // Your Task's code here.  This method runs
            // on a background thread, so don't reference
            // the Swing GUI from here.
            List<Entry> result = null;

            try {
               result = parseAdmins();
            } catch (final AdminNotAuthorizedException_Exception ex) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(
                                AdministratorsFrame.this, ex.getMessage(),
                        "Authorization denied", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
            // return your result
            return result;
        }
        @Override protected void succeeded(List<Entry> result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().

            if (result == null) {
                result = Collections.emptyList();
            }
            entries = result;
            adminsTable.revalidate();
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JTable adminsTable;
    private javax.swing.JButton editButton;
    private javax.swing.JTextField editCertSerialNoTextField;
    private javax.swing.JTextField editIssuerDNTextField;
    private javax.swing.JPanel editPanel;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton refreshButton;
    private javax.swing.JButton removeButton;
    // End of variables declaration//GEN-END:variables

    private List<Entry> parseAdmins()
            throws AdminNotAuthorizedException_Exception {
        String admins = null;

        for (WsGlobalConfiguration.Config.Entry entry
                    : SignServerAdminGUIApplication.getAdminWS()
                        .getGlobalConfiguration().getConfig().getEntry()) {
            if (entry.getKey().equals(GlobalConfiguration.SCOPE_GLOBAL
                    + "WSADMINS")) {
                admins = (String) entry.getValue();
            }
        }

        final List<Entry> entries = new LinkedList<Entry>();

        if (admins != null) {
            if (admins.contains(";")) {
                for (String entry : admins.split(";")) {
                    final String[] parts = entry.split(",", 2);
                    entries.add(new Entry(parts[0], parts[1]));
                }
            }
        }
        return entries;
    }

    private static String serializeAdmins(final List<Entry> entries) {
        final StringBuilder buff = new StringBuilder();
        for (Entry entry : entries) {
            buff.append(entry.getCertSerialNo());
            buff.append(",");
            buff.append(entry.getIssuerDN());
            buff.append(";");
        }
        return buff.toString();
    }

    private static class Entry {
        private String certSerialNo;
        private String issuerDN;

        public Entry(String certSerialNo, String issuerDN) {
            this.certSerialNo = certSerialNo;
            this.issuerDN = issuerDN;
        }

        public String getCertSerialNo() {
            return certSerialNo;
        }

        public String getIssuerDN() {
            return issuerDN;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Entry other = (Entry) obj;
            if ((this.certSerialNo == null) ? (other.certSerialNo != null) : !this.certSerialNo.equals(other.certSerialNo)) {
                return false;
            }
            if ((this.issuerDN == null) ? (other.issuerDN != null) : !this.issuerDN.equals(other.issuerDN)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + (this.certSerialNo != null ? this.certSerialNo.hashCode() : 0);
            hash = 89 * hash + (this.issuerDN != null ? this.issuerDN.hashCode() : 0);
            return hash;
        }

    }
}
