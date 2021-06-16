import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;

public class ImportExportController {

    //load in the fxml elements
    @FXML Label fileLabel;
    @FXML Label folderLabel;
    @FXML Label errorMessage;

    private String selectedFile1;
    private String selectedDirectory1;
    private static LocalDatabase db;
    //method to receive a database object
    public static void receiveDB(LocalDatabase database){
        db = database;
    }

    //method to go back to homepage
    public void goToHome(ActionEvent event){
        HomeController.receiveDB(db);
        WindowSwitcher.goToPage(event, "HomeView", 600, 400);
    }

    //method for opening the file chooser and choosing a file
    public void openFileChooser(ActionEvent e){
        try{
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File selectedFile = fileChooser.showOpenDialog(((Node) e.getSource()).getScene().getWindow());
            fileLabel.setText(selectedFile.getName());
            selectedFile1 = selectedFile.getAbsolutePath();
        }catch(NullPointerException ignored){}
    }

    //method for opening the folder choosing and choosing a folder
    public void openFolderChooser(ActionEvent e){
        try{
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(((Node) e.getSource()).getScene().getWindow());
            folderLabel.setText(selectedDirectory.getName());
            selectedDirectory1 = selectedDirectory.getAbsolutePath();
        }catch(NullPointerException ignored){}
    }

    //method for importing data from a text file
    public void importData(ActionEvent e){
        try{
            db.importData(selectedFile1);
            errorMessage.setTextFill(Color.GREEN);
            errorMessage.setText("Successfully imported data");
        }catch(IOException | DateTimeParseException exception){
            errorMessage.setTextFill(Color.RED);
            errorMessage.setText("Error, cannot find file or wrong type of data");
        }
    }
    //method for exporting data to a text file
    public void exportData(ActionEvent e){
        try{
            db.exportData(selectedDirectory1);
            errorMessage.setTextFill(Color.GREEN);
            errorMessage.setText("Successfully exported data");
        }catch(IOException | SQLException exception){
            errorMessage.setTextFill(Color.RED);
            errorMessage.setText("Error, cannot find folder");
        }
    }

}
