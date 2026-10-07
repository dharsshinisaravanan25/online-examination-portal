package com.examportal.service;

import com.examportal.model.*;
import com.examportal.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ExamRepository examRepository,
                           QuestionRepository questionRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedExamsAndQuestions();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User(
                    "admin",
                    "admin@examportal.com",
                    "System Administrator",
                    passwordEncoder.encode("admin123"),
                    Role.ADMIN
            );
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("student")) {
            User student = new User(
                    "student",
                    "student@examportal.com",
                    "Alex Rivera",
                    passwordEncoder.encode("student123"),
                    Role.STUDENT
            );
            userRepository.save(student);
        }
    }

    private void seedExamsAndQuestions() {
        if (examRepository.count() > 0) {
            return;
        }

        // 1. Java 21 & Spring Boot Core Exam
        Exam javaExam = new Exam(
                "Java 21 & Spring Boot Full-Stack Certification",
                "Comprehensive assessment covering Java 21 LTS modern features (virtual threads, records, pattern matching), Spring Data JPA, Spring Security, and RESTful architectures.",
                "Java & Spring",
                15,
                50,
                30,
                0.5,
                false,
                true
        );
        javaExam = examRepository.save(javaExam);

        addQ(javaExam,
                "Which feature introduced in Java 21 provides lightweight threads that drastically reduce the overhead of writing high-throughput concurrent applications?",
                "Platform Threads",
                "Virtual Threads (Project Loom)",
                "ForkJoin Pools",
                "Goroutines",
                "B",
                "Virtual Threads, delivered as part of Project Loom in Java 21, allow millions of lightweight threads to run concurrently on standard JVM hardware.",
                5, "MEDIUM");

        addQ(javaExam,
                "What is the primary purpose of the '@Transactional' annotation in Spring Framework?",
                "To encrypt network traffic between client and server",
                "To automatically manage database transaction boundaries (commit / rollback)",
                "To compile Java classes into native binaries",
                "To schedule recurring cron background jobs",
                "B",
                "@Transactional automatically begins, commits on success, or rolls back on runtime exceptions for database operations.",
                5, "EASY");

        addQ(javaExam,
                "In Java 17+, how do you declare an immutable data carrier class with automatic equals, hashCode, and toString methods?",
                "public sealed class Data {}",
                "public record Data(String name, int value) {}",
                "public dynamic class Data {}",
                "public abstract struct Data {}",
                "B",
                "Records in Java provide a concise syntax for declaring shallowly immutable transparent holders for shallow data.",
                5, "EASY");

        addQ(javaExam,
                "What does the Spring Boot starter 'spring-boot-starter-data-jpa' provide out-of-the-box?",
                "Direct raw socket networking",
                "Hibernate ORM, Spring Data repositories, and HikariCP connection pooling",
                "React.js and Vue.js compilation toolchains",
                "Embedded Redis distributed cache only",
                "B",
                "Spring Data JPA starter bundles Hibernate, Spring ORM, Spring Data repositories, and HikariCP connection pool.",
                5, "MEDIUM");

        addQ(javaExam,
                "Which HTTP status code should a well-designed REST API return when a resource is successfully created?",
                "200 OK",
                "201 Created",
                "204 No Content",
                "202 Accepted",
                "B",
                "HTTP 201 Created signifies that the request succeeded and led to the creation of a new resource on the server.",
                5, "EASY");

        addQ(javaExam,
                "What is the default bean scope in Spring Framework?",
                "Prototype",
                "Request",
                "Singleton",
                "Session",
                "C",
                "Spring container creates exactly one instance of a bean per Spring ApplicationContext by default (Singleton).",
                5, "MEDIUM");

        addQ(javaExam,
                "How does Spring Security typically verify and hash passwords securely?",
                "MD5 hashing",
                "Plain SHA-256 without salt",
                "BCryptPasswordEncoder with adaptive work factor and salting",
                "Base64 encoding",
                "C",
                "BCrypt applies iterative cryptographic hashing with salt to protect against dictionary and rainbow table attacks.",
                5, "MEDIUM");

        addQ(javaExam,
                "What will happen if an unhandled 'RuntimeException' occurs inside a '@Transactional' Spring method?",
                "The transaction is immediately committed",
                "The transaction is automatically rolled back",
                "The server crashes and powers down",
                "The exception is silently discarded",
                "B",
                "Spring rolls back transactions by default on unchecked (RuntimeException and Error) exceptions.",
                5, "MEDIUM");

        addQ(javaExam,
                "Which method of 'Optional<T>' returns the value if present, or evaluates a supplier if empty without throwing an exception?",
                "optional.get()",
                "optional.orElseGet(Supplier<? extends T> supplier)",
                "optional.orElseThrow()",
                "optional.isPresent()",
                "B",
                "orElseGet allows lazy evaluation of fallback values only when the Optional is empty.",
                5, "MEDIUM");

        addQ(javaExam,
                "In Java Pattern Matching for switch statements, which keyword is used to conditionally qualify a pattern?",
                "where",
                "when",
                "if",
                "guard",
                "B",
                "Java 21 uses the 'when' clause (guarded pattern) to add boolean conditions to switch case labels.",
                5, "HARD");

        // 2. Data Structures & Algorithms Exam
        Exam dsaExam = new Exam(
                "Data Structures & Algorithms Proficiency Test",
                "Evaluate your problem solving skills across asymptotic time complexities, tree traversals, graphs, and dynamic programming.",
                "Algorithms",
                10,
                25,
                15,
                0.25,
                false,
                true
        );
        dsaExam = examRepository.save(dsaExam);

        addQ(dsaExam,
                "What is the average and worst-case time complexity of searching in a balanced Binary Search Tree (AVL / Red-Black Tree)?",
                "Average: O(1), Worst: O(n)",
                "Average: O(log n), Worst: O(log n)",
                "Average: O(n), Worst: O(n^2)",
                "Average: O(log n), Worst: O(n)",
                "B",
                "Self-balancing BSTs guarantee O(log n) height, yielding O(log n) worst and average search time.",
                5, "EASY");

        addQ(dsaExam,
                "Which algorithm is best suited for finding the shortest path in a graph with non-negative edge weights?",
                "Bellman-Ford Algorithm",
                "Dijkstra's Algorithm",
                "Depth First Search",
                "Floyd-Warshall Algorithm for all pairs only",
                "B",
                "Dijkstra's algorithm efficiently computes single-source shortest paths in graphs with non-negative weights using a priority queue.",
                5, "MEDIUM");

        addQ(dsaExam,
                "What data structure is used to implement Breadth-First Search (BFS) in a graph?",
                "Stack (LIFO)",
                "Queue (FIFO)",
                "Priority Queue",
                "Hash Set",
                "B",
                "BFS explores neighbors level-by-level using a First-In First-Out Queue.",
                5, "EASY");

        addQ(dsaExam,
                "Which sorting algorithm guarantees O(n log n) worst-case time complexity and is stable?",
                "Quick Sort",
                "Merge Sort",
                "Heap Sort",
                "Selection Sort",
                "B",
                "Merge sort provides guaranteed O(n log n) time in all cases and preserves the relative order of equal elements (stable).",
                5, "MEDIUM");

        addQ(dsaExam,
                "In Dynamic Programming, what are the two essential characteristics a problem must possess?",
                "Linear structure and sorting property",
                "Optimal substructure and overlapping subproblems",
                "Greedy choice property and divide-and-conquer",
                "Deterministic state machine and hashing",
                "B",
                "Dynamic programming applies when subproblems overlap and optimal solutions to subproblems combine into an overall optimal solution.",
                5, "HARD");

        // 3. Database Architecture Exam
        Exam dbExam = new Exam(
                "Database Architecture & SQL Mastery",
                "Test core relational concepts, ACID transactions, indexing strategies, normalization, and query performance.",
                "Database",
                10,
                20,
                12,
                0.0,
                false,
                true
        );
        dbExam = examRepository.save(dbExam);

        addQ(dbExam,
                "In ACID transaction properties, which property ensures that once a transaction commits, its changes survive system crashes?",
                "Atomicity",
                "Consistency",
                "Isolation",
                "Durability",
                "D",
                "Durability guarantees that committed data will be saved permanently and recovered even in case of power failure.",
                4, "EASY");

        addQ(dbExam,
                "Which SQL JOIN returns all rows from the left table, and matching rows from the right table, filling nulls if no match exists?",
                "INNER JOIN",
                "LEFT OUTER JOIN",
                "RIGHT OUTER JOIN",
                "CROSS JOIN",
                "B",
                "LEFT OUTER JOIN retains every record from the left table regardless of whether a matching record exists on the right.",
                4, "EASY");

        addQ(dbExam,
                "What database structure is most commonly used to accelerate SQL SELECT query lookups on indexed columns?",
                "Linked List",
                "B+ Tree",
                "Min-Heap",
                "Adjacency Matrix",
                "B",
                "B+ Trees maintain balanced depth and sequential leaves, optimizing both single-point lookups and range scans on disk storage.",
                4, "MEDIUM");

        addQ(dbExam,
                "Which normal form requires removing transitive functional dependencies (non-key attributes depending on other non-key attributes)?",
                "First Normal Form (1NF)",
                "Second Normal Form (2NF)",
                "Third Normal Form (3NF)",
                "Boyce-Codd Normal Form (BCNF)",
                "C",
                "3NF states every non-prime attribute must be directly dependent on the primary key, eliminating transitive dependencies.",
                4, "MEDIUM");

        addQ(dbExam,
                "What phenomenon occurs when a transaction reads data that was modified by another concurrent uncommitted transaction?",
                "Phantom Read",
                "Dirty Read",
                "Non-repeatable Read",
                "Lost Update",
                "B",
                "A Dirty Read happens when a transaction reads uncommitted changes that might subsequently be rolled back.",
                4, "HARD");
    }

    private void addQ(Exam exam, String text, String a, String b, String c, String d,
                      String correct, String explanation, int marks, String difficulty) {
        Question q = new Question(exam, text, a, b, c, d, correct, explanation, marks, difficulty);
        exam.addQuestion(q);
        questionRepository.save(q);
    }
}
