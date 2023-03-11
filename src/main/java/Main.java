public class Main {
    public static void main(String[] args) throws Exception {
        FileList fileList = new FileList("files.txt");
        FileManager.setup(fileList, "config.txt");
        UI ui = new UI(fileList);
        ui.mainWindow();
    }
}
