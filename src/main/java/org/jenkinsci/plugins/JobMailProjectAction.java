package org.jenkinsci.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.mail.internet.InternetAddress;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import java.util.Set;

import jenkins.model.Jenkins;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import jenkins.model.JenkinsLocationConfiguration;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Build;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.Messages;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.Mailer;
import hudson.util.RunList;

public class JobMailProjectAction extends JobMailBaseAction {

    private static final Logger LOGGER = Logger
            .getLogger(JobMailProjectAction.class.getName());
    private AbstractProject<?, ?> project;
    private ExtendedEmailPublisherDescriptor extMailDescriptor;
    protected JobMailGlobalConfiguration conf;

    public JobMailProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
        this.extMailDescriptor = new ExtendedEmailPublisherDescriptor();
    }

    public String getFromProperty() throws AddressException {
        if (this.getUserEmail(User.current()) != Constants.EMAIL_USER_ERROR) {
            return this.getUserEmail(User.current());
        }
        return this.getAdminEmail();
    }

    /**
     * The whole input of the email page.
     * 
     * @param req
     *            request
     * @param rsp
     *            response
     * @throws IOException
     * @throws ServletException
     * @throws MessagingException
     * @throws InterruptedException
     */
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException, MessagingException,
            InterruptedException {
        JSONObject form = req.getSubmittedForm();
        this.sendMail(form);
        rsp.sendRedirect(this.getRedirectUrl());
    }


    public void init() {
        this.conf = JobMailGlobalConfiguration.get();
    }

    public String getDefaultSubject() throws java.io.IOException, java.lang.InterruptedException {
        return this.getProjectName();
    }

    public String getDefaultRecipients() {
        if (this.extMailDescriptor != null) {
            return this.extMailDescriptor.getDefaultRecipients();
        }
        return "";
    }

    public List<JobMailGlobalConfiguration.Template> getTemplates() {
        return this.conf.getTemplates();
    }

    public String getTemplateText(
            JobMailGlobalConfiguration.Template currentTemplate) {
        String text = "";
        text += currentTemplate.getText();

        text += "\n";
        if (currentTemplate.isProjectNameEnabled()) {
            text += "\n" + Constants.PROJECT_NAME + getProjectName();
        }
        
        if (currentTemplate.isUrlEnabled()) {
            text += "\n" + Constants.PROJECT_URL + getProjectUrl();
        }
        
        if(this.conf.getSignature() != null) {
            text += "\n";
            text += "\n" + this.conf.getSignature();
        }
        return text;
    }

    public String getContentFromTemplate(String template) {
        return template + ":)";
    }

    protected String getRedirectUrl() {
        return this.getProjectUrl();
    }

    private String getServer() {
        if (extMailDescriptor != null
                || extMailDescriptor.getSmtpServer() != null) {
            return extMailDescriptor.getSmtpServer();
        }
        if (Mailer.descriptor() != null) {
            return Mailer.descriptor().getSmtpServer();
        }
        return "no SmtpServer";
    }

    private void sendMail(JSONObject form) throws MessagingException,
            IOException, InterruptedException {
        MimeMessage msg = createMessage(form);

        try {
            Transport.send(msg);
            LOGGER.info("EMAIL SENT TO EVERYONE!");
        } catch (SendFailedException e) {
            LOGGER.info("EMAIL NOT SENT TO EVERYONE!");
        }

    }

    private MimeMessage createMessage(JSONObject form)
            throws MessagingException, IOException, InterruptedException {
        MimeMessage msg = null;
        // create Session
        msg = createMimeMessage(msg);

        if (msg == null) {
            LOGGER.info(Constants.ERROR_1);
            return null;
        }
        // set from
        msg.setFrom(new InternetAddress(this.getFromProperty()));
        // set date
        msg.setSentDate(new Date());
        // set subject
        msg.setSubject(form.getString("subject"));

        // set header
        msg.setHeader("Precedence", "bulk");

        // set body and eventual attachments
        Multipart multiPart = new MimeMultipart();

        MimeBodyPart msgBodyPart = new MimeBodyPart();
        String text = form.getString("content");

        msgBodyPart.setContent(text, "text/plain");
        multiPart.addBodyPart(msgBodyPart);
        msg.setContent(multiPart);
        
        // add recipients added manually
        Set<InternetAddress> restRecipients = getRecipients(
                form.getString("to"), (form.getString("addDev") == "true"));
        msg.setRecipients(Message.RecipientType.TO, restRecipients
                .toArray(new InternetAddress[restRecipients.size()]));
        for (InternetAddress ia : restRecipients) {
            LOGGER.info("SENDING MAIL TO......." + ia);
        }
        return msg;
    }

    private String getProjectUrl() {
        return this.project.getAbsoluteUrl();
    }
    
    protected String getProjectName() {
        return this.project.getFullName();
    }

    private MimeMessage createMimeMessage(MimeMessage msg) {
        if (this.extMailDescriptor.getOverrideGlobalSettings()) {
            msg = new MimeMessage(this.extMailDescriptor.createSession());
        } else {
            if (Mailer.descriptor() != null) {
                msg = new MimeMessage(Mailer.descriptor().createSession());
            }
        }
        return msg;
    }

    private Set<InternetAddress> getRecipients(String recipients, boolean addDev) {
        Set<InternetAddress> rslt = new HashSet<InternetAddress>();
        // add manually added recipients
        addManualRecipients(recipients, rslt);

        // add Developers if checked
        if (addDev) {
            addLastCommitters(rslt);
        }

        // add user trigering the build, THIS FEATURE IS DISABLED!
        // addUserTriggeringTheBuild(rslt);
        LOGGER.info("Recipients added through TO: " + rslt.size());
        return rslt;
    }

    private String getAdminEmail() throws AddressException {
        String mailAddress = null;
        if (this.extMailDescriptor.getOverrideGlobalSettings()) {
            mailAddress = this.extMailDescriptor.getAdminAddress();
        } else {
            if (Mailer.descriptor() != null) {
                mailAddress = Mailer.descriptor().getAdminAddress();
            }
        }
        if (mailAddress == null) {
            mailAddress = JenkinsLocationConfiguration.get().getAdminAddress();
        }
        return mailAddress;
    }

    private String getUserEmail(User user) {
        if (user != null) {
            Mailer.UserProperty mailProperty = user
                    .getProperty(Mailer.UserProperty.class);
            if (mailProperty != null) {
                return mailProperty.getAddress();
            }
            if (Mailer.descriptor() != null) {
                return user.getId() + Mailer.descriptor().getDefaultSuffix();
            }
            if (extMailDescriptor != null) {
                return user.getId() + extMailDescriptor.getDefaultSuffix();
            }

        }
        return Constants.EMAIL_USER_ERROR;
    }

    /*
     * private void addUserTriggeringTheBuild(Set<InternetAddress> rslt) {
     * AbstractBuild<?, ?> build = this.project.getLastBuild(); if(build ==
     * null) { return; } UserIdCause userIdCause =
     * build.getCause(Cause.UserIdCause.class); if(userIdCause != null) {
     * LOGGER.info( User.get(userIdCause.getUserId()).toString());
     * this.addUserToRecipientsSet(rslt, User.get(userIdCause.getUserId())); }
     * 
     * }
     */

    /**
     * Adds manually written recipients, the ones standing in TO field.
     * 
     * @param recipients
     *            the user input as String
     * @param rslt
     *            the set with recipients
     */
    private void addManualRecipients(String recipients,
            Set<InternetAddress> rslt) {
        if (recipients == null) {
            return;
        }
        final String[] addresses = StringUtils.trim(recipients).split(
                Constants.COMMA_SEPARATED_SPLIT_REGEXP);

        for (String address : addresses) {
            if (!StringUtils.isBlank(address)) {
                address = address.trim();

                address = createAddressFromString(address);
                try {
                    rslt.add(new InternetAddress(address));
                } catch (AddressException e) {
                    LOGGER.info("Could not add user address to set. User address was: "
                            + address);
                    e.printStackTrace();
                }
            }
        }
    }

    private String createAddressFromString(String address) {
        // check if user email is configured.
        if (!address.contains("@")) {
            address = getUserEmail(User.get(address, false));
        }

        if (address.startsWith("cc:")) {
            address = address.substring(3);
        }

        return address;
    }

    private void addLastCommitters(Set<InternetAddress> rslt) {
        if (this.project != null && this.project.getLastBuild() != null) {
            Set<User> users = new HashSet<User>();
            RunList<AbstractBuild<?, ?>> allBuilds = getAllBuilds();
            ChangeLogSet<? extends Entry> changes = null;
            int count = allBuilds.size() - 1;
            while ((changes == null || changes.isEmptySet()) && count >= 0) {
                changes = allBuilds.get(count).getChangeSet();
                count--;
            }
            for (Entry change : changes) {
                users.add(change.getAuthor());
            }
            for (User user : users) {
                addUserToRecipientsSet(rslt, user);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private RunList<AbstractBuild<?, ?>> getAllBuilds() {
        return (RunList<AbstractBuild<?, ?>>) this.project.getBuilds();
    }

    private void addUserToRecipientsSet(Set<InternetAddress> rslt, User user) {
        String email = this.getUserEmail(user);
        if (email != null && email != Constants.EMAIL_USER_ERROR) {
            try {
                rslt.add(new InternetAddress(email));
            } catch (AddressException e) {
                LOGGER.info("Could not add user address to set of recipients. ID of the user: "
                        + user.getId());
                e.printStackTrace();
            }
        }
    }

    // This method adds the recipients from the ExtMailer plugin
    /*
     * private Set<InternetAddress> addRecipientsFromExtMailer() { EnvVars env =
     * null; try { if (this.project.getLastBuild() != null) { env =
     * this.project.getLastBuild().getEnvironment(); } else { env = new
     * EnvVars(); }
     * 
     * } catch (Exception e) { // create an empty set of env vars env = new
     * EnvVars(); } try { return new
     * EmailRecipientUtils().convertRecipientString(
     * this.extMailDescriptor.getDefaultRecipients(), env,
     * EmailRecipientUtils.TO); } catch (AddressException e) {
     * LOGGER.info("Converting default recipients unsuccessful! :(");
     * e.printStackTrace(); }
     * 
     * return null; }
     */
}