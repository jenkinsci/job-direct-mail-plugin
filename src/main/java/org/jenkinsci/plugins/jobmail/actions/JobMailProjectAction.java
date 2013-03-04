package org.jenkinsci.plugins.jobmail.actions;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.mail.internet.InternetAddress;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import java.util.Set;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.jobmail.configuration.JobMailGlobalConfiguration;
import org.jenkinsci.plugins.jobmail.utils.Constants;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import jenkins.model.JenkinsLocationConfiguration;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.Mailer;
import hudson.util.RunList;

/**
 * Implements the mail action visible from the project view. contains most of
 * the methods for creating the email and setting its properties.
 * 
 * @author yboev
 * 
 */
public class JobMailProjectAction extends JobMailBaseAction {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
            .getLogger(JobMailProjectAction.class.getName());
    /**
     * The current project.
     */
    private AbstractProject<?, ?> project;

    /**
     * This is used to obtain values or call methods implemented in the
     * ext-mailer-plugin for Jenkins.
     */
    private ExtendedEmailPublisherDescriptor extMailDescriptor;

    /**
     * The global configuration.
     */
    protected JobMailGlobalConfiguration conf;

    /**
     * Constructor method.
     * 
     * @param project
     *            project for which the action is being created
     */
    public JobMailProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
        this.extMailDescriptor = new ExtendedEmailPublisherDescriptor();
    }

    /**
     * Returns the from propery of the email as a String. This is the current
     * user(if logged in) or the admin's email address otherwise.
     * 
     * @return property as String
     * @throws AddressException
     *             Address problem
     */
    public String getFromProperty() throws AddressException {
        if (!this.getUserEmail(User.current()).equals(Constants.EMAIL_USER_ERROR)) {
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
     *             Input/output problem
     * @throws ServletException
     *             servlet problem
     * @throws MessagingException
     *             Messaging problem
     * @throws InterruptedException
     *             Interrupt
     */
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException, MessagingException,
            InterruptedException {
        final JSONObject form = req.getSubmittedForm();
        this.sendMail(form);
        rsp.sendRedirect(this.getRedirectUrl());
    }

    /**
     * Initialize method, loads the configuration. Called in the jelly, every
     * time the action is clicked.
     */
    public void init() {
        this.conf = JobMailGlobalConfiguration.get();
    }

    /**
     * Returns the default subject for the email.
     * 
     * @return default subject as string
     * @throws java.io.IOException
     *             Input/output
     * @throws java.lang.InterruptedException
     *             Interrupt
     */
    public String getDefaultSubject() throws java.io.IOException,
            java.lang.InterruptedException {
        return this.getProjectName();
    }

    /**
     * Returns the default recipients.
     * 
     * @return default recipients as String.
     */
    public String getDefaultRecipients() {
        if (this.extMailDescriptor != null) {
            return this.extMailDescriptor.getDefaultRecipients();
        }
        return "";
    }

    /**
     * Returns all configured templates.
     * 
     * @return list of all templates.
     */
    public List<JobMailGlobalConfiguration.Template> getTemplates() {
        return this.conf.getTemplates();
    }

    /**
     * Constructs the content part of the email from a template.
     * 
     * @param currentTemplate
     *            the template from which we are constructing text
     * @return the constructed text as String
     */
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

        if (this.conf != null && this.conf.getSignature() != null) {
            text += "\n";
            text += "\n" + this.conf.getSignature();
        }
        return text;
    }

    /**
     * Returns the url to which the user is redirected after sending the mail.
     * 
     * @return the url as String
     */
    protected String getRedirectUrl() {
        return this.getProjectUrl();
    }

    /*
     * private String getServer() { if (extMailDescriptor != null ||
     * extMailDescriptor.getSmtpServer() != null) { return
     * extMailDescriptor.getSmtpServer(); } if (Mailer.descriptor() != null) {
     * return Mailer.descriptor().getSmtpServer(); } return "no SmtpServer"; }
     */

    /**
     * Sends the created email.
     * 
     * @param form
     *            JSON file containing all info from the email writing page.
     * @throws MessagingException
     *             Message problem
     * @throws IOException
     *             input/output problem
     * @throws InterruptedException
     *             Interrupt
     */
    private void sendMail(JSONObject form) throws MessagingException,
            IOException, InterruptedException {
        final MimeMessage msg = createMessage(form);
        try {
            Transport.send(msg);
            LOGGER.info("EMAIL SENT TO EVERYONE!");
        } catch (SendFailedException e) {
            LOGGER.info("EMAIL NOT SENT TO EVERYONE!");
        }

    }

    /**
     * Creates an email message from the given JSON object.
     * 
     * @param form
     *            the JSON object
     * @return the MimeMessage
     * @throws MessagingException
     *             Message problem
     * @throws IOException
     *             input/output problem
     * @throws InterruptedException
     *             Interrupt
     */
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
        final Multipart multiPart = new MimeMultipart();

        final MimeBodyPart msgBodyPart = new MimeBodyPart();
        final String text = form.getString("content");

        msgBodyPart.setContent(text, "text/plain");
        multiPart.addBodyPart(msgBodyPart);
        msg.setContent(multiPart);

        // add recipients added manually
        final Set<InternetAddress> restRecipients = getRecipients(
                form.getString("to"), (form.getString("addDev").equals("true")));
        msg.setRecipients(Message.RecipientType.TO, restRecipients
                .toArray(new InternetAddress[restRecipients.size()]));
        for (InternetAddress ia : restRecipients) {
            LOGGER.info("SENDING MAIL TO......." + ia);
        }
        return msg;
    }

    /**
     * Returns the project url.
     * 
     * @return the url as String
     */
    private String getProjectUrl() {
        return this.project.getAbsoluteUrl();
    }

    /**
     * Return the project name.
     * 
     * @return the name as String
     */
    protected String getProjectName() {
        return this.project.getFullName();
    }

    /**
     * Creates MimeMessage using the ext-mailer plugin or the Mailer plugin for
     * jenkins.
     * 
     * @param msg
     *            the MimeMessage to be created.
     * @return mimemessage
     * 
     */
    private MimeMessage createMimeMessage(MimeMessage msg) {
        if (this.extMailDescriptor.getOverrideGlobalSettings()) {
            LOGGER.info("Creating session with extMail plugin");
            msg = new MimeMessage(this.extMailDescriptor.createSession());
        } else {
            if (Mailer.descriptor() != null) {
                LOGGER.info("Creating session with Mailer plugin");
                msg = new MimeMessage(Mailer.descriptor().createSession());
            }
        }
        return msg;
    }

    /**
     * Returns set of recipients from String of recipients. Also adds the
     * developers if checked.
     * 
     * @param recipients
     *            Recipients as String
     * @param addDev
     *            Should developers be added
     * @return set of all recipients
     */
    private Set<InternetAddress> getRecipients(String recipients, boolean addDev) {
        final Set<InternetAddress> rslt = new HashSet<InternetAddress>();
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

    /**
     * Returns the admin email address.
     * 
     * @return the email address as String
     * @throws AddressException
     *             address problem
     */
    @SuppressWarnings("deprecation")
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

    /**
     * Returns the email address of a given user.
     * 
     * @param user
     *            the user
     * @return email address for this user as String
     */
    private String getUserEmail(User user) {
        if (user != null) {
            final Mailer.UserProperty mailProperty = user
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

    /**
     * Creates a valid email address from a String.
     * 
     * @param address
     *            input address
     * @return valid output address
     */
    @SuppressWarnings("deprecation")
    private String createAddressFromString(String address) {
        // check if user email is configured.
        if (!address.contains("@")) {
            address = getUserEmail(User.get(address, false));
        }

        if (address.startsWith("cc:")) {
            address = address.substring("cc:".length());
        }

        return address;
    }

    /**
     * Adds the last committers to the set of recipients.
     * 
     * @param rslt
     *            set with the recipients
     */
    @SuppressWarnings("deprecation")
    private void addLastCommitters(Set<InternetAddress> rslt) {
        if (this.project != null && this.project.getLastBuild() != null) {
            final Set<User> users = new HashSet<User>();
            final RunList<AbstractBuild<?, ?>> allBuilds = getAllBuilds();
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

    /**
     * Returns all build for this project.
     * 
     * @return list of all builds
     */
    @SuppressWarnings("unchecked")
    private RunList<AbstractBuild<?, ?>> getAllBuilds() {
        return (RunList<AbstractBuild<?, ?>>) this.project.getBuilds();
    }

    /**
     * Adds a user to the set of recipients.
     * 
     * @param rslt
     *            the set of recipients.
     * @param user
     *            the user, who's email is to be added to the set
     */
    private void addUserToRecipientsSet(Set<InternetAddress> rslt, User user) {
        final String email = this.getUserEmail(user);
        if (!email.equals(null) && !email.equals(Constants.EMAIL_USER_ERROR)) {
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