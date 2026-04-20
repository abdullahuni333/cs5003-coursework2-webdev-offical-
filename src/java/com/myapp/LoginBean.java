package com.myapp;

import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

// stuff for databases
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Named("loginBean")
@SessionScoped // keeps user logged in across pages

public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L; // required for session scoped bean

    // instance variables that store login details
    private String email;
    private String password;
    private String username;
    private int userId;
    private boolean loggedIn = false;

    // allow the server to inject the DataSource
    @Resource(lookup = "jdbc/ComputerWebsiteDB")
    DataSource dataSource;

    // check login details against USERS table
    public String login() throws SQLException {

        if (dataSource == null) {
            throw new SQLException("Unable to obtain DataSource"); // check datasource exists
        }

        Connection connection = dataSource.getConnection();

        if (connection == null) {
            throw new SQLException("Unable to connect to DataSource"); // check connection works
        }

        try {
            // create PreparedStatement to find matching user
            PreparedStatement checkUser = connection.prepareStatement(
                    "SELECT USERID, USERNAME FROM APP.USERS "
                    + "WHERE CAST(EMAIL AS VARCHAR(100)) = ? "
                    + "AND CAST(PASSWORD AS VARCHAR(100)) = ?");

            checkUser.setString(1, getEmail()); // set email from login form
            checkUser.setString(2, getPassword()); // set password from login form

            java.sql.ResultSet results = checkUser.executeQuery(); // call checkUser query

            if (results.next()) {
                userId = results.getInt("USERID");
                username = results.getString("USERNAME"); // get username from database
                loggedIn = true; // update login status

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Login successful", null));

                return "/index.xhtml?faces-redirect=true"; // redirect to home page
            } else {
                loggedIn = false; // login failed

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Invalid email or password", null));

                return null; // stay on login page
            }
        } finally {
            connection.close(); // return connection to pool
        }
    }

// logout user and clear session values
    public String logout() {

        FacesContext.getCurrentInstance().getExternalContext().invalidateSession(); // clear session values so saved builds does not linger when loggin out and logging into another account

        email = null; // clear stored email
        password = null; // clear stored password
        username = null; // clear stored username
        userId = 0;
        loggedIn = false; // reset login status

        return "/index.xhtml?faces-redirect=true"; // go back to home page  
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email; // sets email from input field
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password; // sets password from input field
    }

    public String getUsername() {
        return username; // used to display logged in username
    }

    public boolean isLoggedIn() {
        return loggedIn; // used to check login state in pages
    }

    // delete current logged in account from USERS table
    public String deleteAccount() throws SQLException {

        if (dataSource == null) {
            throw new SQLException("Unable to obtain DataSource"); // check datasource exists
        }

        Connection connection = dataSource.getConnection();

        if (connection == null) {
            throw new SQLException("Unable to connect to DataSource"); // check connection works
        }

        try {
            PreparedStatement deleteAddress = connection.prepareStatement(
                    "DELETE FROM APP.ADDRESSES WHERE FKUSERID = ?");
            deleteAddress.setInt(1, userId);
            deleteAddress.executeUpdate();

            PreparedStatement deleteUser = connection.prepareStatement(
                    "DELETE FROM APP.USERS WHERE USERID = ?");
            deleteUser.setInt(1, userId);

            int result = deleteUser.executeUpdate(); // call deleteUser

            if (result > 0) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Account deleted", null));

                // clear session values after deletion
                email = null;
                password = null;
                username = null;
                userId = 0;
                loggedIn = false;

                return "/index.xhtml?faces-redirect=true"; // return to home page
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Account could not be deleted", null));

                return null; // stay on current page
            }

        } finally {
            connection.close(); // return connection to pool
        }
    }
}
