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
    private String ID;
    private String name;
    private String email;
    private String password;
    private String country;
    private String city;
    private String street;

    @Resource(lookup = "jdbc/ComputerWebsiteDB")
    DataSource dataSource;

        public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID; // sets username from registration form
    }
    
    
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
   public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country; // sets country
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city; // sets city
    }
    
        public String getStreet() {
        return street;
    }
    public void setStreet(String street) {
        this.street = street; // sets street
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
            setID(newCount);
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
            PreparedStatement addUser = connection.prepareStatement(
                    "INSERT INTO USERS (USERID,USERNAME,PASSWORD,EMAIL) values (?,?,?,?)");

            addUser.setString(1, generateID()); // generates a new USERID
            addUser.setString(2, getName()); // stores user name
            addUser.setString(3, getPassword()); // stores password in PASSWORD column
            addUser.setString(4, getEmail()); // stores email in EMAIL column

            addUser.executeUpdate(); // executes insert query

            PreparedStatement registerAddress =  connection.prepareStatement("INSERT INTO ADDRESSES (ADDRESSKEY,FKUSERID,COUNTRY,CITY,STREET) values (?,?,?,?,?)");
            registerAddress.setString(1,getID());
            registerAddress.setString(2,getID());
            registerAddress.setString(3,getCountry());
            registerAddress.setString(4,getCity());
            registerAddress.setString(5,getStreet());
            
            registerAddress.executeUpdate();
            // clear values after registration (session scoped bean)
            name = null;
            email = null;
            password = null;
            ID = null;

            return "index"; // return to home page

        } finally {
            connection.close(); // return connection to pool
        }
    }
}
