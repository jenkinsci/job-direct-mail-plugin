package org.jenkinsci.plugins;

import java.util.List;
import java.util.logging.Logger;

import hudson.Extension;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import jenkins.model.GlobalConfiguration;

@Extension
public class JobMailGlobalConfiguration extends GlobalConfiguration {

    private String signature;
    private static final Logger LOGGER = Logger
            .getLogger(JobMailGlobalConfiguration.class.getName());
    private List<Template> templates;

    /**
     * Constructor. Loads the configuration upon invoke.
     */
    public JobMailGlobalConfiguration() {
        load();
    }

    /**
     * Constructor. DataBound because this constructor is used to populate
     * values entered from the user.
     */
    @DataBoundConstructor
    public JobMailGlobalConfiguration(String signature, List<Template> templates) {
        // to do
        this.signature = signature;
        this.templates = templates;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json)
            throws FormException {
        
        if (json.containsKey("optionBlock")) {
            this.templates = req.bindJSON(JobMailGlobalConfiguration.class,
                    json.getJSONObject("optionBlock")).templates;
            this.signature = json.getJSONObject("optionBlock").getString(
                    "signature");
            save();
        }
        return true;
    }

    public String getSignature() {
        return this.signature;
    }

    public List<Template> getTemplates() {
        return this.templates;
    }

    /**
     * Finds and returns the configuration class.
     * 
     * @return the JobMailGlobalConfiguration.
     */
    public static JobMailGlobalConfiguration get() {
        return GlobalConfiguration.all().get(JobMailGlobalConfiguration.class);
    }

    /**
     * Class for handling regular expressions.
     * 
     * @author yboev
     * 
     */
    public static class Template {

        private String name;
        /**
         * Text of the template as String.
         */
        private String text;
        private boolean addProjectName = true;
        private boolean addUrl = true;
        private boolean addBuildStatus = true;

        @DataBoundConstructor
        public Template(String name, String text, boolean addProjectName,
                boolean addUrl, boolean addBuildStatus) {
            this.name = name;
            this.text = text;
            this.addUrl = addUrl;
            this.addProjectName = addProjectName;
            this.addBuildStatus = addBuildStatus;
        }

        public String getText() {
            return this.text;
        }

        public String getName() {
            return this.name;
        }

        public boolean isProjectNameEnabled() {
            return this.addProjectName;
        }

        public boolean isUrlEnabled() {
            return this.addUrl;
        }

        public boolean isBuildStatusEnabled() {
            return this.addBuildStatus;
        }

    }

}
