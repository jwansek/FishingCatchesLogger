import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;

public class WindowSwitcher {

    //logic for getting the current stage from the action event
    public static void goToPage(ActionEvent event, String filename, double width, double height) {
        goToPage((Stage) ((Node) event.getSource()).getScene().getWindow(), filename, width, height);
    }

    //method for switching pages
    public static void goToPage(Stage stage, String filename, double width, double height)
    {
        try {
            FXMLLoader loader = new FXMLLoader(new File( "src/sample/" + filename + ".fxml").toURI().toURL());
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);
            stage.show();

        } catch (java.io.IOException e)
        {
            System.err.println(e);
        }
    }

}