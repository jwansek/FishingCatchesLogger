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

public class HomeSellController  {

    private static LocalDatabase db;

    //load in the fxml elements
    @FXML Label stockTotal;
    @FXML Label errorMessage;
    @FXML ComboBox choiceBox;
    @FXML TextField searchField;

    @FXML private TableView<LocalDatabase.SellRecord> tableView;
    @FXML private TableColumn<LocalDatabase.SellRecord, String> dateTimeColumn;
    @FXML private TableColumn<LocalDatabase.SellRecord, Double> weightColumn;
    @FXML private TableColumn<LocalDatabase.SellRecord, Double> priceColumn;

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
    //method to switch to the catch data page
    public void switchDataTables(ActionEvent e){
        HomeController.receiveDB(db);
        WindowSwitcher.goToPage(e, "HomeView", 600, 400);
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
    public void search() {
        try {
            switch (choiceBox.getValue().toString()) {
                case "DateTime":
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                        LocalDateTime dateTime = LocalDateTime.parse(searchField.getText(), formatter);
                        ArrayList<LocalDatabase.Record> recordsArrayList = db.getRecordsByDate(dateTime);
                        ObservableList<LocalDatabase.SellRecord> sellRecords = FXCollections.observableArrayList();
                        for (LocalDatabase.Record Record : recordsArrayList) {
                            if (Record instanceof LocalDatabase.SellRecord) {
                                sellRecords.add((LocalDatabase.SellRecord) Record);
                            }
                        }
                        tableView.setItems(sellRecords);
                        errorMessage.setText("");
                    } catch (DateTimeParseException | SQLException e) {
                        errorMessage.setText("Please enter a valid date time");
                        searchField.clear();
                    }
                    break;
                case "Weight":
                    try {
                        double Weight = Double.parseDouble(searchField.getText());
                        ArrayList<LocalDatabase.Record> recordsArrayList = db.getRecordsByWeight(Weight);
                        ObservableList<LocalDatabase.SellRecord> sellRecords = FXCollections.observableArrayList();
                        for (LocalDatabase.Record Record : recordsArrayList) {
                            if (Record instanceof LocalDatabase.SellRecord) {
                                sellRecords.add((LocalDatabase.SellRecord) Record);
                            }
                        }
                        tableView.setItems(sellRecords);
                        errorMessage.setText("");
                    } catch (NumberFormatException | SQLException e) {
                        errorMessage.setText("Please enter a valid numeric weight");
                        searchField.clear();
                    }

                    break;
                case "Price":
                    try {
                        double Price = Double.parseDouble(searchField.getText());
                        ArrayList<LocalDatabase.SellRecord> recordsArrayList = db.getRecordsByRevenue(Price);
                        ObservableList<LocalDatabase.SellRecord> sellRecords = FXCollections.observableArrayList();
                        for (LocalDatabase.SellRecord Record : recordsArrayList) {
                            if (Record.revenue == Price) {
                                sellRecords.add(Record);
                            }
                        }
                        tableView.setItems(sellRecords);
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
        LocalDatabase.SellRecord recordSelected = tableView.getSelectionModel().getSelectedItem();
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
            LocalDatabase.SellRecord recordSelected = tableView.getSelectionModel().getSelectedItem();
            recordSelected.editWeight((Double) edditedCell.getNewValue());
            calculateStock();
            errorMessage.setText("");
        }
        catch(NumberFormatException e){
            errorMessage.setText("that's not a number");
        }
    }
    //method for editing the latitude of a record
    public void changePriceCellEvent(TableColumn.CellEditEvent edditedCell) throws SQLException {
        try{
            LocalDatabase.SellRecord recordSelected = tableView.getSelectionModel().getSelectedItem();
            recordSelected.editRevenue(Double.parseDouble(edditedCell.getNewValue().toString()));
            calculateStock();
            errorMessage.setText("");
        }
        catch(NumberFormatException e){
            errorMessage.setText("that's not a number");
        }
    }

    //method for deleting records
    public void deleteButtonPushed() throws SQLException {
        ObservableList<LocalDatabase.SellRecord> selectedRows;
        selectedRows = tableView.getSelectionModel().getSelectedItems();

        for (LocalDatabase.SellRecord sellRecord: selectedRows){
            db.deleteData(sellRecord.record_id);
        }

        initialize();
    }
    //method for initializing/reset the table
    public void initialize() throws SQLException {
        calculateStock();
        dateTimeColumn.setCellValueFactory(new PropertyValueFactory<LocalDatabase.SellRecord, String>("dateTime"));
        weightColumn.setCellValueFactory(new PropertyValueFactory<LocalDatabase.SellRecord, Double>("weight"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<LocalDatabase.SellRecord, Double>("price"));


        ObservableList<LocalDatabase.SellRecord> sellRecords = FXCollections.observableArrayList();

        try {
            ArrayList<LocalDatabase.SellRecord> sellRecordArrayList = db.getAllSellRecords();
            sellRecords.addAll(sellRecordArrayList);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        tableView.setItems(sellRecords);
        dateTimeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        try{
            weightColumn.setCellFactory(TextFieldTableCell.forTableColumn(converter));
            priceColumn.setCellFactory(TextFieldTableCell.forTableColumn(converter));
        } catch (Exception e) {
            System.out.print(e);
        }
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

}
