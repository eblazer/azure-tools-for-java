package com.microsoft.azureexplorer.forms;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azureexplorer.Activator;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureArmManagerImpl;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.storage.ArmStorageAccount;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.Location;
import com.microsoft.tooling.msservices.model.ReplicationTypes;
import com.microsoftopentechnologies.wacommon.utils.Messages;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuTier;

public class CreateArmStorageAccountForm extends Dialog {
    private static final String PRICING_LINK = "<a href=\"http://go.microsoft.com/fwlink/?LinkID=400838\">Read more about replication services and pricing details</a>";
    private static Map<String, Kind> ACCOUNT_KIND = new TreeMap<>();
    static {
    	ACCOUNT_KIND.put("General purpose", Kind.STORAGE);
    	ACCOUNT_KIND.put("Blob storage", Kind.BLOB_STORAGE);
    }
    
    private Button buttonOK;
    private Button buttonCancel;

    private Label subscriptionLabel;
    private Combo subscriptionComboBox;
    private Label nameLabel;
    private Text nameTextField;
    private Label resourceGroupLabel;
    private Button createNewRadioButton;
    private Button useExistingRadioButton;
    private Text resourceGrpField;
    private Combo resourceGrpCombo;
    private Label regionLabel;
    private Combo regionComboBox;
    private Label kindLabel;
    private Combo kindCombo;
    private Label performanceLabel;
    private Combo performanceCombo;
    private Label replicationLabel;
    private Combo replicationComboBox;
    private Link pricingLabel;
    private Label userInfoLabel;

    private ComboViewer resourceGroupViewer;

    private Runnable onCreate;
    private Subscription subscription;
    private Region region;
    private ArmStorageAccount storageAccount;

    public CreateArmStorageAccountForm(Shell parentShell, Subscription subscription, Region region) {
        super(parentShell);
        this.subscription = subscription;
        this.region = region;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Create Storage Account");
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        GridData gridData = new GridData();
        gridData.verticalAlignment = SWT.FILL;
        gridData.horizontalAlignment = SWT.FILL;
        parent.setLayoutData(gridData);
        Control ctrl = super.createButtonBar(parent);
        buttonOK = getButton(IDialogConstants.OK_ID);
        buttonOK.setEnabled(false);
        buttonOK.setText("Create");
        buttonCancel = getButton(IDialogConstants.CANCEL_ID);
        buttonCancel.setText("Close");
        return ctrl;
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        container.setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.widthHint = 350;
        container.setLayoutData(gridData);

        userInfoLabel = new Label(container, SWT.LEFT);

        subscriptionLabel = new Label(container, SWT.LEFT);
        subscriptionLabel.setText("Subscription:");
        subscriptionComboBox = new Combo(container, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        subscriptionComboBox.setLayoutData(gridData);

        nameLabel = new Label(container, SWT.LEFT);
        nameLabel.setText("Name:");
        nameTextField = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        nameTextField.setLayoutData(gridData);
        
        resourceGroupLabel = new Label(container, SWT.LEFT);
        resourceGroupLabel.setText("Resource group:");
        Group group = new Group(container, SWT.NONE);
        group.setLayout(new RowLayout(SWT.HORIZONTAL));
        createNewRadioButton = new Button(group, SWT.RADIO);
        createNewRadioButton.setText("Create new");
        createNewRadioButton.setSelection(true);
        useExistingRadioButton = new Button(group, SWT.RADIO);
        useExistingRadioButton.setText("Use existing");
        
        SelectionListener updateListener = new SelectionAdapter() {
        	@Override
			public void widgetSelected(SelectionEvent arg0) {
        		 updateResourceGroup();
			}
		};
        createNewRadioButton.addSelectionListener(updateListener);
        useExistingRadioButton.addSelectionListener(updateListener);	
        
        resourceGrpField = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpField.setLayoutData(gridData);
        
        resourceGrpCombo = new Combo(container, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpCombo.setLayoutData(gridData);
        resourceGroupViewer = new ComboViewer(resourceGrpCombo);
        resourceGroupViewer.setContentProvider(ArrayContentProvider.getInstance());
        
        updateResourceGroup();
        
        regionLabel = new Label(container, SWT.LEFT);
        regionLabel.setText("Region:");
        regionComboBox = new Combo(container, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        regionComboBox.setLayoutData(gridData);
//        regionViewer = new ComboViewer(regionComboBox);
//        regionViewer.setContentProvider(ArrayContentProvider.getInstance());
        
        kindLabel = new Label(container, SWT.LEFT);
        kindLabel.setText("Account kind:");
        kindCombo = new Combo(container, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        kindCombo.setLayoutData(gridData);

        performanceLabel = new Label(container, SWT.LEFT);
        performanceLabel.setText("Performance:");
        performanceCombo = new Combo(container, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        performanceCombo.setLayoutData(gridData);
        
        replicationLabel = new Label(container, SWT.LEFT);
        replicationLabel.setText("Replication");
        replicationComboBox = new Combo(container, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        replicationComboBox.setLayoutData(gridData);

        pricingLabel = new Link(container, SWT.LEFT);
        pricingLabel.setText(PRICING_LINK);
        pricingLabel.setLayoutData(gridData);
        pricingLabel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.text));
                } catch (Exception ex) {
					/*
					 * only logging the error in log file
					 * not showing anything to end user
					 */
                    Activator.getDefault().log("Error occurred while opening link in default browser.", ex);
                }
            }
        });

        nameTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                validateEmptyFields();
            }
        });

        regionComboBox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                validateEmptyFields();
            }
        });
        
        resourceGrpField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                validateEmptyFields();
            }
        });
        
        resourceGrpCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                validateEmptyFields();
            }
        });

        if (AzureManagerImpl.getManager().authenticated()) {
            String upn = AzureManagerImpl.getManager().getUserInfo().getUniqueName();
            userInfoLabel.setText("Signed in as: " + (upn.contains("#") ? upn.split("#")[1] : upn));
        } else {
            userInfoLabel.setText("");
        }
        fillFields();

        return super.createContents(parent);
    }
    
    private void updateResourceGroup() {
		final boolean isNewGroup = createNewRadioButton.getSelection();
         resourceGrpField.setVisible(isNewGroup);
         resourceGrpCombo.setVisible(!isNewGroup);
	}

    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(nameTextField.getText().isEmpty() || regionComboBox.getText().isEmpty()
        		|| (createNewRadioButton.getSelection() && resourceGrpField.getText().trim().isEmpty())
                || (useExistingRadioButton.getSelection() && resourceGrpCombo.getText().isEmpty()));

        buttonOK.setEnabled(allFieldsCompleted);
    }

    @Override
    protected void okPressed() {
        if (nameTextField.getText().length() < 3
                || nameTextField.getText().length() > 24
                || !nameTextField.getText().matches("[a-z0-9]+")) {
            DefaultLoader.getUIHelper().showError("Invalid storage account name. The name should be between 3 and 24 characters long and \n" +
                    "can contain only lowercase letters and numbers.", "Azure Explorer");
            return;
		}
		String name = nameTextField.getText();

		String region = regionComboBox.getText();//((IStructuredSelection) regionViewer.getSelection()).getFirstElement().toString();
		String replication = replicationComboBox.getData(replicationComboBox.getText()).toString();
		final boolean isNewResourceGroup = createNewRadioButton.getSelection();
		final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getText();

		storageAccount = new ArmStorageAccount(name, subscription.getId().toString(), null);
		storageAccount.setType(replication);
		storageAccount.setLocation(region);
		storageAccount.setNewResourceGroup(isNewResourceGroup);
		storageAccount.setResourceGroupName(resourceGroupName);
		storageAccount.setKind((Kind) kindCombo.getData(kindCombo.getText()));
		if (regionComboBox.isEnabled()) {
		DefaultLoader.getIdeHelper().runInBackground(null, "Creating storage account...", false, true,
				"Creating storage account...", new Runnable() {
					@Override
					public void run() {
						createStorageAccount();
					}

					
				});
		} else {
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(PluginUtil.getParentShell());
			try {
				dialog.run(true, false, new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask("Creating storage account...", 100);
						createStorageAccount();
						monitor.worked(100);
						monitor.done();
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err,
						"An error occurred while creating the storage account.", e);
			}
		}
		super.okPressed();
    }
    
    private void createStorageAccount() {
		try {
			storageAccount = AzureArmManagerImpl.getManager(null).createStorageAccount(storageAccount);
			// AzureManagerImpl.getManager().refreshStorageAccountInformation(storageAccount);
			if (onCreate != null) {
				onCreate.run();
			}
		} catch (AzureCmdException e) {
			storageAccount = null;
			DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
				@Override
				public void run() {
					PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err,
							"An error occurred while creating the storage account.", e);
				}
			});
		}
	}

    public void fillFields() {
        if (subscription == null) {
        	loadRegions();
        	for (Map.Entry<String, Kind> entry : ACCOUNT_KIND.entrySet()) {
            	kindCombo.add(entry.getKey());
            	kindCombo.setData(entry.getKey(), entry.getValue());
            }
        	kindCombo.select(0);
            try {
                subscriptionComboBox.setEnabled(true);

                java.util.List<Subscription> fullSubscriptionList = AzureManagerImpl.getManager().getFullSubscriptionList();
                for (Subscription sub : fullSubscriptionList) {
                    subscriptionComboBox.add(sub.getName());
                    subscriptionComboBox.setData(sub.getName(), sub);
                }
                subscriptionComboBox.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        CreateArmStorageAccountForm.this.subscription = (Subscription) subscriptionComboBox.getData(subscriptionComboBox.getText());
                        loadGroups();
                        loadRegions();
                    }
                });

                if (fullSubscriptionList.size() > 0) {
                    this.subscription = fullSubscriptionList.get(0);
                    subscriptionComboBox.select(0);
                    loadGroups();
                    loadRegions();
                }
            } catch (AzureCmdException e) {
            	PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err,
            			"An error occurred while loading subscriptions.", e);
            }
        } else {
            subscriptionComboBox.setEnabled(false);
            subscriptionComboBox.add(subscription.getName());
            subscriptionComboBox.select(0);
            kindCombo.add("General purpose"); // only General purpose accounts supported for VMs
            kindCombo.setData(Kind.STORAGE);
            kindCombo.setEnabled(false);
            kindCombo.select(0);
            regionComboBox.add(region.toString());
            regionComboBox.setEnabled(false);
            regionComboBox.select(0);
            loadGroups();
            loadRegions();
        }
        for (SkuTier skuTier : SkuTier.values()) {
    		performanceCombo.add(skuTier.toString());
    	}
    	performanceCombo.select(0);
    	performanceCombo.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			fillReplicationTypes();
            }
		});
    	fillReplicationTypes();
    }
    
    private void fillReplicationTypes() {
    	replicationComboBox.removeAll();
    	if (performanceCombo.getText().equals(SkuTier.STANDARD.toString())) {
    		// Create storage account from Azure Explorer
    		if (regionComboBox.getEnabled()) {
    			for (ReplicationTypes replicationType : new ReplicationTypes[] {ReplicationTypes.Standard_ZRS, ReplicationTypes.Standard_LRS, ReplicationTypes.Standard_GRS, ReplicationTypes.Standard_RAGRS}) {
                    replicationComboBox.add(replicationType.getDescription());
                    replicationComboBox.setData(replicationType.getDescription(), replicationType);
                }
    		} else {
        		// Create storage account from VM creation
    			for (ReplicationTypes replicationType : new ReplicationTypes[] {ReplicationTypes.Standard_LRS, ReplicationTypes.Standard_GRS, ReplicationTypes.Standard_RAGRS}) {
                    replicationComboBox.add(replicationType.getDescription());
                    replicationComboBox.setData(replicationType.getDescription(), replicationType);
                }
    		}
    	} else {    		
    		replicationComboBox.add(ReplicationTypes.Premium_LRS.getDescription());
            replicationComboBox.setData(ReplicationTypes.Premium_LRS.getDescription(), ReplicationTypes.Premium_LRS);
    	}
        replicationComboBox.select(0);
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public ArmStorageAccount getStorageAccount() {
        return storageAccount;
    }

    public void loadRegions() {
    	for (Region region : region.values()) {
    		regionComboBox.add(region.toString());
    	}
//        regionComboBox.add("<Loading...>");
//
//        DefaultLoader.getIdeHelper().runInBackground(null, "Loading regions...", false, true, "Loading regions...", new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    final java.util.List<Location> locations = AzureManagerImpl.getManager().getLocations(subscription.getId().toString());
//
//                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
//                        @Override
//                        public void run() {
//                            final Vector<Object> vector = new Vector<Object>();
//                            vector.addAll(locations);
//                            regionViewer.setInput(vector);
//                            regionComboBox.select(1);
//                        }
//                    });
//                } catch (AzureCmdException e) {
//                	PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err,
//                			"An error occurred while loading the regions list.", e);
//                }
//            }
//        });
    }
    
    public void loadGroups() {
    	resourceGrpCombo.add("<Loading...>");

        DefaultLoader.getIdeHelper().runInBackground(null, "Loading resource groups...", false, true, "Loading resource groups...", new Runnable() {
            @Override
            public void run() {
                try {
                    final List<ResourceGroup> resourceGroups = AzureArmManagerImpl.getManager(null).getResourceGroups(subscription.getId());

                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final Vector<Object> vector = new Vector<Object>();
                            vector.addAll(resourceGroups);
                            resourceGroupViewer.setInput(vector);
                            if (resourceGroups.size() > 0) {
                            	resourceGrpCombo.select(1);
                            }
                        }
                    });
                } catch (AzureCmdException e) {
                	PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err,
                			"An error occurred while loading the resource groups list.", e);
                }
            }
        });
    }
}