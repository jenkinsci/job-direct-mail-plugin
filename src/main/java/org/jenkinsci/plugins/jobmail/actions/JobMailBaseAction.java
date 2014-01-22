package org.jenkinsci.plugins.jobmail.actions;

//import hudson.plugins.emailext.*;
//import org.jenkins-ci.plugins.*;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.jobmail.utils.Constants;

import hudson.model.Action;
import hudson.model.Actionable;
import hudson.security.Permission;

/**
 * Base action class which other action classes extend. Contains some methods
 * that are common for all actions, like name, icon url and so on.
 * 
 * @author yboev
 * 
 */
public class JobMailBaseAction extends Actionable implements Action {

    // private static final Logger LOGGER =
    // Logger.getLogger(JobMailBaseAction.class.getName());

    /**
     * The display name for the action.
     * 
     * @return the name as String
     */
    public final String getDisplayName() {
        return this.hasPermission() ? Constants.NAME : null;
    }

    /**
     * The icon for this action.
     * 
     * @return the icon file as String
     */
    public final String getIconFileName() {
        return this.hasPermission() ? Constants.ICONFILENAME : null;
    }

    /**
     * The url for this action.
     * 
     * @return the url as String
     */
    public String getUrlName() {
        return this.hasPermission() ? Constants.URL : null;
    }

    /**
     * Search url for this action.
     * 
     * @return the url as String
     */
    public String getSearchUrl() {
        return this.hasPermission() ? Constants.URL : null;
    }

    /**
     * Checks if the user has CONFIGURE permission.
     * 
     * @return true - user has permission, false - no permission.
     */
    private boolean hasPermission() {
        return Jenkins.getInstance().hasPermission(Permission.CONFIGURE);
    }

}
