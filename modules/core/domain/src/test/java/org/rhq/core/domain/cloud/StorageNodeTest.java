/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.core.domain.cloud;

import org.testng.annotations.Test;

import org.rhq.core.domain.cloud.StorageNode.OperationMode;

@Test
public class StorageNodeTest {
    public void testEquals() {
        StorageNode localhost1 = new StorageNode();
        assert localhost1 != null;
        assert !localhost1.equals(null);

        StorageNode localhost2 = new StorageNode();
        assert localhost2 != null;
        assert localhost1.equals(localhost2);
        assert localhost2.equals(localhost1);

        localhost1.setAddress("127.0.0.1");
        assert !localhost1.equals(localhost2);
        assert !localhost2.equals(localhost1);

        localhost2.setAddress("127.0.0.1");
        assert localhost1.equals(localhost2);
        assert localhost2.equals(localhost1);

        StorageNode localhost3 = new StorageNode(42);
        localhost3.setAddress("sn.com");
        assert !localhost3.equals(null);
        assert !localhost3.equals(localhost1);
        assert localhost3.hashCode() != localhost1.hashCode();
        assert localhost2.hashCode() == localhost1.hashCode();

        localhost3.setAddress("127.0.0.1");
        assert localhost3.equals(localhost1);
        assert localhost3.hashCode() == localhost1.hashCode();
    }

    public void testNodeInformation1() {
        StorageNode localhost1 = new StorageNode();
        localhost1.setAddress("127.0.0.1");
        localhost1.setCqlPort(4321);
        assert "127.0.0.1".equals(localhost1.getAddress());
        assert localhost1.getCqlPort() == 4321;

        localhost1.setOperationMode(OperationMode.INSTALLED);
        assert localhost1.getOperationMode() == OperationMode.INSTALLED;
        assert localhost1.getOperationMode().getMessage() != null;
        assert localhost1.getOperationMode().getMessage() != null;
        localhost1.setMtime(42);
        assert localhost1.getMtime() == 42;

        StorageNode localhost2 = new StorageNode();
        localhost2.setAddress("127.0.0.1");
        assert localhost1.equals(localhost2);
    }
}
