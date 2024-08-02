package com.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DemoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Database credentials
    private static final String DB_URL = "jdbc:postgresql://localhost:5433/webapplication";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "bruce";

    private static final Logger logger = Logger.getLogger(DemoServlet.class.getName());
    private FileHandler fileHandler;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // Ensure the PostgreSQL JDBC Driver is available
            Class.forName("org.postgresql.Driver");

            // Configure logger
            fileHandler = new FileHandler("app.log", true); // Append mode
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false); // Prevent logging to console by default
        } catch (ClassNotFoundException e) {
            logger.severe("PostgreSQL JDBC Driver not found: " + e.getMessage());
            throw new ServletException("PostgreSQL JDBC Driver not found", e);
        } catch (IOException e) {
            logger.severe("Error initializing log file: " + e.getMessage());
            throw new ServletException("Error initializing log file", e);
        }

        // Create the table if it does not exist
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS records ("
                    + "student_id INTEGER PRIMARY KEY, "
                    + "student_name VARCHAR(255) NOT NULL)";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.severe("Unable to initialize database: " + e.getMessage());
            throw new ServletException("Unable to initialize database", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("viewRecords".equals(action)) {
            doViewRecords(request, response);
        } else if ("error".equals(action)) {
            displayError(request, response);
        } else {
            displayForm(response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("submitForm".equals(action)) {
            handleFormSubmission(request, response);
        }
    }

    private void displayForm(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        try (PrintWriter pw = response.getWriter()) {
            pw.println("<html>");
            pw.println("<head><title>Form Page</title>");
            pw.println("<style>");
            pw.println("body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }");
            pw.println("h1 { color: #4CAF50; text-align: center; }");
            pw.println("form { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1); margin: 0 auto; max-width: 400px; }");
            pw.println("input[type='text'], input[type='submit'] { width: calc(100% - 22px); padding: 10px; margin: 10px 0; border: 1px solid #ddd; border-radius: 4px; }");
            pw.println("input[type='submit'] { background-color: #2196F3; color: white; cursor: pointer; }");
            pw.println("input[type='submit']:hover { background-color: #1976D2; }");
            pw.println("</style>");
            pw.println("</head>");
            pw.println("<body>");
            pw.println("<h1>Input Form</h1>");
            pw.println("<form method='post' action='DemoServlet?action=submitForm'>");
            pw.println("Student ID: <input type='text' name='studentId' required><br>");
            pw.println("Student Name: <input type='text' name='studentName' required><br>");
            pw.println("<input type='submit' value='Submit'>");
            pw.println("</form>");
            pw.println("<form method='get' action='DemoServlet'>");
            pw.println("<input type='hidden' name='action' value='viewRecords'>");
            pw.println("<input type='submit' value='View Records'>");
            pw.println("</form>");
            pw.println("</body>");
            pw.println("</html>");
        }
    }

    private void handleFormSubmission(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        try (PrintWriter pw = response.getWriter()) {
            String studentIdStr = request.getParameter("studentId");
            String studentName = request.getParameter("studentName");

            logger.info("Received form submission: Student ID = " + studentIdStr + ", Student Name = " + studentName);

            // Convert studentIdStr to integer
            int studentId;
            try {
                studentId = Integer.parseInt(studentIdStr);
            } catch (NumberFormatException e) {
                logger.warning("Invalid student ID: " + studentIdStr);
                throw new ServletException("Invalid student ID", e);
            }

            // Check for duplicate student ID
            boolean duplicateId = false;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM records WHERE student_id = ?")) {
                checkStmt.setInt(1, studentId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        duplicateId = true;
                    }
                }
            } catch (SQLException e) {
                logger.severe("Unable to check for duplicate student ID: " + e.getMessage());
                throw new ServletException("Unable to check for duplicate student ID", e);
            }

            if (duplicateId) {
                logger.warning("Duplicate Student ID detected: " + studentId);
                response.sendRedirect("DemoServlet?action=error&message=Student ID already exists. Please use a different ID.");
                return;
            }

            // Insert the record
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO records (student_id, student_name) VALUES (?, ?)")) {
                pstmt.setInt(1, studentId);
                pstmt.setString(2, studentName);
                pstmt.executeUpdate();
                logger.info("Data stored successfully: Student ID = " + studentId + ", Student Name = " + studentName);
            } catch (SQLException e) {
                logger.severe("Unable to store data: " + e.getMessage());
                throw new ServletException("Unable to store data", e);
            }

            // Redirect to the records view page
            response.sendRedirect("DemoServlet?action=viewRecords");
        }
    }

    private void doViewRecords(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        try (PrintWriter pw = response.getWriter()) {
            pw.println("<html>");
            pw.println("<head><title>Records</title>");
            pw.println("<style>");
            pw.println("body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }");
            pw.println("h1 { color: #4CAF50; text-align: center; }");
            pw.println("table { width: 100%; margin: 20px 0; border-collapse: collapse; }");
            pw.println("table, th, td { border: 1px solid #ddd; }");
            pw.println("th, td { padding: 12px; text-align: left; }");
            pw.println("th { background-color: #2196F3; color: white; }");
            pw.println("tr:nth-child(even) { background-color: #f2f2f2; }");
            pw.println("input[type='button'] { background-color: #4CAF50; color: white; border: none; padding: 10px 15px; border-radius: 4px; cursor: pointer; }");
            pw.println("input[type='button']:hover { background-color: #388E3C; }");
            pw.println("</style>");
            pw.println("</head>");
            pw.println("<body>");
            pw.println("<h1>Records</h1>");
            pw.println("<table>");
            pw.println("<tr><th>Student ID</th><th>Student Name</th></tr>");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM records")) {
                while (rs.next()) {
                    int studentId = rs.getInt("student_id");
                    String studentName = rs.getString("student_name");
                    pw.println("<tr><td>" + studentId + "</td><td>" + studentName + "</td></tr>");
                }
            } catch (SQLException e) {
                logger.severe("Unable to retrieve records: " + e.getMessage());
                throw new ServletException("Unable to retrieve records", e);
            }

            pw.println("</table>");
            
            // Button to navigate back to the form
            pw.println("<div style='text-align: center;'>");
            pw.println("<input type='button' value='Back to Form' onclick=\"window.location.href='DemoServlet'\">");
            pw.println("</div>");

            pw.println("</body>");
            pw.println("</html>");
        }
    }

    private void displayError(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        String message = request.getParameter("message");
        try (PrintWriter pw = response.getWriter()) {
            pw.println("<html>");
            pw.println("<head><title>Error</title>");
            pw.println("<style>");
            pw.println("body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; text-align: center; }");
            pw.println("h1 { color: #f44336; }");
            pw.println("a { color: #2196F3; text-decoration: none; font-weight: bold; }");
            pw.println("a:hover { text-decoration: underline; }");
            pw.println("</style>");
            pw.println("</head>");
            pw.println("<body>");
            pw.println("<h1>Error</h1>");
            pw.println("<p>" + (message != null ? message : "An error occurred.") + "</p>");
            pw.println("<p><a href='DemoServlet'>Go Back</a></p>");
            pw.println("</body>");
            pw.println("</html>");
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}
