package com.documentgenerator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

public class TrialManager {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.documerge";
    private static final String CONFIG_FILE = CONFIG_DIR + "/app.config";
    private static final String INSTALL_KEY = "installation.date";
    private static final int TRIAL_DAYS = 160; // 6 months

    static {
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
        } catch (IOException e) {
            // Silent fail - creates hidden directory
        }
    }

    public static void initializeTrial() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                // First time installation
                Properties props = new Properties();
                props.setProperty(INSTALL_KEY, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    props.store(fos, "Application Configuration");
                }
            }
        } catch (IOException e) {
            // Silent fail
        }
    }

    public static boolean isTrialValid() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                return false; // No installation record
            }

            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }

            String installDateStr = props.getProperty(INSTALL_KEY);
            if (installDateStr == null) {
                return false;
            }

            LocalDate installDate = LocalDate.parse(installDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate currentDate = LocalDate.now();
            long daysSinceInstall = ChronoUnit.DAYS.between(installDate, currentDate);

            return daysSinceInstall <= TRIAL_DAYS;

        } catch (Exception e) {
            return false; // Any error = trial invalid
        }
    }

    public static long getDaysRemaining() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists())
                return 0;

            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }

            String installDateStr = props.getProperty(INSTALL_KEY);
            if (installDateStr == null)
                return 0;

            LocalDate installDate = LocalDate.parse(installDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate expiryDate = installDate.plusDays(TRIAL_DAYS);
            LocalDate currentDate = LocalDate.now();

            return ChronoUnit.DAYS.between(currentDate, expiryDate);

        } catch (Exception e) {
            return 0;
        }
    }
}
