package desktopCleaner;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileSystemView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class Model {
    private HashMap<String, Image> mapOfFileExtToSmallIcon = new HashMap<String, Image>();
    private final String CONFIG_FILE = "config.json";

    // check what OS the program is running on
    public String checkOS() {
        String os = System.getProperty("os.name");
        return os;
    }

    // check how long the system has been running
    public long getSystemUptime() throws Exception {
        long uptime = -1;
        String os = checkOS().toLowerCase();
        if (os.contains("win")) {
            Process uptimeProc = Runtime.getRuntime().exec("net stats workstation");
            BufferedReader in = new BufferedReader(new InputStreamReader(uptimeProc.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("Statistics since")) {
                    SimpleDateFormat format = new SimpleDateFormat("'Statistics since' dd/MM/yyyy hh:mm:ss");
                    Date boottime = format.parse(line);
                    uptime = System.currentTimeMillis() - boottime.getTime();
                    break;
                }
            }
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            Process uptimeProc = Runtime.getRuntime().exec("uptime");
            BufferedReader in = new BufferedReader(new InputStreamReader(uptimeProc.getInputStream()));
            String line = in.readLine();
            if (line != null) {
                Pattern parse = Pattern.compile("((\\d+) days,)? (\\d+):(\\d+)");
                Matcher matcher = parse.matcher(line);
                if (matcher.find()) {
                    String _days = matcher.group(2);
                    String _hours = matcher.group(3);
                    String _minutes = matcher.group(4);
                    int days = _days != null ? Integer.parseInt(_days) : 0;
                    int hours = _hours != null ? Integer.parseInt(_hours) : 0;
                    int minutes = _minutes != null ? Integer.parseInt(_minutes) : 0;
                    uptime = TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS) + TimeUnit.MILLISECONDS.convert(hours, TimeUnit.HOURS) + TimeUnit.MILLISECONDS.convert(minutes, TimeUnit.MINUTES);
                }
            }
        }
        return uptime;
    }

    // get Desktop path
    public String getDesktopPath() {
        String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
        return desktopPath;
    }

    // List all non-hidden files on the desktop
    public List<File> listAllFiles() {
        File desktop = new File(getDesktopPath());
        File[] filesArray = desktop.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return !file.isHidden();
            }
        });
        List<File> filesList = Arrays.asList(filesArray);
        return filesList;
    }
    
    public long calculateDays(long date) {
        Date modifiedDate = new Date(date);
        Date currentDate = new Date();
        long differenceInTime = currentDate.getTime() - modifiedDate.getTime();
        long differenceInDays = (differenceInTime / (1000 * 60 * 60 * 24)) % 365;
        return differenceInDays;
    }

    // list files that last modified & last opened longer than 2 days
    public List<File> listFiles() {
        List<File> filesList = listAllFiles();
        List<File> oldFiles = new ArrayList<>();
        for (File file : filesList) {
            if (file.isFile()) {
                long lastModified = file.lastModified();
                long differenceInDays = calculateDays(lastModified);
                Path path = file.toPath();
                try {
                    BasicFileAttributes fatr = Files.readAttributes(path, BasicFileAttributes.class);
                    long timeInDays = calculateDays(fatr.lastAccessTime().toMillis());
                    if (differenceInDays > 2) {
                        if (timeInDays > 2) {
                            oldFiles.add(file);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return oldFiles;
    }

    public String getFileExt(String fileName) {
        String ext = ".";
        int p = fileName.lastIndexOf('.');
        if (p >= 0) {
            ext = fileName.substring(p);
        }
        return ext.toLowerCase();
    }

    public javax.swing.Icon getJSwingIconFromFileSystem(File file) {

        // Windows {
        FileSystemView view = FileSystemView.getFileSystemView();
        javax.swing.Icon icon = view.getSystemIcon(file);
        // }

        // OS X {
        // final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        // javax.swing.Icon icon = fc.getUI().getFileView(fc).getIcon(file);
        // }

        return icon;
    }

    public Image getFileIcon(String fileName) {
        final String ext = getFileExt(fileName);

        Image fileIcon = mapOfFileExtToSmallIcon.get(ext);
        if (fileIcon == null) {

            javax.swing.Icon jswingIcon = null;

            File file = new File(fileName);
            if (file.exists()) {
                jswingIcon = getJSwingIconFromFileSystem(file);
            } else {
                File tempFile = null;
                try {
                    tempFile = File.createTempFile("icon", ext);
                    jswingIcon = getJSwingIconFromFileSystem(tempFile);
                } catch (IOException ignored) {
                    // Cannot create temporary file.
                } finally {
                    if (tempFile != null)
                        tempFile.delete();
                }
            }

            if (jswingIcon != null) {
                fileIcon = jswingIconToImage(jswingIcon);
                mapOfFileExtToSmallIcon.put(ext, fileIcon);
            }
        }

        return fileIcon;
    }

    public Image jswingIconToImage(javax.swing.Icon jswingIcon) {
        BufferedImage bufferedImage = new BufferedImage(jswingIcon.getIconWidth(), jswingIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        jswingIcon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }
    
    public List<String> readConfig(){
        File file = new File(CONFIG_FILE);
        Gson gson = null;
        List<String> files = null;
        if (file.exists()) {
            gson = new GsonBuilder().setPrettyPrinting().create();
            Reader reader = null;
            try {
                reader = Files.newBufferedReader(Paths.get(CONFIG_FILE), Charset.forName("UTF-8"));
                files = gson.fromJson(reader, new TypeToken<List<String>>() {}.getType());
                
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return files;
    }
    
    public void initConfig(List<String> ignoredFiles) {
        if (ignoredFiles != null) {
            File file = new File(CONFIG_FILE);
            Gson gson = null;
            OutputStreamWriter writer = null;
            if (!file.exists()) {
                try {
                    gson = new GsonBuilder().setPrettyPrinting().create();
                    writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
                    gson.toJson(ignoredFiles, writer);
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                appendJson(ignoredFiles);
            }   
        }
    }
    
    public void appendJson(List<String> ignoredFile) {
        File file = new File(CONFIG_FILE);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            gson.toJson(ignoredFile, writer);
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } 
    }
    
    /*
     *  To sort files according to their extensions in directories
     *  Step 1:
     *  we create an array from the files list
     *  then iterate over the array and store its values in a HashMap with key = the extension and value = name of the file 
     *  Step 2: 
     *  iterate on the lists in HashMap and create Directories with the keys 
     *  and move the files(values) into to corresponding directory(key)
     *  Step 3: 
     *  we pray it works xD
     */
    
    // function that return HashMap with lists of duplicates extensions   
    public <T> HashMap<String, List<String>> getDuplicates(String[] array){
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        for(String item : array) {
            String itemExt = getFileExtNoDot(item);
            List<String> duplicates = result.get(itemExt);
            if (duplicates == null) {
                result.put(itemExt, duplicates = new ArrayList<>());
                duplicates.add(item);
            }else {
                duplicates.add(item);
            }
        }
        return result;
    }
    
    public String getFileExtNoDot(String fileName) {
        String ext = ".";
        int p = fileName.lastIndexOf('.');
        if (p >= 0) {
            ext = fileName.substring(p + 1);
        }
        return ext.toLowerCase();
    }
    
    // delete File
    public boolean deleteFile(String fileName) {
        boolean isDeleted = false;
        String path = getDesktopPath();
        path = path + File.separator + fileName;
        try {
            File file = new File(path);
            if (file.delete()) {
                isDeleted = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isDeleted;
    }
    
    // create directory
    public void createDir(String dirName, String fileName) {
        String desktopPath = getDesktopPath() + File.separator;
        String dirPath = desktopPath + dirName;
        String oldFilePath = desktopPath + fileName;
        String newFilePath = dirPath + File.separator + fileName;
        File dir = new File(dirPath);
        File oldFile = new File(oldFilePath);
        if (oldFile.exists()) {
            if (dir.mkdir()) {
                moveFile(oldFilePath, newFilePath);
            }else {
                moveFile(oldFilePath, newFilePath);
            }
        }
    }
    
    // function to move files to the new Directory 
    public boolean moveFile(String sourcePath, String targetPath) {

        boolean fileMoved = true;
        try {
            Files.move(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            fileMoved = false;
            e.printStackTrace();
        }
        return fileMoved;
    }
}
