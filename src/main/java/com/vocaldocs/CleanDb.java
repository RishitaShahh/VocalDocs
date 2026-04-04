package com.vocaldocs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CleanDb {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC";
        try (Connection conn = DriverManager.getConnection(url, "root", "");
             Statement stmt = conn.createStatement()) {
            System.out.println("Dropping tables...");
            stmt.execute("DROP DATABASE IF EXISTS vocaldocs_db");
            stmt.execute("CREATE DATABASE vocaldocs_db");
            System.out.println("Cleaned!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
