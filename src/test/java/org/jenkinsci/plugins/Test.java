package org.jenkinsci.plugins;

import hudson.model.Hudson;
import org.jenkinsci.plugins.jobmail.actions.JobMailBaseAction;
import org.jenkinsci.plugins.jobmail.actions.JobMailProjectAction;
import org.jenkinsci.plugins.jobmail.configuration.JobMailGlobalConfiguration.Template;
import org.jenkinsci.plugins.jobmail.utils.Constants;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

public class Test extends HudsonTestCase {

    public void testTemplate() throws Exception {
        Template t = new Template("SampleName", "SampleTextBlaBlaBla", false, false, false);
        assertNotNull(t);
        assertEquals(t.getName(), "SampleName");
        assertEquals(t.getText(), "SampleTextBlaBlaBla");
        assertEquals(t.isBuildStatusEnabled(), false);
        assertEquals(t.isProjectNameEnabled(), false);
        assertEquals(t.isUrlEnabled(), false);

    }

    @LocalData
    public void testFactory() {
        JobMailProjectAction action = Hudson.getInstance().getProjects().get(0).getAction(JobMailProjectAction.class);
        assertNotNull(action);
        
        testBaseAction();
        testProjectAction(action);
        
    }

    public void testProjectAction(JobMailProjectAction action) {
        
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
