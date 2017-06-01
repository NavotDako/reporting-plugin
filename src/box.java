import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;

import javax.mail.MessagingException;
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
    Properties properties;
    public box() {

        super("Send", "Item description", IconLoader.getIcon("/pack/download.jpg"));
    }

    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        SelectionModel selectionModel = editor.getSelectionModel();
        String markedText = selectionModel.getSelectedText();
        start(project, markedText);
    }

    private void start(Project project, String markedText) {
        String path = "-1";
        String name = "-1";
        String subject = "-1";
        boolean flag = false;
        File dir = null;
        path = getInput(project, "Path", "Enter The Folder Path:", "Folder Path", markedText);
        if (!path.equals("-1")) {
            dir = getAppDataDir();
            properties = getEmailsProperties(dir);
            name = getInput(project, "Name", "Enter The QA Ninja Name:", "Ninja Name", "");
            if (!name.equals("-1")) {
                subject = getInput(project, "Subject", "Enter The Subject:", "Subject", "");
                if (!subject.equals("-1")) {
                    flag = continueToSend(project, path, name, subject, dir,getRecipient(properties, name));
                }
            }
        }
        if (!flag) {
            int answer = Messages.showYesNoDialog(project, "Do You Want To Try Again?", "title", Messages.getQuestionIcon());
            if (answer == 0) {
                start(project, markedText);
            }
        }
        try {
            FileUtils.forceDelete(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean continueToSend(Project project, String path, String name, String subject, File dir,String recipientEmail) {

        ZipAndPutOnTheServer appZip = new ZipAndPutOnTheServer(path);
        String subjectString = subject.replace(" ", "_");

        if (recipientEmail != null && !recipientEmail.equals("")) {
            Messages.showMessageDialog(project, "Subject - " + subjectString + "\nReport - " + path + "\nRecipientEmail - " + recipientEmail, "Information", Messages.getInformationIcon());
            appZip.generateFileList(new File(path));
            appZip.zipIt(subjectString, path);
            try {
                SendEmail.Send(recipientEmail, "", subjectString, "http://192.168.2.72:8181/logs/" + subjectString + ".zip", credsProp(dir));
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
        File dir = new File(System.getenv("APPDATA") + "\\reporting");
        dir.mkdir();
        return dir;
    }

    private Properties credsProp(File dir) {
        File file = new File(dir.getAbsolutePath() + "\\creds.properties");
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

    private String getRecipient(Properties properties, String name) {

        return String.valueOf(properties.get(name.toLowerCase()));
    }

    @NotNull
    private Properties getEmailsProperties(File dir) {
        File file = new File(dir.getAbsolutePath() + "\\emails.properties");
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
        return properties;
    }

    private String getInput(Project project, String topic, String message, String title, String markedText) {
        String value="";

        switch (topic){
            case "Path":{
                value = Messages.showInputDialog(project, message, title, Messages.getQuestionIcon(), markedText, new InputValidator() {
                    @Override
                    public boolean checkInput(String path) {
                        File file = new File(path);
                        if (file.isDirectory())
                            return true;
                        else
                            return false;
                    }

                    @Override
                    public boolean canClose(String s) {
                        return true;
                    }
                });
                break;
            }
            case "Name":{
                value = Messages.showInputDialog(project, message, title, Messages.getQuestionIcon(), "Enter A Ninja", new InputValidator() {
                    @Override
                    public boolean checkInput(String name) {
                        String recipientEmail = getRecipient(properties, name);
                        if (recipientEmail!=null && !recipientEmail.equals("null"))
                            return true;
                        else
                            return false;
                    }

                    @Override
                    public boolean canClose(String s) {
                        return true;
                    }
                });
                break;
            }
            case "Subject":{
                value = Messages.showInputDialog(project, message, title, Messages.getQuestionIcon(), markedText, new InputValidator() {
                    @Override
                    public boolean checkInput(String subject) {
                       if(subject.length()>2)
                            return true;
                        else
                            return false;
                    }

                    @Override
                    public boolean canClose(String s) {
                        return true;
                    }
                });
                break;
            }
        }

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
