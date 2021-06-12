import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.sql.SQLException;

public class LoginController {


    @FXML TextField username;
    @FXML TextField password;
    @FXML Label errorMessage;


    public void GoToSignUp(ActionEvent e){
        WindowSwitcher.goToPage(e, "SignUpView", 600, 400);
    }

    public void Login(ActionEvent e) throws LocalDatabase.IncorrectPasswordException, SQLException {
        String usernameText = username.getText().trim();
        String passwordText = password.getText().trim();

        if(usernameText.isEmpty()) {
            errorMessage.setText("Please enter a username.");
            return;
        } else if(passwordText.isEmpty()) {
            errorMessage.setText("Please enter a password.");
            return;
        }

        LocalDatabase db = new LocalDatabase();

        try{
            db.searchForUser(usernameText, passwordText);
            db.changeUser(usernameText, passwordText);
            WindowSwitcher.goToPage(e, "HomeView", 600, 400);

        } catch (LocalDatabase.UserNotFoundException | LocalDatabase.IncorrectPasswordException exception) {
            errorMessage.setText("Username or password is incorrect.");
        }
    }
}
