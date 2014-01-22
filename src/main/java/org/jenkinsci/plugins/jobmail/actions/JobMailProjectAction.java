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
import hudson.plugins.emailext.ExtendedEmailPublisher;
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
     * The global configuration.
     */
    private JobMailGlobalConfiguration config;

    /**
     * Constructor method.
     * 
     * @param project
     *            project for which the action is being created
     */
    public JobMailProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    /**
     * Returns the from propery of the email as a String. This is the current
     * user(if logged in) or the admin's email address otherwise.
     * 
     * @return property as String
     * @throws AddressException
     *             Address problem
     */
    // public String getFromProperty() throws AddressException {
    // if (!this.getUserEmail(User.current()).equals(
    // Constants.EMAIL_USER_ERROR)) {
    // return this.getUserEmail(User.current());
    // }
    // return this.getAdminEmail();
    // }

    /**
     * Getter method.
     * 
     * @return the configuration for this action.
     */
    public JobMailGlobalConfiguration getConfig() {
        return config;
    }

    /**
     * Returns the email address of the current user or an appropriate error
     * message(defined under Constants.EMAIL_USER_ERROR).
     * 
     * @return the email or the error message
     */
    public String getCurrentUserEmail() {
        return this.getUserEmail(User.current());
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
        this.config = JobMailGlobalConfiguration.get();
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
    public String getDefaultSubject() throws IOException,
            java.lang.InterruptedException {
        return this.getProjectName();
    }

    /**
     * Returns the default recipients.
     * 
     * @return default recipients as String.
     * @throws IOException
     */
    public String getDefaultRecipients() throws IOException {
        final ExtendedEmailPublisherDescriptor extMailDescriptor = new ExtendedEmailPublisherDescriptor();
        String recipients = "";
        try {
            recipients = project.getPublishersList().get(
                    ExtendedEmailPublisher.class).recipientList;
        } catch (NullPointerException e) {
            // values could not be retrieved, probably disabled
            return "";
        }
        String defRecipients = "";
        defRecipients = extMailDescriptor.getDefaultRecipients();
        recipients = recipients.replaceAll(
                java.util.regex.Pattern.quote(Constants.DEFAULT_RECIPIENTS),
                defRecipients);
        recipients = recipients.replaceAll(
                java.util.regex.Pattern.quote(", ,"), ",");
        recipients = recipients.replaceAll(java.util.regex.Pattern.quote(",,"),
                ",");
        if (recipients.startsWith(",")) {
            recipients = recipients.substring(1).trim();
        }
        return recipients;
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
     * Returns the admin email address.
     * 
     * @return the email address as String
     * @throws AddressException
     *             address problem
     */
    public String getAdminEmail() throws AddressException {
        String mailAddress = null;
        final ExtendedEmailPublisherDescriptor extMailDescriptor = new ExtendedEmailPublisherDescriptor();
        if (extMailDescriptor.getOverrideGlobalSettings()) {
            mailAddress = extMailDescriptor.getAdminAddress();
        }
        if (mailAddress == null) {
            mailAddress = JenkinsLocationConfiguration.get().getAdminAddress();
        }
        return mailAddress;
    }

    /**
     * Returns the url to which the user is redirected after sending the mail.
     * 
     * @return the url as String
     */
    protected String getRedirectUrl() {
        return this.getProjectUrl();
    }

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
        msg = createMimeMessage();

        // if (msg == null) {
        // LOGGER.info(Constants.ERROR_1);
        // return null;
        // }
        // set from
        msg.setFrom(new InternetAddress(form.getString("from").replaceAll(
                java.util.regex.Pattern.quote(" "), ".")));
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

        msgBodyPart.setContent(text, "text/plain; charset=UTF-8");
        multiPart.addBodyPart(msgBodyPart);
        msg.setContent(multiPart);

        // add recipients
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
    private MimeMessage createMimeMessage() {
        ExtendedEmailPublisherDescriptor extMailDescriptor = new ExtendedEmailPublisherDescriptor();
        return new MimeMessage(extMailDescriptor.createSession());
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
            if (mailProperty != null && mailProperty.getAddress() != null) {
                return mailProperty.getAddress();
            }
            ExtendedEmailPublisherDescriptor extMailDescriptor = new ExtendedEmailPublisherDescriptor();
            if (user.getId() + extMailDescriptor.getDefaultSuffix() != null) {
                return user.getId() + extMailDescriptor.getDefaultSuffix();
            }
        }
        return Constants.EMAIL_USER_ERROR;
    }

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
            if (address != null && !StringUtils.isBlank(address)
                    && !address.trim().contains(" ")) {
                address = address.trim();

                // address = createAddressFromString(address);
                try {
                    rslt.add(new InternetAddress(address));
                } catch (AddressException e) {
                    LOGGER.info("Could not add user address to set. User address was: "
                            + address);
                }
            }
        }
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
}