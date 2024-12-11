package org.wstone.concurrent;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Visualization extends JPanel {

    // Define the grid dimensions
    private final int height;
    private final int width;
    private final int cellSize = 1;
    private ConcurrentHashMap<String, Double> tempMap;
    private final ReentrantLock lock = new ReentrantLock();
    private final double s, t;

    public Visualization(ConcurrentHashMap<String, Double> tempMap, int height, int width, double s, double t) {
        this.tempMap = tempMap;
        this.height = height;
        this.width = width;
        this.s = s;
        this.t = t;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int[] p = {x,y};
                Double color = tempMap.get(Arrays.toString(p));
                if (color == null) g.setColor(new Color(0, 0, 255));
                else{g.setColor(setColor(color));}
                g.fillRect(x, y, cellSize, cellSize);
                System.out.println("(0,0) is ... - > " + tempMap.get(Arrays.toString(new int[]{0, 0})) + "\n"+ "bottom right corner is ... - > " + tempMap.get(Arrays.toString(new int[]{width - 1, height - 1})) + "\n\n\n");

            }
        }
    }

    private Color setColor(double d) {
        double b = Math.min(s, t);
        d = Math.max(0, Math.min(b, d));

        int red = (int) (255 * (d / b));
        int blue = (int) (255 * (1 - d / b));

        return new Color(red, 0, blue);
    }

    Color randColor() {
        Random rand = new Random();
        int red = rand.nextInt(256); // 0 to 255
        int green = rand.nextInt(256);
        int blue = rand.nextInt(256);
        return new Color(red, green, blue);
    }

    public int getHeight(){ return height;}
    public int getWidth(){return width;}

    // will change this so I am referencing the same maps that the simulation is using by memory and the update logic would be to recompute the visualization.
    public void updateMap(ConcurrentHashMap<String, Double> tempMap) {
        lock.lock();
        try {
            this.tempMap = tempMap;
        } finally {
            lock.unlock();
        }
    }


    public static void main(String[] args) {
        // Create the JFrame
//        JFrame frame = new JFrame("Heat Simulation");
//        Visualization panel = new Visualization();
//
//        // Set up the frame
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(panel.width, panel.height);
//        frame.add(panel);
//        frame.setVisible(true);
//    }
    }
}

