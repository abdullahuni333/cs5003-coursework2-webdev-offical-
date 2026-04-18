package com.myapp;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.io.Serializable;

@Named("buildBean")
@SessionScoped
public class BuildBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cpu;
    private String gpu;
    private String ram;

    @Resource(lookup = "jdbc/ComputerWebsiteDB") // connects to database
    DataSource dataSource;

    public double getTotalPrice() {
        double total = 0;

        if ("Ryzen 5 5600X".equals(cpu)) {
            total += 180;
        }
        if ("Core i5-12400F".equals(cpu)) {
            total += 170;
        }

        if ("RTX 4060".equals(gpu)) {
            total += 290;
        }
        if ("RX 7600".equals(gpu)) {
            total += 260;
        }

        if ("16GB DDR4".equals(ram)) {
            total += 45;
        }
        if ("32GB DDR4".equals(ram)) {
            total += 80;
        }

        return total;
    }

    // saves current build to SAVEDBUILDS table
    public String saveBuild(String userEmail) throws SQLException {

        if (dataSource == null) { // check datasource exists
            throw new SQLException("Unable to obtain DataSource");
        }

        Connection connection = dataSource.getConnection(); // open DB connection

        if (connection == null) { // check connection worked
            throw new SQLException("Unable to connect to DataSource");
        }

        try {
            PreparedStatement addBuild = connection.prepareStatement(
                    "INSERT INTO APP.SAVEDBUILDS (BUILDID, EMAIL, CPU, GPU, RAM, TOTALPRICE) VALUES (?, ?, ?, ?, ?, ?)"
            );

            addBuild.setInt(1, generateBuildID()); // unique build id
            addBuild.setString(2, userEmail); // logged in user email
            addBuild.setString(3, getCpu()); // selected cpu
            addBuild.setString(4, getGpu()); // selected gpu
            addBuild.setString(5, getRam()); // selected ram
            addBuild.setDouble(6, getTotalPrice()); // calculated total price

            addBuild.executeUpdate(); // run insert query

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Build saved successfully", null));

            return "/Savedbuilds.xhtml?faces-redirect=true";

        } finally {
            connection.close(); // return connection to pool
        }
    }

    // gets next build id from database
    public int generateBuildID() throws SQLException {
        int currentCount = 0;

        if (dataSource == null) {
            throw new SQLException("Unable to obtain DataSource");
        }

        Connection connection = dataSource.getConnection();

        try {
            PreparedStatement generateID = connection.prepareStatement(
                    "SELECT BUILDID FROM APP.SAVEDBUILDS ORDER BY BUILDID DESC FETCH FIRST 1 ROWS ONLY"
            );

            ResultSet rs = generateID.executeQuery();

            if (rs.next()) {
                currentCount = rs.getInt("BUILDID");
            }

            return currentCount + 1;

        } finally {
            connection.close();
        }
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu; // sets selected cpu from build form
    }

    public String getGpu() {
        return gpu;
    }

    public void setGpu(String gpu) {
        this.gpu = gpu; // sets selected gpu from build form
    }

    public String getRam() {
        return ram;
    }

    public void setRam(String ram) {
        this.ram = ram; // sets selected ram from build form
    }
}
