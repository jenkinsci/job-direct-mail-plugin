package org.jenkinsci.plugins.jobmail.configuration;

import java.util.List;

import hudson.Extension;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import jenkins.model.GlobalConfiguration;

/**
 * Configuration class for the jobmail plugin. Here are stored all templates and
 * the signature. Everything is global, there is no local configuration for this
 * plugin.
 * 
 * @author yboev
 * 
 */
@Extension
public class JobMailGlobalConfiguration extends GlobalConfiguration {

    /**
     * Signature for all emails.
     */
    private String signature;
    // private static final Logger LOGGER = Logger
    // .getLogger(JobMailGlobalConfiguration.class.getName());
    /**
     * List of all available templates added by the user.
     */
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
     * 
     * @param signature
     *            the signature value from the config page.
     * @param templates
     *            the lsit of templates from the config page.
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

    /**
     * Returns the signature.
     * 
     * @return signature as String.
     */
    public String getSignature() {
        return this.signature;
    }

    /**
     * Returns all templates.
     * 
     * @return list of templates
     */
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

        /**
         * Name of the template, given by the user.
         */
        private String name;

        /**
         * Text of the template as String.
         */
        private String text;

        /**
         * Should project name be added automatically to this current template.
         */
        private boolean addProjectName = true;

        /**
         * Should url be added automatically to this current template.
         */
        private boolean addUrl = true;

        /**
         * Should build status be added automatically to this current template.
         */
        private boolean addBuildStatus = true;

        /**
         * Databound constructor for populating with values.
         * 
         * @param name
         *            The name of the template
         * @param text
         *            Content(text) of the template
         * @param addProjectName
         *            Should project name be added to the template
         * @param addUrl
         *            Shoul url be added to the template
         * @param addBuildStatus
         *            Should build status be added to the tempalate
         */
        @DataBoundConstructor
        public Template(String name, String text, boolean addProjectName,
                boolean addUrl, boolean addBuildStatus) {
            this.name = name;
            this.text = text;
            this.addUrl = addUrl;
            this.addProjectName = addProjectName;
            this.addBuildStatus = addBuildStatus;
        }

        /**
         * Return the text for this text.
         * 
         * @return the text as String
         */
        public String getText() {
            return this.text;
        }

        /**
         * Returns the name of the template.
         * 
         * @return the name as String
         */
        public String getName() {
            return this.name;
        }

        /**
         * Checks if the project name should be included automatically.
         * 
         * @return true if the project name should be included, false otherwise
         */
        public boolean isProjectNameEnabled() {
            return this.addProjectName;
        }

        /**
         * Checks if url should be included automatically.
         * 
         * @return true if the url should be included, false otherwise
         */
        public boolean isUrlEnabled() {
            return this.addUrl;
        }

        /**
         * Checks if build status should be included automatically.
         * 
         * @return true if the build status should be included, false otherwise
         */
        public boolean isBuildStatusEnabled() {
            return this.addBuildStatus;
        }

    }

}
