/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.client.security.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.SerializablePermission;
import java.security.PermissionCollection;
import java.util.Collections;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.bindings.SandboxedScriptEngine;
import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.StandardScriptPermissions;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.LocalClient;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.jndi.AllowRhqServerInternalsAccessPermission;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class JndiAccessTest extends AbstractEJB3Test {

    public void testEjbsAccessibleThroughPrivilegedCode() {
        LookupUtil.getSubjectManager().getOverlord();
    }
    
    public void testEjbsAccessibleThroughLocalClient() throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        
        ScriptEngine engine = getEngine(overlord);
        
        engine.eval("SubjectManager.getSubjectByName('rhqadmin');");
    }
    
    public void testLocalEjbsInaccessibleThroughJndiLookup() throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        
        ScriptEngine engine = getEngine(overlord);
        
        try {
            engine.eval(""
                + "context = new javax.naming.InitialContext();\n"
                + "subjectManager = context.lookup('SubjectManagerBean/local');\n"
                + "subjectManager.getOverlord();");
            
            Assert.fail("The script shouldn't have been able to call local SLSB method.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }
    }
    
    public void testRemoteEjbsInaccessibleThroughJndiLookup() throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        
        ScriptEngine engine = getEngine(overlord);
        
        try {
            engine.eval(""
                + "context = new javax.naming.InitialContext();\n"
                + "subjectManager = context.lookup('SubjectManagerBean/remote');\n"
                + "subjectManager.getSubjectByName('rhqadmin');");
            
            Assert.fail("The script shouldn't have been able to call remote SLSB method directly.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }
    }
    
    public void testScriptCantUseSessionManagerMethods() throws Exception {

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();        
        
        final ScriptEngine engine = getEngine(overlord);
            
        class G {
            private String sessionManager = ""
                + "org.rhq.enterprise.server.auth.SessionManager.getInstance().";
            
            public void testInvoke(String methodCall) throws ScriptException {
                String code = sessionManager + methodCall;

                try {
                    engine.eval(code);               
                    Assert.fail("The script shouldn't have been able to call a method on a SessionManager: " + methodCall);
                } catch (ScriptException e) {
                    checkIsDesiredSecurityException(e);
                }
            }
        };
        G manager = new G();
        
        manager.testInvoke("getlastAccess(0);");
        manager.testInvoke("getOverlord()");
        manager.testInvoke("getSubject(2);");
        manager.testInvoke("invalidate(0);");
        manager.testInvoke("invalidate(\"\");");
        manager.testInvoke("put(new org.rhq.core.domain.auth.Subject());");
        manager.testInvoke("put(new org.rhq.core.domain.auth.Subject(), 0);");
    }
    
    public void testScriptCantObtainRawJDBCConnectionsWithoutCredentials() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        
        ScriptEngine engine = getEngine(overlord);
        
        try {
            engine.eval(""
                + "context = new javax.naming.InitialContext();\n"
                + "datasource = context.lookup('java:/RHQDS');\n"
                + "con = datasource.getConnection();");
            
            Assert.fail("The script shouldn't have been able to obtain the datasource from the JNDI.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }
    }
    
    public void testScriptCantUseEntityManager() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();        
        
        ScriptEngine engine = getEngine(overlord);

        try {
            engine.eval(""
                + "context = new javax.naming.InitialContext();\n"
                + "entityManagerFactory = context.lookup('java:/RHQEntityManagerFactory');\n"
                + "entityManager = entityManagerFactory.createEntityManager();\n"
                + "entityManager.find(java.lang.Class.forName('org.rhq.core.domain.resource.Resource'), java.lang.Integer.valueOf('10001'));");
            
            Assert.fail("The script shouldn't have been able to use the EntityManager.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }   
        
        //try harder with manually specifying the initial context factory
        try {
            engine.eval(""
                + "env = new java.util.Hashtable();"
                + "env.put('java.naming.factory.initial', 'org.jnp.interfaces.LocalOnlyContextFactory');"
                + "env.put('java.naming.factory.url.pkgs', 'org.jboss.naming:org.jnp.interfaces');"
                + "context = new javax.naming.InitialContext(env);\n"
                + "entityManagerFactory = context.lookup('java:/RHQEntityManagerFactory');\n"
                + "entityManager = entityManagerFactory.createEntityManager();\n"
                + "entityManager.find(java.lang.Class.forName('org.rhq.core.domain.resource.Resource'), java.lang.Integer.valueOf('10001'));");
            
            Assert.fail("The script shouldn't have been able to use the EntityManager even using custom initial context factory.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }           
    }
    
    private ScriptEngine getEngine(Subject subject) throws ScriptException, IOException {
        StandardBindings bindings = new StandardBindings(new PrintWriter(System.out), new LocalClient(subject));
        ScriptEngine engine = ScriptEngineFactory.getScriptEngine("JavaScript", new PackageFinder(Collections.<File>emptyList()), bindings);
        
        PermissionCollection perms = new StandardScriptPermissions();
        perms.add(new SerializablePermission("enableSubclassImplementation"));
        
        return new SandboxedScriptEngine(engine, perms);
    }
    
    private static void checkIsDesiredSecurityException(ScriptException e) {
        String message = e.getMessage();
        String permissionTrace = AllowRhqServerInternalsAccessPermission.class.getName();
        
        Assert.assertTrue(message.contains(permissionTrace), "The script exception doesn't seem to be caused by the AllowRhqServerInternalsAccessPermission security exception. " + message);
    }
}
