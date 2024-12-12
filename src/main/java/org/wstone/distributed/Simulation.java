package org.wstone.distributed;

import javax.swing.*;
import java.util.Arrays;
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
        ConcurrentHashMap<String, Double> tempMap = new ConcurrentHashMap<>();
        Visualization vis;


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
            vis = new Visualization(this.tempMap, height, width, s, t);

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


        void simulateHeatTransfer(JFrame frame){
            frame.setSize((int) (vis.getWidth())+3, (int) (vis.getHeight()+30));
            frame.add(vis);
            frame.setVisible(true);

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
                if(iteration % 5 == 0 && iteration != 0){
                    writeToMap();
                    SwingUtilities.invokeLater(vis::repaint);
                }
            }
        }
        // map the cartesian coordinates to the value. An alternative to this would be to modify the logic in the grid[][]
        // and replace it with a concurrent hashmap, but this is due in two days, so...
        private void writeToMap(){
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int[] p = {x, y};
                    tempMap.put(Arrays.toString(p), grid[y][x].temperature);
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
        }

        public void shutdown() {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    //wait again to ensure shutdown
                    if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                        System.err.println("ExecutorService did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
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
                // 20% variation
                thermalCoefficient = (c1*generateUniformRandom(0.8, 1.2) + c2*generateUniformRandom(0.8, 1.2) + (c3*generateUniformRandom(0.8, 1.2))) / 3;
            }

            static double generateUniformRandom(double min, double max) {
                return min + (max - min) * ThreadLocalRandom.current().nextDouble();
            }

            void setTemperature(double temperature) {
                lock.lock();
                try {
                    this.temperature = temperature;
                }finally {
                    lock.unlock();
                }
            }
        }
    }
    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        Alloy alloy = simulation.new Alloy(100, 100, 1000.0, 800.0, 0.75, 1.0, 1.25, 60000000);

        JFrame frame = new JFrame("Heat Transfer Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        alloy.simulateHeatTransfer(frame);

    }
}
