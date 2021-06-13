import javafx.event.ActionEvent;

public class HomeController {

    private static LocalDatabase db;

    public static void receiveDB(LocalDatabase database){
        db = database;
    }

    public void Logout(ActionEvent e){
        WindowSwitcher.goToPage(e, "LoginView", 600, 400);
    }

    public void goToInput(ActionEvent e){
        InputController.receiveDB(db);
        WindowSwitcher.goToPage(e, "InputView", 600, 400);
    }

}
