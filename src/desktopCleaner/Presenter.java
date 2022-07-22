package desktopCleaner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.collections4.CollectionUtils;

import javafx.scene.layout.VBox;

public class Presenter {
    private Model model;
    private View view;
    private Timer timer;
    private boolean firstTime = true;
    private TimerTask timerTask;
    private List<String> ignoredFiles = new ArrayList<>();
    private List<String> files;
    private ScheduledExecutorService ses;
    
    public void setModel(Model model) {
        this.model = model;
    }
    public void setView(View view) {
        this.view = view;
    }
    
    // Setters and Getters
    public Timer getTimer() {
        return timer;
    }
    public void setTimer(Timer timer) {
        this.timer = timer;
    }
    public TimerTask getTimerTask() {
        return timerTask;
    }
    public void setTimerTask(TimerTask timerTask) {
        this.timerTask = timerTask;
    }
    public boolean getFirstTime() {
        return firstTime;
    }
    public void setFirstTime(boolean firstTime) {
        this.firstTime = firstTime;
    }
    
    public List<String> getIgnoredFiles() {
        return ignoredFiles;
    }
    public void setIgnoredFiles(List<String> ignoredFiles) {
        this.ignoredFiles = ignoredFiles;
    }
    
    public List<String> getFiles() {
        return files;
    }
    public void setFiles(List<String> files) {
        this.files = files;
    }

    public VBox getGui() {
        return view;
    }
    
    public void createGui() {
        view.createGui();
    }
    
    
    // list all files on desktop
    public List<File> listAllFiles(){
        return model.listAllFiles();
    }
    
    // list files that are modified or opened longer than two days
    public List<String> listFiles(){
        List<String>filesNames = new ArrayList<>();
        List<File>files = model.listFiles();
        for (File file : files) {
            if (file != null) {
                filesNames.add(file.getName());
            }
        }
        return filesNames;
    }
    // read json file to get user preferences
    public List<String> readConfig(){
        List<String> ignoredFiles =  model.readConfig();
        List<String> ignoredList = null;
        if (ignoredFiles != null) {
            ignoredList = compareLists(ignoredFiles);
        }
        return ignoredList;
    }
    // compare the ignored files list with list of all files
    public List<String> compareLists(List<String>ignoredFiles){
        List<String> filteredList = new ArrayList<>();
        List<String> oldFiles = listFiles();
        for (String string : oldFiles) {
            for (String myfile : ignoredFiles) {
                if(myfile.equals(string)) {
                    filteredList.add(string);
                }
            }            
        }
        return filteredList;
    }
    // initialize config.json file
    public void initConfig() {
        List<String> ignoredFile = getIgnoredFiles();
        if (ignoredFile != null) {
            model.initConfig(ignoredFile);            
        }
    }
    // save the user preference for action: either organize files or delete
    public void saveAction(String action) {
        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        try {
            prefs.clear();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        prefs.put("action", action);
    }
    // get the action from preferences
    public String getAction() {
        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        String action = prefs.get("action", null);
        return action;
    }
    // delete files
    public boolean deleteFiles(List<String>files) {
        boolean isDeleted = false;
        if (!files.isEmpty()) {
            for (String string : files) {
                if (model.deleteFile(string)) {
                    isDeleted = true;
                }else {
                    isDeleted = false;
                }
            }            
        }
        return isDeleted;
    }
    // create directory and move files into the new directory
    public void createDirectory(List<String> files) {
        if (!files.isEmpty()) {
            String[] filesArray = files.toArray(new String[0]);
            HashMap<String, List<String>> filesMap = model.getDuplicates(filesArray);
            for(String key : filesMap.keySet()) {
                for(String value : filesMap.get(key)) {
                    model.createDir(key, value);
                }
            }            
        }
    }
    
    public void manager(String action) {
        ses = Executors.newScheduledThreadPool(1);
        Runnable task1 = () ->{
          managerHelper(action); 
        };
        
        ses.scheduleAtFixedRate(task1, 0, 24, TimeUnit.HOURS);
    }
    
    public void closeTask() {
        ses.shutdown();
    }
    
    public void managerHelper(String action) {
        if (getFirstTime()) {
            switch (action) {
                case "delete":
                    deleteAction();
                    break;
                case "organize":
                    organizeAction();
                    break;
            }
        }else {
            setFiles(rescan());
            switch (action) {
                case "delete":
                    deleteAction();
                    break;
                case "organize":
                    organizeAction();
                    break;
            }
        }
    }
    
    
    public void deleteAction() {
        if (getFiles() != null) {
            deleteFiles(getFiles());
            files.clear();
            setFirstTime(false);
        }
    }
    
    public void organizeAction() {
        if (getFiles() != null) {
            createDirectory(getFiles());
            files.clear();
            setFirstTime(false);
        }
    }
    
    public void onClose() {
        view.onClose();
    }
    
    /* 
     * basically what we doing here is the following operation:
     * list files, list ignoredFiles (Mengen Operation)
     * files - ignoredFiles
     */
    public List<String> rescan(){
        List<String> filteredList = null;
        List<String> ignoredFiles = getIgnoredFiles();
        List<String> files = listFiles();
        if (ignoredFiles != null) {
            Collection<String> filteredCollection = CollectionUtils.removeAll(files, ignoredFiles);
            filteredList = new ArrayList<>(filteredCollection);            
        }else {
            filteredList = files;
        }
        return filteredList;
    }
}
