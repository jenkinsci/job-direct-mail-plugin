package org.jenkinsci.plugins.jobmail.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.TransientBuildActionFactory;
import hudson.model.Run;

/**
 * Action Factory for the build view. Adds the "Send Mail" action to every
 * build.
 * 
 * @author yboev
 * 
 */
@Extension
public class MailBuildActionFactory extends TransientBuildActionFactory {

    /** Our logger. */
    // private static final Logger LOG =
    // Logger.getLogger(MailProjectActionFactory.class.getName());

    /**
     * @param build
     *            the current build
     * @return Collection of actions for this build with the new one added.
     */
    @Override
    public Collection<? extends Action> createFor(
            @SuppressWarnings("rawtypes") Run build) {
        final List<JobMailBuildAction> buildActions = build
                .getActions(JobMailBuildAction.class);
        final ArrayList<Action> actions = new ArrayList<Action>();
        if (buildActions.isEmpty()) {
            final JobMailBuildAction newAction = new JobMailBuildAction(build);
            actions.add(newAction);
            return actions;
        } else {
            return buildActions;
        }
    }

}
