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

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.apache.tools.ant.taskdefs.LoadProperties;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.PackageCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A form to configure the CLI script alert notification.
 *
 * @author Lukas Krejci
 */
public class CliNotificationSenderForm extends AbstractNotificationSenderForm {

    private static final String PROP_PACKAGE_ID = "packageId";
    private static final String PROP_REPO_ID = "repoId";
    private static final String PROP_USER_ID = "userId";

    boolean formBuilt;
    
    private SelectItem repoSelector;
    private SelectItem packageSelector;
    private RadioGroupWithComponentsItem userSelector;
    
    private PackageType cliScriptPackageType;
    private static final String PACKAGE_TYPE_NAME = "__SERVER_SIDE_CLI_SCRIPT";
    
    private static class Config {
        List<Repo> allRepos;
        Repo selectedRepo;
        
        List<Package> allPackages;
        Package selectedPackage;
        
        Subject selectedSubject;
        
        /*
         * This is a counter to keep track if all the async
         * loading of the above data has finished.
         * 
         * In principle, this should be AtomicInteger but GWT didn't like it.
         * 
         * The reason why it still works as a normal integer even though it 
         * is being updated from within the AsyncCallbacks is that the javascript
         * engines in the browsers are single threaded and therefore, even though
         * the data is loaded in the background, it is processed (and this variable
         * updated) only in that single thread.
         */
        int __handlerCounter = 3;
        
        public void setSelectedRepo(int repoId) {
            if (allRepos != null) {
                for(Repo r : allRepos) { 
                    if (r.getId() == repoId) {
                        selectedRepo = r;
                        break;
                    }
                }
            }
        }

        public void setSelectedPackage(int packageId) {
            if (allPackages != null) {
                for(Package p : allPackages) { 
                    if (p.getId() == packageId) {
                        selectedPackage = p;
                        break;
                    }
                }
            }
        }
    }
    
    public CliNotificationSenderForm(String locatorId, AlertNotification notif, String sender) {
        super(locatorId, notif, sender);
    }

    @Override
    protected void onInit() {
        super.onInit();
        
        if (!formBuilt) {     
            LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("form"));
            
            repoSelector = new SelectItem("repoSelector", "Repository:"); //TODO i18n
            repoSelector.setDefaultToFirstOption(false);
            repoSelector.setWrapTitle(false);
            repoSelector.setRedrawOnChange(true);
            repoSelector.setWidth("*");            
            repoSelector.setValueMap(MSG.common_msg_loading());
            repoSelector.setDisabled(true);

            packageSelector = new SelectItem("packageSelector", "Script:"); //TODO i18n
            packageSelector.setDefaultToFirstOption(false);
            packageSelector.setWrapTitle(false);
            packageSelector.setRedrawOnChange(true);
            packageSelector.setWidth("*");            
            packageSelector.setValueMap(MSG.common_msg_loading());
            packageSelector.setDisabled(true);

            DynamicForm anotherUserForm = createAnotherUserForm();
            
            LinkedHashMap<String, DynamicForm> userSelectItems = new LinkedHashMap<String, DynamicForm>();
            
            userSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_thisUser(), null);
            userSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_anotherUser(), anotherUserForm);
            
            userSelector = new RadioGroupWithComponentsItem(
                extendLocatorId("userSelector"), 
                MSG.view_alert_definition_notification_cliScript_editor_whichUser(), userSelectItems, form);
                        
            form.setFields(repoSelector, packageSelector, userSelector);
            addMember(form);
            
            loadPackageType(new AsyncCallback<PackageType>() {
                public void onFailure(Throwable t) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_alert_definition_notification_cliScript_editor_loadFailed(),
                        t);
                }
                
                public void onSuccess(PackageType result) {
                    cliScriptPackageType = result;

                    loadConfig(new AsyncCallback<Config>() {
                        public void onFailure(Throwable t) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_alert_definition_notification_cliScript_editor_loadFailed(),
                                t);
                        }
                        
                        public void onSuccess(Config config) {
                            setupRepoSelector(config);                    
                            setupPackageSelector(config);                    
                            setupUserSelector(config);
                        }
                    });
                }
            });
            
            formBuilt = true;
        }
    }
    
    private void setupUserSelector(Config config) {
        if (config.selectedSubject != null && !UserSessionManager.getSessionSubject().equals(config.selectedSubject)) {
            //TODO select the second radio and put it the user name..
        }
    }

    private void setupPackageSelector(Config config) {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for(Package p : config.allPackages) {
            map.put(String.valueOf(p.getId()), p.getName());
        }
        
        packageSelector.setValueMap(map);
        if (config.selectedPackage != null) {
            packageSelector.setValue(config.selectedPackage.getId());
        } else {
            packageSelector.setValue("");
        }
        
        packageSelector.setDisabled(false);
    }

    private void setupRepoSelector(final Config config) {        
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for(Repo r : config.allRepos) {
            map.put(String.valueOf(r.getId()), r.getName());
        }
        
        repoSelector.setValueMap(map);
        if (config.selectedRepo != null) {
            repoSelector.setValue(config.selectedRepo.getId());
        } else {
            repoSelector.setValue("");
        }
        
        repoSelector.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                Integer repoId = Integer.valueOf(event.getItem().getValue().toString());
                config.setSelectedRepo(repoId);
                
                PackageCriteria pc = new PackageCriteria();
                pc.addFilterRepoId(repoId);
                pc.addFilterPackageTypeId(cliScriptPackageType.getId());
                
                packageSelector.setDisabled(true);
                packageSelector.setValueMap(MSG.common_msg_loading());
                                
                GWTServiceLookup.getContentService().findPackagesByCriteria(pc, new AsyncCallback<PageList<Package>>() {
                    public void onSuccess(PageList<Package> result) {
                        config.allPackages = result;
                        config.selectedPackage = null;
                        setupPackageSelector(config);
                    }
                    
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_alert_definition_notification_cliScript_editor_loadFailed(),
                            caught);
                    }
                });                
            }
        });
        
        repoSelector.setDisabled(false);
    }

    public boolean validate() {
        // TODO implement
        return true;
    }
    
    private void loadConfig(final AsyncCallback<Config> handler) {
        final String repoId = getConfiguration().getSimpleValue(PROP_REPO_ID, null);
        final String packageId = getConfiguration().getSimpleValue(PROP_PACKAGE_ID, null);
        final String subjectId = getConfiguration().getSimpleValue(PROP_USER_ID, null);

        final Config config = new Config();

        RepoCriteria rc = new RepoCriteria();
        
        GWTServiceLookup.getRepoService().findReposByCriteria(rc, new AsyncCallback<PageList<Repo>>() {
            public void onSuccess(PageList<Repo> result) {
                config.allRepos = result;
                
                if (repoId != null && repoId.trim().length() > 0) {
                    final int rid = Integer.parseInt(repoId);
                    config.setSelectedRepo(rid);
                }
                
                if (--config.__handlerCounter == 0) {
                    handler.onSuccess(config);
                }
            }

            public void onFailure(Throwable caught) {
                handler.onFailure(caught);
            }
        });
        
        if (repoId != null && repoId.trim().length() > 0) {
            PackageCriteria pc = new PackageCriteria();
            pc.addFilterRepoId(Integer.parseInt(repoId));
            pc.addFilterPackageTypeId(cliScriptPackageType.getId());
            
            GWTServiceLookup.getContentService().findPackagesByCriteria(pc, new AsyncCallback<PageList<Package>>() {
                public void onSuccess(PageList<Package> result) {
                    config.allPackages = result;
                    
                    if (packageId != null && packageId.trim().length() > 0) {
                        int pid = Integer.parseInt(packageId);
                        config.setSelectedPackage(pid);
                    }

                    if (--config.__handlerCounter == 0) {
                        handler.onSuccess(config);
                    }
                }
                
                public void onFailure(Throwable caught) {
                    handler.onFailure(caught);
                }
            });
        } else {
            config.allPackages = Collections.emptyList();
            --config.__handlerCounter;
        }
        
        if (subjectId != null && subjectId.trim().length() > 0) {
           int sid = Integer.parseInt(subjectId);
           SubjectCriteria c = new SubjectCriteria();
           c.addFilterId(sid);
           
           GWTServiceLookup.getSubjectService().findSubjectsByCriteria(c, new AsyncCallback<PageList<Subject>>() {
               public void onSuccess(PageList<Subject> result) {
                   if (result.size() > 0) {
                       config.selectedSubject = result.get(0);
                   }

                   if (--config.__handlerCounter == 0) {
                       handler.onSuccess(config);
                   }
               }
               
               public void onFailure(Throwable caught) {
                   handler.onFailure(caught);
               }
           });
        } else {
            --config.__handlerCounter;
        }
    }
    
    DynamicForm createAnotherUserForm() {
        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("anotherUserForm"));
        
        TextItem userNameItem = new TextItem("userName", MSG.dataSource_users_field_name());
        PasswordItem passwordItem = new PasswordItem("password", MSG.dataSource_users_field_password());
        ButtonItem verifyItem = new ButtonItem("verify", MSG.view_alert_definition_notification_cliScript_editor_verifyAuthentication());
        form.setFields(userNameItem, passwordItem, verifyItem);
        
        return form;
    }
    
    private void loadPackageType(AsyncCallback<PackageType> handler) {
        GWTServiceLookup.getContentService().findPackageType(null, PACKAGE_TYPE_NAME, handler);
    }
    
    
}       
