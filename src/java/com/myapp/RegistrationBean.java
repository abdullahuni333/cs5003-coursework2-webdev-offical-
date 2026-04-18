package com.myapp;

import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

// stuff for databases
import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@DataSourceDefinition(
        name = "jdbc:derby://localhost:1527/ComputerWebsiteDB",
        className = "org.apache.derby.jdbc.ClientDataSource",
        url = "jdbc:derby://localhost:1527/ComputerWebsiteDB",
        databaseName = "ComputerWebsiteDB",
        user = "APP",
        password = "APP")

@Named("registrationBean")
@SessionScoped 
public class RegistrationBean implements Serializable {

    private String name;
    private String email;
    private String password;

    @Resource(lookup = "jdbc/ComputerWebsiteDB")
    DataSource dataSource;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name; // sets username from registration form
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email; // sets email from registration form
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password; // sets password from registration form
    }

    public String generateID() throws SQLException {
        int currentCount = 0;

        if (dataSource == null) {
            throw new SQLException("no datasource"); // check datasource exists
        }

        Connection connection = dataSource.getConnection();

        try {
            // gets the highest current USERID from USERS table
            PreparedStatement generateID = connection.prepareStatement(
                    "SELECT USERID FROM USERS ORDER BY USERID DESC FETCH FIRST 1 ROWS ONLY");

            java.sql.ResultSet count = generateID.executeQuery();

            if (count.next()) {
                currentCount = count.getInt("USERID"); // gets current highest id
            }

            String newCount = String.valueOf(currentCount + 1); // adds 1 for next id
            return newCount;

        } finally {
            connection.close(); // return connection to pool
        }
    }

    public String gosubmit() throws SQLException {

        if (dataSource == null) {
            throw new SQLException("no datasource"); // check datasource exists
        }

        Connection connection = dataSource.getConnection();

        if (connection == null) {
            throw new SQLException("cant connect to datasource");
        }

        try {
            // insert new user into USERS table
            PreparedStatement addEntry = connection.prepareStatement(
                    "INSERT INTO USERS (USERID,USERNAME,PASSWORD,EMAIL) values (?,?,?,?)");

            addEntry.setString(1, generateID()); // generates a new USERID
            addEntry.setString(2, getName()); // stores user name
            addEntry.setString(3, getPassword()); // stores password in PASSWORD column
            addEntry.setString(4, getEmail()); // stores email in EMAIL column

            addEntry.executeUpdate(); // executes insert query

            // clear values after registration (session scoped bean)
            name = null;
            email = null;
            password = null;

            return "index"; // return to home page

        } finally {
            connection.close(); // return connection to pool
        }
    }
}
