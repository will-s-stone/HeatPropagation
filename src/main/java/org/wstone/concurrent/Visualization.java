package org.wstone.concurrent;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Random;

public class Visualization extends JPanel {

    // Define the grid dimensions
    private final int width = 2000;
    private final int height = 500;
    private final int cellSize = 1;
    private final Path CSV_FILE_PATH = Path.of("test.csv");

    @Override
    protected void paintComponent(Graphics g) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(CSV_FILE_PATH.toFile()));
            super.paintComponent(g);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    g.setColor(randColor());
                    g.fillRect(x, y, cellSize, cellSize);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    Color randColor() {
        Random rand = new Random();
        int red = rand.nextInt(256); // 0 to 255
        int green = rand.nextInt(256);
        int blue = rand.nextInt(256);
        return new Color(red, green, blue);
    }




    public static void main(String[] args) {
        // Create the JFrame
        JFrame frame = new JFrame("Heat Simulation");
        Visualization panel = new Visualization();

        // Set up the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(panel.width, panel.height);
        frame.add(panel);
        frame.setVisible(true);
    }
}

