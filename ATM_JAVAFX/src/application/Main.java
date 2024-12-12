package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScene.fxml"));
            Parent root = loader.load();
            
            // Create the Scene
            Scene scene = new Scene(root);

            // Set the Scene to the Stage
            primaryStage.setTitle("Hello, World");
            primaryStage.setScene(scene);

            // Make the application adaptable to full-screen
            primaryStage.setMaximized(true); // Start maximized
            primaryStage.setFullScreenExitHint(""); // Optional: disable the exit hint for full-screen
            primaryStage.setFullScreen(true); // Enable full-screen mode
            
            // Show the Stage
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
