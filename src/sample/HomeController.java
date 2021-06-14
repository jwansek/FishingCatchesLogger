import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class HomeController  {

    private static LocalDatabase db;

    @FXML Label stockTotal;
    @FXML Label errorMessage;
    @FXML ChoiceBox choiceBox;
    @FXML TextField searchField;

    @FXML private TableView<LocalDatabase.CatchRecord> tableView;
    @FXML private TableColumn<LocalDatabase.CatchRecord, String> dateTimeColumn;
    @FXML private TableColumn<LocalDatabase.CatchRecord, Double> weightColumn;
    @FXML private TableColumn<LocalDatabase.CatchRecord, Double> longitudeColumn;
    @FXML private TableColumn<LocalDatabase.CatchRecord, Double> latitudeColumn;

    private static final DoubleStringConverter converter = new DoubleStringConverter();

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

                    break;
                case "Longitude":

                    break;
                case "Latitude":

                    break;
            }
        } catch(NullPointerException e) {
            errorMessage.setText("Please select a search category");
        }
    }


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
    public void changeLongitudeCellEvent(TableColumn.CellEditEvent edditedCell) throws SQLException {
        LocalDatabase.CatchRecord recordSelected = tableView.getSelectionModel().getSelectedItem();
        recordSelected.editLocation(recordSelected.latitude , Double.parseDouble(edditedCell.getNewValue().toString()));
        errorMessage.setText("");
    }
    public void changeLatitudeCellEvent(TableColumn.CellEditEvent edditedCell) throws SQLException {
        LocalDatabase.CatchRecord recordSelected = tableView.getSelectionModel().getSelectedItem();
        recordSelected.editLocation(Double.parseDouble(edditedCell.getNewValue().toString()) , recordSelected.longitude);
        errorMessage.setText("");
    }

    public void deleteButtonPushed() throws SQLException {
        ObservableList<LocalDatabase.CatchRecord> selectedRows;
        selectedRows = tableView.getSelectionModel().getSelectedItems();

        for (LocalDatabase.CatchRecord catchRecord: selectedRows){
         //database delete method needs to go here
        }
    }

    public void initialize() throws SQLException {
        calculateStock();
        choiceBox.getItems().addAll("DateTime", "Weight", "Longitude", "Latitude");
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
        tableView.setEditable(true);
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
