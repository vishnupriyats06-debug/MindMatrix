-- ============================================
-- MindMatrix Database Setup Script
-- Run this in phpMyAdmin or MySQL CLI
-- ============================================

CREATE DATABASE IF NOT EXISTS mindmatrix
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mindmatrix;

-- Users table for registration and login
CREATE TABLE IF NOT EXISTS users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    avatar_id   INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User progress table for storing game stats
-- NOTE: best_times and stars use ASCII-safe sentinel values.
--       The dashboard JavaScript converts them to display symbols.
CREATE TABLE IF NOT EXISTS user_progress (
    user_id          INT PRIMARY KEY,
    score            INT DEFAULT 0,
    streak           INT DEFAULT 0,
    unlocked_level   INT DEFAULT 1,
    games_played     INT DEFAULT 0,
    best_streak      INT DEFAULT 0,
    hints            INT NOT NULL DEFAULT 3,
    best_scores      VARCHAR(1000) DEFAULT '[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]',
    best_times       VARCHAR(1000) DEFAULT '["-","-","-","-","-","-","-","-","-","-","-","-","-","-","-","-","-","-","-","-"]',
    stars            VARCHAR(1000) DEFAULT '["0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0"]',
    last_played_date DATE DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Activity tracking table for streak calculation
CREATE TABLE IF NOT EXISTS user_activity_dates (
    user_id       INT NOT NULL,
    activity_date DATE NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, activity_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
