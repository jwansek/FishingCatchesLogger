import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class WindowSwitcher {
    //Logic for page transfer
    public static void goToPage(ActionEvent event, String filename, double width, double height) {
        goToPage((Stage) ((Node) event.getSource()).getScene().getWindow(), filename, width, height);
    }

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