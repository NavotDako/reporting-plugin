import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.mail.MessagingException;
import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import com.intellij.openapi.util.IconLoader;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Created by navot.dako on 5/29/2017.
 */
public class box extends AnAction {
    public box() {

        super("Send", "Item description", IconLoader.getIcon("/pack/download.jpg"));
    }

    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        start(project);
    }

    private void start(Project project) {
        String path = "-1";
        String name = "-1";
        String subject = "-1";
        boolean flag = false;
        File dir=null;
        path = getInput(project, "Path", "Enter The Folder Path:", "Folder Path");
        if (!path.equals("-1")) {
            name = getInput(project, "Name", "Enter The QA Ninja Name:", "Ninja Name");
            if (!name.equals("-1")) {
                subject = getInput(project, "Subject", "Enter The Subject:", "Subject");
                if (!subject.equals("-1")) {
                    dir = getAppDataDir();
                    flag = continueToSend(project, path, name, subject,dir);
                }
            }
        }
        if (!flag) {
            int answer = Messages.showYesNoDialog(project, "Do You Want To Try Again?", "title", Messages.getQuestionIcon());
            if (answer == 0) {
                start(project);
            }
        }
        try {
            FileUtils.forceDelete(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean continueToSend(Project project, String path, String name, String subject, File dir) {

        ZipAndPutOnTheServer appZip = new ZipAndPutOnTheServer(path);
        String subjectString = subject.replace(" ", "_");
        String recipientEmail = getRecipient(dir,name);
        if (recipientEmail != null && !recipientEmail.equals("")) {
            Messages.showMessageDialog(project, "Subject - " + subjectString + "\nReport - " + path + "\nRecipientEmail - " + recipientEmail, "Information", Messages.getInformationIcon());
            appZip.generateFileList(new File(path));
            appZip.zipIt(subjectString, path);
            try {
                SendEmail.Send(recipientEmail, "", subjectString, "http://192.168.2.72:8181/logs/" + subjectString + ".zip",credsProp(dir));
                Messages.showMessageDialog(project, "Done! Email Was Sent!\nThe Email is - " + recipientEmail + "\nThe Subject is - " + subjectString, "Information", Messages.getInformationIcon());

            } catch (MessagingException e) {
                e.printStackTrace();
                Messages.showMessageDialog(project, "Error!! We Cloud Not Send The Email!\nCheck out the logs", "Information", Messages.getInformationIcon());
                return false;
            }
        } else {
            Messages.showMessageDialog(project, "Cloud Not Find The Name - '" + name + "' In The email.properties", "Information", Messages.getInformationIcon());
            return false;
        }
        return true;
    }

    @NotNull
    private File getAppDataDir() {
        File dir = new File(System.getenv("APPDATA")+"\\reporting");
        dir.mkdir();
        return dir;
    }

    private Properties credsProp(File dir) {
        File file = new File(dir.getAbsolutePath()+"\\creds.properties");
        try {
            FileUtils.copyURLToFile(new URL("http://192.168.2.72:8181/emails/creds.properties"), file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Properties properties = new Properties();
        try {
            FileInputStream fileInput = new FileInputStream(file);
            properties.load(fileInput);
            fileInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private String getRecipient(File dir,String name) {

        File file = new File(dir.getAbsolutePath()+"\\emails.properties");
        try {
            FileUtils.copyURLToFile(new URL("http://192.168.2.72:8181/emails/emails.properties"), file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Properties properties = new Properties();
        try {
            FileInputStream fileInput = new FileInputStream(file);
            properties.load(fileInput);
            fileInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return String.valueOf(properties.get(name.toLowerCase()));
    }

    private String getInput(Project project, String topic, String message, String title) {
        String value;
        value = Messages.showInputDialog(project, message, title, Messages.getQuestionIcon());
        if (!value.equals("")) {
            return value;
        } else {
            value = Messages.showInputDialog(project, "The " + topic + " Is Empty - Stop playing!\nEnter The real " + topic + " value:", message, Messages.getQuestionIcon());
            if (!value.equals("")) {
                return value;
            } else {
                Messages.showMessageDialog(project, "The " + topic + " Is Empty - Stopping", "Information", Messages.getInformationIcon());
                return "-1";
            }
        }
    }
}
