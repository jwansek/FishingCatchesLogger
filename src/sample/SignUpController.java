import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.sql.SQLException;
import java.awt.*;

public class SignUpController {

    private static final String EMAIL_REGEX = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
    @FXML TextField username;
    @FXML TextField email;
    @FXML TextField password;
    @FXML TextField confirmPassword;
    @FXML Label errorMessage;

    public void GoToLogin(ActionEvent e){
        WindowSwitcher.goToPage(e, "LoginView", 600, 400);
    }

    public void SignUp(ActionEvent e) throws SQLException, LocalDatabase.IncorrectPasswordException {
        String usernameText = username.getText().trim();
        String emailText = email.getText().trim();
        String passwordText = password.getText().trim();
        String confirmPasswordText = confirmPassword.getText().trim();

        if(usernameText.isEmpty()) {
            errorMessage.setText("Please enter a username.");
            return;
        }
        else if (!emailText.matches(EMAIL_REGEX)) {
            errorMessage.setText("The given email isn't valid.");
            return;
        }
        else if(passwordText.isEmpty()) {
            errorMessage.setText("Please enter a password.");
            return;
        }
        else if(confirmPasswordText.isEmpty()) {
            errorMessage.setText("Please enter a password.");
            return;
        }
        else if(!confirmPasswordText.equals(passwordText)) {
            errorMessage.setText("Passwords did not match");
            return;
        }

        LocalDatabase db = new LocalDatabase();

        try{
            if (db.searchForUser(usernameText, passwordText) != null){
                errorMessage.setText("An user with that username already exists");
            }

        } catch (LocalDatabase.UserNotFoundException exception) {
            db.addUser(usernameText, emailText, passwordText);
            WindowSwitcher.goToPage(e, "HomeView", 600, 400);
        }
    }
}
