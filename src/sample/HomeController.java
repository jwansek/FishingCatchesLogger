import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;

public class HomeController  {

    private static LocalDatabase db;

    //load in the fxml elements
    @FXML Label stockTotal;
    @FXML Label errorMessage;
    @FXML ComboBox choiceBox;
    @FXML TextField searchField;

    @FXML private TableView<LocalDatabase.CatchRecord> tableView;
    @FXML private TableColumn<LocalDatabase.CatchRecord, String> dateTimeColumn;
    @FXML private TableColumn<LocalDatabase.CatchRecord, Double> weightColumn;
    @FXML private TableColumn<LocalDatabase.CatchRecord, Double> longitudeColumn;
    @FXML private TableColumn<LocalDatabase.CatchRecord, Double> latitudeColumn;

    //creates a new string to double converter
    private static final DoubleStringConverter converter = new DoubleStringConverter();

    //method to receive a database object
    public static void receiveDB(LocalDatabase database){
        db = database;
    }

    //method to go back to the login page
    public void Logout(ActionEvent e){
        WindowSwitcher.goToPage(e, "LoginView", 600, 400);
    }

    //method to switch to the sale data page
    public void switchDataTables(ActionEvent e){
        HomeSellController.receiveDB(db);
        WindowSwitcher.goToPage(e, "HomeSellView", 600, 400);
    }

    //method to go to the import export page
    public void goToImportExport(ActionEvent e){
        ImportExportController.receiveDB(db);
        WindowSwitcher.goToPage(e, "ImportExportView", 600, 400);
    }

    //method to go to the input page
    public void goToInput(ActionEvent e){
        InputController.receiveDB(db);
        WindowSwitcher.goToPage(e, "InputView", 600, 400);
    }

    //method that clears the current search
    public void clear() throws SQLException {
        searchField.clear();
        initialize();
    }
    //method that allows the user to search by a data category and only show in the table the records that match to that search
    public void search(){
        try {
            switch (choiceBox.getValue().toString()) {
                case "DateTime":
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                        LocalDateTime dateTime = LocalDateTime.parse(searchField.getText(), formatter);
                        ArrayList<LocalDatabase.Record> recordsArrayList = db.getRecordsByDate(dateTime);
                        ObservableList<LocalDatabase.CatchRecord> catchRecords = FXCollections.observableArrayList();
                        for(LocalDatabase.Record Record: recordsArrayList) {
                            if (Record instanceof LocalDatabase.CatchRecord) {
                                    catchRecords.add((LocalDatabase.CatchRecord) Record);
                            }
                        }
                        tableView.setItems(catchRecords);
                        errorMessage.setText("");
                    } catch (DateTimeParseException | SQLException e) {
                        errorMessage.setText("Please enter a valid date time");
                        searchField.clear();
                    }
                    break;
                case "Weight":
                    try{
                        double Weight = Double.parseDouble(searchField.getText());
                        ArrayList<LocalDatabase.Record> recordsArrayList = db.getRecordsByWeight(Weight);
                        ObservableList<LocalDatabase.CatchRecord> catchRecords = FXCollections.observableArrayList();
                        for(LocalDatabase.Record Record: recordsArrayList) {
                            if (Record instanceof LocalDatabase.CatchRecord) {
                                catchRecords.add((LocalDatabase.CatchRecord) Record);
                            }
                        }
                        tableView.setItems(catchRecords);
                        errorMessage.setText("");
                    } catch (NumberFormatException | SQLException e) {
                        errorMessage.setText("Please enter a valid numeric weight");
                        searchField.clear();
                    }

                    break;
                case "Longitude":
                    try{
                        double Longitude = Double.parseDouble(searchField.getText());
                        ArrayList<LocalDatabase.CatchRecord> recordsArrayList = db.getAllCatchRecords();
                        ObservableList<LocalDatabase.CatchRecord> catchRecords = FXCollections.observableArrayList();
                        for(LocalDatabase.CatchRecord Record: recordsArrayList) {
                            if (Record.longitude == Longitude) {
                                catchRecords.add(Record);
                            }
                        }
                        tableView.setItems(catchRecords);
                        errorMessage.setText("");
                    } catch (NumberFormatException | SQLException e) {
                        errorMessage.setText("Please enter a valid numeric longitude");
                        searchField.clear();
                    }
                    break;
                case "Latitude":
                    try{
                        double Latitude = Double.parseDouble(searchField.getText());
                        ArrayList<LocalDatabase.CatchRecord> recordsArrayList = db.getAllCatchRecords();
                        ObservableList<LocalDatabase.CatchRecord> catchRecords = FXCollections.observableArrayList();
                        for(LocalDatabase.CatchRecord Record: recordsArrayList) {
                            if (Record.latitude == Latitude) {
                                catchRecords.add(Record);
                            }
                        }
                        tableView.setItems(catchRecords);
                        errorMessage.setText("");
                    } catch (NumberFormatException | SQLException e) {
                        errorMessage.setText("Please enter a valid numeric longitude");
                        searchField.clear();
                    }
                    break;
            }
        } catch(NullPointerException e) {
            errorMessage.setText("Please select a search category");
        }
    }

    //method that calculates the current weight of fish in stock of the user
    public void calculateStock() throws SQLException {
        ArrayList<LocalDatabase.CatchRecord> catchRecordArrayList = db.getAllCatchRecords();
        Iterator<LocalDatabase.CatchRecord> i = catchRecordArrayList.iterator();
        ArrayList<LocalDatabase.SellRecord> SellRecordArrayList = db.getAllSellRecords();
        Iterator<LocalDatabase.SellRecord> x = SellRecordArrayList.iterator();
        double catchTotal = 0, sellTotal = 0;
        while (i.hasNext()){
            catchTotal+=i.next().getWeight();
        }
        while (x.hasNext()){
            sellTotal+=x.next().getWeight();
        }
        stockTotal.setText((catchTotal-sellTotal) + "kg");
    }

    //method for editing date time of a record
    public void changeDateTimeCellEvent(TableColumn.CellEditEvent edditedCell) throws SQLException {
        LocalDatabase.CatchRecord recordSelected = tableView.getSelectionModel().getSelectedItem();
        //check if the new value in eddited cell is in correct local date time format
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        try{
            LocalDateTime dateTime = LocalDateTime.parse(edditedCell.getNewValue().toString(), formatter);
            recordSelected.editDate(dateTime);
            errorMessage.setText("");
        } catch (DateTimeParseException e) {
            errorMessage.setText("Please enter a valid date time");
            initialize();
        }
    }

    //method for editing weight of a record
    public void changeWeightCellEvent(TableColumn.CellEditEvent edditedCell) throws SQLException {
        try{
            LocalDatabase.CatchRecord recordSelected = tableView.getSelectionModel().getSelectedItem();
            recordSelected.editWeight((Double) edditedCell.getNewValue());
            calculateStock();
            errorMessage.setText("");
        }
      catch(NumberFormatException e){
          errorMessage.setText("thats not a number");
      }
    }

    //method for editing the longitude of a record
    public void changeLongitudeCellEvent(TableColumn.CellEditEvent edditedCell) throws SQLException {
        LocalDatabase.CatchRecord recordSelected = tableView.getSelectionModel().getSelectedItem();
        recordSelected.editLocation(recordSelected.latitude , Double.parseDouble(edditedCell.getNewValue().toString()));
        errorMessage.setText("");
    }

    //method for editing the latitude of a record
    public void changeLatitudeCellEvent(TableColumn.CellEditEvent edditedCell) throws SQLException {
        LocalDatabase.CatchRecord recordSelected = tableView.getSelectionModel().getSelectedItem();
        recordSelected.editLocation(Double.parseDouble(edditedCell.getNewValue().toString()) , recordSelected.longitude);
        errorMessage.setText("");
    }

    //method for deleting records
    public void deleteButtonPushed() throws SQLException {
        ObservableList<LocalDatabase.CatchRecord> selectedRows;
        selectedRows = tableView.getSelectionModel().getSelectedItems();

        for (LocalDatabase.CatchRecord catchRecord: selectedRows){
         db.deleteData(catchRecord.record_id);
        }

        initialize();
    }

    //method for initializing/reset the table
    public void initialize() throws SQLException {
        calculateStock();
        dateTimeColumn.setCellValueFactory(new PropertyValueFactory<LocalDatabase.CatchRecord, String>("dateTime"));
        weightColumn.setCellValueFactory(new PropertyValueFactory<LocalDatabase.CatchRecord, Double>("weight"));
        latitudeColumn.setCellValueFactory(new PropertyValueFactory<LocalDatabase.CatchRecord, Double>("latitude"));
        longitudeColumn.setCellValueFactory(new PropertyValueFactory<LocalDatabase.CatchRecord, Double>("longitude"));

        ObservableList<LocalDatabase.CatchRecord> catchRecords = FXCollections.observableArrayList();

        try {
            ArrayList<LocalDatabase.CatchRecord> catchRecordArrayList = db.getAllCatchRecords();
            catchRecords.addAll(catchRecordArrayList);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        tableView.setItems(catchRecords);
        dateTimeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        try{
            weightColumn.setCellFactory(TextFieldTableCell.forTableColumn(converter));
            longitudeColumn.setCellFactory(TextFieldTableCell.forTableColumn(converter));
            latitudeColumn.setCellFactory(TextFieldTableCell.forTableColumn(converter));
        } catch (Exception e) {
            System.out.print(e);
        }
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

}
