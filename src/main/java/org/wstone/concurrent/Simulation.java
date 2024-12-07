package org.wstone.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Simulation {



    class Alloy{
        Region[][] grid;
        private final int width;
        private final int height;
        private final ExecutorService executorService;
        private final int iterations;
        private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();


        public Alloy(int height, int width, double s, double t, double c1, double c2, double c3, int iterations){
            this.width = width;
            this.height = height;
            this.grid = new Region[height][width];
            this.iterations = iterations;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    grid[y][x] = new Region(c1, c2, c3);
                }
            }
            setUpNeighbors();
            this.executorService = Executors.newFixedThreadPool(NUM_THREADS);

            initializeHeatSources(s, t);
        }

        private void setUpNeighbors() {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Region curRegion = grid[y][x];

                    int[][] surroundings = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};

                    for (int[] s : surroundings) {
                        int nX = x + s[1];
                        int nY = y + s[0];
                        if(nX >= 0 && nX < width && nY >=0 && nY < height){
                            curRegion.neighbors.add(grid[nY][nX]);
                        }
                    }
                }
            }
        }

        void initializeHeatSources(double s, double t){
            grid[0][0].setTemperature(s);
            grid[height - 1][width -1].setTemperature(t);
        }


        void simulateHeatTransfer(){
            for (int iteration = 0; iteration < iterations; iteration++) {
                CountDownLatch latch = new CountDownLatch(NUM_THREADS);
                int rowsPerThread = height/NUM_THREADS;
                for (int threadIndex = 0; threadIndex < NUM_THREADS; threadIndex++) {
                    final int startRow = threadIndex * rowsPerThread;
                    final int endRow = (threadIndex == NUM_THREADS - 1) ? height : startRow + rowsPerThread;
                    executorService.submit(() -> {
                        try{
                            computeHeatTransfer(startRow, endRow);
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                try {
                    latch.await();
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void computeHeatTransfer(int startRow, int endRow){
            for (int y = startRow; y < endRow; y++) {
                for (int x = 0; x < width; x++) {
                    Region curRegion = grid[y][x];
                    if((x==0 && y==0) || (x==width-1 && y==height-1)){
                        continue;
                    }
                    double totalChange = 0.0;
                    int neighborCount = 0;
                    for (Region neighbor : curRegion.neighbors){
                        // lock the regions
                        curRegion.lock.lock();
                        neighbor.lock.lock();
                        try {
                            double tempDifference = neighbor.temperature - curRegion.temperature;
                            double heatTransfer = tempDifference * curRegion.thermalCoefficient;

                            totalChange += heatTransfer;
                            neighborCount++;
                        } finally {
                            neighbor.lock.unlock();
                            curRegion.lock.unlock();
                        }
                    }
                    if (neighborCount > 0){
                        curRegion.lock.lock();
                        try {
                            curRegion.temperature += totalChange/neighborCount;
                        }finally {
                            curRegion.lock.unlock();
                        }
                    }
                }

            }
        }
        public void printFinalTemperatureDistribution() {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    System.out.printf("%.2f ", grid[y][x].temperature);
                }
                System.out.println();
            }
        }
        public void shutdown() {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }


        class Region{
            CopyOnWriteArrayList<Region> neighbors;
            double thermalCoefficient;
            private volatile double temperature;
            ReentrantLock lock;

            // we assume any region is made of 3 alloys with 3 different thermal coefficients with a 20% variance in the amount each alloy.
            // in this context we can interpret that the variance scales linearly to the thermal coefficient
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

            void setTemperature(double temperature) {
                this.temperature = temperature;
            }
        }
    }
    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        Simulation.Alloy alloy = simulation.new Alloy(400, 100, 1000.0, 800.0, 0.75, 1.0, 1.25, 10);

        alloy.simulateHeatTransfer();
        alloy.printFinalTemperatureDistribution();
        alloy.shutdown();
    }
}
