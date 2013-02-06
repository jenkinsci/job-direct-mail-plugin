package org.jenkinsci.plugins;

import hudson.model.Hudson;

import org.jenkinsci.plugins.JobMailGlobalConfiguration.Template;
import org.jvnet.hudson.test.HudsonTestCase;

public class Test extends HudsonTestCase {
    
    public void testTemplate() throws Exception {
        Template t = new Template("t1", "blabla", false, false, false);
        assertEquals(t.getName(), "t1");
        assertEquals(t.getText(), "blabla");
        assertEquals(t.isBuildStatusEnabled(), false);
        assertEquals(t.isProjectNameEnabled(), false);
        assertEquals(t.isUrlEnabled(), false);
        
    }
    
    /*
    public void testProjectAction() {
        try {
        JobMailProjectAction pa = Hudson.getInstance().getProjects().get(0).getAction(JobMailProjectAction.class);
        assertNotNull(pa);
        } catch(NullPointerException e ) {
            assertNull(Hudson.getInstance().getProjects());
        }
    }*/
    
}
