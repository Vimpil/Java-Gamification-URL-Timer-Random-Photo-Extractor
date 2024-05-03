import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RandomPhotoTimerApp {

    private JFrame frame;
    private JTextField websiteInput;
    private JComboBox<String> timerLimit;
    private JLabel photoLabel, timerLabel;
    private JList<String> photoList;
    private DefaultListModel<String> photoListModel;
    private Timer timer;
    private int timeRemaining;
    private boolean timerRunning = false;
    private int selectedIndex = -1;

    public void createAndShowGUI() {
        frame = new JFrame("Random Photo Timer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        addComponents();
        frame.setVisible(true);
    }

    private void addComponents() {
        JPanel topPanel = new JPanel(new FlowLayout());
        websiteInput = new JTextField(20);
        timerLimit = new JComboBox<>(new String[]{"5 minutes", "10 minutes", "15 minutes", "20 minutes", "30 minutes"});
        JButton startStopButton = new JButton("Start Timer");
        JButton pauseResumeButton = new JButton("Pause Timer");
        JButton endButton = new JButton("End Timer");
        JButton setListButton = new JButton("Set List");
        JButton saveButton = new JButton("Save");
        JButton loadButton = new JButton("Load");

        startStopButton.addActionListener(e -> toggleTimer());
        pauseResumeButton.addActionListener(e -> pauseResumeTimer());
        endButton.addActionListener(e -> stopTimer());
        setListButton.addActionListener(e -> fetchAndDisplayPhotos());
        saveButton.addActionListener(e -> saveState());
        loadButton.addActionListener(e -> loadState());

        topPanel.add(websiteInput);
        topPanel.add(timerLimit);
        topPanel.add(startStopButton);
        topPanel.add(pauseResumeButton);
        topPanel.add(endButton);
        topPanel.add(setListButton);
        topPanel.add(saveButton);
        topPanel.add(loadButton);

        photoLabel = new JLabel("HELLO!");
        topPanel.add(photoLabel);

        timerLabel = new JLabel("Time Remaining: ");
        frame.add(timerLabel, BorderLayout.NORTH);

        photoListModel = new DefaultListModel<>();
        photoList = new JList<>(photoListModel);
        JScrollPane scrollPane = new JScrollPane(photoList);
        frame.add(scrollPane, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.CENTER);
    }

    private void toggleTimer() {
        if (timerRunning) {
            timer.stop();
            timerRunning = false;
        } else {
            int selectedTime = Integer.parseInt((String) timerLimit.getSelectedItem().toString().split(" ")[0]) * 60;
            timeRemaining = selectedTime;
            timer = new Timer(1000, e -> updateTimer());
            timer.start();
            timerRunning = true;
        }
    }

    private void updateTimer() {
        if (timeRemaining > 0) {
            timeRemaining--;
            timerLabel.setText("Time Remaining: " + formatTime(timeRemaining));
        } else {
            timer.stop();
            timerRunning = false;
            displayNextPhoto();
            JOptionPane.showMessageDialog(null, "Time's up!");
        }
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + " minutes " + seconds + " seconds";
    }

    private void pauseResumeTimer() {
        if (timerRunning) {
            timer.stop();
            timerRunning = false;
        } else {
            timer.start();
            timerRunning = true;
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timerRunning = false;
            displayNextPhoto();
        }
        timerLabel.setText("");
    }

    private void fetchAndDisplayPhotos() {
        String websiteURL = websiteInput.getText();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(websiteURL).openStream()))) {
            String line;
            StringBuilder htmlContent = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                htmlContent.append(line);
            }
            List<String> imageUrls = extractImageUrls(htmlContent.toString());
            photoListModel.clear();
            imageUrls.forEach(photoListModel::addElement);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error fetching photos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String currentImageUrl; // Variable to hold the URL of the current image

    private void saveState() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("state.ser"))) {
            // Write necessary variables to the file
            outputStream.writeObject(selectedIndex);
            outputStream.writeObject(timeRemaining);
            outputStream.writeObject(timerRunning);
            outputStream.writeObject(photoListModel);
            outputStream.writeObject(currentImageUrl); // Save current image URL
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadState() {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("state.ser"))) {
            // Read variables from the file
            selectedIndex = (int) inputStream.readObject();
            timeRemaining = (int) inputStream.readObject();
            timerRunning = (boolean) inputStream.readObject();
            photoListModel = (DefaultListModel<String>) inputStream.readObject();
            photoList.setModel(photoListModel);
            currentImageUrl = (String) inputStream.readObject(); // Load current image URL
            loadImage(currentImageUrl); // Set the loaded image to photoLabel
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private List<String> extractImageUrls(String html) {
        List<String> imageUrls = new ArrayList<>();
        String regex = "<img.*?src=['\"](.*?)['\"].*?>";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            imageUrls.add(matcher.group(1));
        }
        return imageUrls;
    }

    private void displayNextPhoto() {
        if (selectedIndex >= photoListModel.getSize() - 1) {
            JOptionPane.showMessageDialog(null, "End of photo list.");
            return;
        }
        selectedIndex++;
        photoList.setSelectedIndex(selectedIndex);
        photoList.ensureIndexIsVisible(selectedIndex);
        loadImage(photoListModel.getElementAt(selectedIndex));
    }

    private void loadImage(String imageUrl) {
        try {
            BufferedImage image = ImageIO.read(new URL(imageUrl));
            ImageIcon icon = new ImageIcon(image);
            photoLabel.setIcon(icon);
            currentImageUrl = imageUrl; // Update current image URL
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to load image: " + e.getMessage(), "Image Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
