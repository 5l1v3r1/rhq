package org.rhq.enterprise.server.content.test;

import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionType;
import org.rhq.enterprise.server.content.DistributionManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Pradeep Kilambi
 */
public class DistributionManagerBeanTest extends AbstractEJB3Test {
    private final static boolean ENABLED = true;
    private DistributionManagerLocal distManager;
    private DistributionType distType;
    private Subject overlord;

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {
        TransactionManager tx = getTransactionManager();
        tx.begin();

        distManager = LookupUtil.getDistributionManagerLocal();
        distType = new DistributionType("kickstart");

        overlord = LookupUtil.getSubjectManager().getOverlord();
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {
        TransactionManager tx = getTransactionManager();
        if (tx != null) {
            tx.rollback();
        }
    }

    @Test(enabled = ENABLED)
    public void createDeleteDistribution() throws Exception {

        String kslabel = "testCreateDeleteRepo";
        String kspath = "/tmp";
        int id = distManager.createDistribution(overlord, kslabel, kspath, distType).getId();
        Distribution distro = distManager.getDistributionByLabel(kslabel);

        assert distro != null;

        assert id == distro.getId();

        distManager.deleteDistributionByDistId(overlord, id);
        distro = distManager.getDistributionByLabel(kslabel);
        assert distro == null;
    }
}
