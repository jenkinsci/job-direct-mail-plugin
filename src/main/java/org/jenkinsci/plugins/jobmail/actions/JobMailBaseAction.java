package org.jenkinsci.plugins.jobmail.actions;

//import hudson.plugins.emailext.*;
//import org.jenkins-ci.plugins.*;
import org.jenkinsci.plugins.jobmail.utils.Constants;

import hudson.model.Action;
import hudson.model.Actionable;

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
        return Constants.NAME;
    }

    /**
     * The icon for this action.
     * 
     * @return the icon file as String
     */
    public final String getIconFileName() {
        return Constants.ICONFILENAME;
    }

    /**
     * The url for this action.
     * 
     * @return the url as String
     */
    public String getUrlName() {
        return Constants.URL;
    }

    /**
     * Search url for this action.
     * 
     * @return the url as String
     */
    public String getSearchUrl() {
        return Constants.URL;
    }
}
