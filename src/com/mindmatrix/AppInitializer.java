package com.mindmatrix;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * AppInitializer – ServletContextListener that initializes database schema and migration
 * asynchronously when Tomcat starts up, keeping API HTTP requests fast and responsive.
 */
@WebListener
public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[MindMatrix] Initializing database and running schema migrations on app startup...");
        new Thread(() -> {
            try {
                DBConnection.initDatabase();
                System.out.println("[MindMatrix] Startup database migration complete.");
            } catch (Exception e) {
                System.err.println("[MindMatrix] Database startup initialization error: " + e.getMessage());
                e.printStackTrace();
            }
        }, "MindMatrix-DB-Init").start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[MindMatrix] Application shutting down.");
    }
}
