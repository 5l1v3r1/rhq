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
package org.rhq.enterprise.server.resource.group.test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.transaction.Status;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;
import org.rhq.enterprise.server.util.SessionTestHelper;

public class RecursiveResourceGroupTest extends AbstractEJB3Test {

    private ResourceGroupManagerLocal resourceGroupManager;
    private ResourceManagerLocal resourceManager;
    private RoleManagerLocal roleManager;
    private SubjectManagerLocal subjectManager;

    @Override
    protected void beforeMethod() throws Exception {
        resourceGroupManager = LookupUtil.getResourceGroupManager();
        resourceManager = LookupUtil.getResourceManager();
        roleManager = LookupUtil.getRoleManager();
        subjectManager = LookupUtil.getSubjectManager();
        prepareScheduler();
    }

    @Override
    protected void afterMethod() throws Exception {
        unprepareScheduler();
    }

    @Test(groups = "integration.session")
    public void testImplicitGroupMembershipFromInventoryUpdate() throws Throwable {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            // setup simple test structures
            Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
            Role role = SessionTestHelper
                .createNewRoleForSubject(em, subject, "fake role", Permission.MANAGE_INVENTORY);

            ResourceGroup recursiveGroup = SessionTestHelper.createNewMixedGroupForRole(em, role, "fake group", true);

            // setup the test tree
            List<Resource> fullTree = getSimpleTree(em);

            // test simple implicit resources
            Resource nodeA = ResourceTreeHelper.findNode(fullTree, "A");

            List<Resource> resourcesFromTreeA = ResourceTreeHelper.getSubtree(nodeA);

            resourceGroupManager.addResourcesToGroup(subject, recursiveGroup.getId(), new int[] { nodeA.getId() });
            List<Resource> initialExplicitResources = resourceManager.findExplicitResourcesByResourceGroup(subject,
                recursiveGroup, PageControl.getUnlimitedInstance());

            assert initialExplicitResources.size() == 1 : "Failed: initial explicit resources, size was "
                + initialExplicitResources.size();
            assert initialExplicitResources.get(0).getId() == nodeA.getId() : "Failed: initial explicit resources id, found "
                + initialExplicitResources.get(0).getId() + ", expected " + nodeA.getId();

            List<Resource> initialImplicitResources = resourceManager.findImplicitResourcesByResourceGroup(subject,
                recursiveGroup, PageControl.getUnlimitedInstance());
            verifyEqualByIds("Failed: initial implicit resources", resourcesFromTreeA, initialImplicitResources);

            // test update implicit resources
            Resource newChildOfNodeA = new Resource("new nodeOne child", "new nodeOne child", nodeA.getResourceType());
            newChildOfNodeA.setUuid("" + new Random().nextInt());
            newChildOfNodeA.setInventoryStatus(InventoryStatus.COMMITTED);

            resourceManager.createResource(subject, newChildOfNodeA, nodeA.getId()); // sets up implicit relationships

            List<Resource> updatedImplicitResources = resourceManager.findImplicitResourcesByResourceGroup(subject,
                recursiveGroup, PageControl.getUnlimitedInstance());

            resourcesFromTreeA.add(newChildOfNodeA);
            verifyEqualByIds("Failed: simple implicit resources", resourcesFromTreeA, updatedImplicitResources);
        } catch (Throwable t) {
            handleThrowable(t);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.session")
    public void testUpdateImplicitGroupMembership() throws Throwable {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            // setup simple test structures
            Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
            Role role = SessionTestHelper
                .createNewRoleForSubject(em, subject, "fake role", Permission.MANAGE_INVENTORY);

            ResourceGroup lineageG1 = SessionTestHelper.createNewMixedGroupForRole(em, role, "gen1", true);
            ResourceGroup lineageG2 = SessionTestHelper.createNewMixedGroupForRole(em, role, "gen2", true);
            ResourceGroup lineageG3 = SessionTestHelper.createNewMixedGroupForRole(em, role, "gen3", true);
            ResourceGroup lineageG4 = SessionTestHelper.createNewMixedGroupForRole(em, role, "gen4", true);

            // setup the test tree
            List<Resource> fullTree = ResourceTreeHelper.createTree(em, "A=1; 1=a; a=i;"); // resource chain
            Resource gen1 = ResourceTreeHelper.findNode(fullTree, "A");
            Resource gen2 = ResourceTreeHelper.findNode(fullTree, "1");
            Resource gen3 = ResourceTreeHelper.findNode(fullTree, "a");
            Resource gen4 = ResourceTreeHelper.findNode(fullTree, "i");

            resourceGroupManager.addResourcesToGroup(subject, lineageG1.getId(), new int[] { gen1.getId() });
            resourceGroupManager.addResourcesToGroup(subject, lineageG2.getId(), new int[] { gen2.getId() });
            resourceGroupManager.addResourcesToGroup(subject, lineageG3.getId(), new int[] { gen3.getId() });
            resourceGroupManager.addResourcesToGroup(subject, lineageG4.getId(), new int[] { gen4.getId() });

            // test update implicit resources
            Resource gen5 = new Resource("g5", "g5", gen4.getResourceType());
            gen5.setUuid("" + new Random().nextInt());
            gen5.setInventoryStatus(InventoryStatus.COMMITTED);
            resourceManager.createResource(subject, gen5, gen4.getId()); // sets up implicit relationships

            // confirm results
            List<Integer> newLineageG1 = resourceManager.findImplicitResourceIdsByResourceGroup(lineageG1.getId());
            List<Integer> newLineageG2 = resourceManager.findImplicitResourceIdsByResourceGroup(lineageG2.getId());
            List<Integer> newLineageG3 = resourceManager.findImplicitResourceIdsByResourceGroup(lineageG3.getId());
            List<Integer> newLineageG4 = resourceManager.findImplicitResourceIdsByResourceGroup(lineageG4.getId());

            List<Resource> treeGen1 = ResourceTreeHelper.getSubtree(gen1);
            List<Resource> treeGen2 = ResourceTreeHelper.getSubtree(gen2);
            List<Resource> treeGen3 = ResourceTreeHelper.getSubtree(gen3);
            List<Resource> treeGen4 = ResourceTreeHelper.getSubtree(gen4);

            // gen5 resource should have been added to all of them
            treeGen1.add(gen5);
            treeGen2.add(gen5);
            treeGen3.add(gen5);
            treeGen4.add(gen5);

            verifyEqual("Failed: updateImplicitGroupMembership gen1", getIds(treeGen1), newLineageG1);
            verifyEqual("Failed: updateImplicitGroupMembership gen2", getIds(treeGen2), newLineageG2);
            verifyEqual("Failed: updateImplicitGroupMembership gen3", getIds(treeGen3), newLineageG3);
            verifyEqual("Failed: updateImplicitGroupMembership gen4", getIds(treeGen4), newLineageG4);
        } catch (Throwable t) {
            handleThrowable(t);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.session")
    public void testImplicitGroupMembershipFromComplexGroupUpdates() throws Throwable {
        Subject subject = null;
        Role role = null;
        ResourceGroup recursiveGroup = null;
        List<Resource> fullTree = null;

        Resource nodeBigA = null;
        Resource nodeOne = null;
        Resource nodeThree = null;
        Resource nodeLittleA = null;
        Resource nodeTripleLittleI = null;

        List<Resource> resultsExplicit = null;

        List<Resource> expectedImplicit = null;
        List<Resource> expectedExplicit = new ArrayList<Resource>();

        try {
            try {
                getTransactionManager().begin();

                EntityManager em = getEntityManager();
                // setup simple test structures
                subject = SessionTestHelper.createNewSubject(em, "fake subject");
                role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role", Permission.MANAGE_INVENTORY);
                recursiveGroup = SessionTestHelper.createNewMixedGroupForRole(em, role, "fake group", true);

                // setup the test tree
                fullTree = getSimpleTree(em);

                ResourceTreeHelper.printForest(fullTree);

                // get the resources from the tree we want to explicitly add
                nodeBigA = ResourceTreeHelper.findNode(fullTree, "A");
                nodeOne = ResourceTreeHelper.findNode(fullTree, "1");
                nodeThree = ResourceTreeHelper.findNode(fullTree, "3");
                nodeLittleA = ResourceTreeHelper.findNode(fullTree, "a");
                nodeTripleLittleI = ResourceTreeHelper.findNode(fullTree, "iii");

                // adding nodeLittleA should give us the subtree under nodeLittleA
                expectedImplicit = ResourceTreeHelper.getSubtree(nodeLittleA);
                implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeLittleA, expectedImplicit);
                resultsExplicit = resourceManager.findExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                expectedExplicit.add(nodeLittleA);
                verifyEqualByIds("explicit add 1", expectedExplicit, resultsExplicit);

                // adding nodeThree should give us the union of the subtrees under nodeLittleA and nodeThree
                expectedImplicit.add(nodeThree);
                implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeThree, expectedImplicit);
                resultsExplicit = resourceManager.findExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                expectedExplicit.add(nodeThree);
                verifyEqualByIds("explicit add 2", expectedExplicit, resultsExplicit);

                // adding nodeBigA should give us the union of the entire A tree with the nodeThree subtree
                expectedImplicit = ResourceTreeHelper.getSubtree(nodeBigA);
                expectedImplicit.addAll(ResourceTreeHelper.getSubtree(nodeThree));
                implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeBigA, expectedImplicit);
                resultsExplicit = resourceManager.findExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                expectedExplicit.add(nodeBigA);
                verifyEqualByIds("explicit add 3", expectedExplicit, resultsExplicit);

                // adding nodeOne, which is a child of nodeBigA, shouldn't effect the expected results
                implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeOne, expectedImplicit);
                resultsExplicit = resourceManager.findExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                expectedExplicit.add(nodeOne);
                verifyEqualByIds("explicit add 4", expectedExplicit, resultsExplicit);

                // adding nodeTripleLittleI shouldn't effect the expected results either
                implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeTripleLittleI, expectedImplicit);
                resultsExplicit = resourceManager.findExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                expectedExplicit.add(nodeTripleLittleI);
                verifyEqualByIds("explicit add 5", expectedExplicit, resultsExplicit);
            } catch (Throwable t) {
                handleThrowable(t);
            } finally {
                handleTransaction();
            }

            try {
                getTransactionManager().begin();
                // removing the subtree nodeTripleLittleI shouldn't affect the expected set
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeTripleLittleI, expectedImplicit);
            } catch (Throwable t) {
                handleThrowable(t);
            } finally {
                handleTransaction();
            }

            try {
                getTransactionManager().begin();

                // removing a descendant of a node that is also in the explicit list should be a no-op
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeOne, expectedImplicit);

                expectedImplicit.remove(nodeThree);
                // removing the wandering nodeThree, so that results will be the complete nodeBigA subtree
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeThree, expectedImplicit);

                // removing a root node should remove all descendants that aren't still in the explicit list
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeBigA,
                    ResourceTreeHelper.getSubtree(nodeLittleA));
            } catch (Throwable t) {
                handleThrowable(t);
            } finally {
                handleTransaction();
            }

            try {
                getTransactionManager().begin();

                // remove a node that wasn't in the group - negative testing
                try {
                    // passing the "real" expected list for the results; this way, if the exception doesn't happen, the helper returns true
                    implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeBigA,
                        ResourceTreeHelper.getSubtree(nodeLittleA));
                    assert false : "Failed: removed non-existent successfully: node = " + nodeBigA.getName();
                } catch (Throwable t) {
                    // expected
                }
            } catch (Throwable t) {
                handleThrowable(t);
            } finally {
                handleTransaction();
            }

            try {
                getTransactionManager().begin();

                // removing the last resource should leave an empty list
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeLittleA, new ArrayList<Resource>());
            } catch (Throwable t) {
                handleThrowable(t);
            } finally {
                handleTransaction();
            }

            try {
                getTransactionManager().begin();
                // remove a node that wasn't in the group - negative testing
                try {
                    // passing the "real" expected list for the results; this way, if the exception doesn't happen, the helper returns true
                    Resource nodeBigB = ResourceTreeHelper.findNode(fullTree, "B");
                    implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeBigB,
                        ResourceTreeHelper.getSubtree(nodeBigA));
                    assert false : "Failed: removed non-existent successfully: node = " + nodeBigB.getName();
                } catch (Throwable t) {
                    // expected
                }
            } catch (Throwable t) {
                handleThrowable(t);
            } finally {
                handleTransaction();
            }

            try {
                getTransactionManager().begin();
                resultsExplicit = resourceManager.findExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                verifyEqualByIds("explicit remove 0", new ArrayList<Resource>(), resultsExplicit);
            } catch (Throwable t) {
                handleThrowable(t);
            } finally {
                handleTransaction();
            }

        } finally {
            // clean up anything that may have gotten created
            try {
                getTransactionManager().begin();

                EntityManager em = getEntityManager();

                Subject overlord = subjectManager.getOverlord();

                if (null != subject) {
                    subjectManager.deleteUsers(overlord, new int[] { subject.getId() });
                }
                if (null != role) {
                    roleManager.deleteRoles(overlord, new int[] { role.getId() });
                }
                if (null != recursiveGroup) {
                    resourceGroupManager.deleteResourceGroup(overlord, recursiveGroup.getId());
                }

                if (null != fullTree) {
                    ResourceTreeHelper.deleteForest(em, fullTree);
                }

            } catch (Throwable t) {
                handleThrowable(t);
            } finally {
                handleTransaction();
            }
        }
    }

    private void handleTransaction() {
        try {
            if (getTransactionManager().getTransaction().getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                getTransactionManager().rollback();
            } else {
                getTransactionManager().commit();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void handleThrowable(Throwable t) throws Throwable {
        if (t instanceof SQLException) {
            String error = JDBCUtil.convertSQLExceptionToString((SQLException) t);
            System.err.println(error);
        } else {
            t.printStackTrace();
        }
        throw t;
    }

    private void printGroup(String prefix, Subject subject, ResourceGroup group) {
        print(prefix + ": exp",
            resourceManager.findExplicitResourcesByResourceGroup(subject, group, PageControl.getUnlimitedInstance()));
        print(prefix + ": imp",
            resourceManager.findImplicitResourcesByResourceGroup(subject, group, PageControl.getUnlimitedInstance()));
    }

    private void print(String prefix, List<Resource> resources) {
        System.out.print(prefix + ": ");
        for (Resource res : resources) {
            System.out.print(res.getName() + " ");
        }
        System.out.println();
    }

    private void implicitGroupMembershipAddHelper(Subject subject, ResourceGroup recursiveGroup, Resource node,
        List<Resource> expectedResults) {
        printGroup("complex implicit before add: node = " + node.getName() + " [" + node.getId() + "]", subject,
            recursiveGroup);
        resourceGroupManager.addResourcesToGroup(subject, recursiveGroup.getId(), new int[] { node.getId() });
        printGroup("complex implicit after add: node = " + node.getName() + " [" + node.getId() + "]", subject,
            recursiveGroup);
        List<Resource> implicitResources = resourceManager.findImplicitResourcesByResourceGroup(subject,
            recursiveGroup, PageControl.getUnlimitedInstance());
        verifyEqualByIds("Failed: complex implicit add: node = " + node.getName() + " [" + node.getId() + "]",
            expectedResults, implicitResources);
    }

    private void implicitGroupMembershipRemoveHelper(Subject subject, ResourceGroup recursiveGroup, Resource node,
        List<Resource> expectedResults) {
        printGroup("complex implicit before remove: node = " + node.getName() + " [" + node.getId() + "]", subject,
            recursiveGroup);
        resourceGroupManager.removeResourcesFromGroup(subject, recursiveGroup.getId(), new int[] { node.getId() });
        printGroup("complex implicit after remove: node = " + node.getName() + " [" + node.getId() + "]", subject,
            recursiveGroup);
        List<Resource> implicitResources = resourceManager.findImplicitResourcesByResourceGroup(subject,
            recursiveGroup, PageControl.getUnlimitedInstance());
        verifyEqualByIds("Failed: complex implicit remove: node = " + node.getName() + " [" + node.getId() + "]",
            expectedResults, implicitResources);
    }

    private void verifyEqualByIds(String errorMessage, List<Resource> expected, List<Resource> results) {
        List<Integer> expectedIds = getIds(expected);
        List<Integer> resultsIds = getIds(results);
        verifyEqual(errorMessage, expectedIds, resultsIds);
    }

    private void verifyEqual(String errorMessage, List<Integer> expectedIds, List<Integer> resultsIds) {
        assert (expectedIds.containsAll(resultsIds) && resultsIds.containsAll(expectedIds)) : errorMessage
            + "\nexpected = " + expectedIds + "\nresults = " + resultsIds;
    }

    private List<Integer> getIds(List<Resource> resources) {
        List<Integer> results = new ArrayList<Integer>();
        for (Resource res : resources) {
            results.add(res.getId());
        }
        Collections.sort(results);
        return results;
    }

    private List<Resource> getSimpleTree(EntityManager em) {
        return ResourceTreeHelper.createTree(em, "A=1,2; 1=a,b; a=i,ii; b=iii,iv; B=3");
    }

}