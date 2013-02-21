package org.jenkinsci.plugins;

import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.SAXException;

import hudson.model.Hudson;
import hudson.model.Job;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.jobmail.actions.JobMailBaseAction;
import org.jenkinsci.plugins.jobmail.actions.JobMailProjectAction;
import org.jenkinsci.plugins.jobmail.configuration.JobMailGlobalConfiguration.Template;
import org.jenkinsci.plugins.jobmail.utils.Constants;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.HudsonTestCase.WebClient;
import org.jvnet.hudson.test.recipes.LocalData;

import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class Test extends HudsonTestCase {

    public void testTemplate() throws Exception {
        Template t = new Template("SampleName", "SampleTextBlaBlaBla", false,
                false, false);
        assertNotNull(t);
        assertEquals(t.getName(), "SampleName");
        assertEquals(t.getText(), "SampleTextBlaBlaBla");
        assertEquals(t.isBuildStatusEnabled(), false);
        assertEquals(t.isProjectNameEnabled(), false);
        assertEquals(t.isUrlEnabled(), false);

    }

    @LocalData
    public void testFactory() throws IOException, SAXException {

        checkIfJobsAreLoaded();

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
        String fromString = "fromRussiaWithLove@beHappy";
        fromField.setValueAttribute(fromString);

        final HtmlTextInput toField = form.getInputByName("to");
        assertNotNull("To text field is null!", toField);
        String toString = "notReadingMails@nowhere";
        toField.setValueAttribute(toString);
        
        final HtmlCheckBoxInput addDevField = form.getInputByName("addDev");
        assertNotNull("Add Committers field is null!", addDevField);
        assertTrue(addDevField.isChecked());
        
        final HtmlTextInput subjectField = form.getInputByName("subject");
        assertNotNull("Subject text field is null!", subjectField);
        String subjectString = "Some Random Message Subject";
        subjectField.setValueAttribute(subjectString);
        
        final HtmlTextArea contentField = form.getTextAreaByName("content");
        assertNotNull("Content text field is null!", contentField);
        String contentString = "Some Random Message Subject";
        contentField.setText(contentString);
        
        form.submit();
        
        testBaseAction();
    }

    private void checkIfJobsAreLoaded() {
        assertNotNull("job missing.. @LocalData problem?", Hudson.getInstance()
                .getItem("test_job"));
        assertNotNull("job missing.. @LocalData problem?", Hudson.getInstance()
                .getItem("success"));
        assertNotNull("job missing.. @LocalData problem?", Hudson.getInstance()
                .getItem("no_change"));
    }

    public void testBaseAction() {
        JobMailBaseAction ba = new JobMailBaseAction();
        assertNotNull(ba);
        assertEquals(ba.getDisplayName(), Constants.NAME);
        assertEquals(ba.getIconFileName(), Constants.ICONFILENAME);
        assertEquals(ba.getUrlName(), Constants.URL);
        assertEquals(ba.getSearchUrl(), ba.getUrlName());
    }

}
