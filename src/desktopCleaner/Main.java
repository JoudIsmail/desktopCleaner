package desktopCleaner;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.imageio.ImageIO;

public class Main extends Application{
    
    private boolean firstTime;
    private TrayIcon trayIcon;
    private Presenter presenter;
    
    

    public Presenter getPresenter() {
        return presenter;
    }

    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        createTrayIcon(primaryStage);
        presenter = new Presenter();
        setPresenter(presenter);
        Model model = new Model();
//        View view = new View();
        View view = new View();
        
        view.setPresenter(presenter);
        presenter.setModel(model);
        presenter.setView(view);
        
        
        presenter.createGui();
        VBox root = presenter.getGui();
        firstTime = true;
        Platform.setImplicitExit(false);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("/folders.png")));
        primaryStage.setTitle("Desktop Cleaner");
        primaryStage.show();
    }
    
    public void createTrayIcon(final Stage stage) {
        if (SystemTray.isSupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // load an image
            java.awt.Image image = null;
            try {
                String imagePath = "D:\\eclipse-workspace\\desktopCleaner\\src\\folders.png";
                image = ImageIO.read(new File(imagePath));
            } catch (IOException ex) {
                System.out.println(ex);
            }


            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    hide(stage);
                    
                }
            });
            // create a action listener to listen for default action executed on the tray icon
            final ActionListener closeListener = new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    //save user Preferences before closing 
                    Presenter presenter = getPresenter();
                    presenter.onClose();
                    presenter.initConfig();
                    System.exit(0);
                }
            };

            ActionListener showListener = new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            stage.show();
                        }
                    });
                }
            };
            // create a popup menu
            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Show");
            showItem.addActionListener(showListener);
            popup.add(showItem);

            MenuItem closeItem = new MenuItem("Close");
            closeItem.addActionListener(closeListener);
            popup.add(closeItem);
            // construct a TrayIcon
            trayIcon = new TrayIcon(image, "Desktop Cleaner", popup);
            // set the TrayIcon properties
            trayIcon.addActionListener(showListener);
            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println(e);
            }
        }
    }
    
    public void showProgramIsMinimizedMsg() {
        if (firstTime) {
            trayIcon.displayMessage("Window closed.",
                    "Program running in background.",
                    TrayIcon.MessageType.INFO);
            firstTime = false;
        }
    }

    private void hide(final Stage stage) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (SystemTray.isSupported()) {
                    stage.hide();
                    showProgramIsMinimizedMsg();
                } else {
                    System.exit(0);
                }
            }
        });
    }
}
