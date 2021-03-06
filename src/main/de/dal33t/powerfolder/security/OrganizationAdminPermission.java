/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: FolderRepository.java 20999 2013-03-11 13:19:11Z glasgow $
 */
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.util.Util;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.75 $
 */
public class OrganizationAdminPermission implements Permission {

    public static final String ID_SEPARATOR = "_OP_";
    public static OrganizationPermissionHelper ORGANIZATION_PERMISSION_HELPER;
    private static final long serialVersionUID = 100L;
    private String organizationOID;

    public OrganizationAdminPermission(String organizationOID) {
        this.organizationOID = organizationOID;
    }

    public boolean implies(Permission impliedPermision) {
        if (ORGANIZATION_PERMISSION_HELPER != null) {
            return ORGANIZATION_PERMISSION_HELPER.implies(this,
                impliedPermision);
        }
        return false;
    }

    public String getId() {
        return organizationOID + ID_SEPARATOR + getClass().getSimpleName();
    }

    public String getOrganizationOID() {
        return organizationOID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((organizationOID == null) ? 0 : organizationOID.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Permission))
            return false;
        Permission other = (Permission) obj;
        return Util.equals(getId(), other.getId());
    }

    public static interface OrganizationPermissionHelper {
        boolean implies(OrganizationAdminPermission organizationPermission,
            Permission impliedPermision);
    }
}
