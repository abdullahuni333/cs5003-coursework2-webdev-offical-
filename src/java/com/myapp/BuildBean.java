package com.myapp;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("buildBean")
@SessionScoped
public class BuildBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cpu;
    private String gpu;
    private String ram;
    private int cpuStock;
    private int gpuStock;
    private int ramStock;

    private List<SavedBuild> savedBuilds; // stores saved builds for current user

    @Resource(lookup = "jdbc/ComputerWebsiteDB") // connects to database
    DataSource dataSource;

    @Inject
    LoginBean loginBean; // gets current logged in user bean

    public double getTotalPrice() {
        return calculateTotal(cpu, gpu, ram); // calculates total for build page
    }

    // calculate total price for selected parts
    private double calculateTotal(String cpu, String gpu, String ram) {
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

    // save current build to SAVEDBUILDS table
    // saves current build to SAVEDBUILDS table
    public String saveBuild() {

        if (loginBean == null || loginBean.getEmail() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "You must be logged in to save a build", null));
            return null;
        }

        try {
            if (dataSource == null) {
                throw new SQLException("Unable to obtain DataSource");
            }

            Connection connection = dataSource.getConnection();

            try {
                PreparedStatement addBuild = connection.prepareStatement(
                        "INSERT INTO APP.SAVEDBUILDS (BUILDID, EMAIL, CPU, GPU, RAM, TOTALPRICE) VALUES (?, ?, ?, ?, ?, ?)"
                );

                addBuild.setInt(1, generateBuildID());
                addBuild.setString(2, loginBean.getEmail()); // gets email from injected loginBean
                addBuild.setString(3, getCpu());
                addBuild.setString(4, getGpu());
                addBuild.setString(5, getRam());
                addBuild.setDouble(6, getTotalPrice());

                addBuild.executeUpdate();
                savedBuilds = null; // refresh saved builds list

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Build saved successfully", null));

                return "/Savedbuilds.xhtml?faces-redirect=true";

            } finally {
                connection.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Failed to save build", null));
            return null;
        }
    }

    // get highest build id
    public int generateBuildID() throws SQLException {
        int currentCount = 0;

        if (dataSource == null) {
            throw new SQLException("Unable to obtain DataSource");
        }

        Connection connection = dataSource.getConnection();

        try {
            PreparedStatement generateID = connection.prepareStatement(
                    "SELECT MAX(BUILDID) FROM APP.SAVEDBUILDS"
            );

            ResultSet rs = generateID.executeQuery(); // execute generateID query

            if (rs.next()) {
                currentCount = rs.getInt(1); // gets max BUILDID
            }

            return currentCount + 1;

        } finally {
            connection.close();
        }
    }

    // load saved builds from database
    public void loadSavedBuilds() {
        savedBuilds = new ArrayList<>();

        // return empty list if user is not logged in
        if (loginBean == null || loginBean.getEmail() == null) {
            return;
        }

        if (dataSource == null) {
            return; // return empty list if no datasource
        }

        try {
            Connection connection = dataSource.getConnection();
            try {
                PreparedStatement getBuilds = connection.prepareStatement(
                        "SELECT BUILDID, EMAIL, CPU, GPU, RAM, TOTALPRICE "
                        + "FROM APP.SAVEDBUILDS WHERE EMAIL = ? ORDER BY BUILDID DESC"
                );

                getBuilds.setString(1, loginBean.getEmail()); // gets builds for logged in user

                ResultSet rs = getBuilds.executeQuery(); // execute getBuilds query

                while (rs.next()) {
                    SavedBuild build = new SavedBuild();
                    build.setBuildId(rs.getInt("BUILDID"));
                    build.setEmail(rs.getString("EMAIL"));
                    build.setCpu(rs.getString("CPU"));
                    build.setGpu(rs.getString("GPU"));
                    build.setRam(rs.getString("RAM"));
                    build.setTotalPrice(rs.getDouble("TOTALPRICE"));
                    build.setEditing(false); // row starts in view mode
                    savedBuilds.add(build); // add each build to list
                }
            } finally {
                connection.close(); // return connection to pool
            }
        } catch (SQLException e) {
            e.printStackTrace(); // log the error
        }
    }

    // gets saved builds for current logged in user
    public List<SavedBuild> getSavedBuilds() {
        if (savedBuilds == null) {
            loadSavedBuilds(); // load builds first time
        }
        return savedBuilds;
    }

    // turn selected row into edit mode
    public String editBuild(SavedBuild build) {
        build.setEditing(true); // show editable fields
        return null; // stay on same page
    }

    // cancel edit and reload original values
    public String cancelEdit() {
        loadSavedBuilds(); // load builds from database
        return null; // stay on same page
    }

    // update the build in SAVEDBUILDS databse table
    public String updateBuild(SavedBuild build) {

        try {
            if (dataSource == null) {
                throw new SQLException("Unable to obtain DataSource");
            }

            Connection connection = dataSource.getConnection();

            try {
                double newTotal = calculateTotal(build.getCpu(), build.getGpu(), build.getRam()); // recalculate total

                PreparedStatement updateBuild = connection.prepareStatement(
                        "UPDATE APP.SAVEDBUILDS SET CPU = ?, GPU = ?, RAM = ?, TOTALPRICE = ? WHERE BUILDID = ?"
                );

                updateBuild.setString(1, build.getCpu()); // updated cpu
                updateBuild.setString(2, build.getGpu()); // updated gpu
                updateBuild.setString(3, build.getRam()); // updated ram
                updateBuild.setDouble(4, newTotal); // updated total
                updateBuild.setInt(5, build.getBuildId()); // update selected build

                updateBuild.executeUpdate(); // execute updateBuild query

                build.setTotalPrice(newTotal); // update displayed total
                build.setEditing(false); // return row to view mode

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Build updated successfully", null));

            } finally {
                connection.close(); // return connection to pool
            }

        } catch (SQLException e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Failed to update build", null));
        }

        return null; // stay on same page
    }

    
    
    
        public String findStock(SavedBuild build) throws SQLException {


        if (dataSource == null) {
            throw new SQLException("no datasource"); // check datasource exists
        }

        Connection connection = dataSource.getConnection();

        try {
            
            PreparedStatement findStock = connection.prepareStatement(
                    "SELECT PRODUCTNAME, QAUNTITY FROM PRODUCT WHERE PRODUCTNAME IN(?,?,?)"
                    , java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, java.sql.ResultSet.CONCUR_READ_ONLY);

                    System.out.println(build.getCpu());
                    System.out.println(build.getGpu());
                    System.out.println(build.getRam());
                    findStock.setString(1, build.getCpu());
                    findStock.setString(2, build.getGpu());
                    findStock.setString(3, build.getRam());
            java.sql.ResultSet stockCount = findStock.executeQuery();
                stockCount.beforeFirst();
            if (stockCount.next()) {
                setCpuStock(stockCount.getInt("QAUNTITY")); // sets the current quantity
                }

            if (stockCount.next()) {
                setGpuStock(stockCount.getInt("QAUNTITY")); // sets the current quantity
                }

            if (stockCount.next()) {
                setRamStock(stockCount.getInt("QAUNTITY")); // sets the current quantity
                }
            System.out.println(String.valueOf("grafdsafs"+getCpuStock()));
            System.out.println(String.valueOf(getGpuStock()));
            System.out.println(String.valueOf(getRamStock()));
            
            
            
            // decreasing stock 

            
            if(getGpuStock()>0 && getCpuStock()>0 && getRamStock()>0){
            
            System.out.println("WORKING");
            // cpu
            PreparedStatement updateCpuStock = connection.prepareStatement("UPDATE PRODUCT SET QAUNTITY = QAUNTITY - 1 WHERE PRODUCTNAME LIKE ?");//used ai for this 
            updateCpuStock.setString(1, "%" + build.getCpu().trim() + "%");//used ai for this 
            updateCpuStock.executeUpdate(); 
            
            //gpu
            
            PreparedStatement updateGpuStock = connection.prepareStatement("UPDATE PRODUCT SET QAUNTITY = QAUNTITY - 1 WHERE PRODUCTNAME = ?");
            updateGpuStock.setString(1, build.getGpu());
            updateGpuStock.executeUpdate();
            
            
            //ram
            
            PreparedStatement updateRamStock = connection.prepareStatement("UPDATE PRODUCT SET QAUNTITY = QAUNTITY - 1 WHERE PRODUCTNAME = ?");
            updateRamStock.setString(1, build.getRam());
            updateRamStock.executeUpdate();  
            
            recordTransaction(build);
            }
            else{
            System.out.println("ERRROR MISSING STOCK");
            
            
            }
            

            
            
            
            
            return "worked";

        } finally {
            connection.close(); // return connection to pool
        }      
    }
        public String recordTransaction(SavedBuild build) throws SQLException {
            var userID = 0;
            var orderID =0;
            var cpuProductId =0;
            var gpuProductId =0;
            var ramProductId =0;
        if (dataSource == null) {
            throw new SQLException("no datasource"); // check datasource exists
        }

        Connection connection = dataSource.getConnection();

        try {
            
            
            //gets an order id to use later 
            PreparedStatement generateTransactionID = connection.prepareStatement(
                    "SELECT ORDERID FROM TRANSACTIONS ORDER BY ORDERID DESC FETCH FIRST 1 ROWS ONLY");

            java.sql.ResultSet newID = generateTransactionID.executeQuery();
            if (newID.next()) {
                orderID = newID.getInt("ORDERID"); // gets current highest id
                orderID = orderID +1;
            }
            
            
            //finds the userid by its associated email
            PreparedStatement findEmail = connection.prepareStatement(
                     "SELECT USERID FROM USERS WHERE EMAIL = ?");
                    findEmail.setString(1, build.getEmail());
                    System.out.println(build.getEmail());
            java.sql.ResultSet foundEmail = findEmail.executeQuery();
            if (foundEmail.next()){
                  userID = foundEmail.getInt("USERID");
                 System.out.println(build.getEmail()+userID);
            }
            
            
            
            //this block of text will be repeated for each object bought
            PreparedStatement findCpuID = connection.prepareStatement(
                     "SELECT PRODUCTID FROM PRODUCT WHERE PRODUCTNAME LIKE ?");
                    findCpuID.setString(1, "%" + build.getCpu().trim() + "%");
                    System.out.println(build.getCpu());
            java.sql.ResultSet cpuID = findCpuID.executeQuery();
           
            
            if (cpuID.next()){
                 cpuProductId = cpuID.getInt("PRODUCTID");
                 System.out.println(build.getCpu()+cpuProductId);
            }           
            PreparedStatement recordCPU = connection.prepareStatement(
                     "INSERT INTO TRANSACTIONS (ORDERID,FK_PRODUCTID,FK_USERID,PURCHASEDQAUNTITY) values (?,?,?,1)");
                    
                    recordCPU.setString(1, String.valueOf(orderID));
                    recordCPU.setString(2, String.valueOf(cpuProductId));
                    recordCPU.setString(3, String.valueOf(userID));
                    recordCPU.executeUpdate();
                    orderID = orderID +1;
                             
                    
            PreparedStatement findGpuID = connection.prepareStatement(
                     "SELECT PRODUCTID FROM PRODUCT WHERE PRODUCTNAME LIKE ?");
                    findGpuID.setString(1, "%" + build.getGpu().trim() + "%");
                    System.out.println(build.getGpu());
            java.sql.ResultSet gpuID = findGpuID.executeQuery();
           
            
            if (gpuID.next()){
                 gpuProductId = gpuID.getInt("PRODUCTID");
                 System.out.println(build.getGpu()+gpuProductId);
            }           
            PreparedStatement recordGPU = connection.prepareStatement(
                     "INSERT INTO TRANSACTIONS (ORDERID,FK_PRODUCTID,FK_USERID,PURCHASEDQAUNTITY) values (?,?,?,1)");
                    
                    recordGPU.setString(1, String.valueOf(orderID));
                    recordGPU.setString(2, String.valueOf(gpuProductId));
                    recordGPU.setString(3, String.valueOf(userID));
                    recordGPU.executeUpdate();
                    orderID = orderID +1;
                    
                    
            PreparedStatement findRamID = connection.prepareStatement(
                     "SELECT PRODUCTID FROM PRODUCT WHERE PRODUCTNAME LIKE ?");
                    findRamID.setString(1, "%" + build.getRam().trim() + "%");
                    System.out.println(build.getRam());
            java.sql.ResultSet ramID = findRamID.executeQuery();
           
            
            if (ramID.next()){
                 ramProductId = ramID.getInt("PRODUCTID");
                 System.out.println(build.getRam()+ramProductId);
            }           
            PreparedStatement recordRam = connection.prepareStatement(
                     "INSERT INTO TRANSACTIONS (ORDERID,FK_PRODUCTID,FK_USERID,PURCHASEDQAUNTITY) values (?,?,?,1)");
                    
                    recordRam.setString(1, String.valueOf(orderID));
                    recordRam.setString(2, String.valueOf(ramProductId));
                    recordRam.setString(3, String.valueOf(userID));
                    recordRam.executeUpdate();
                    

            
            
          
            

            
            
            
            
            return "worked";

        } finally {
            connection.close(); // return connection to pool
        }      
    }

    
    
    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu; // sets selected cpu from build form
    }
    
    
        public int getCpuStock() {
        return cpuStock; 
    }

    public void setCpuStock(int cpuStock) {
        this.cpuStock = cpuStock; 
    }

    
    
    public String getGpu() {
        return gpu;
    }

    public void setGpu(String gpu) {
        this.gpu = gpu; // sets selected gpu from build form
    }
    
        public int getGpuStock() {
        return gpuStock; 
    }

    public void setGpuStock(int gpuStock) {
        this.gpuStock = gpuStock; 
    }
    public String getRam() {
        return ram;
    }

    public void setRam(String ram) {
        this.ram = ram; // sets selected ram from build form
    }
        public int getRamStock() {
        return ramStock; 
    }

    public void setRamStock(int ramStock) {
        this.ramStock = ramStock; 
    }
    // inner class used to hold one saved build
    public static class SavedBuild implements Serializable {

        private int buildId;
        private String email;
        private String cpu;
        private String gpu;
        private String ram;
        private double totalPrice;
        private boolean editing;

        public int getBuildId() {
            return buildId;
        }

        public void setBuildId(int buildId) {
            this.buildId = buildId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCpu() {
            return cpu;
        }

        public void setCpu(String cpu) {
            this.cpu = cpu;
        }

        public String getGpu() {
            return gpu;
        }

        public void setGpu(String gpu) {
            this.gpu = gpu;
        }

        public String getRam() {
            return ram;
        }

        public void setRam(String ram) {
            this.ram = ram;
        }

        public double getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(double totalPrice) {
            this.totalPrice = totalPrice;
        }

        public boolean isEditing() {
            return editing;
        }

        public void setEditing(boolean editing) {
            this.editing = editing;
        }
    }
}
