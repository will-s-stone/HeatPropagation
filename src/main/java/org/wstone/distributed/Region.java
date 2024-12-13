package org.wstone.distributed;

import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

public class Region implements Serializable{
        CopyOnWriteArrayList<Simulation.Alloy.Region> neighbors;
        double thermalCoefficient;
        protected volatile double temperature;
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

