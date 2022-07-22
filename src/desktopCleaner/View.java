package desktopCleaner;

import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class View extends VBox{
    private Presenter presenter;
    private ObservableList<String> files;
    private ObservableList<String> ignoredFiles;
    private ListView<String> filesListView, ignoredListView;
    private Button movebtn, backbtn;
    private ToggleGroup toggleGroup;
    private RadioButton delete, organize;

    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }



    public void createGui() {
        files = FXCollections.observableArrayList(presenter.listFiles());
        ignoredFiles = FXCollections.observableArrayList();
        if (presenter.readConfig() != null) {
            List<String> ignoredList = presenter.readConfig();
            for (String string : ignoredList) {
                files.remove(string);
                ignoredFiles.add(string);
            }
        }
        this.getChildren().addAll(boxesWrapper(), createRadioBox(), createButtonsBox());
        this.setPadding(new Insets(5));
    }
    
    public void refresh() {
        files.clear();
        files = FXCollections.observableArrayList(presenter.rescan());
        if (presenter.readConfig() != null) {
            List<String> ignoredList = presenter.readConfig();
            for (String string : ignoredList) {
                if (!ignoredFiles.contains(string)) {
                    ignoredFiles.add(string);
                }
            }
        }
        this.getChildren().addAll(boxesWrapper(), createRadioBox(), createButtonsBox());
        this.setPadding(new Insets(5));
    }
    
    public void onClose() {
        presenter.setFiles(filesListView.getItems());
        presenter.setIgnoredFiles(ignoredListView.getItems());
    }
    
     public VBox createFilesBox() {
         VBox container = new VBox(2);
         Label filesListLabel = new Label("List of unused Files detected:");
         filesListView = new ListView<String>();
         filesListView.setItems(files);
         filesListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
             @Override
             public ListCell<String> call(ListView<String> list) {
                 return new AttachmentListCell();
             }
         });
         container.getChildren().addAll(filesListLabel, filesListView);
         return container;
     }
     
     public VBox createMoveBackButtonsBox() {
         VBox container = new VBox(5);
         movebtn = new Button("=>");
         backbtn = new Button("<=");
         
         movebtn.setOnAction(e -> {
             String selectedItem = filesListView.getSelectionModel().getSelectedItem();
             if (selectedItem != null) {
                 files.remove(selectedItem);
                 ignoredFiles.add(selectedItem); 
                 presenter.setIgnoredFiles(ignoredFiles);
                 presenter.setFiles(files);
             }
         });
         
         backbtn.setOnAction(e -> {
             String selectedItem = ignoredListView.getSelectionModel().getSelectedItem();
             if (selectedItem != null) {
                 files.add(selectedItem);
                 ignoredFiles.remove(selectedItem); 
                 presenter.setIgnoredFiles(ignoredFiles);
                 presenter.setFiles(files);
             }
         });
         
         container.getChildren().addAll(movebtn, backbtn);
         container.setAlignment(Pos.CENTER);
         return container;
     }
     
     public VBox createIgnoredBox() {
         VBox container = new VBox(2);
         Label ignoredListLabel = new Label("List of Files to be ignored:");
         ignoredListView = new ListView<String>();
         ignoredListView.setItems(ignoredFiles);
         ignoredListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
             @Override
             public ListCell<String> call(ListView<String> list) {
                 return new AttachmentListCell();
             }
         });
         container.getChildren().addAll(ignoredListLabel, ignoredListView);
         return container;
     }
     
     public HBox createRadioBox() {
         // Container
         HBox container = new HBox(5);
         // create Label
         Label actionLabel = new Label("Action:");
         // create ToggleGroup
         toggleGroup = new ToggleGroup();
         // create RadioButtons
         delete = new RadioButton("delete");
         delete.setId("delete");
         organize = new RadioButton("organize");
         organize.setId("organize");
         // setting the toggleGroup for the radioButtons
         delete.setToggleGroup(toggleGroup);
         organize.setToggleGroup(toggleGroup);
         
         if (presenter.getAction()!= null) {
             String action = presenter.getAction();
             switch (action) {
                 case "delete":
                     delete.setSelected(true);
                     break;
                 case "organize":
                     organize.setSelected(true);
                     break;
                 default:
                     delete.setSelected(true);
                     break;
             }
         }
         
         toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {

             @Override
             public void changed(ObservableValue<? extends Toggle> observableValue, Toggle oldValue, Toggle newValue) {
                 RadioButton radioButton = (RadioButton) toggleGroup.getSelectedToggle();
                 if (radioButton != null) {
                     String action = radioButton.getId();
                     presenter.saveAction(action); 
                 }
             }
         });
         // wrap everything in container
         container.getChildren().addAll(actionLabel, delete, organize);
         return container;
     }
     
     public HBox createButtonsBox() {
         HBox container = new HBox(2);
         Button startbtn = new Button("Start");
         Button stopbtn = new Button("Stop");
         stopbtn.setDisable(true);
         /*
          * The idea here is when (start) button is pressed then the buttons (Start, move, back) 
          * must be set to disabled and (stop) button must be enabled
          * then we check if the function is running for first time or not.
          * the function will be scheduled to run once every 24 Hours
          * if it's running for first time then there is no need to rescan the desktop for files
          * because it means that the user just opened the the application
          * if it's not running for first time(the program has been running for 24 hours or more)
          * then we scan the desktop and capture the files.
          * in the case of rescanning.. Following steps must be done:
          * step 1: 
          *        make sure to filter the ignored files
          * step 2: 
          *        update the old files list
          */
         startbtn.setOnAction(e ->{
             startbtn.setDisable(true);
             movebtn.setDisable(true);
             backbtn.setDisable(true);
             delete.setDisable(true);
             organize.setDisable(true);
             stopbtn.setDisable(false);
             presenter.setFiles(files);
             presenter.setIgnoredFiles(ignoredFiles);
             RadioButton action = (RadioButton) toggleGroup.getSelectedToggle();
             String actionString = action.getId();
             presenter.manager(actionString);
             filesListView.setDisable(true);
             ignoredListView.setDisable(true);
             files.clear();
         });
         
         /*
          * when the (Stop) button is pressed then we remove all items from the VBox and rescan for files
          * in case the user stopped the program but didn't close it, so when the user press start again
          * we can capture the changes in desktop.
          * the task must be stopped here.
          */
         stopbtn.setOnAction(e ->{
             presenter.closeTask();
             presenter.initConfig();
             this.getChildren().clear();
             refresh();
         });
         container.getChildren().addAll(startbtn, stopbtn);
         container.setAlignment(Pos.BOTTOM_RIGHT);
         return container;
     }
     
     public HBox boxesWrapper() {
         HBox container = new HBox(5);
         container.getChildren().addAll(createFilesBox(), createMoveBackButtonsBox(), createIgnoredBox());
         return container;
     }
     
     private class AttachmentListCell extends ListCell<String> {
         Model model = new Model();
         @Override
         public void updateItem(String item, boolean empty) {
             super.updateItem(item, empty);
             if (empty) {
                 setGraphic(null);
                 setText(null);
             } else {
                 Image fxImage = model.getFileIcon(item);
                 ImageView imageView = new ImageView(fxImage);
                 setGraphic(imageView);
                 setText(item);
             }
         }
     }
}
