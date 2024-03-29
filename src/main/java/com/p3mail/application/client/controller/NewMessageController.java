package com.p3mail.application.client.controller;

import com.p3mail.application.ClientMain;
import com.p3mail.application.client.model.Client;
import com.p3mail.application.connection.model.Email;
import com.p3mail.application.connection.request.SendRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class NewMessageController {
    Socket socketConnection = null;
    ObjectOutputStream out = null;

    @FXML
    private TextArea textContent;

    @FXML
    private TextField objectField;

    @FXML
    private TextField receiversField;

    @FXML
    private Button sendButton;

    @FXML
    private Label moreRecipientsLabel;

    private Stage stage;
    private double oldWidth;
    private double oldHeight;
    private Client model;
    private Boolean isNewMessage;
    private List<String> notDuplicateRecipients;
    private boolean alreadyChecked;
    private boolean syntaxIsCorrect;
    private final String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." +
            "[a-zA-Z0-9_+&*-]+)*@" +
            "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
            "A-Z]{2,7}$";
    // private final String emailRegex = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
    //        + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

    /**
     * It takes a Boolean value from MainWindowController class. This is necessary to set
     * the object field ad not editable in case of an email forwarding.
     *
     * @param isNewMessage
     * @param model
     * @param email
     */
    @FXML
    public void initialize(Boolean isNewMessage, Client model, Email email, Stage stage) {
        this.stage = stage;
        this.model = model;
        this.isNewMessage = isNewMessage;
        this.socketConnection = model.getSocket();
        this.out = model.getOut();
        notDuplicateRecipients = new ArrayList<>();
        alreadyChecked = true;
        syntaxIsCorrect = true;
        moreRecipientsLabel.setVisible(false);
        if (!isNewMessage) {
            objectField.setEditable(false);
            if (email.getObject().startsWith("Fwd: "))
				objectField.setText(email.getObject());
			else if (email.getObject().startsWith("Re: "))
				objectField.setText("Fwd: " + email.getObject().substring(4));
			else
				objectField.setText("Fwd: " + email.getObject());

			System.out.println(objectField);

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(email.getReceivers().get(0));
			for (int i = 1; i < email.getReceivers().size(); i++) {
				stringBuilder.append(", ").append(email.getReceivers().get(i));
			}
			textContent.setText('\n' + "----------Messaggio inoltrato----------" + '\n' + "Da: " + email.getSender() + '\n' + "Data email ricevuta: " + email.getDate() + '\n' + "Oggetto: " + email.getObject() + '\n' + "A: " + stringBuilder + '\n' + "Contenuto: " + email.getText());
		}
    }

    /**
     * This method checks if all emails in list are syntactically correct
     * by calling isValid(email) method on each email of list.
     */
    private boolean emailsSyntaxIsCorrect() {
        if (!alreadyChecked) {
            boolean valid = true;
            List <String> allRecipients = new ArrayList();
			String recipients = receiversField.getText();
			allRecipients = List.of(recipients.split(", "));
			for (String rec : allRecipients) {
				valid = valid && isValid(rec);
				if(!notDuplicateRecipients.contains(rec))
					notDuplicateRecipients.add(rec);
			}
            alreadyChecked = true;
            syntaxIsCorrect = valid;
        }
        return syntaxIsCorrect;
    }

    /**
     * This method checks if an email is syntactically correct.
     *
     * @param email email to check
     */
    private boolean isValid(String email) {
        Pattern pat = Pattern.compile(emailRegex);
        if (email == null)
            return false;
        return pat.matcher(email).matches();
    }


    private void recipientsEmailWrong() {
        moreRecipientsLabel.setText("Email non valida!");
        moreRecipientsLabel.setTextFill(Color.web("#ff0000ff"));
        moreRecipientsLabel.setVisible(true);
    }

    /**
     * This methods checks if recipients are correct.
     *
     * @param mouseEvent
     */
    public void handleCheckIfTextChanged(MouseEvent mouseEvent) {
        if (!(emailsSyntaxIsCorrect()))
            recipientsEmailWrong();
        else
            moreRecipientsLabel.setVisible(false);
    }

    /**
     * When text in recipients changes alreadyChecked variable is set to false.
     *
     * @param keyEvent
     */
    public void handleRecipientsChanged(KeyEvent keyEvent) {
        alreadyChecked = false;
        if (receiversField.getText().isEmpty()) {
            alreadyChecked = true;
            syntaxIsCorrect = true;
        }
    }

    /**
     * When newRecipients button is clicked, the moreRecipientsLabel is set as visible.
     * The label disappears with a transition thanks to java FadeTransition class.
     *
     * @param mouseEvent
     */
    public void handleNewRecipients(MouseEvent mouseEvent) {
        if (emailsSyntaxIsCorrect()) {
            moreRecipientsLabel.setVisible(false);
            moreRecipientsLabel.setText("Inserisci una virgola seguita da uno spazio se vuoi aggiungere più destinatari");
            moreRecipientsLabel.setTextFill(Color.web("#06bf9d"));
            moreRecipientsLabel.setVisible(true);
//			FadeTransition fadeOut = new FadeTransition(Duration.millis(8000), moreRecipientsLabel);
//			fadeOut.setFromValue(2);
//			fadeOut.setToValue(0);
//			fadeOut.play();
        } else
            recipientsEmailWrong();
    }

    /**
     * When send button is clicked it changes controller and fxml file to MainWindowController and mainWindow.fxml.
     *
     * @param mouseEvent
     */
    public void handleSendButton(MouseEvent mouseEvent) throws IOException {
        if (!receiversField.getText().isEmpty() && emailsSyntaxIsCorrect()) {
            moreRecipientsLabel.setVisible(false);
            if (objectField.getText().isEmpty() || textContent.getText().isEmpty()) {
                if (!informationDialog())
                    return;
            }

            Email emailToSend = new Email(
                    model.emailAddressProperty().get(),
                    notDuplicateRecipients,
                    objectField.getText(),
                    textContent.getText());
            System.out.println("You want to send the email: "); //debug purpose
            System.out.println(emailToSend);
            try {
                model.writeOut(new SendRequest(emailToSend));
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("The server doesn't seem connected");
                alert.show();
            } finally {
                FXMLLoader loader = new FXMLLoader((ClientMain.class.getResource("mainWindow.fxml")));
                Parent root = (Parent) loader.load();
                MainWindowController newMainWindowController = loader.getController();
                newMainWindowController.initialize(false, model, stage);
                oldWidth = stage.getWidth();
                oldHeight = stage.getHeight();
                Scene scene = new Scene(root);
                Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
                stage.setTitle("Email client");
                stage.setScene(scene);
                stage.setWidth(oldWidth);
                stage.setHeight(oldHeight);
                stage.show();
            }
        } else
            recipientsEmailWrong();
    }

    /**
     * When cancel button is clicked it changes controller and fxml file to MainWindowController and mainWindow.fxml.
     *
     * @param mouseEvent
     */
    public void handleCancelButton(MouseEvent mouseEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader((ClientMain.class.getResource("mainWindow.fxml")));
        Parent root = (Parent) loader.load();
        MainWindowController newMainWindowController = loader.getController();
        newMainWindowController.initialize(false, model, stage);
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        stage.setTitle("Email client");
        stage.setScene(scene);
        stage.setWidth(oldWidth);
        stage.setHeight(oldHeight);
        stage.show();
    }

    /**
     * This method checks if objectField field or textContent are empty.
     */
    private boolean informationDialog() {
        Alert alert = null;
        if (objectField.getText().isEmpty() && textContent.getText().isEmpty()) {
            alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("Campo oggetto e testo vuoto, confermi di voler inviare ugualmente la mail?");
        } else if (objectField.getText().isEmpty()) {
            alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("Campo oggetto vuoto, confermi di voler inviare ugualmente la mail?");
        } else if (textContent.getText().isEmpty()) {
            alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("Campo testo vuoto, confermi di voler inviare ugualmente la mail?");
        }
        if (alert != null) {
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES)
                return true;
        }
        return false;
    }

}
