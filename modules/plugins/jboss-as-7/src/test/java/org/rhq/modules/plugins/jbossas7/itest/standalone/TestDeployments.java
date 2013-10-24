package org.rhq.modules.plugins.jbossas7.itest.standalone;

import java.io.IOException;
import java.io.InputStream;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.stream.StreamUtil;

/**
* @author Thomas Segismont
*/
enum TestDeployments {
    DEPLOYMENT_1("test-simple.war"), //
    DEPLOYMENT_2("test-simple-2.war"), //
    JAVAEE6_TEST_APP("javaee6-test-app.war");

    private String deploymentName;
    private String resourcePath;
    private byte[] hash;

    TestDeployments(String warName) {
        this.deploymentName = warName;
        this.resourcePath = "itest/" + warName;
        InputStream resourceAsStream = DeploymentTest.class.getClassLoader().getResourceAsStream(resourcePath);
        try {
            hash = MessageDigestGenerator.getDigest(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            StreamUtil.safeClose(resourceAsStream);
        }
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public byte[] getHash() {
        return hash;
    }
}
