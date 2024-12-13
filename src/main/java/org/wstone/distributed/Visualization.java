package org.wstone.distributed;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Visualization extends JPanel {

    // Define the grid dimensions
    private final int height;
    private final int width;
    private final int cellSize = 20;
    private ConcurrentHashMap<String, Double> tempMap;
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
                g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
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


    public int getHeight(){ return height;}
    public int getWidth(){return width;}

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

