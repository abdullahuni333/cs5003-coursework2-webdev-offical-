package com.myapp;

import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

// stuff for databases
import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.resource.cci.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;



@DataSourceDefinition(
        name = "jdbc:derby://localhost:1527/ComputerWebsiteDB",
        className= "org.apache.derby.jdbc.ClientDataSource",
        url = "jdbc:derby://localhost:1527/ComputerWebsiteDB",
        databaseName = "ComputerWebsiteDB",
        user = "APP",
        password = "APP")


@Named("loginBean")
@SessionScoped
public class LoginBean implements Serializable {
    
    @Resource(lookup="jdbc/ComputerWebsiteDB")
    DataSource dataSource;

    private String email;
    private String password;
    private boolean loggedIn = false;

    public String login() {
        if ("admin@email.com".equals(email) && "1234".equals(password)) {
            loggedIn = true;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Login successful", null));
            return "/index.xhtml?faces-redirect=true";
        } else {
            loggedIn = false;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid email or password", null));
            return null;
        }
    }

    public String logout() {
        email = null;
        password = null;
        loggedIn = false;
        return "/index.xhtml?faces-redirect=true";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
    
      
    
    
    public ResultSet getDetails() throws SQLException{

     if (dataSource == null)
    {
       throw new SQLException("no datasource");
    }
    Connection connection = dataSource.getConnection();
    //check connection
    if(connection == null)
     throw new SQLException("cant connect to datasource");
    try 
    {
        
        PreparedStatement getDetails = connection.prepareStatement("SELECT USERNAME, PASSWORD, EMAIL FROM USERS");   
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
         rowSet.populate( getDetails.executeQuery() );
        return (ResultSet) rowSet; 
            
        }
    finally{
    connection.close();
    }
    }
    
    
    
}