import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.json.JSONObject;

public class FileManager {
    private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH_mm_ss");
    static String executable;
    static String url;
    static String backupDir;
    static String activeDir;
    static File active;
    static FileList list;

    public static void setup(FileList list, String configFilename) throws IOException {
        FileManager.list = list;

        String configContent = readFromFile(configFilename);
        JSONObject config = new JSONObject(configContent);

        executable = config.getString("executable");
        url = config.getString("url");
        backupDir = config.getString("backupDir");
        activeDir = config.getString("activeDir");
        active = new File(activeDir);
    }

    public static void pull() throws Exception {
        String dest = newBackupDir("Pull");
        List<String> fullFilenames = list.getFullFilenames();
        List<String> filenames = list.getFilenames();

        List<String> commands = new ArrayList<>();

        for (int i = 0; i < fullFilenames.size(); i++) {
            String command = "\"get " + fullFilenames.get(i) + " " + dest + "\\" + filenames.get(i) + "\"";
            commands.add(command);
        }

        executeFtpCommand(commands);

        restore(dest);
    }

    public static void push(int fileNum) throws Exception {
        String currentFile = activeDir + "\\" + list.getFilenames().get(fileNum);
        String dest = list.getFullFilenames().get(fileNum);

        String command = "\"put " + currentFile + " " + dest + "\"";

        executeFtpCommand(Collections.singletonList(command));
    }

    public static File[] listBackups() {
        return new File(backupDir).listFiles();
    }

    public static void restore(String backupName) throws IOException {
        cleanActive();

        File backup = new File(backupName);
        for (File file : backup.listFiles()) {
            if (!file.isDirectory()) {
                String dest = activeDir + "\\" + file.getName();
                File destFile = new File(dest);
                Files.copy(file.toPath(), destFile.toPath());
            }
        }
    }

    public static void backup(String postfix) throws IOException {
        String backup = newBackupDir(postfix);
        for (File file : active.listFiles()) {
            if (!file.isDirectory()) {
                String dest = backup + "\\" + file.getName();
                File destFile = new File(dest);
                Files.copy(file.toPath(), destFile.toPath());
            }
        }
    }

    private static void cleanActive() {
        for (File file : active.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

    private static String newBackupDir(String postfix) {
        if (postfix == null) {
            postfix = "";
        }

        Date date = new Date();
        String dir = backupDir + "\\Backup_" + formatter.format(date) + "_" + postfix + "\\";
        File folder = new File(dir);
        folder.mkdirs();

        return dir;
    }

    private static String readFromFile(String filename) throws IOException {
        File file = new File(filename);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder builder = new StringBuilder();
        reader.lines().forEach(builder::append);
        reader.close();

        return builder.toString();
    }

    private static void executeFtpCommand(List<String> commands) throws Exception {
        ProcessBuilder builder = new ProcessBuilder().command(
                executable,
                "/command",
                "open " + url,
                String.join(" ", commands),
                "exit"
        );
        builder.inheritIO();
        builder.start().waitFor();
    }
}
