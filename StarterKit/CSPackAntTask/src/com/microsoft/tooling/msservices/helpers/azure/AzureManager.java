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
package com.microsoft.tooling.msservices.helpers.azure;

import java.util.List;

import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.azure.management.websites.models.WebHostingPlan;
import com.microsoft.tooling.msservices.helpers.IDEHelper.ArtifactDescriptor;
import com.microsoft.tooling.msservices.helpers.IDEHelper.ProjectDescriptor;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.auth.UserInfo;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.AffinityGroup;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoft.tooling.msservices.model.vm.Location;
import com.microsoft.tooling.msservices.model.vm.VirtualMachine;
import com.microsoft.tooling.msservices.model.vm.VirtualMachineImage;
import com.microsoft.tooling.msservices.model.vm.VirtualMachineSize;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import com.microsoft.tooling.msservices.model.ws.WebHostingPlanCache;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper.EventWaitHandle;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.management.compute.models.DeploymentCreateParameters;
import com.microsoft.windowsazure.management.compute.models.DeploymentGetResponse;
import com.microsoft.windowsazure.management.compute.models.DeploymentSlot;
import com.microsoft.windowsazure.management.compute.models.ServiceCertificateListResponse;
import com.microsoft.windowsazure.management.models.SubscriptionGetResponse;

public interface AzureManager {
	void authenticate() throws AzureCmdException;

	boolean authenticated();

	boolean authenticated(@NotNull String subscriptionId);

	@Nullable
	UserInfo getUserInfo();

	void clearAuthentication();

	void importPublishSettingsFile(@NotNull String publishSettingsFilePath)
			throws AzureCmdException;

	boolean usingCertificate();

	boolean usingCertificate(@NotNull String subscriptionId);

	void clearImportedPublishSettingsFiles();

	String getAccessToken(String subscriptionId);

	@NotNull
	List<Subscription> getFullSubscriptionList()
			throws AzureCmdException;

	@NotNull
	List<Subscription> getSubscriptionList();

	void setSelectedSubscriptions(@NotNull List<String> selectedList)
			throws AzureCmdException;

	@NotNull
	EventWaitHandle registerSubscriptionsChanged()
			throws AzureCmdException;

	void unregisterSubscriptionsChanged(@NotNull EventWaitHandle handle)
			throws AzureCmdException;

	@NotNull
	List<CloudService> getCloudServices(@NotNull String subscriptionId)
			throws AzureCmdException;

	@NotNull
	List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId)
			throws AzureCmdException;

	@NotNull
	VirtualMachine refreshVirtualMachineInformation(@NotNull VirtualMachine vm)
			throws AzureCmdException;

	void startVirtualMachine(@NotNull VirtualMachine vm)
			throws AzureCmdException;

	void shutdownVirtualMachine(@NotNull VirtualMachine vm, boolean deallocate) throws AzureCmdException;

	void restartVirtualMachine(@NotNull VirtualMachine vm) throws AzureCmdException;

	void deleteVirtualMachine(@NotNull VirtualMachine vm, boolean deleteFromStorage) throws AzureCmdException;

	@NotNull
	byte[] downloadRDP(@NotNull VirtualMachine vm) throws AzureCmdException;

	@NotNull
	List<StorageAccount> getStorageAccounts(@NotNull String subscriptionId, boolean detailed) throws AzureCmdException;

	@NotNull
	public Boolean checkStorageNameAvailability(@NotNull final String subscriptionId, final String storageAccountName)
			throws AzureCmdException;

	@NotNull
	List<VirtualMachineImage> getVirtualMachineImages(@NotNull String subscriptionId) throws AzureCmdException;

	@NotNull
	List<VirtualMachineSize> getVirtualMachineSizes(@NotNull String subscriptionId) throws AzureCmdException;

	@NotNull
	List<Location> getLocations(@NotNull String subscriptionId)
			throws AzureCmdException;

	@NotNull
	public SubscriptionGetResponse getSubscription(@NotNull Configuration config) throws AzureCmdException;

	@NotNull
	List<AffinityGroup> getAffinityGroups(@NotNull String subscriptionId) throws AzureCmdException;

	@NotNull
	List<VirtualNetwork> getVirtualNetworks(@NotNull String subscriptionId) throws AzureCmdException;

	OperationStatusResponse createStorageAccount(@NotNull StorageAccount storageAccount)
			throws AzureCmdException;

	void createCloudService(@NotNull CloudService cloudService)
			throws AzureCmdException;

	CloudService getCloudServiceDetailed(@NotNull CloudService cloudService) throws AzureCmdException;

	public Boolean checkHostedServiceNameAvailability(@NotNull final String subscriptionId, final String hostedServiceName) throws AzureCmdException;

	void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
			@NotNull StorageAccount storageAccount, @NotNull String virtualNetwork,
			@NotNull String username, @NotNull String password, @NotNull byte[] certificate)
					throws AzureCmdException;

	void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
			@NotNull String mediaLocation, @NotNull String virtualNetwork,
			@NotNull String username, @NotNull String password, @NotNull byte[] certificate)
					throws AzureCmdException;

	@NotNull
	OperationStatusResponse createDeployment(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull String slotName,
			@NotNull DeploymentCreateParameters parameters, @NotNull String unpublish)
					throws AzureCmdException;

	@NotNull
	public OperationStatusResponse deleteDeployment(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull String deploymentName, boolean deleteFromStorage)
			throws AzureCmdException;

	@NotNull
	public DeploymentGetResponse getDeploymentBySlot(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull DeploymentSlot deploymentSlot)
			throws AzureCmdException;

	@NotNull
	public OperationStatusResponse waitForStatus(@NotNull String subscriptionId, @NotNull OperationStatusResponse operationStatusResponse)
			throws AzureCmdException;

	@NotNull
	StorageAccount refreshStorageAccountInformation(@NotNull StorageAccount storageAccount)
			throws AzureCmdException;

	String createServiceCertificate(@NotNull String subscriptionId, @NotNull String serviceName,
			@NotNull byte[] data, @NotNull String password, boolean needThumbprint)
					throws AzureCmdException;

	List<ServiceCertificateListResponse.Certificate> getCertificates(@NotNull String subscriptionId, @NotNull String serviceName)
			throws AzureCmdException;

	void deleteStorageAccount(@NotNull ClientStorageAccount storageAccount)
			throws AzureCmdException;

	@NotNull
	List<WebSite> getWebSites(@NotNull String subscriptionId, @NotNull String webSpaceName)
			throws AzureCmdException;

	@NotNull
	List<WebHostingPlanCache> getWebHostingPlans(@NotNull String subscriptionId, @NotNull String webSpaceName)
			throws AzureCmdException;

	@NotNull
	WebSiteConfiguration getWebSiteConfiguration(@NotNull String subscriptionId, @NotNull String webSpaceName,
			@NotNull String webSiteName)
					throws AzureCmdException;

	@NotNull
	WebSitePublishSettings getWebSitePublishSettings(@NotNull String subscriptionId, @NotNull String webSpaceName,
			@NotNull String webSiteName)
					throws AzureCmdException;

	void restartWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName)
			throws AzureCmdException;

	void stopWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName)
			throws AzureCmdException;

	void startWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName)
			throws AzureCmdException;

	@NotNull
	WebSite createWebSite(@NotNull String subscriptionId, @NotNull WebHostingPlanCache webHostingPlan, @NotNull String webSiteName)
			throws AzureCmdException;

	@NotNull
	Void deleteWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName) throws AzureCmdException;

	WebSite getWebSite(@NotNull String subscriptionId, @NotNull final String webSpaceName, @NotNull String webSiteName)
			throws AzureCmdException;

	@NotNull
	WebSiteConfiguration updateWebSiteConfiguration(@NotNull String subscriptionId,
			@NotNull String webSpaceName,
			@NotNull String webSiteName,
			@NotNull String location,
			@NotNull WebSiteConfiguration webSiteConfiguration) throws AzureCmdException;

	@NotNull WebHostingPlan createWebHostingPlan(@NotNull String subscriptionId, @NotNull WebHostingPlanCache webHostingPlan)
			throws AzureCmdException;

	@Nullable
	ArtifactDescriptor getWebArchiveArtifact(@NotNull ProjectDescriptor projectDescriptor)
			throws AzureCmdException;

	void deployWebArchiveArtifact(@NotNull ProjectDescriptor projectDescriptor,
			@NotNull ArtifactDescriptor artifactDescriptor,
			@NotNull WebSite webSite,
			@NotNull boolean isDeployRoot,
			AzureManager manager);

	void publishWebArchiveArtifact(@NotNull String subscriptionId, @NotNull String webSpaceName,
			@NotNull String webSiteName, @NotNull String artifactPath,
			@NotNull boolean isDeployRoot, @NotNull String artifactName) throws AzureCmdException;

	List<String> getResourceGroupNames(@NotNull String subscriptionId) throws AzureCmdException;

	ResourceGroupExtended createResourceGroup(@NotNull String subscriptionId, @NotNull String name, @NotNull String location) throws AzureCmdException;

	List<Resource> getApplicationInsightsResources(@NotNull String subscriptionId) throws AzureCmdException;

	List<String> getLocationsForApplicationInsights(@NotNull String subscriptionId) throws AzureCmdException;

	Resource createApplicationInsightsResource(@NotNull String subscriptionId,
			@NotNull String resourceGroupName,
			@NotNull String resourceName,
			@NotNull String location) throws AzureCmdException;

	Void enableWebSockets(@NotNull String subscriptionId,
			@NotNull String webSpaceName,
			@NotNull String webSiteName,
			@NotNull String location,
			@NotNull boolean enableSocket) throws AzureCmdException;
}