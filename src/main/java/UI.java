import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class UI {
    public static final String title = "PTT DevScript";
    private FileList list;

    public UI(FileList list) {
        this.list = list;
    }

    public void mainWindow() throws Exception {
        String[] actions = {"Pull", "Push", "Backup", "Restore"};
        int result = JOptionPane.showOptionDialog(null, "Choose an action:", title, JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE, null, actions, actions[0]);

        // Close if nothing chosen
        if (result == -1) {
            return;
        }

        switch (result) {
            case 0:
                // Pull code
                boolean confirm1 = confirmWindow("Are you sure you want to pull?");
                if (!confirm1) {
                    System.out.println("Pull cancelled");
                    break;
                }

                FileManager.pull();
                break;
            case 1:
                // Push code
                List<String> filenames = list.getFilenames();
                VerticalOptionPane pane1 = new VerticalOptionPane(filenames);
                int fileNum = pane1.show();
                if (fileNum == -1) {
                    System.out.println("Push cancelled");
                    break;
                }

                String filename = filenames.get(fileNum);
                boolean confirm2 = confirmWindow("Are you sure you want to push " + filename + "?");
                if (!confirm2) {
                    System.out.println("Push cancelled");
                    break;
                }

                FileManager.push(fileNum);
                break;
            case 2:
                // Backup code
                String postfix = JOptionPane.showInputDialog(null, "Enter a postfix", title, JOptionPane.PLAIN_MESSAGE);
                FileManager.backup(postfix);
                break;
            case 3:
                // Restore code
                File[] backups = FileManager.listBackups();
                List<String> backupNames = Arrays.stream(backups).map(File::getName).collect(Collectors.toList());
                VerticalOptionPane pane2 = new VerticalOptionPane(backupNames);
                int backupNum = pane2.show();
                if (backupNum == -1) {
                    System.out.println("Restore cancelled");
                    break;
                }
                String backupPath = backups[backupNum].getPath();
                FileManager.restore(backupPath);

                break;
            default:
                // Close
                return;
        }

        mainWindow();
    }

    private boolean confirmWindow(String message) {
        return JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}

class VerticalOptionPane {
    private List<String> options;
    private int result;
    private CountDownLatch latch;
    public VerticalOptionPane(List<String> options) {
        this.options = options;
        this.result = -1;
        latch = new CountDownLatch(1);
    }

    public int show() throws InterruptedException {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(options.size() + 1, 1));
        panel.add(new JLabel("Choose one:"));
        for (int i = 0; i < options.size(); i++) {
            JButton button = new JButton(options.get(i));
            button.addActionListener(new ButtonListener(this, i));
            panel.add(button);
        }

        JDialog dialog = new JDialog();
        dialog.setTitle(UI.title);
        dialog.setModal(true);

        JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{});

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                latch.countDown();
            }
        });

        dialog.setContentPane(optionPane);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setModal(false);
        dialog.setVisible(true);

        latch.await();
        dialog.dispose();

        return result;
    }

    public void receiveResult(int choice) {
        result = choice;
        latch.countDown();
    }
}

class ButtonListener implements ActionListener {
    private int choice;
    private VerticalOptionPane parent;

    public ButtonListener(VerticalOptionPane parent, int choice) {
        this.choice = choice;
        this.parent = parent;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        parent.receiveResult(choice);
    }
}
