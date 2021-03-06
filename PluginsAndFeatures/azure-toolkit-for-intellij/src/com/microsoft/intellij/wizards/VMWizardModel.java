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
package com.microsoft.intellij.wizards;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.ui.wizard.WizardModel;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.VirtualMachineImage;
import com.microsoft.tooling.msservices.model.vm.VirtualMachineSize;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.SimpleDateFormat;

public abstract class VMWizardModel extends WizardModel {
    private final String BASE_HTML_VM_IMAGE = "<html>\n" +
            "<body style=\"padding: 5px; width: 250px\">\n" +
            "    <p style=\"font-family: 'Segoe UI';font-size: 14pt;font-weight: bold;\">#TITLE#</p>\n" +
            "    <p style=\"font-family: 'Segoe UI';font-size: 11pt; width:200px \">#DESCRIPTION#</p>\n" +
            "    <p>\n" +
            "        <table style='width:200px'>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;width:60px;vertical-align:top;\"><b>PUBLISHED</b></td>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;\">#PUBLISH_DATE#</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;vertical-align:top;\"><b>PUBLISHER</b></td>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;\">#PUBLISH_NAME#</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;vertical-align:top;\"><b>OS FAMILY</b></td>\n" +
            "                <td style =\"font-family: 'Segoe UI';font-size: 12pt;\">#OS#</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;v-align:top;font-weight:bold;\">LOCATION</td>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;\">#LOCATION#</td>\n" +
            "            </tr>\n" +
            "        </table>\n" +
            "    </p>\n" +
            "    #PRIVACY#\n" +
            "    #LICENCE#\n" +
            "</body>\n" +
            "</html>";

    private Subscription subscription;
    private String name;
    private VirtualMachineSize size;
    private String userName;
    private String password;
    private String certificate;
    private String subnet;

    public VMWizardModel() {
        super(ApplicationNamesInfo.getInstance().getFullProductName() + " - Create new Virtual Machine");
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void configStepList(JList jList, int step) {

        jList.setListData(getStepTitleList());
        jList.setSelectedIndex(step);
        jList.setBorder(new EmptyBorder(10, 0, 10, 0));

        jList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean b, boolean b1) {
                return super.getListCellRendererComponent(jList, "  " + o.toString(), i, b, b1);
            }
        });

        for (MouseListener mouseListener : jList.getMouseListeners()) {
            jList.removeMouseListener(mouseListener);
        }

        for (MouseMotionListener mouseMotionListener : jList.getMouseMotionListeners()) {
            jList.removeMouseMotionListener(mouseMotionListener);
        }
    }

    public abstract String[] getStepTitleList();

    public String getHtmlFromVMImage(VirtualMachineImage virtualMachineImage) {
        String html = BASE_HTML_VM_IMAGE;
        html = html.replace("#TITLE#", virtualMachineImage.getLabel());
        html = html.replace("#DESCRIPTION#", virtualMachineImage.getDescription());
        html = html.replace("#PUBLISH_DATE#", new SimpleDateFormat("dd-M-yyyy").format(virtualMachineImage.getPublishedDate().getTime()));
        html = html.replace("#PUBLISH_NAME#", virtualMachineImage.getPublisherName());
        html = html.replace("#OS#", virtualMachineImage.getOperatingSystemType());
        html = html.replace("#LOCATION#", virtualMachineImage.getLocation());

        html = html.replace("#PRIVACY#", virtualMachineImage.getPrivacyUri().isEmpty()
                ? ""
                : "<p><a href='" + virtualMachineImage.getPrivacyUri() + "' style=\"font-family: 'Segoe UI';font-size: 12pt;\">Privacy statement</a></p>");


        html = html.replace("#LICENCE#", virtualMachineImage.getEulaUri().isEmpty()
                ? ""
                : "<p><a href='" + virtualMachineImage.getEulaUri() + "' style=\"font-family: 'Segoe UI';font-size: 12pt;\">Licence agreement</a></p>");

        return html;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VirtualMachineSize getSize() {
        return size;
    }

    public void setSize(VirtualMachineSize size) {
        this.size = size;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
}
