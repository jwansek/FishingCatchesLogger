import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class InputController {
    //if a string matches these regex they are either a numeric (including decimal) or a no decimal numeric
    private static final String numeric_regex = "-?\\d+(\\.\\d+)?";
    private static final String noDecimal_numeric_regex = "^\\d+$";
    private static LocalDatabase db;

    //load in the fxml elements
    @FXML DatePicker date;
    @FXML TextField weight;
    @FXML TextField longitude;
    @FXML TextField latitude;
    @FXML Label errorMessage;
    @FXML TextField hour;
    @FXML TextField minute;

    @FXML DatePicker date1;
    @FXML TextField weight1;
    @FXML TextField price;
    @FXML Label errorMessage1;
    @FXML TextField hour1;
    @FXML TextField minute1;
    //method to receive a database object
    public static void receiveDB(LocalDatabase database){
        db = database;
    }

    //method for inputting a new catch data record
    public void inputCatch(ActionEvent e) throws SQLException {

        if(date.getValue() == null){
            errorMessage.setText("Please enter a date.");
            return;
        }
        else if (hour.getText().isEmpty()){
            errorMessage.setText("Please enter an hour.");
            return;
        }
        else if (!hour.getText().matches(noDecimal_numeric_regex)){
            errorMessage.setText("Please enter an hour with no decimals or negatives.");
            return;
        }
        else if (Double.parseDouble(hour.getText()) > 23 || Double.parseDouble(hour.getText()) < 0){
            errorMessage.setText("Please enter an hour between 0 and 23");
            return;
        }
        else if (minute.getText().isEmpty()){
            errorMessage.setText("Please enter minutes.");
            return;
        }
        else if (!minute.getText().matches(noDecimal_numeric_regex)){
            errorMessage.setText("Please enter minutes with no decimals or negatives.");
            return;
        }
        else if (Double.parseDouble(minute.getText()) > 59 || Double.parseDouble(minute.getText()) < 0){
            errorMessage.setText("Please enter minutes between 0 and 59.");
            return;
        }

        else if(weight.getText().isEmpty()){
            errorMessage.setText("Please enter a weight.");
            return;
        }
        else if(!weight.getText().matches(numeric_regex)){
            errorMessage.setText("Please enter a numeric weight.");
            return;
        }
        else if(longitude.getText().isEmpty()){
            errorMessage.setText("Please enter a longitude.");
            return;
        }
        else if(!longitude.getText().matches(numeric_regex)){
            errorMessage.setText("Please enter a numeric longitude.");
            return;
        }
        else if(latitude.getText().isEmpty()){
            errorMessage.setText("Please enter a latitude.");
            return;
        }
        else if(!latitude.getText().matches(numeric_regex)){
            errorMessage.setText("Please enter a numeric latitude.");
            return;
        }

        double numericWeight = Double.parseDouble(weight.getText());
        double numericLongitude = Double.parseDouble(longitude.getText());
        double numericLatitude = Double.parseDouble(latitude.getText());
        LocalTime time = LocalTime.of(Integer.parseInt(hour.getText()), Integer.parseInt(minute.getText()), 0);
        LocalDateTime  DateTime = LocalDateTime.of(date.getValue(), time);

        db.inputCatchData(DateTime, numericWeight, numericLatitude, numericLongitude);

        HomeController.receiveDB(db);
        WindowSwitcher.goToPage(e, "HomeView", 600, 400);
    }

    //method for inputting sell records
    public void inputSell(ActionEvent e) throws SQLException {

        if(date1.getValue() == null){
            errorMessage1.setText("Please enter a date.");
            return;
        }
        else if (hour1.getText().isEmpty()){
            errorMessage1.setText("Please enter an hour.");
            return;
        }
        else if (!hour1.getText().matches(noDecimal_numeric_regex)){
            errorMessage1.setText("Please enter an hour with no decimals or negatives.");
            return;
        }
        else if (Double.parseDouble(hour1.getText()) > 23 || Double.parseDouble(hour1.getText()) < 0){
            errorMessage1.setText("Please enter an hour between 0 and 23");
            return;
        }
        else if (minute1.getText().isEmpty()){
            errorMessage1.setText("Please enter minutes.");
            return;
        }
        else if (!minute1.getText().matches(noDecimal_numeric_regex)){
            errorMessage1.setText("Please enter minutes with no decimals or negatives.");
            return;
        }
        else if (Double.parseDouble(minute1.getText()) > 59 || Double.parseDouble(minute1.getText()) < 0){
            errorMessage1.setText("Please enter minutes between 0 and 59.");
            return;
        }

        else if(weight1.getText().isEmpty()){
            errorMessage1.setText("Please enter a weight.");
            return;
        }
        else if(!weight1.getText().matches(numeric_regex)){
            errorMessage1.setText("Please enter a numeric weight.");
            return;
        }
        else if(price.getText().isEmpty()){
            errorMessage1.setText("Please enter a price.");
            return;
        }
        else if(!price.getText().matches(numeric_regex)){
            errorMessage1.setText("Please enter a numeric price.");
            return;
        }

        double numericWeight = Double.parseDouble(weight1.getText());
        double numericPrice = Double.parseDouble(price.getText());

        LocalTime time = LocalTime.of(Integer.parseInt(hour1.getText()), Integer.parseInt(minute1.getText()), 0);
        LocalDateTime  DateTime = LocalDateTime.of(date1.getValue(), time);

        db.inputSellData(DateTime, numericWeight, numericPrice);

        HomeController.receiveDB(db);
        WindowSwitcher.goToPage(e, "HomeView", 600, 400);
    }

    //method to go back to the homepage
    public void goToHome(ActionEvent event){
        HomeController.receiveDB(db);
        WindowSwitcher.goToPage(event, "HomeView", 600, 400);
    }
}
