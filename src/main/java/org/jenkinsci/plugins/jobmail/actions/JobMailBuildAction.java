package org.jenkinsci.plugins.jobmail.actions;

import org.jenkinsci.plugins.jobmail.configuration.JobMailGlobalConfiguration;
import org.jenkinsci.plugins.jobmail.utils.Constants;

import hudson.model.Build;
import hudson.model.Run;

public class JobMailBuildAction extends JobMailProjectAction {

    Build<?, ?> build;

    public JobMailBuildAction(Run<?, ?> build) {
        super(((Build<?, ?>) build).getProject());
        this.build = (Build<?, ?>) build;
    }

    @Override
    public String getDefaultSubject() throws java.io.IOException,
            java.lang.InterruptedException {
        return this.getProjectName() + " - " + "Build # "
                + this.getBuildNumber() + " - " + this.getBuildResult() + "!";
    }

    @Override
    public String getTemplateText(
            JobMailGlobalConfiguration.Template currentTemplate) {
        String text = "";
        text += currentTemplate.getText();

        text += "\n";
        if (currentTemplate.isProjectNameEnabled()) {
            text += "\n" + Constants.PROJECT_NAME + getProjectName();
        }
        if (currentTemplate.isUrlEnabled()) {
            text += "\n" + Constants.BUILD_URL + getBuildUrl();
        }

        if (currentTemplate.isBuildStatusEnabled()) {
            text += "\n" + Constants.BUILD_STATUS + getBuildResult();
        }

        if (this.conf.getSignature() != null) {
            text += "\n";
            text += "\n" + this.conf.getSignature();
        }
        return text;
    }
    
    @Override
    protected String getRedirectUrl() {
        return this.getBuildUrl();
    }

    private String getBuildNumber() {
        return this.build.getNumber() + "";

    }

    private String getBuildResult() {
        if (this.build != null) {
            return this.build.getResult().toString();
        }
        return Constants.NA;
    }

    private String getBuildUrl() {
        if (this.build.getProject() != null) {
            return this.build.getProject().getAbsoluteUrl()
                    + this.getBuildNumber();
        }
        return this.build.getAbsoluteUrl();
    }

}
