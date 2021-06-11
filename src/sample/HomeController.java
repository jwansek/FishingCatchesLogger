import javafx.event.ActionEvent;

public class HomeController {

    public void Logout(ActionEvent e){
        WindowSwitcher.goToPage(e, "LoginView", 600, 400);
    }

}
