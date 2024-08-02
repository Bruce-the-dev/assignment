<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Welcome</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f5f5f5;
            color: #333;
            text-align: center;
            margin: 0;
            padding: 20px;
        }
        h1 {
            color: #4CAF50;
        }
        .button {
            display: inline-block;
            padding: 12px 24px;
            margin: 10px;
            border: none;
            border-radius: 8px;
            background-color: #2196F3;
            color: white;
            text-decoration: none;
            font-size: 18px;
            cursor: pointer;
            transition: background-color 0.3s;
        }
        .button:hover {
            background-color: #0b79d0;
        }
        .button-danger {
            background-color: #f44336;
        }
        .button-danger:hover {
            background-color: #c62828;
        }
    </style>
</head>
<body>
    <h1>Welcome to the Student Records Application</h1>
    <a href="DemoServlet" class="button">Go to Form</a>
    <a href="DemoServlet?action=viewRecords" class="button">View Records</a>
</body>
</html>
