package org.jenkinsci.plugins.jobmail.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.TransientBuildActionFactory;
import hudson.model.Run;

@Extension
public class MailBuildActionFactory extends TransientBuildActionFactory {

    /** Our logger. */
    // private static final Logger LOG =
    // Logger.getLogger(MailProjectActionFactory.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Action> createFor(
            Run build) {
        final List<JobMailProjectAction> projectActions = build
                .getActions(JobMailProjectAction.class);
        final ArrayList<Action> actions = new ArrayList<Action>();
        if (projectActions.isEmpty()) {
            final JobMailBuildAction newAction = new JobMailBuildAction(build);
            actions.add(newAction);
            return actions;
        } else {
            return projectActions;
        }
    }

}
