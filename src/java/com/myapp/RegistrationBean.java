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
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;

        
@DataSourceDefinition(
        name = "jdbc:derby://localhost:1527/ComputerWebsiteDB",
        className= "org.apache.derby.jdbc.ClientDataSource",
        url = "jdbc:derby://localhost:1527/ComputerWebsiteDB",
        databaseName = "ComputerWebsiteDB",
        user = "APP",
        password = "APP")


@Named("registrationBean")
@jakarta.faces.view.ViewScoped
//@SessionScoped - commented out because viewscop is used for signups 
public class RegistrationBean implements Serializable {

    private String name;
    private String email;
    private String password;
    private String id;
    int tempID = 0;
    
    
    @Resource(lookup="jdbc/ComputerWebsiteDB")
    DataSource dataSource;
    
    public String register() {
        // TEMPORARY (no database yet)
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Account created for " + email, null));

        // after registration, go to login page
        return "/Login.xhtml?faces-redirect=true";
    }

    
        public String getID() {
        return id;
    }

    public String setID() {
        
         id = String.valueOf(tempID) ;
         tempID++;
         return id;
    }
    
    
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        
        PreparedStatement getDetails = connection.prepareStatement("select username from users");   
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
         rowSet.populate( getDetails.executeQuery() );
        return (ResultSet) rowSet; 
            
        }
    finally{
    connection.close();
    }
    }
    
    
   public String generateID() throws SQLException{
       int currentCount =0;
    if (dataSource == null)
    {
       throw new SQLException("no datasource");
    }
    Connection connection = dataSource.getConnection();
   try{
       PreparedStatement generateID = connection.prepareStatement("SELECT USERID FROM  USERS ORDER BY USERID DESC  FETCH FIRST 1 ROWS ONLY");
        java.sql.ResultSet count = generateID.executeQuery();
        if(count.next()){
        currentCount = count.getInt("USERID");
        }
        String newCount = String.valueOf(currentCount+1);
        return newCount;
   }
   finally{
   connection.close();
   }
   
   }
    
    
    
    public String gosubmit() throws SQLException{
    
     if (dataSource == null)
    
       throw new SQLException("no datasource");
    
    Connection connection = dataSource.getConnection();
    //check connection
    if(connection == null)
     throw new SQLException("cant connect to datasource");
    
    try{ 
    PreparedStatement addEntry = connection.prepareStatement("INSERT INTO USERS"+ "(USERID,USERNAME,PASSWORD,EMAIL)"+"values(?,?,?,?)");
    addEntry.setString(1, generateID());
    addEntry.setString(2, getName());
    addEntry.setString(3, getEmail());
    addEntry.setString(4, getPassword());
    addEntry.executeUpdate();
    return "index";
    }
    finally{
        connection.close();
        
    }
}
}