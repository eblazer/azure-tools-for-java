/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsPageTableElement;
import com.microsoft.applicationinsights.preference.ApplicationInsightsPageTableElements;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.forms.ManageSubscriptionPanel;
import com.microsoft.intellij.ui.components.DefaultDialogWrapper;
import com.microsoft.intellij.util.MethodUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.wizards.WizardCacheManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class AppInsightsMngmtPanel implements AzureAbstractConfigurablePanel {
    private JPanel contentPane;
    private JButton importInstrumentationKeysFromButton;
    private JTable insightsTable;
    private JButton newButton;
    private JButton removeButton;
    private JButton detailsButton;
    private JButton addButton;
    private Project myProject;
    private static final String DISPLAY_NAME = "Application Insights";

    public AppInsightsMngmtPanel(Project project) {
        this.myProject = project;
        init();
    }

    protected void init() {
        insightsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        AzureSettings.getSafeInstance(myProject).loadAppInsights();
        loadInfoFirstTime();
        insightsTable.setModel(new InsightsTableModel(getTableContent()));
        for (int i = 0; i < insightsTable.getColumnModel().getColumnCount(); i++) {
            TableColumn each = insightsTable.getColumnModel().getColumn(i);
            each.setPreferredWidth(InsightsTableModel.getColumnWidth(i, 450));
        }
        importInstrumentationKeysFromButton.addActionListener(importButtonListener());
        newButton.addActionListener(newButtonListener());
        removeButton.addActionListener(removeButtonListener());
        addButton.addActionListener(addButtonListener());
        detailsButton.addActionListener(detailsButtonListener());

        detailsButton.setEnabled(false);
        removeButton.setEnabled(false);

        insightsTable.getSelectionModel().addListSelectionListener(createAccountsTableListener());
    }

    @Override
    public boolean doOKAction() {
        return true;
    }

    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    public JComponent getPanel() {
        return contentPane;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getSelectedValue() {
        int selectedIndex = insightsTable.getSelectedRow();
        if (selectedIndex >= 0) {
            return ((InsightsTableModel) insightsTable.getModel()).getKeyAtIndex(selectedIndex);
        }
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void reset() {
    }

    private void loadInfoFirstTime() {
        try {
            AzureManager manager = AzureManagerImpl.getManager(myProject);
            List<Subscription> subList = manager.getSubscriptionList();
            if (subList.size() > 0) {
                if (!AzureSettings.getSafeInstance(myProject).isAppInsightsLoaded()) {
                    if (manager.authenticated()) {
                        // authenticated using AD. Proceed for updating application insights registry.
                        updateApplicationInsightsResourceRegistry(subList, myProject);
                    } else {
                        // imported publish settings file. just show manually added list from preferences
                        // Neither clear subscription list nor show sign in dialog as user may just want to add key manually.
                        keeepManuallyAddedList(myProject);
                    }
                } else {
                    // show list from preferences - getTableContent() does it. So nothing to handle here
                }
            } else {
                // just show manually added list from preferences
                keeepManuallyAddedList(myProject);
            }
        } catch(Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
    }

    private void createSubscriptionDialog(boolean invokeSignIn) {
        try {
            final ManageSubscriptionPanel manageSubscriptionPanel = new ManageSubscriptionPanel(myProject, false);
            final DefaultDialogWrapper subscriptionsDialog = new DefaultDialogWrapper(myProject, manageSubscriptionPanel) {
                @Nullable
                @Override
                protected JComponent createSouthPanel() {
                    return null;
                }
                @Override
                protected JComponent createTitlePane() {
                    return null;
                }
            };
            manageSubscriptionPanel.setDialog(subscriptionsDialog);
            JButton signInBtn = manageSubscriptionPanel.getSignInButton();
            if (invokeSignIn && signInBtn != null && signInBtn.getText().equalsIgnoreCase("Sign In...")) {
                signInBtn.doClick();
            }
            subscriptionsDialog.show();
            List<Subscription> subList = AzureManagerImpl.getManager(myProject).getSubscriptionList();
            if (subList.size() == 0) {
                keeepManuallyAddedList(myProject);
            } else {
                updateApplicationInsightsResourceRegistry(subList, myProject);
            }
            ((InsightsTableModel) insightsTable.getModel()).setResources(getTableContent());
            ((InsightsTableModel) insightsTable.getModel()).fireTableDataChanged();
        } catch(Exception ex) {
            PluginUtil.displayErrorDialog(message("errTtl"), message("importErrMsg"));
        }
    }

    private List<ApplicationInsightsPageTableElement> getTableContent() {
        AzureSettings.getSafeInstance(myProject).loadAppInsights();
        List<ApplicationInsightsResource> resourceList = ApplicationInsightsResourceRegistry.getAppInsightsResrcList();
        List<ApplicationInsightsPageTableElement> tableRowElements = new ArrayList<ApplicationInsightsPageTableElement>();
        for (ApplicationInsightsResource resource : resourceList) {
            if (resource != null) {
                ApplicationInsightsPageTableElement ele = new ApplicationInsightsPageTableElement();
                ele.setResourceName(resource.getResourceName());
                ele.setInstrumentationKey(resource.getInstrumentationKey());
                tableRowElements.add(ele);
            }
        }
        ApplicationInsightsPageTableElements elements = new ApplicationInsightsPageTableElements();
        elements.setElements(tableRowElements);
        return elements.getElements();
    }

    private ActionListener importButtonListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    AzureManager manager = AzureManagerImpl.getManager(myProject);
                    List<Subscription> subList = manager.getSubscriptionList();
                    if (subList.size() > 0) {
                        if (manager.authenticated()) {
                            createSubscriptionDialog(false);
                        } else {
                            // imported publish settings file.
                            manager.clearImportedPublishSettingsFiles();
                            WizardCacheManager.clearSubscriptions();
                            createSubscriptionDialog(true);
                        }
                    } else {
                        createSubscriptionDialog(true);
                    }
                } catch(Exception ex) {
                    AzurePlugin.log(ex.getMessage(), ex);
                }
            }
        };
    }

    public static void updateApplicationInsightsResourceRegistry(List<Subscription> subList, Project project) throws IOException, RestOperationException, AzureCmdException {
        for (Subscription sub : subList) {
            AzureManager manager = AzureManagerImpl.getManager(project);
            // fetch resources available for particular subscription
            List<Resource> resourceList = manager.getApplicationInsightsResources(sub.getId());

            // Removal logic
            List<ApplicationInsightsResource> registryList = ApplicationInsightsResourceRegistry.
                    getResourceListAsPerSub(sub.getId());
            List<ApplicationInsightsResource> importedList = ApplicationInsightsResourceRegistry.
                    prepareAppResListFromRes(resourceList, sub);
            List<String> inUsekeyList = MethodUtils.getInUseInstrumentationKeys(project);
            for (ApplicationInsightsResource registryRes : registryList) {
                if (!importedList.contains(registryRes)) {
                    String key = registryRes.getInstrumentationKey();
                    int index = ApplicationInsightsResourceRegistry.getResourceIndexAsPerKey(key);
                    if (inUsekeyList.contains(key)) {
						/*
						 * key is used by project but not present in cloud,
						 * so make it as manually added resource and not imported.
						 */
                        ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(
                                key, key, message("unknown"), message("unknown"),
                                message("unknown"), message("unknown"), false);
                        ApplicationInsightsResourceRegistry.getAppInsightsResrcList().set(index, resourceToAdd);
                    } else {
                        // key is not used by any project then delete it.
                        ApplicationInsightsResourceRegistry.getAppInsightsResrcList().remove(index);
                    }
                }
            }

            // Addition logic
            List<ApplicationInsightsResource> list = ApplicationInsightsResourceRegistry.
                    getAppInsightsResrcList();
            for (Resource resource : resourceList) {
                ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(
                        resource.getName(), resource.getInstrumentationKey(),
                        sub.getName(), sub.getId(),
                        resource.getLocation(), resource.getResourceGroup(), true);
                if (list.contains(resourceToAdd)) {
                    int index = ApplicationInsightsResourceRegistry.
                            getResourceIndexAsPerKey(resource.getInstrumentationKey());
                    ApplicationInsightsResource objectFromRegistry = list.get(index);
                    if (!objectFromRegistry.isImported()) {
                        ApplicationInsightsResourceRegistry.
                                getAppInsightsResrcList().set(index, resourceToAdd);
                    }
                } else {
                    ApplicationInsightsResourceRegistry.
                            getAppInsightsResrcList().add(resourceToAdd);
                }
            }
        }
        AzureSettings.getSafeInstance(project).saveAppInsights();
        AzureSettings.getSafeInstance(project).setAppInsightsLoaded(true);
    }

    public static void keeepManuallyAddedList(Project project) {
        List<ApplicationInsightsResource> addedList = ApplicationInsightsResourceRegistry.getAddedResources();
        List<String> addedKeyList = new ArrayList<String>();
        for (ApplicationInsightsResource res : addedList) {
            addedKeyList.add(res.getInstrumentationKey());
        }
        List<String> inUsekeyList = MethodUtils.getInUseInstrumentationKeys(project);
        for (String inUsekey : inUsekeyList) {
            if (!addedKeyList.contains(inUsekey)) {
                ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(
                        inUsekey, inUsekey, message("unknown"), message("unknown"),
                        message("unknown"), message("unknown"), false);
                addedList.add(resourceToAdd);
            }
        }
        ApplicationInsightsResourceRegistry.setAppInsightsResrcList(addedList);
        AzureSettings.getSafeInstance(project).saveAppInsights();
        AzureSettings.getSafeInstance(project).setAppInsightsLoaded(true);
    }

    public static void refreshDataForDialog() {
        try {
            Project project = PluginUtil.getSelectedProject();
            AzureManager manager = AzureManagerImpl.getManager(project);
            List<Subscription> subList = manager.getSubscriptionList();
            if (subList.size() > 0 && !AzureSettings.getSafeInstance(project).isAppInsightsLoaded()) {
                if (manager.authenticated()) {
                    // authenticated using AD. Proceed for updating application insights registry.
                    updateApplicationInsightsResourceRegistry(subList, project);
                } else {
                    // imported publish settings file. just show manually added list from preferences
                    // Neither clear subscription list nor show sign in dialog as user may just want to add key manually.
                    keeepManuallyAddedList(project);
                }
            }
        } catch(Exception ex) {
            AzurePlugin.log(ex.getMessage());
        }
    }

    private ActionListener newButtonListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    AzureManager manager = AzureManagerImpl.getManager(myProject);
                    List<Subscription> subList = manager.getSubscriptionList();
                    if (subList.size() > 0) {
                        if (!manager.authenticated()) {
                            // imported publish settings file.
                            manager.clearImportedPublishSettingsFiles();
                            WizardCacheManager.clearSubscriptions();
                            createSubscriptionDialog(true);
                        }
                    } else {
                        createSubscriptionDialog(true);
                    }
                    createNewDilaog();
                } catch(Exception ex) {
                    AzurePlugin.log(ex.getMessage(), ex);
                }
            }
        };
    }

    private void createNewDilaog() {
        try {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    ApplicationInsightsNewDialog dialog = new ApplicationInsightsNewDialog();
                    dialog.show();
                    if (dialog.isOK()) {
                        ApplicationInsightsResource resource = ApplicationInsightsNewDialog.getResource();
                        if (resource != null &&
                                !ApplicationInsightsResourceRegistry.getAppInsightsResrcList().contains(resource)) {
                            ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resource);
                            AzureSettings.getSafeInstance(myProject).saveAppInsights();
                            ((InsightsTableModel) insightsTable.getModel()).setResources(getTableContent());
                            ((InsightsTableModel) insightsTable.getModel()).fireTableDataChanged();
                        }
                    }
                }
            }, ModalityState.defaultModalityState());
        } catch(Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
    }

    private ActionListener addButtonListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ApplicationInsightsAddDialog dialog = new ApplicationInsightsAddDialog(myProject);
                dialog.show();
                if (dialog.isOK()) {
                    ((InsightsTableModel) insightsTable.getModel()).setResources(getTableContent());
                    ((InsightsTableModel) insightsTable.getModel()).fireTableDataChanged();
                }
            }
        };
    }

    private ActionListener detailsButtonListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = insightsTable.getSelectedRow();
                ApplicationInsightsResource resource = ApplicationInsightsResourceRegistry.getAppInsightsResrcList().get(index);
                ApplicationInsightsDetailsDialog dialog = new ApplicationInsightsDetailsDialog(resource);
                dialog.show();
            }
        };
    }

    private ActionListener removeButtonListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int curSelIndex = insightsTable.getSelectedRow();
                if (curSelIndex > -1) {
                    String keyToRemove = ApplicationInsightsResourceRegistry.getKeyAsPerIndex(curSelIndex);
                    String moduleName = MethodUtils.getModuleNameAsPerKey(myProject, keyToRemove);
                    if (moduleName != null && !moduleName.isEmpty()) {
                        PluginUtil.displayErrorDialog(message("aiErrTtl"), String.format(message("rsrcUseMsg"), moduleName));
                    } else {
                        int choice = Messages.showOkCancelDialog(message("rsrcRmvMsg"), message("aiErrTtl"), Messages.getQuestionIcon());
                        if (choice == Messages.OK) {
                            ApplicationInsightsResourceRegistry.getAppInsightsResrcList().remove(curSelIndex);
                            AzureSettings.getSafeInstance(myProject).saveAppInsights();
                            ((InsightsTableModel) insightsTable.getModel()).setResources(getTableContent());
                            ((InsightsTableModel) insightsTable.getModel()).fireTableDataChanged();
                        }
                    }
                }
            }
        };
    }

    private ListSelectionListener createAccountsTableListener() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean buttonsEnabled = insightsTable.getSelectedRow() > -1;
                detailsButton.setEnabled(buttonsEnabled);
                removeButton.setEnabled(buttonsEnabled);
            }
        };
    }

    private static class InsightsTableModel extends AbstractTableModel {
        public static final String[] COLUMNS = new String[]{"Resource Name", "Instrumentation Keys"};
        private java.util.List<ApplicationInsightsPageTableElement> resources;

        public InsightsTableModel(List<ApplicationInsightsPageTableElement> accounts) {
            this.resources = accounts;
        }

        public void setResources(List<ApplicationInsightsPageTableElement> accounts) {
            this.resources = accounts;
        }

        public String getKeyAtIndex(int index) {
            return resources.get(index).getInstrumentationKey();
        }

        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        public static int getColumnWidth(int column, int totalWidth) {
            switch (column) {
                case 0:
                    return (int) (totalWidth * 0.4);
                default:
                    return (int) (totalWidth * 0.6);
            }
        }

        public int getRowCount() {
            return resources.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            ApplicationInsightsPageTableElement resource = resources.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return resource.getResourceName();
                case 1:
                    return resource.getInstrumentationKey();
            }
            return null;
        }
    }
}
