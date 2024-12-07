package org.wstone;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

public class Alloy {
    private Region[][] grid;
    public Alloy(int rows, int cols, double c1, double c2, double c3){
        grid = new Region[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Region(c1, c2, c3);
            }
        }
        // assign neighbors
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (i > 0) grid[i][j].addNeighbor(grid[i - 1][j]); // top
                if (i < rows - 1) grid[i][j].addNeighbor(grid[i + 1][j]); // bottom
                if (j > 0) grid[i][j].addNeighbor(grid[i][j - 1]); // left
                if (j < cols - 1) grid[i][j].addNeighbor(grid[i][j + 1]); // right
            }
        }
    }

    void setInitialTemperatures(double s, double t){
        grid[0][0].setTemperature(s); // top left
        grid[grid.length - 1][grid.length - 1].setTemperature(s); // bottom right
    }


    class Region{
        CopyOnWriteArrayList<Region> neighbors;
        double thermalCoefficient;
        private volatile double temperature;
        ReentrantLock lock;


        public Region(double c1, double c2, double c3){
            neighbors = new CopyOnWriteArrayList<>();
            lock = new ReentrantLock();
            thermalCoefficient = (c1*generateGaussianRandom(0.8, 1.2, 1.0, 0.1) +
                    c2*generateGaussianRandom(0.8, 1.2, 1.0, 0.1) +
                    c3*generateGaussianRandom(0.8, 1.2, 1.0, 0.1)) / 3;
        }
        // this is a little extra...
        // the percentages of each allow is now randomized according to a Gaussian :)
        static double generateGaussianRandom(double min, double max, double mean, double stdDev) {
            double value;
            do {value = mean + ThreadLocalRandom.current().nextGaussian() * stdDev;
            } while (value < min || value > max);
            return value;
        }
        double getTemperature(){
            return temperature;
        }
        void setTemperature(double temperature){
            this.temperature = temperature;
        }


        void addNeighbor(Region neighbor){
            this.neighbors.add(neighbor);
        }
        private void notifyNeighbors(){
            for(Region neighbor: neighbors){
                neighbor.neighborTempChange(this);
            }
        }
        void neighborTempChange(Region neighbor){
            updateTemperature();
        }
        private void updateTemperature(){
            double tempSum = 0;
            for(Region neighbor : neighbors){
                tempSum += neighbor.getTemperature();
            }
            double avgTemp = tempSum / neighbors.size();
            this.temperature = thermalCoefficient * avgTemp;

        }


    }
}
