package com.railreserve.model;

/** Train master entry. Train numbers are numeric and unique. */
public class Train {

    private final int trainNumber;
    private final String trainName;

    public Train(int trainNumber, String trainName) {
        this.trainNumber = trainNumber;
        this.trainName = trainName;
    }

    public int getTrainNumber() {
        return trainNumber;
    }

    public String getTrainName() {
        return trainName;
    }

    @Override
    public String toString() {
        return trainNumber + " — " + trainName;
    }
}
