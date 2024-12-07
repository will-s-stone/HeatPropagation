package org.wstone.archive.working;

import java.util.Random;

public class HeatTransferSimulation {

    public static void main(String[] args) {
        // Parameters
        int width = 40; // Four times as wide as high
        int height = 10;
        double C1 = 0.75, C2 = 1.0, C3 = 1.25;
        double S = 100.0, T = 200.0;
        double threshold = 0.01; // Convergence threshold
        int maxIterations = 100000000;

        // Initialize alloy grid
        double[][] temperatures = new double[width][height];
        double[][][] metalPercentages = generateMetalPercentages(width, height);

        // Set initial temperatures
        temperatures[0][0] = S;
        temperatures[width - 1][height - 1] = T;

        // Simulate heat propagation
        simulateHeatTransfer(temperatures, metalPercentages, C1, C2, C3, S, T, threshold, maxIterations);

        // Print final temperatures
        printGrid(temperatures);
    }

    private static void simulateHeatTransfer(double[][] temperatures, double[][][] metalPercentages,
                                             double C1, double C2, double C3,
                                             double S, double T,
                                             double threshold, int maxIterations) {
        int width = temperatures.length;
        int height = temperatures[0].length;
        double[][] newTemperatures = new double[width][height];
        Random random = new Random();

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            boolean converged = true;

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if ((i == 0 && j == 0) || (i == width - 1 && j == height - 1)) {
                        // Apply heat variation to corners
                        newTemperatures[i][j] = (i == 0 && j == 0)
                                ? S + random.nextGaussian()
                                : T + random.nextGaussian();
                        continue;
                    }

                    double sum = 0.0;
                    int neighborCount = 0;

                    // Get neighbors and calculate weighted sum
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            if ((di == 0 && dj == 0) || !isValidNeighbor(i + di, j + dj, width, height))
                                continue;

                            double neighborTemp = temperatures[i + di][j + dj];
                            double weightedSum = 0.0;

                            // Sum contributions of each metal
                            weightedSum += C1 * neighborTemp * metalPercentages[i + di][j + dj][0];
                            weightedSum += C2 * neighborTemp * metalPercentages[i + di][j + dj][1];
                            weightedSum += C3 * neighborTemp * metalPercentages[i + di][j + dj][2];

                            sum += weightedSum;
                            neighborCount++;
                        }
                    }

                    newTemperatures[i][j] = sum / neighborCount;

                    // Check for convergence
                    if (Math.abs(newTemperatures[i][j] - temperatures[i][j]) > threshold) {
                        converged = false;
                    }
                }
            }

            // Update temperatures
            for (int i = 0; i < width; i++) {
                System.arraycopy(newTemperatures[i], 0, temperatures[i], 0, height);
            }

            if (converged) {
                System.out.println("Converged after " + iteration + " iterations.");
                return;
            }
        }

        System.out.println("Reached maximum iterations without convergence.");
    }

    private static boolean isValidNeighbor(int i, int j, int width, int height) {
        return i >= 0 && i < width && j >= 0 && j < height;
    }

    private static double[][][] generateMetalPercentages(int width, int height) {
        Random random = new Random();
        double[][][] percentages = new double[width][height][3];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double p1 = 0.4 + 0.2 * random.nextDouble();
                double p2 = 0.4 + 0.2 * random.nextDouble();
                double p3 = 1.0 - p1 - p2;

                percentages[i][j][0] = p1;
                percentages[i][j][1] = p2;
                percentages[i][j][2] = p3;
            }
        }

        return percentages;
    }

    private static void printGrid(double[][] grid) {
        for (double[] row : grid) {
            for (double value : row) {
                System.out.printf("%.2f ", value);
            }
            System.out.println();
        }
    }
}

