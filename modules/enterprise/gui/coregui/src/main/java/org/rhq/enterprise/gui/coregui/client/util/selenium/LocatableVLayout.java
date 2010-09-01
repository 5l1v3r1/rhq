package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.layout.VLayout;

/**
 * Wrapper for com.smartgwt.client.widgets.layout.VLayout that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableVLayout extends VLayout implements Locatable {

    private String locatorId;

    /** Not Recommended */
    public LocatableVLayout() {
        this("DEFAULT_ID");
    }

    /** 
     * <pre>
     * ID Format: "scClassname-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableVLayout(String locatorId) {
        super();
        init(locatorId);
    }

    /** 
     * <pre>
     * ID Format: "scClassname-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     * @param membersMargin 
     */
    public LocatableVLayout(String locatorId, int membersMargin) {
        super(membersMargin);
        init(locatorId);
    }

    private void init(String locatorId) {
        this.locatorId = locatorId;
        SeleniumUtility.setID(this, locatorId);
    }

    public String getLocatorId() {
        return locatorId;
    }

    public String extendLocatorId(String extension) {
        return this.locatorId + "-" + extension;
    }

    public void destroyMembers() {
        SeleniumUtility.destroyMembers(this);
    }

}
