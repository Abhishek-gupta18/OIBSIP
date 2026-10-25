package com.railreserve.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One booked (or cancelled) ticket. The PNR is the 10-char primary key. */
public class Reservation {

    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final String pnr;
    private final int userId;
    private final String passengerName;
    private final int trainNumber;
    private final String trainName;
    private final String classType;
    private final LocalDate journeyDate;
    private final String sourceStation;
    private final String destinationStation;
    private final String status;
    private final LocalDateTime createdAt;

    public Reservation(String pnr, int userId, String passengerName, int trainNumber,
                       String trainName, String classType, LocalDate journeyDate,
                       String sourceStation, String destinationStation,
                       String status, LocalDateTime createdAt) {
        this.pnr = pnr;
        this.userId = userId;
        this.passengerName = passengerName;
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.classType = classType;
        this.journeyDate = journeyDate;
        this.sourceStation = sourceStation;
        this.destinationStation = destinationStation;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getPnr() {
        return pnr;
    }

    public int getUserId() {
        return userId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public int getTrainNumber() {
        return trainNumber;
    }

    public String getTrainName() {
        return trainName;
    }

    public String getClassType() {
        return classType;
    }

    public LocalDate getJourneyDate() {
        return journeyDate;
    }

    public String getSourceStation() {
        return sourceStation;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(status);
    }

    /** Human-readable block used by confirmation dialogs and PNR display. */
    public String toDisplayString() {
        return String.format(
                "PNR            : %s%n"
                        + "Passenger      : %s%n"
                        + "Train          : %d — %s%n"
                        + "Class          : %s%n"
                        + "Journey Date   : %s%n"
                        + "From           : %s%n"
                        + "To             : %s%n"
                        + "Status         : %s",
                pnr, passengerName, trainNumber, trainName, classType,
                journeyDate, sourceStation, destinationStation, status);
    }
}
