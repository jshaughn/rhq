/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.gui.coregui.client.admin.topology;

import static org.rhq.enterprise.gui.coregui.client.admin.topology.AffinityGroupWithCountsDatasource.Fields.FIELD_NAME;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Shows details of a server.
 * 
 * @author Jiri Kremser
 */
public class AffinityGroupDetailView extends LocatableVLayout implements BookmarkableView {

    //    private final CloudGWTServiceAsync cloudManager = GWTServiceLookup.getCloudService();
    private final int affinityGroupId;

    private final LocatableSectionStack sectionStack;
    private SectionStackSection detailsSection = null;
    private SectionStackSection agentSection = null;
    private SectionStackSection serverSection = null;

    private volatile int initSectionCount = 0;

    public AffinityGroupDetailView(String locatorId, int affinityGroupId) {
        super(locatorId);
        this.affinityGroupId = affinityGroupId;
        setHeight100();
        setWidth100();
        setOverflow(Overflow.AUTO);

        sectionStack = new LocatableSectionStack(extendLocatorId("stack"));
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setMargin(5);
        sectionStack.setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void onInit() {
        super.onInit();
        GWTServiceLookup.getCloudService().getAffinityGroupById(this.affinityGroupId,
            new AsyncCallback<AffinityGroup>() {
                public void onSuccess(final AffinityGroup affinityGroup) {
                    prepareDetailsSection(sectionStack, affinityGroup);

                    //                GWTServiceLookup.getCloudService().getAgentsByServerName(server.getName(),
                    //                    new AsyncCallback<List<Agent>>() {
                    //                        public void onSuccess(List<Agent> agents) {
                    //                            prepareAgentSection(sectionStack, server, agents);
                    //                        };
                    //
                    //                        public void onFailure(Throwable caught) {
                    //                            //TODO: CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), caught);
                    //                        }
                    //                    });
                }

                public void onFailure(Throwable caught) {
                    SC.say("er1:" + caught);
                    //TODO: CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), caught);
                }
            });
        prepareAgentSection(sectionStack);
        prepareServerSection(sectionStack);
    }

    public boolean isInitialized() {
        return initSectionCount >= 2;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // wait until we have all of the sections before we show them. We don't use InitializableView because,
        // it seems they are not supported (in the applicable renderView()) at this level.
        new Timer() {
            final long startTime = System.currentTimeMillis();

            public void run() {
                if (isInitialized()) {
                    if (null != detailsSection) {
                        sectionStack.addSection(detailsSection);
                    }
                    if (null != agentSection) {
                        sectionStack.addSection(agentSection);
                    }
                    if (null != serverSection) {
                        sectionStack.addSection(serverSection);
                    }

                    addMember(sectionStack);
                    markForRedraw();

                } else {
                    // don't wait forever, give up after 20s and show what we have
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    if (elapsedMillis > 20000) {
                        initSectionCount = 3;
                    }
                    schedule(100); // Reschedule the timer.
                }
            }
        }.run(); // fire the timer immediately
    }

    private void prepareAgentSection(SectionStack stack) {
        SectionStackSection section = new SectionStackSection(MSG.view_adminTopology_affinityGroups_agetnMembers());
        section.setExpanded(true);
        AgentTableView agentsTable = new AgentTableView(extendLocatorId(AgentTableView.VIEW_ID.getName()), null,
            affinityGroupId, true);
        section.setItems(agentsTable);

        agentSection = section;
        initSectionCount++;
        return;
    }

    private void prepareServerSection(SectionStack stack) {
        SectionStackSection section = new SectionStackSection(MSG.view_adminTopology_affinityGroups_serverMembers());
        section.setExpanded(true);
        ServerTableView serverTable = new ServerTableView(extendLocatorId(AgentTableView.VIEW_ID.getName()), null,
            affinityGroupId, true);
        section.setItems(serverTable);

        serverSection = section;
        initSectionCount++;
        return;
    }

    private void prepareDetailsSection(SectionStack stack, AffinityGroup affinityGroup) {
        final LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("detailsForm"));
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        TextItem nameItem = new TextItem(FIELD_NAME.propertyName(), FIELD_NAME.title());
        nameItem.setValue(affinityGroup.getName());

        ButtonItem saveButton = new ButtonItem();
        saveButton.setOverflow(Overflow.VISIBLE);
        saveButton.setTitle(MSG.common_button_save());
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                RecordList list = form.getRecordList();
                SC.say("name is " + list.get(0).getAttribute("name"));
            }
        });

        form.setItems(nameItem, saveButton);

        SectionStackSection section = new SectionStackSection(MSG.common_title_details());
        section.setExpanded(true);
        section.setItems(form);

        detailsSection = section;
        initSectionCount++;
    }

    @Override
    public void renderView(ViewPath viewPath) {
        Log.debug("AffinityGroupDetailView: " + viewPath);
    }
}
