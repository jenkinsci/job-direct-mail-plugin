package org.jenkinsci.plugins;

import java.io.IOException;

import org.xml.sax.SAXException;

import hudson.model.Hudson;

import org.jenkinsci.plugins.jobmail.actions.JobMailBaseAction;
import org.jenkinsci.plugins.jobmail.actions.JobMailBuildAction;
import org.jenkinsci.plugins.jobmail.actions.JobMailProjectAction;
import org.jenkinsci.plugins.jobmail.configuration.JobMailGlobalConfiguration.Template;
import org.jenkinsci.plugins.jobmail.utils.Constants;
import org.junit.Assert;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class Test extends HudsonTestCase {

    /**
     * Tests the template class.
     * @throws Exception exception
     */
    public void testTemplate() throws Exception {
        final Template t = getNewTemplate();
        //final HtmlPage page = new WebClient().goTo("configure");
        assertNotNull(t);
        assertEquals(t.getName(), "SampleName");
        assertEquals(t.getText(), "SampleTextBlaBlaBla");
        assertEquals(t.isBuildStatusEnabled(), true);
        assertEquals(t.isProjectNameEnabled(), true);
        assertEquals(t.isUrlEnabled(), false);

    }
    
    /**
     * Tests a project action.
     * @throws IOException exception
     * @throws SAXException exception
     */
    @LocalData
    public void testProjectAction() throws IOException, SAXException {

        addTemplates();
        checkIfJobsAreLoaded();
        testBaseAction();

        final HtmlPage page = new WebClient().goTo("job/test_job/send_mail");
        final String allElements = page.asText();

        assertTrue(allElements.contains("From"));
        assertTrue(allElements.contains("To"));
        assertTrue(allElements.contains("Send to last committers"));
        assertTrue(allElements.contains("Subject"));
        assertTrue(allElements.contains("Load Template"));

        System.out.println(allElements);
        HtmlForm form = page.getFormByName("mailForm");
        assertNotNull("Form in project is null!", form);

        final HtmlTextInput fromField = form.getInputByName("from");
        assertNotNull("From text field is null!", fromField);
        final String fromString = "fromRussiaWithLove@beHappy, gfagdfagadfgadf, cc:gfadgafdgfadga";
        fromField.setValueAttribute(fromString);

        final HtmlTextInput toField = form.getInputByName("to");
        assertNotNull("To text field is null!", toField);
        final String toString = "notReadingMails@nowhere";
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
        
        final Template t = getNewTemplate();
        JobMailProjectAction a = new JobMailProjectAction(Hudson.getInstance().getProjects().get(0));
        assertNotNull(a.getTemplateText(t));
        try {
            assertNotNull(a.getDefaultSubject());
        } catch (InterruptedException e) {
            Assert.fail();
            e.printStackTrace();
        }
        
        form.submit();
    }
    
    /**
     * Tests the build action.
     * @throws IOException exception
     * @throws SAXException exception
     */
    @LocalData
    public void testBuildAction() throws IOException, SAXException {
        
        addTemplates();
        checkIfJobsAreLoaded();
        final HtmlPage page = new WebClient().goTo("job/test_job/4/send_mail");
        final String allElements = page.asText();

        assertTrue(allElements.contains("From"));
        assertTrue(allElements.contains("To"));
        assertTrue(allElements.contains("Send to last committers"));
        assertTrue(allElements.contains("Subject"));
        assertTrue(allElements.contains("Load Template"));

        System.out.println(allElements);
        final HtmlForm form = page.getFormByName("mailForm");
        assertNotNull("Form in project is null!", form);

        final HtmlTextInput fromField = form.getInputByName("from");
        assertNotNull("From text field is null!", fromField);
        final String fromString = "fromRussiaWithLove@beHappy";
        fromField.setValueAttribute(fromString);

        final HtmlTextInput toField = form.getInputByName("to");
        assertNotNull("To text field is null!", toField);
        final String toString = "notReadingMails@nowhere";
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
        /*
        final Template t = getNewTemplate();
        JobMailBuildAction a = new JobMailBuildAction(Hudson.getInstance().getProjects().get(0).getLastBuild());
        assertNotNull(a.getTemplateText(t));
        try {
            assertNotNull(a.getDefaultSubject());
        } catch (InterruptedException e) {
            Assert.fail();
            e.printStackTrace();
        }*/
        
        form.submit();
    }
    
    private Template getNewTemplate() {
        return new Template("SampleName", "SampleTextBlaBlaBla", true,
                false, true);
    }
    
    // not ext-mailerblabla.js found.
    private void addTemplates() throws IOException, SAXException {
        /*
        final HtmlPage page = new WebClient().goTo("configure");
        final String allElements = page.asText();
        
        assertTrue(allElements.contains("Send Mail from job or build view"));
        assertTrue(allElements.contains("Show Templates and Options"));
        HtmlForm globalConfigForm = page.getFormByName("config");
        assertNotNull("GlobalConfigForm is null!", globalConfigForm);
        final HtmlInput ob = globalConfigForm.getInputByName("optionBlock");
        assertNotNull("OptionBlock is null!", ob);
        ob.setValueAttribute("true");
        final HtmlElement templates = (HtmlElement) globalConfigForm.getByXPath(
                "//tr[td='Text Templates']").get(0);
        assertNotNull("Templates list is null!", templates);
        assertNotNull("Add button not found for templates",
                templates.getFirstByXPath(".//button"));
        ((HtmlButton) templates.getFirstByXPath(".//button")).click();
        
        final HtmlTextInput template1Name = (HtmlTextInput) templates
                .getFirstByXPath("//input[@name='" + "templates.name" + "']");
        assertNotNull("template1Name is null!", template1Name);
        template1Name.setValueAttribute("TestTemplate");
        
        final HtmlTextInput template1Text = (HtmlTextInput) templates
                .getFirstByXPath("//input[@name='" + "templates.text" + "']");
        assertNotNull("template1Text is null!", template1Text);
        template1Text.setValueAttribute("Some RrRRRrandom text.");
        
        final HtmlTextInput template1AddProjectname = (HtmlTextInput) templates
                .getFirstByXPath("//input[@name='" + "templates.addProjectName" + "']");
        assertNotNull("template1AddProjectname is null!", template1AddProjectname);
        template1AddProjectname.setValueAttribute("true");
        */
    }

    private void checkIfJobsAreLoaded() {
        assertNotNull("job missing.. @LocalData problem?", Hudson.getInstance()
                .getItem("test_job"));
        assertNotNull("job missing.. @LocalData problem?", Hudson.getInstance()
                .getItem("success"));
        assertNotNull("job missing.. @LocalData problem?", Hudson.getInstance()
                .getItem("no_change"));
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
