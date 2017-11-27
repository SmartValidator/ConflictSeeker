package com.company;
import org.apache.commons.net.util.SubnetUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConflictSeeker {

    private Connection connection;
    private List<Roa> validated_roas;
    private Map<String, List<RoaEntry>> validatedAnnouncements;
    public class RoaEntry{
        private Roa roaEntry;
        private String validity;

        public RoaEntry(Roa roa, String validity){
            this.roaEntry = roa;
            this.validity = validity;

        }
    }

    public class Roa{
        private int roa_id;
        private String asn;
        private String prefix;
        private String maxLength;
        private String validity;

        public Roa(int roa_id, String asn, String prefix, String maxLength){
            this.roa_id = roa_id;
            this.asn = asn;
            this.prefix = prefix;
            this.maxLength = maxLength;
        }
    }

    public ConflictSeeker(){
        System.out.println("-------- PostgreSQL "
                + "JDBC Connection ------------");

        try {

            Class.forName("org.postgresql.Driver");

        } catch (ClassNotFoundException e) {

            System.out.println("Where is your PostgreSQL JDBC Driver? "
                    + "Include in your library path!");
            e.printStackTrace();
            return;

        }

        System.out.println("PostgreSQL JDBC Driver Registered!");

        connection = null;

        try {

            connection = DriverManager.getConnection(
                    "jdbc:postgresql://smart-validator.net/smart_validator_test_1", "svt1",
                    "XagO7kfuGA1ZMxhP45u7zkna7eMB235zSTfCSzFpOy76OckuubXqlCxGyyC");

        } catch (SQLException e) {

            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return;

        }

        if (connection != null) {
            System.out.println("You made it, take control your database now!");
            this.runConflictSeeker();
        } else {
            System.out.println("Failed to make connection!");
        }
    }
    private void updateValidationMap(RoaEntry entry,String key){
        if(validatedAnnouncements.get(key) == null){
            List<RoaEntry> newList = new ArrayList<>();
            newList.add(entry);
            validatedAnnouncements.put(key,newList);
        }else{
            List<RoaEntry> curList = validatedAnnouncements.get(key);
            curList.add(entry);
            validatedAnnouncements.put(key, curList);
        }

    }

    private boolean checkAsn(Roa curRoa,ResultSet rs) throws Exception{
        if(curRoa.asn.equals(rs.getString(2)))
            return true;
        return false;

    }
    private boolean checkLength(Roa curRoa,ResultSet rs) throws Exception{
        int checkRoa = Math.max(Integer.parseInt(curRoa.prefix.split("\\/")[1]),Integer.parseInt(curRoa.maxLength));
        int annPrefix = Integer.parseInt(rs.getString(3).split("\\/")[1]);
        if(annPrefix <= checkRoa){
            return true;
        }
        return false;
    }

    private void detectOverlap(){
        try{
//            Statement stmt = connection.createStatement();
//            ResultSet test = stmt.executeQuery("SELECT * FROM announcements");
//            int count =0;
//            while(count < 38){
//                test.next();
//                count++;
//            }

            ResultSet rs;
            for(Roa roa : validated_roas){
                rs = connection.createStatement().executeQuery("SELECT * FROM announcements WHERE prefix <<= inet '" + roa.prefix+ "'");
                while (rs.next() ) {
                    String key = rs.getString(2)+":"+rs.getString(3);
                    if(!checkAsn(roa,rs)){
                        updateValidationMap(new RoaEntry(roa,"InvalidAsn"),key);
                    }else if(!checkLength(roa,rs)){
                        updateValidationMap(new RoaEntry(roa,"InvalidLength"),key);
                    }else{
                        updateValidationMap(new RoaEntry(roa,"Valid"),key);
                    }
                }
            }
        }catch(Exception e){
            System.out.println(e.getMessage());

        }


    }


    private void getRoas( ) {
        try {
            // Get the database metadata

            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM validated_roas");
            while (rs.next()){
                validated_roas.add(new Roa(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4)));
            }
//            ResultSetMetaData rsmd = rs.getMetaData();
//            DatabaseMetaData metadata = connection.getMetaData();
//
//            // Specify the type of object; in this case we want tables
//
//            String[] types = {"TABLE"};
//
//            ResultSet resultSet = metadata.getTables(null, null, "%", types);
//
//
//            while (resultSet.next()) {
//
//                String tableName = resultSet.getString(3);
//
//                String tableCatalog = resultSet.getString(1);
//
//                String tableSchema = resultSet.getString(2);
//
//
//                System.out.println("Table : " + tableName + "nCatalog : " + tableCatalog + "nSchema : " + tableSchema);
////
////
////            }
//            }
        } catch (Exception e) {
            System.out.println(e.getMessage());

        }
    }

    private void runConflictSeeker(){
        validated_roas = new ArrayList<>();
        validatedAnnouncements = new HashMap<>();
        this.getRoas();
        this.detectOverlap();

    }


}