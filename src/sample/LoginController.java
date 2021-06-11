import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;

public class LoginController {

    public void GoToSignUp(ActionEvent e){
        WindowSwitcher.goToPage(e, "SignUpView", 600, 400);
    }

    public void Login(ActionEvent e){
        WindowSwitcher.goToPage(e, "HomeView", 600, 400);
    }

}
