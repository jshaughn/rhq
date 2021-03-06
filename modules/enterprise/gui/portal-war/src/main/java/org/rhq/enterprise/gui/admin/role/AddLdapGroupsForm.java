/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.admin.role;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * A subclass of <code>Spider</code> representing the <em>Add Role Resource Groups</em> form.
 */
public class AddLdapGroupsForm extends BaseValidatorForm {
    //-------------------------------------instance variables

    private String[] availableGroups;
    private String[] pendingGroups;
    private Integer psa;
    private Integer psp;
    private Integer r;

    //-------------------------------------constructors

    public AddLdapGroupsForm() {
        super();
    }

    //-------------------------------------public methods

    public String[] getAvailableGroup() {
        return this.availableGroups;
    }

    public String[] getAvailableGroups() {
        return getAvailableGroup();
    }

    public void setAvailableGroup(String[] availableGroups) {
        this.availableGroups = availableGroups;
    }

    public void setAvailableGroups(String[] availableGroups) {
        setAvailableGroup(availableGroups);
    }

    public String[] getPendingGroup() {
        return this.pendingGroups;
    }

    public String[] getPendingGroups() {
        return getPendingGroup();
    }

    public void setPendingGroup(String[] pendingGroups) {
        this.pendingGroups = pendingGroups;
    }

    public void setPendingGroups(String[] pendingGroups) {
        setPendingGroup(pendingGroups);
    }

    public Integer getPsa() {
        return this.psa;
    }

    public void setPsa(Integer ps) {
        this.psa = ps;
    }

    public Integer getPsp() {
        return this.psp;
    }

    public void setPsp(Integer ps) {
        this.psp = ps;
    }

    public Integer getR() {
        return this.r;
    }

    public void setR(Integer r) {
        this.r = r;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        this.psa = null;
        this.psp = null;
        this.r = null;
        this.pendingGroups = new String[0];
        this.availableGroups = new String[0];
    }

    public String toString() {
        StringBuilder s = new StringBuilder(super.toString());
        s.append("r=" + r + " ");
        s.append("psa=" + psa + " ");
        s.append("psp=" + psp + " ");

        s.append("availableGroups={");
        listToString(s, availableGroups);
        s.append("} ");

        s.append("pendingGroups={");
        listToString(s, pendingGroups);
        s.append("}");

        return s.toString();
    }

    private void listToString(StringBuilder s, String[] l) {
        if (l != null) {
            for (int i = 0; i < l.length; i++) {
                s.append(l[i]);
                if (i < (l.length - 1)) {
                    s.append(", ");
                }
            }
        }
    }
}