package test;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.profileservice.spi.ProfileService;

public class RmiClient
{
   private static final String NAMING_CONTEXT_FACTORY = "org.jnp.interfaces.NamingContextFactory";

   private static final String PROFILE_SERVICE_JNDI_NAME = "ProfileService";
   
   public static void main(String[] args)
      throws Exception
   {           
      Properties env = new Properties();
      env.setProperty(Context.PROVIDER_URL, "jnp://127.0.0.1:1099");
      env.setProperty(Context.INITIAL_CONTEXT_FACTORY, NAMING_CONTEXT_FACTORY);
      env.setProperty("jnp.disableDiscovery", "true");      
      env.setProperty("jnp.timeout", "120");
      env.setProperty("jnp.sotimeout", "120");
      InitialContext initialContext = new InitialContext(env);
      
      ProfileService profileService = (ProfileService)initialContext.lookup(PROFILE_SERVICE_JNDI_NAME);      
      System.err.println("ProfileService: " + profileService);
      ManagementView managementView = profileService.getViewManager();
      System.err.println("ManagementView: " + managementView);     
      DeploymentManager deploymentManager = profileService.getDeploymentManager();
      System.err.println("DeploymentManager: " + deploymentManager);      
      
      profileService.getDomains();
      profileService.getProfileKeys();
      managementView.load();
      managementView.getDeploymentNames();      
      deploymentManager.getProfiles();
   }
}
