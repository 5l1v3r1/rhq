package test;

import java.io.File;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.profileservice.spi.DeploymentOption;
import org.jboss.profileservice.spi.ProfileService;

import java.net.URL;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EjbClient
{   
   private static final String JNDI_LOGIN_INITIAL_CONTEXT_FACTORY = "org.jboss.security.jndi.JndiLoginInitialContextFactory";

   private static final String SECURE_PROFILE_SERVICE_JNDI_NAME = "SecureProfileService/remote";
   private static final String SECURE_MANAGEMENT_VIEW_JNDI_NAME = "SecureManagementView/remote";
   private static final String SECURE_DEPLOYMENT_MANAGER_JNDI_NAME = "SecureDeploymentManager/remote";
   
   private static final String PROFILE_SERVICE_PRINCIPAL = "admin";
   private static final String PROFILE_SERVICE_CREDENTIALS = "admin";
   
   public static void main(String[] args)
      throws Exception
   {
      //System.setProperty("org.jboss.security.SecurityAssociation.ThreadLocal", "false");

      Properties env = new Properties();
      env.setProperty(Context.PROVIDER_URL, "jnp://127.0.0.1:1099/");
      env.setProperty(Context.INITIAL_CONTEXT_FACTORY, JNDI_LOGIN_INITIAL_CONTEXT_FACTORY);
      env.setProperty(Context.SECURITY_PRINCIPAL, PROFILE_SERVICE_PRINCIPAL);
      env.setProperty(Context.SECURITY_CREDENTIALS, PROFILE_SERVICE_CREDENTIALS);      
      
      InitialContext initialContext = createInitialContext(env);
      ProfileService profileService = (ProfileService)lookup(initialContext, SECURE_PROFILE_SERVICE_JNDI_NAME);      
      ManagementView managementView = (ManagementView)lookup(initialContext, SECURE_MANAGEMENT_VIEW_JNDI_NAME);     
      DeploymentManager deploymentManager = (DeploymentManager)lookup(initialContext, SECURE_DEPLOYMENT_MANAGER_JNDI_NAME);      
      
      profileService.getDomains();
      profileService.getProfileKeys();            
      managementView.load();
      managementView.getDeploymentNames();  
      deploymentManager.getProfiles();      

      tryToDeploySomething(deploymentManager);
      
      Worker worker = new Worker(managementView);
      worker.run();
      for (int i = 0; i < 50; i++) {
          Thread.sleep(100);
          worker = new Worker(managementView);
          Thread thread = new Thread(worker);
          thread.start();          
      }
      Thread.sleep(50);
      worker = new Worker(managementView);
      worker.run();       
   }
   
    private static InitialContext createInitialContext(Properties env) throws NamingException
    { 
        System.out.println("Creating JNDI InitialContext with env [" + env + "]...");              
        InitialContext initialContext = new InitialContext(env);
        System.out.println("Created JNDI InitialContext [" + initialContext + "].");
        return initialContext;
    }

    private static Object lookup(InitialContext initialContext, String name) throws NamingException
    {
        System.out.println("Looking up name '" + name + "' from InitialContext...");              
        Object obj = initialContext.lookup(name);
        System.out.println("Found Object: " + obj);
        return obj;
    }   
    
    private static void tryToDeploySomething(DeploymentManager deploymentManager) {
        File tmpFile = null;
        
        try {
            tmpFile = File.createTempFile("ejb-client-test", ".war");
            InputStream dummyWar = EjbClient.class.getClassLoader().getResourceAsStream("dummy.war");
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile));
            byte[] buffer = new byte[16384];
            
            int cnt = 0;
            while ((cnt = dummyWar.read(buffer)) >= 0) {
                out.write(buffer, 0, cnt);
            }
            dummyWar.close();
            out.close();
        } catch (IOException e) {
            return;
        }
        try {
            URL url = tmpFile.toURI().toURL();
            DeploymentProgress progress = deploymentManager.distribute(tmpFile.getName(), url, new DeploymentOption[] {});
            progress.run();
            
            if (!progress.getDeploymentStatus().isFailed()) {
                String[] deploymentNames = progress.getDeploymentID().getRepositoryNames();
                progress = deploymentManager.start(deploymentNames);
                progress.run();
            }
        } catch (Exception e) {
            System.out.println("Deployment failed");
            e.printStackTrace();
        } finally {
            tmpFile.delete();
        }
    }
}
