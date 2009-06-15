package org.rhq.enterprise.client.commands;

import java.util.ArrayList;
import java.util.List;

import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RHQRemoteClient;
import org.rhq.enterprise.server.ws.AvailabilityType;
import org.rhq.enterprise.server.ws.OperationManagerRemote;
import org.rhq.enterprise.server.ws.Resource;
import org.rhq.enterprise.server.ws.ResourceManagerRemote;
import org.rhq.enterprise.server.ws.ResourceOperationSchedule;
import org.rhq.enterprise.server.ws.Subject;
import org.rhq.enterprise.server.ws.SubjectManagerRemote;

public class StartJBossAsCommand implements ClientCommand {

    public boolean execute(ClientMain client, String[] args) {
        try {
            // check to see if user logged in
            if (client.getSubject() == null) {
                client.getPrintWriter().println("Unable to start JBossAS instances until successfully logged in");
                return true;
            }

            // Retrieve the parameters passed in
            // String example="Ex. startJBossAS 09875";
            // String syntax = "startJBossAS (*)resource.id";
            String applicationServerResourcecId = args[1] + "";
            // do type checking
            int asId = Integer.valueOf(applicationServerResourcecId);

            // instantiate SLSB ref
            ResourceManagerRemote res = client.getRemoteClient().getResourceManagerRemote();
            OperationManagerRemote operationManager = client.getRemoteClient().getOperationManagerRemote();
            ResourceOperationSchedule schedule = null;
            // check for instance
            Resource resource = res.getResourceById(client.getSubject(), asId);
            if ((resource != null) && (resource.getCurrentAvailability() != null)) {
                // check for down
                if (resource.getCurrentAvailability().getAvailabilityType().compareTo(AvailabilityType.DOWN) == 0) {
                    // send the command
                    try {
                        schedule = operationManager.scheduleResourceOperation(client.getSubject(), resource.getId(),
                            "start", 0L, 0L, 0, 0, null, "Remote Client from:" + client.getHost());
                    } catch (Exception ex) {
                        // TODO: fix .. eat for now.
                    }
                    client.getPrintWriter().println(
                        "The command to start JBossAS instance with id '" + asId
                            + "' has been sent. Please allow a few moments for the requested action to occur.");
                } else {// Instance already has availability status of DOWN
                    client.getPrintWriter().println(
                        "The JBossAS instance with id '" + asId + "' is already started. No action taken.");
                    return true;
                }
            }
        } catch (Exception e) {
            client.getPrintWriter().println("Attempt to start JBossAS instance failed: " + e.getMessage());
        }

        return true;
    }

    public String getDetailedHelp() {
        String detailed = "";
        return detailed;
    }

    public String getHelp() {
        return "Starts a stopped (non-RHQ) AS instance.";
    }

    public String getPromptCommandString() {
        return "startJBossAS";
    }

    public String getSyntax() {
        String example = "Ex. startJBossAS 09875";
        String syntax = "startJBossAS (*)resource.id";

        return example + "\n" + syntax;
    }

}
