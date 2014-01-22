package org.jenkinsci.plugins;

import hudson.model.AbstractProject;

import java.io.IOException;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;
import static org.junit.Assert.*;
import org.jenkinsci.plugins.jobmail.actions.JobMailBaseAction;
import org.jenkinsci.plugins.jobmail.actions.JobMailBuildAction;
import org.jenkinsci.plugins.jobmail.actions.JobMailProjectAction;
import org.jenkinsci.plugins.jobmail.configuration.JobMailGlobalConfiguration.Template;
import org.jenkinsci.plugins.jobmail.configuration.JobMailGlobalConfiguration;
import org.jenkinsci.plugins.jobmail.utils.Constants;
import org.junit.Assert;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class JobDirectMailTest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    /**
     * Tests the global configuration class.
     * 
     * @throws Exception
     *             exception
     */
    @Test
    public void testGlobalConfig() throws Exception {
        final Template t = getNewTemplate();
        assertNotNull(t);
        assertEquals(t.getName(), "SampleName");
        assertEquals(t.getText(), "SampleTextBlaBlaBla");
        assertEquals(t.isBuildStatusEnabled(), true);
        assertEquals(t.isProjectNameEnabled(), true);
        assertEquals(t.isUrlEnabled(), false);
        JobMailGlobalConfiguration globalConfig = new JobMailGlobalConfiguration(
                "Some signature blabla", null);
        assertNotNull(globalConfig);
        assertEquals(globalConfig.getSignature(), "Some signature blabla");
        assertNotNull(globalConfig.getTemplates());
    }

    /**
     * Tests a project action.
     * 
     * @throws IOException
     *             exception
     * @throws SAXException
     *             exception
     */
    @LocalData
    @Test
    public void testProjectAction() throws IOException, SAXException {
        final Logger LOGGER = Logger.getLogger(JobMailProjectAction.class
                .getName());
        checkIfJobsAreLoaded();
        //addTemplates();
        testBaseAction();

        final HtmlPage page = jenkinsRule.createWebClient().goTo(
                "job/test_job/send_mail");

        checkElementsAsStrings(page.asText());

        final HtmlForm form = populateForm(page);

        final Template t = getNewTemplate();
        JobMailProjectAction a = new JobMailProjectAction(
                (AbstractProject<?, ?>) Jenkins.getInstance().getAllItems()
                        .get(0));
        assertNotNull(a.getTemplateText(t));
        try {
            assertNotNull(a.getDefaultSubject());
        } catch (InterruptedException e) {
            Assert.fail();
            e.printStackTrace();
        }
        try {
            form.submit();
        } catch (FailingHttpStatusCodeException e) {
            LOGGER.info(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tests the build action.
     * 
     * @throws IOException
     *             exception
     * @throws SAXException
     *             exception
     */
    @LocalData
    public void testBuildAction() throws IOException, SAXException {

        checkIfJobsAreLoaded();
        addTemplates();
        final HtmlPage page = jenkinsRule.createWebClient().goTo(
                "job/test_job/4/send_mail");

        checkElementsAsStrings(page.asText());

        final HtmlForm form = populateForm(page);

        final Template t = getNewTemplate();
        JobMailBuildAction a = new JobMailBuildAction(
                ((AbstractProject<?, ?>) Jenkins.getInstance().getAllItems()
                        .get(0)).getLastBuild());
        assertNotNull(a.getTemplateText(t));
        try {
            assertNotNull(a.getDefaultSubject());
        } catch (InterruptedException e) {
            Assert.fail();
            e.printStackTrace();
        }

        form.submit();
    }

    private void checkElementsAsStrings(final String allElements) {
        assertTrue(allElements.contains("From"));
        assertTrue(allElements.contains("To"));
        assertTrue(allElements.contains("Send to last committers"));
        assertTrue(allElements.contains("Subject"));
        assertTrue(allElements.contains("Load Template"));
    }

    private Template getNewTemplate() {
        return new Template("SampleName", "SampleTextBlaBlaBla", true, false,
                true);
    }

    // not ext-mailerblabla.js found.
    // Tests in error:
    // testBuildAction(org.jenkinsci.plugins.Test): 404 Not Found for
    // http://localhost:58693/plugin/email-ext/scripts/emailext-behavior.js

    private void addTemplates() throws IOException, SAXException {
        final HtmlPage page = jenkinsRule.createWebClient().goTo("configure");
        final String allElements = page.asText();
        assertTrue(allElements.contains("Send Mail from job or build view"));
        assertTrue(allElements.contains("Show Templates and Options"));
        HtmlForm globalConfigForm = page.getFormByName("config");
        assertNotNull("GlobalConfigForm is null!", globalConfigForm);
        final HtmlInput ob = globalConfigForm.getInputByName("optionBlock");
        assertNotNull("OptionBlock is null!", ob);
        ob.setValueAttribute("true");
        final HtmlElement templates = (HtmlElement) globalConfigForm
                .getByXPath("//tr[td='Text Templates']").get(0);
        assertNotNull("Templates list is null!", templates);
        assertNotNull("Add button not found for templates",
                templates.getFirstByXPath(".//button"));
        ((HtmlElement) templates.getFirstByXPath(".//button")).click();

        final HtmlTextInput template1Name = (HtmlTextInput) templates
                .getFirstByXPath("//input[@name='" + "templates.name" + "']");
        assertNotNull("template1Name is null!", template1Name);
        template1Name.setValueAttribute("TestTemplate");

        final HtmlTextInput template1Text = (HtmlTextInput) templates
                .getFirstByXPath("//input[@name='" + "templates.text" + "']");
        assertNotNull("template1Text is null!", template1Text);
        template1Text.setValueAttribute("Some RrRRRrandom text.");

        final HtmlTextInput template1AddProjectname = (HtmlTextInput) templates
                .getFirstByXPath("//input[@name='" + "templates.addProjectName"
                        + "']");
        assertNotNull("template1AddProjectname is null!",
                template1AddProjectname);
        template1AddProjectname.setValueAttribute("true");
    }

    private HtmlForm populateForm(final HtmlPage page) {
        final HtmlForm form = page.getFormByName("mailForm");
        assertNotNull("Form in project is null!", form);

        final HtmlSelect fromSelect = form.getSelectByName("from");
        assertNotNull("From combo box field is null!", fromSelect);
        assertNotSame(fromSelect.getSelectedOptions().get(0),
                Constants.EMAIL_USER_ERROR);

        final HtmlTextInput toField = form.getInputByName("to");
        assertNotNull("To text field is null!", toField);
        final String toString = "notReadingMails@nowhere, notReadingMails@nowhere,gfagdfagadfgadf@blabla, gfadgafdgfadga@glbbgfff";
        toField.setValueAttribute(toString);

        final HtmlCheckBoxInput addDevField = form.getInputByName("addDev");
        assertNotNull("Add Committers field is null!", addDevField);
        assertTrue(addDevField.isChecked());

        final HtmlTextInput subjectField = form.getInputByName("subject");
        assertNotNull("Subject text field is null!", subjectField);
        final String subjectString = "Some Random Message Subject";
        subjectField.setValueAttribute(subjectString);

        final HtmlTextArea contentField = form.getTextAreaByName("content");
        assertNotNull("Content text field is null!", contentField);
        final String contentString = "Some Random Message Subject";
        contentField.setText(contentString);
        return form;
    }

    private void checkIfJobsAreLoaded() {
        assertNotNull("job missing.. @LocalData problem?", Jenkins
                .getInstance().getItem("test_job"));
        assertNotNull("job missing.. @LocalData problem?", Jenkins
                .getInstance().getItem("success"));
        assertNotNull("job missing.. @LocalData problem?", Jenkins
                .getInstance().getItem("no_change"));
    }

    /**
     * Tests the base action.
     */
    public void testBaseAction() {
        final JobMailBaseAction ba = new JobMailBaseAction();
        assertNotNull(ba);
        assertEquals(ba.getDisplayName(), Constants.NAME);
        assertEquals(ba.getIconFileName(), Constants.ICONFILENAME);
        assertEquals(ba.getUrlName(), Constants.URL);
        assertEquals(ba.getSearchUrl(), ba.getUrlName());
    }

}
