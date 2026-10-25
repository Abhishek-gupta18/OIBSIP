package com.railreserve.dao;

import com.railreserve.config.DatabaseConfig;
import com.railreserve.model.Train;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC data access for the trains master table. All SQL uses PreparedStatement. */
public class TrainDAO {

    /** Look up a train's name by its numeric train number. */
    public Optional<Train> findByNumber(int trainNumber) throws SQLException {
        String sql = "SELECT train_number, train_name FROM trains WHERE train_number = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, trainNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Train(rs.getInt("train_number"), rs.getString("train_name")));
                }
                return Optional.empty();
            }
        }
    }

    /** All trains, ordered by number — used to populate the booking combo box. */
    public List<Train> findAll() throws SQLException {
        String sql = "SELECT train_number, train_name FROM trains ORDER BY train_number";
        List<Train> trains = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                trains.add(new Train(rs.getInt("train_number"), rs.getString("train_name")));
            }
        }
        return trains;
    }
}
