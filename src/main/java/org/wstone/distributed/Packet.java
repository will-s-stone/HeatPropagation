package org.wstone.distributed;

import java.io.Serializable;

public class Packet implements Serializable {
    private int width;
    private int height;
    private Region[][] grid;
    private int startRow;
    private int endRow;

    public Packet(int width, int height, Region[][] grid, int startRow, int endRow){
        this.width = width;
        this.height = height;
        this.grid = grid;
        this.startRow = startRow;
        this.endRow = endRow;
    }

    protected Region[][] computeHeatTransfer(){
        for (int y = startRow; y < endRow; y++) {
            for (int x = 0; x < width; x++) {
                Region curRegion = grid[y][x];
                if((x==0 && y==0) || (x==width-1 && y==height-1)){
                    continue;
                }
                double totalChange = 0.0;
                int neighborCount = 0;

                for (Simulation.Alloy.Region neighbor : curRegion.neighbors){
                    // alternative: collect all changes without locking
                    double tempDifference = neighbor.temperature - curRegion.temperature;
                    double heatTransfer = tempDifference * curRegion.thermalCoefficient;
                    //totalChange += heatTransfer;
                    totalChange += heatTransfer;
                    neighborCount++;

                }
                if (neighborCount > 0){
                    double change = totalChange / neighborCount;
                    double t = curRegion.temperature;
                    curRegion.setTemperature(t+change);
                }
            }
        }
        return grid;
    }
}
