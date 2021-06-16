import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.sql.SQLException;

public class SignUpController {
    //if a string matches this it is a valid email
    private static final String EMAIL_REGEX = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";

    //load in the fxml elements
    @FXML TextField username;
    @FXML TextField email;
    @FXML TextField password;
    @FXML TextField confirmPassword;
    @FXML Label errorMessage;

    //method to go to the login page
    public void GoToLogin(ActionEvent e){
        WindowSwitcher.goToPage(e, "LoginView", 600, 400);
    }

    //method to sign up a new user using the details entered, and then login the user
    public void SignUp(ActionEvent e) throws SQLException, LocalDatabase.UserNotFoundException, LocalDatabase.IncorrectPasswordException {
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
            errorMessage.setText("Please confirm the password.");
            return;
        }
        else if(!confirmPasswordText.equals(passwordText)) {
            errorMessage.setText("Passwords did not match");
            return;
        }

        LocalDatabase db = new LocalDatabase();

        try{
            if (db.searchForUser(usernameText) != null){
                errorMessage.setText("A user with that username already exists");
            }

        } catch (LocalDatabase.UserNotFoundException exception) {
            db.addUser(usernameText, emailText, passwordText);
            db.changeUser(usernameText, passwordText);
            WindowSwitcher.goToPage(e, "HomeView", 600, 400);
        }
    }
}
