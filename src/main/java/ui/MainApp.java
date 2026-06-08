package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX 應用程式啟動點
 * <p>
 * 負責：
 * 1. 載入主畫面 FXML
 * 2. 建立 Scene
 * 3. 設定主視窗標題與大小
 * 4. 啟動 JavaFX GUI
 */
public class MainApp extends Application {
    private static final String APP_TITLE = "CPU Scheduling Simulator";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ui/MainView.fxml")
        );

        Scene scene = new Scene(
                loader.load(),
                WINDOW_WIDTH,
                WINDOW_HEIGHT
        );

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
