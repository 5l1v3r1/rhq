/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.domain.configuration;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class PropertyListTest {

    @Test
    public void deepCopyShouldCopySimpleFields() {
        PropertyList original = createPropertyList();

        PropertyList copy = original.deepCopy();

        assertEquals(copy.getName(), original.getName(), "Failed to copy the name property");
        assertEquals(copy.getErrorMessage(), original.getErrorMessage(), "Failed to copy the errorMessage property");
    }

    @Test
    public void deepCopyShouldNotCopyIdField() {
        PropertyList original = createPropertyList();
        PropertyList copy = original.deepCopy();

        assertFalse(copy.getId() == original.getId(), "The original id property should not be copied.");
    }

    @Test
    public void deepCopyShouldNotCopyReferenceOfUnderlyingList() {
        PropertyList original = createPropertyList();
        PropertyList copy = original.deepCopy();

        assertNotSame(original.getList(), copy.getList(), "The values in the underlying list should be copied, not the variable reference to the list.");
    }

    @Test
    public void deepCopyShouldCopySimpleProperties() {
        PropertyList original = createPropertyList();

        PropertySimple simpleProperty = new PropertySimple("simeplProperty", "Simple Property");
        original.add(simpleProperty);

        PropertyList copy = original.deepCopy();

        assertEquals(copy.getList().size(), original.getList().size(), "Failed to copy simple property contained in original property list");

        assertNotSame(copy.getList().get(0), original.getList().get(0), "Properties in the list should be copied by value as opposed to just copying the references");
    }

    private PropertyList createPropertyList() {
        PropertyList propertyList = new PropertyList("listProperty");
        propertyList.setId(1);
        propertyList.setErrorMessage("error message");

        // This makes sure that the underlying list gets initialized. Probably ought to refactor PropertyList so that
        // it is property initialized.
        propertyList.getList();

        return propertyList;
    }

}
