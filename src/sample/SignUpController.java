import javafx.event.ActionEvent;

public class SignUpController {

    public void GoToLogin(ActionEvent e){
        WindowSwitcher.goToPage(e, "LoginView", 600, 400);
    }

    public void SignUp(ActionEvent e){
        WindowSwitcher.goToPage(e, "HomeView", 600, 400);
    }

}
