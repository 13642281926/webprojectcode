-- ============================================
-- AI Learning Platform Database Schema
-- ============================================

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS ai_learning 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_learning;

-- ============================================
-- Table: users
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar VARCHAR(255),
    signature VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    study_days INT NOT NULL DEFAULT 0,
    total_hours INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: course
-- ============================================
CREATE TABLE IF NOT EXISTS course (
    id VARCHAR(40) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(40) NOT NULL,
    cover VARCHAR(255),
    description VARCHAR(500) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    teacher VARCHAR(50) NOT NULL,
    lessons INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: course_chapter
-- ============================================
CREATE TABLE IF NOT EXISTS course_chapter (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    duration VARCHAR(30) NOT NULL,
    done BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT,
    course_id VARCHAR(40) NOT NULL,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: course_knowledge_point
-- ============================================
CREATE TABLE IF NOT EXISTS course_knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id VARCHAR(40) NOT NULL,
    knowledge_point VARCHAR(100) NOT NULL,
    sort_order INT,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: study_plan
-- ============================================
CREATE TABLE IF NOT EXISTS study_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(500),
    deadline DATE,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATE NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: resources
-- ============================================
CREATE TABLE IF NOT EXISTS resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    type VARCHAR(50),
    category VARCHAR(50),
    size VARCHAR(20) NOT NULL,
    url VARCHAR(500) NOT NULL,
    description VARCHAR(500),
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: notes
-- ============================================
CREATE TABLE IF NOT EXISTS notes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT,
    category VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: achievements
-- ============================================
CREATE TABLE IF NOT EXISTS achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    icon VARCHAR(100),
    unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    unlocked_at TIMESTAMP NULL,
    progress INT NOT NULL DEFAULT 0,
    target INT NOT NULL,
    category VARCHAR(50),
    rarity VARCHAR(50),
    points INT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: wrong_questions
-- ============================================
CREATE TABLE IF NOT EXISTS wrong_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    answer TEXT,
    analysis TEXT,
    category VARCHAR(50),
    difficulty VARCHAR(20),
    mastered BOOLEAN NOT NULL DEFAULT FALSE,
    wrong_count INT NOT NULL DEFAULT 1,
    tags VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Initial Data: Create a demo user
-- ============================================
INSERT INTO users (username, password, nickname, avatar, signature, role, study_days, total_hours)
VALUES (
    'admin',
    '123456', 
    'AI Learning Assistant', 
    'https://api.dicebear.com/9.x/avataaars/svg?seed=admin',
    'admin',
    'Focus on learning and continuous improvement',
    128,
    486
)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

-- ============================================
-- Initial Data: Demo Courses
-- ============================================
INSERT INTO course (id, title, category, cover, description, progress, teacher, lessons) VALUES
('c1', 'Vue3 Core Technology', 'frontend', 'https://images.unsplash.com/photo-1516116216624-53e697fedbea?w=400', 'Master Vue3 + TypeScript for enterprise-level application development', 85, 'Evan You', 48),
('c2', 'Java Backend Development', 'language', 'https://images.unsplash.com/photo-1587620962725-abab7fe55159?w=400', 'Quick start guide with Spring Boot + MyBatis-Plus', 45, 'Teacher Zhang', 64),
('c3', 'Computer Operating Systems', 'cs', 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=400', 'In-depth understanding of operating system core principles', 30, 'Professor Li', 32),
('c4', 'React 18 Complete Guide', 'frontend', 'https://images.unsplash.com/photo-1633356122544-f134324a6cee?w=400', 'Complete tutorial on React 18 new features, Hooks, and performance optimization', 60, 'Dan Abramov', 56),
('c5', 'Python Data Analysis', 'language', 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=400', 'Hands-on data science with Pandas, NumPy, and Matplotlib', 20, 'Andrew Ng', 40),
('c6', 'Computer Networks', 'cs', 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400', 'Comprehensive analysis of TCP/IP, HTTP, and network security', 75, 'Professor Xie', 48)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- ============================================
-- Initial Data: Course Knowledge Points
-- ============================================
INSERT INTO course_knowledge_point (course_id, knowledge_point, sort_order) VALUES
('c1', 'Composition API', 1),
('c1', 'Vue Router', 2),
('c1', 'Pinia', 3),
('c1', 'TypeScript', 4),
('c2', 'Spring Boot', 1),
('c2', 'MyBatis-Plus', 2),
('c2', 'MySQL', 3),
('c2', 'Redis', 4),
('c3', 'Process Management', 1),
('c3', 'Memory Allocation', 2),
('c3', 'File System', 3),
('c3', 'Concurrency Control', 4),
('c4', 'Hooks', 1),
('c4', 'Suspense', 2),
('c4', 'Redux', 3),
('c4', 'Performance Optimization', 4),
('c5', 'Pandas', 1),
('c5', 'NumPy', 2),
('c5', 'Visualization', 3),
('c5', 'Data Cleaning', 4),
('c6', 'TCP/IP', 1),
('c6', 'HTTP', 2),
('c6', 'Routing', 3),
('c6', 'Network Security', 4);

-- ============================================
-- Initial Data: Course Chapters
-- ============================================
INSERT INTO course_chapter (id, title, duration, done, sort_order, course_id) VALUES
('c1-ch1', 'Introduction to Vue3', '30min', TRUE, 1, 'c1'),
('c1-ch2', 'Composition API Basics', '45min', TRUE, 2, 'c1'),
('c1-ch3', 'Vue Router 4', '50min', FALSE, 3, 'c1'),
('c1-ch4', 'State Management with Pinia', '40min', FALSE, 4, 'c1'),
('c2-ch1', 'Spring Boot Quick Start', '35min', TRUE, 1, 'c2'),
('c2-ch2', 'RESTful API Development', '55min', FALSE, 2, 'c2');

-- ============================================
-- Initial Data: Demo Study Plans
-- ============================================
INSERT INTO study_plan (user_id, title, content, deadline, priority, status, created_at) VALUES
(1, 'Vue3 Component Practice', 'Complete comprehensive practice of Composition API and Pinia', '2026-06-18', 'high', 'pending', '2026-05-20'),
(1, 'Frontend Engineering Review', 'Review Vite, Webpack, Babel and other tool configurations', '2026-06-25', 'medium', 'doing', '2026-05-22'),
(1, 'Daily Algorithm Practice', 'LeetCode Hot 100, at least 2 questions per day', '2026-07-30', 'high', 'doing', '2026-05-25');

-- ============================================
-- Initial Data: Demo Notes
-- ============================================
INSERT INTO notes (user_id, title, content, category, created_at, updated_at) VALUES
(1, 'Vue3 Reactivity Principles', 'Vue3 uses Proxy instead of Object.defineProperty in Vue2', 'study', '2026-05-20 10:00:00', '2026-05-20 10:00:00'),
(1, 'Java Concurrency Programming Notes', 'ThreadLocal can solve thread safety issues, but pay attention to memory leaks', 'thought', '2026-05-22 14:30:00', '2026-05-22 14:30:00'),
(1, 'CSS Grid Layout Techniques', 'Use grid-template-areas to create complex layouts, combined with repeat and auto-fit for responsive design', 'study', '2026-05-25 09:15:00', '2026-05-25 09:15:00');

-- ============================================
-- Initial Data: Demo Resources
-- ============================================
INSERT INTO resources (user_id, title, type, category, size, url, description, download_count, created_at) VALUES
(1, 'Spring Boot Core Notes', 'pdf', 'book', '2.5MB', 'https://example.com/spring-boot.pdf', 'Spring Boot 2.x core technology documentation', 5, '2026-05-20 11:00:00'),
(1, 'Vue3 Practical Source Code', 'zip', 'code', '15.8MB', 'https://example.com/vue3-project.zip', 'Complete Vue3 project source code', 12, '2026-05-21 09:30:00'),
(1, 'JavaScript Advanced Tutorial', 'pdf', 'book', '4.2MB', 'https://example.com/javascript.pdf', 'Deep understanding of JavaScript closures, prototype chains, and more', 8, '2026-05-23 16:45:00');

-- ============================================
-- Initial Data: Demo Achievements
-- ============================================
INSERT INTO achievements (user_id, title, description, icon, unlocked, unlocked_at, progress, target, category, rarity, points) VALUES
(1, 'Learning Newbie', 'Complete your first study session', 'CircleCheck', TRUE, '2026-05-20 10:00:00', 1, 1, 'growth', 'common', 10),
(1, 'Knowledge Explorer', 'Complete 3 study sessions', 'List', TRUE, '2026-05-22 14:00:00', 3, 3, 'learning', 'uncommon', 20),
(1, 'Note Master', 'Create 5 notes', 'Document', TRUE, '2026-05-25 09:00:00', 5, 5, 'creation', 'uncommon', 25),
(1, 'Collector of Wrong Questions', 'Add 10 wrong questions', 'Warning', FALSE, NULL, 0, 10, 'learning', 'rare', 30),
(1, 'Continuous Learner', 'Study for 7 consecutive days', 'Timer', FALSE, NULL, 0, 7, 'persistence', 'epic', 50),
(1, 'Resource Master', 'Upload 20 resources', 'Folder', FALSE, NULL, 0, 20, 'tasks', 'rare', 35),
(1, 'Course Beginner', 'Start learning your first course', 'Reading', TRUE, '2026-05-20 11:00:00', 1, 1, 'course', 'common', 15),
(1, 'Course Collector', 'Add 5 courses', 'Reading', TRUE, '2026-05-23 16:00:00', 5, 5, 'course', 'uncommon', 30);

-- ============================================
-- Initial Data: Demo Wrong Questions
-- ============================================
INSERT INTO wrong_questions (user_id, title, content, answer, analysis, category, difficulty, mastered, wrong_count, tags, created_at, updated_at) VALUES
(1, 'About JVM Garbage Collection', 'What are the differences between CMS and G1?', 'CMS uses mark-sweep algorithm, G1 uses mark-compact algorithm', 'CMS focuses on pause time, G1 focuses on throughput', 'cs', '困难', TRUE, 3, 'jvm,gc', '2026-05-20 15:00:00', '2026-05-25 15:00:00'),
(1, 'Vue3 Reactivity System', 'What are the differences between ref and reactive?', 'ref is for primitive types, reactive is for objects; ref requires .value, reactive does not', 'Note that ref is automatically unwrapped in templates', 'frontend', '中等', FALSE, 2, 'vue3,reactivity', '2026-05-22 10:30:00', '2026-05-22 10:30:00'),
(1, 'JavaScript Prototype', 'Explain how prototype chain works', 'Every object has __proto__ pointing to its prototype object, forming a prototype chain', 'prototype is a property of constructor functions, __proto__ is a property of objects', 'frontend', '困难', FALSE, 4, 'js,prototype', '2026-05-23 14:20:00', '2026-05-23 14:20:00'),
(1, 'SQL Join Query', 'Differences between inner join and outer join?', 'Inner join returns only matching rows, outer join returns non-matching rows as well', 'Left outer join preserves all rows from left table, right outer join preserves all from right', 'cs', '中等', FALSE, 2, 'sql,join', '2026-05-24 11:00:00', '2026-05-24 11:00:00');

-- ============================================
-- Create indexes for better performance
-- ============================================
CREATE INDEX idx_user_id_study_plan ON study_plan(user_id);
CREATE INDEX idx_user_id_resources ON resources(user_id);
CREATE INDEX idx_user_id_notes ON notes(user_id);
CREATE INDEX idx_user_id_achievements ON achievements(user_id);
CREATE INDEX idx_user_id_wrong_questions ON wrong_questions(user_id);
CREATE INDEX idx_course_id_chapter ON course_chapter(course_id);
CREATE INDEX idx_course_id_knowledge ON course_knowledge_point(course_id);

-- ============================================
-- Schema creation completed
-- ============================================
SELECT 'Database schema created successfully!' AS message;
