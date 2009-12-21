package org.rhq.sample.perspective;

import java.util.Iterator;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;

/**
 * When creating Groups, Roles and the Users controlled by them, the following
 * order seems to work with the least pain and redirection.
 * 
 * 1) Create the 'Everything' Group. a) Go to Resources -> Platforms and click
 * on 'New Group' link. Name the group, go 'Mixed Resources' and go recursive.
 * 
 * Rinse and repeat as many times as necessary ... 2) Create [desired role] with
 * appropriate permissions and at the end add the 'Everything' Group to the role
 * 3) Go to Administration -> Security -> Users and create the 'New User' and
 * select the previously defined 'Role' to assign to the current user.
 * 
 * Once you get this motion down it's less disjoint to do typical authorization.
 * 
 * @author Simeon Pinder
 * 
 */
public class WizardBean {

    // Fields
    private static String NOT_YET_SET = "";
    private String title = "Creating a New JBoss Operator account...";
    private String titleNote = "";

    //----------------- Defines Wizard Steps ---------------------------------//
    enum Step {
        One, Two, Three, Confirm, Complete;
        boolean completed = false;

        boolean completed() {
            return completed;
        }

        public String getName() {
            return this.name();
        }

        public void setName() {
        }
    }

    //Session variables to cache initialized components for final transaction.
    private Step currentStep = Step.One;
    private String start = null;
    private String end = null;
    private RemoteClient remoteClient = null;
    private Subject subject = null;
    private ResourceGroup resourceGroup = null;
    private Role role = null;
    private Subject newSubject = null;

    //-------------------- Variable Definition by STEP -----------------------------
    //// STEP 1: Create the group to see all values.(See )
    private String groupName = NOT_YET_SET; //REQUIRED
    private String groupDescription = "";
    private String groupLocation = "";
    private boolean isRecursive = false;

    //define enumeration to enforce type restriction.
    enum Group {//Compatible == homogeneous AND mixed != homogeneous
        Compatible, Mixed;
    }

    private String groupType = Group.Mixed.name(); //REQUIRED: defaults to mixed to all resources show up
    private String step1Note = "By choosing 'Next' a new group will be created and persisted to the database.";

    //// STEP 2: Create appropriate Role
    private String roleName = NOT_YET_SET; //REQUIRED
    private String roleDescription = "";
    private String roleDescriptoinNote = "Limit desciption to 100 characters.";

    /////GLOBAL Permissions
    private boolean manageSecurityEnabled = false;
    private String manageSecurityNote = "**(users/roles) --This permission "
        + "implicitly grants (and explicitly forces selection of) all other permissions";
    private boolean manageInventoryEnabled = false;
    private String manageInventoryNote = "(resources/groups)";
    private boolean administerRhqServerSettingsEnabled = false;

    /////RESOURCE Permissions
    private boolean modifyEnabled = false;
    private boolean deleteEnabled = false;
    private boolean createChildrenEnabled = false;
    private boolean alertEnabled = false;
    private boolean measureEnabled = false;
    private boolean contentEnabled = false;
    private boolean controlEnabled = false;
    private boolean configureEnabled = false;
    private String step2Note = "By choosing 'Next' a new Role will be created and persisted to the database.";

    //// STEP 3: Create appropriate user
    private String firstName = NOT_YET_SET; //REQUIRED
    private String lastName = "";
    private String newUserName = NOT_YET_SET;//REQUIRED
    private String step3Note = "By choosing 'Complete', the 'Create Jon Administrator' user will be completed.";
    private String phone;
    private String email;
    private String department;
    private String password; //REQUIRED
    private String password2; //REQUIRED
    private boolean enableLogin = true;

    //// STEP N:

    //// STEP N+1:

    // Methods

    //----------------------------    The JSF event processor for all steps.---------------------
    public String processActions() {
        //Debug only: remove after:
        Iterator<FacesMessage> messages = FacesContext.getCurrentInstance().getMessages();
        if (messages.hasNext()) {
            while (messages.hasNext()) {
                FacesMessage msg = messages.next();
                System.out.println("---------MESG:" + msg.getDetail() + ":S:" + msg.getSummary());
            }
        }

        String stepCompleted = "(incomplete)";

        //lazy initialization.
        if (remoteClient == null) {
            remoteClient = new RemoteClient("127.0.0.1", 7080);
            try {
                subject = remoteClient.login("rhqadmin", "rhqadmin");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("There were problems connecting via RHQ Remote API: " + e.getMessage());
            }
        }

        //iterate over the steps to find out which fields to operate on
        for (Step screen : Step.values()) {

            if (!screen.completed()) {//operate on then bail
                switch (screen) {
                case One: //create Group for visibility
                    ResourceGroup rg = new ResourceGroup(groupName);
                    rg.setDescription(groupDescription);
                    rg.setLocation(groupLocation);
                    rg.setRecursive(isRecursive);
                    //TODO: figure out how to make these calls correctly.
                    String groupDefinition = "Compatible";
                    //                                      rg.getGroupCategory()
                    //                                      rg.
                    //                                      groupManager.createResourceGroup(subject, rg);
                    resourceGroup = rg;
                    stepCompleted = screen.name();
                    screen.completed = true;
                    setCurrentStep(Step.Two);
                    return stepCompleted;

                case Two: //create Role for permissions
                    Role role = new Role(roleName);
                    role.setDescription(roleDescription);
                    if (manageSecurityEnabled) {
                        role.addPermission(Permission.MANAGE_SECURITY);
                    }
                    if (manageInventoryEnabled) {
                        role.addPermission(Permission.MANAGE_INVENTORY);
                    }
                    if (administerRhqServerSettingsEnabled) {
                        role.addPermission(Permission.MANAGE_SETTINGS);
                    }
                    if (modifyEnabled) {
                        role.addPermission(Permission.MODIFY_RESOURCE);
                    }
                    if (deleteEnabled) {
                        role.addPermission(Permission.DELETE_RESOURCE);
                    }
                    if (createChildrenEnabled) {
                        role.addPermission(Permission.CREATE_CHILD_RESOURCES);
                    }
                    if (alertEnabled) {
                        role.addPermission(Permission.MANAGE_ALERTS);
                    }
                    if (measureEnabled) {
                        role.addPermission(Permission.MANAGE_MEASUREMENTS);
                    }
                    if (contentEnabled) {
                        role.addPermission(Permission.MANAGE_CONTENT);
                    }
                    if (controlEnabled) {
                        role.addPermission(Permission.CONTROL);
                    }
                    if (configureEnabled) {
                        role.addPermission(Permission.CONFIGURE);
                    }
                    //add everything group to role
                    role.addResourceGroup(resourceGroup);
                    this.role = role;

                    stepCompleted = screen.name();
                    screen.completed = true;
                    setCurrentStep(Step.Three);
                    //                                      break;
                    return stepCompleted;

                case Three: //create User and attach previous two
                    newSubject = new Subject();
                    newSubject.setDepartment(department);
                    newSubject.setEmailAddress(email);
                    newSubject.setFirstName(firstName);
                    newSubject.setFirstName(lastName);
                    newSubject.setName(newUserName);
                    newSubject.setPhoneNumber(phone);
                    newSubject.addRole(this.role);

                    stepCompleted = screen.name();
                    screen.completed = true;
                    setCurrentStep(Step.Confirm);
                    return stepCompleted;

                case Confirm: //create User and attach previous two
                    stepCompleted = screen.name();
                    screen.completed = true;
                    setCurrentStep(Step.Complete);
                    return stepCompleted;

                case Complete:// execute all operations as atomic operation
                    //do check for no null values
                    //commit group created
                    ResourceGroupManagerRemote groupManager = remoteClient.getResourceGroupManagerRemote();
                    groupManager.createResourceGroup(subject, resourceGroup);
                    //commit Role created
                    RoleManagerRemote roleManager = remoteClient.getRoleManagerRemote();
                    roleManager.createRole(subject, this.role);
                    //commit User previously created
                    SubjectManagerRemote subjectManager = remoteClient.getSubjectManagerRemote();
                    subjectManager.createSubject(subject, newSubject);
                    //null out all reference and reset Screens all to !completed
                    remoteClient.logout();
                    this.remoteClient = null;
                    this.role = null;
                    this.newSubject = null;
                    for (Step step : Step.values()) {
                        step.completed = false;
                    }
                    return stepCompleted;
                default:
                    System.out.println("Unrecognized screen condition. No processing is being done.");
                    break;
                }
            }
        }
        return stepCompleted;
    }

    //----- a few methods for navigation ----------------------
    /** Backs up the wizard process to previous or to the initial screen.*/
    public String processReverse() {
        Step prev = null;
        for (int i = 0; i < Step.values().length; i++) {
            Step screen = Step.values()[i];
            if (prev == null) {
                prev = screen;
            }//initialize to first screen
            if (screen.completed) {
                prev = screen;
            } else {
                prev.completed = false;
                setCurrentStep(prev);
                break;
            }
        }
        String reversedToStep = "(uninitialized)";
        if (prev != null)
            reversedToStep = prev.getName();
        return reversedToStep;
    }

    public Step[] getAllSteps() {
        return Step.values();
    }

    public String getStart() {
        if (start == null) {
            if (Step.values().length > 0) {
                start = Step.values()[0].getName();
            }
        }
        return start;
    }

    public String getEnd() {
        if (end == null) {
            if (Step.values().length > 0) {
                end = Step.values()[Step.values().length - 1].getName();
            }
        }
        return end;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        WizardBean b = new WizardBean();
        System.out.println("CURRENT STEP:" + b.getCurrentStep() + ":" + b.getCurrentStep().getName() + ":");
        b.processActions();//move to step 2
        b.processActions();//move to step 3
        b.processReverse();//move back to step 2
        b.processActions();//move to step 3
        b.processActions();//move to step Finish
    }

    ////##################   Generic Getter/Setter logic. ############################################
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTITLENote() {
        return titleNote;
    }

    public void setTitleNote(String titleNote) {
        this.titleNote = titleNote;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }

    public String getGroupLocation() {
        return groupLocation;
    }

    public void setGroupLocation(String groupLocation) {
        this.groupLocation = groupLocation;
    }

    public boolean isRecursive() {
        return isRecursive;
    }

    public void setRecursive(boolean isRecursive) {
        this.isRecursive = isRecursive;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public String getStep1Note() {
        return step1Note;
    }

    public void setStep1Note(String step1Note) {
        this.step1Note = step1Note;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }

    public String getRoleDescriptoinNote() {
        return roleDescriptoinNote;
    }

    public void setRoleDescriptoinNote(String roleDescriptoinNote) {
        this.roleDescriptoinNote = roleDescriptoinNote;
    }

    public boolean isManageSecurityEnabled() {
        return manageSecurityEnabled;
    }

    public void setManageSecurityEnabled(boolean manageSecurityEnabled) {
        this.manageSecurityEnabled = manageSecurityEnabled;
        if (this.manageInventoryEnabled) {
            //TODO: have to go through and enable/disable all depending upon boolean value.
        }
    }

    public String getManageSecurityNote() {
        return manageSecurityNote;
    }

    public void setManageSecurityNote(String manageSecurityNote) {
        this.manageSecurityNote = manageSecurityNote;
    }

    public boolean isManageInventoryEnabled() {
        return manageInventoryEnabled;
    }

    public void setManageInventoryEnabled(boolean manageInventoryEnabled) {
        this.manageInventoryEnabled = manageInventoryEnabled;
    }

    public String getManageInventoryNote() {
        return manageInventoryNote;
    }

    public void setManageInventoryNote(String manageInventoryNote) {
        this.manageInventoryNote = manageInventoryNote;
    }

    public boolean isAdministerRhqServerSettingsEnabled() {
        return administerRhqServerSettingsEnabled;
    }

    public void setAdministerRhqServerSettingsEnabled(boolean administerRhqServerSettingsEnabled) {
        this.administerRhqServerSettingsEnabled = administerRhqServerSettingsEnabled;
    }

    public boolean isModifyEnabled() {
        return modifyEnabled;
    }

    public void setModifyEnabled(boolean modifyEnabled) {
        this.modifyEnabled = modifyEnabled;
    }

    public boolean isDeleteEnabled() {
        return deleteEnabled;
    }

    public void setDeleteEnabled(boolean deleteEnabled) {
        this.deleteEnabled = deleteEnabled;
    }

    public boolean isCreateChildrenEnabled() {
        return createChildrenEnabled;
    }

    public void setCreateChildrenEnabled(boolean createChildrenEnabled) {
        this.createChildrenEnabled = createChildrenEnabled;
    }

    public boolean isAlertEnabled() {
        return alertEnabled;
    }

    public void setAlertEnabled(boolean alertEnabled) {
        this.alertEnabled = alertEnabled;
    }

    public boolean isMeasureEnabled() {
        return measureEnabled;
    }

    public void setMeasureEnabled(boolean measureEnabled) {
        this.measureEnabled = measureEnabled;
    }

    public boolean isContentEnabled() {
        return contentEnabled;
    }

    public void setContentEnabled(boolean contentEnabled) {
        this.contentEnabled = contentEnabled;
    }

    public boolean isControlEnabled() {
        return controlEnabled;
    }

    public void setControlEnabled(boolean controlEnabled) {
        this.controlEnabled = controlEnabled;
    }

    public boolean isConfigureEnabled() {
        return configureEnabled;
    }

    public void setConfigureEnabled(boolean configureEnabled) {
        this.configureEnabled = configureEnabled;
    }

    public String getStep2Note() {
        return step2Note;
    }

    public void setStep2Note(String step2Note) {
        this.step2Note = step2Note;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNewUserName() {
        return newUserName;
    }

    public void setNewUserName(String username) {
        this.newUserName = username;
    }

    public String getStep3Note() {
        return step3Note;
    }

    public void setStep3Note(String step3Note) {
        this.step3Note = step3Note;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    public boolean isEnableLogin() {
        return enableLogin;
    }

    public void setEnableLogin(boolean enableLogin) {
        this.enableLogin = enableLogin;
    }

    public Step getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Step currentStep) {
        this.currentStep = currentStep;
    }
}
