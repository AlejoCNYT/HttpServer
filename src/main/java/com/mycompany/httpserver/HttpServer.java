/*
 * HTTP Server implementation for handling static files and REST services
 */
package com.mycompany.httpserver;

import java.net.*;
import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpServer {

    private static final String API_KEY = "Q1QZFVJQ21K7C6XM";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("json", "application/json");
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(35000);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }
        
        System.out.println("Server running on port 35000...");
        System.out.println("Try these endpoints:");
        System.out.println("http://localhost:35000/app/hello?name=John");
        System.out.println("http://localhost:35000/stocks?symbol=fb");
        System.out.println("http://localhost:35000/");
        System.out.println("http://localhost:35000/static/");

        boolean running = true;
        while (running) {
            Socket clientSocket = null;
            try {
                System.out.println("\nReady to receive...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                continue;
            }

            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream()))) {
                
                String inputLine;
                URI requesturi = null;
                boolean isFirstLine = true;
                String httpMethod = "GET";

                while ((inputLine = in.readLine()) != null) {
                    if (isFirstLine && !inputLine.isEmpty()) {
                        String[] requestParts = inputLine.split(" ");
                        if (requestParts.length > 1) {
                            httpMethod = requestParts[0];
                            requesturi = new URI(requestParts[1]);
                            System.out.println("[" + httpMethod + "] Requested Path: " + requesturi.getPath());
                        }
                        isFirstLine = false;
                    }
                    if (!in.ready()) {
                        break;
                    }
                }

                String outputLine;
                if (requesturi == null) {
                    outputLine = buildResponse(400, "text/plain", "Bad Request");
                } else if (requesturi.getPath().startsWith("/app/hello")) {
                    outputLine = handleHelloRequest(requesturi);
                } else if (requesturi.getPath().startsWith("/stocks")) {
                    outputLine = handleStockRequest(requesturi);
                } else if (requesturi.getPath().startsWith("/static/")) {
                    outputLine = handleFileRequest(requesturi);
                } else if (requesturi.getPath().equals("/")) {
                    outputLine = handleFileRequest(new URI("/static/index.html"));
                } else {
                    outputLine = handleFileRequest(new URI("/static" + requesturi.getPath()));
                }

                out.println(outputLine);
            } catch (Exception e) {
                System.err.println("Error handling client: " + e.getMessage());
                try {
                    clientSocket.getOutputStream().write(buildResponse(500, "text/plain", "Internal Server Error").getBytes());
                } catch (IOException ex) {
                    System.err.println("Could not send error response: " + ex.getMessage());
                }
            } finally {
                try {
                    if (clientSocket != null) clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
        
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    private static String handleHelloRequest(URI requesturi) {
        try {
            String name = "User";
            if (requesturi.getQuery() != null && requesturi.getQuery().contains("name=")) {
                name = requesturi.getQuery().split("=")[1];
            }
            String jsonResponse = String.format("{\"message\": \"Hola %s\"}", name);
            return buildResponse(200, "application/json", jsonResponse);
        } catch (Exception e) {
            return buildResponse(500, "text/plain", "Error processing hello request");
        }
    }

    private static String handleStockRequest(URI requesturi) {
        try {
            String symbol = "fb";
            if (requesturi.getQuery() != null && requesturi.getQuery().contains("symbol=")) {
                symbol = requesturi.getQuery().split("symbol=")[1].split("&")[0];
            }

            String apiUrl = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" + 
                          symbol + "&apikey=" + API_KEY;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return buildResponse(200, "application/json", response.body());
            } else {
                return buildResponse(502, "text/plain", "Error fetching stock data: " + response.statusCode());
            }
        } catch (Exception e) {
            return buildResponse(500, "text/plain", "Server error processing stock request: " + e.getMessage());
        }
    }

    private static String handleFileRequest(URI requesturi) throws IOException {
        String path = requesturi.getPath().replaceFirst("^/static", "");
        if (path.isEmpty() || path.equals("/")) path = "/index.html";
        
        // Security: prevent directory traversal
        path = path.replaceAll("\\.\\.", "").replaceAll("//", "/");
        
        // Try multiple locations for the file
        Path filePath = tryResolveFile(path);
        
        if (filePath == null || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            return buildResponse(404, "text/plain", "File not found: " + path);
        }

        // Determine content type
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            String extension = getFileExtension(path);
            contentType = MIME_TYPES.getOrDefault(extension, "text/plain");
        }

        byte[] fileContent = Files.readAllBytes(filePath);
        
        if (contentType.startsWith("text/") || contentType.equals("application/javascript") || contentType.equals("application/json")) {
            return buildBinaryResponse(200, contentType, fileContent);
        } else {
            // For binary files (images)
            return buildBinaryResponse(200, contentType, fileContent);
        }
    }

private static Path tryResolveFile(String path) {
    // 1. Primero busca en la ruta de desarrollo tradicional
    Path devPath = Paths.get("src/main/resources/static" + path);
    if (Files.exists(devPath)) {
        return devPath;
    }
    
    // 2. Busca en la ruta alternativa donde está tu logo
    Path altPath = Paths.get("src/main/java/com/mycompany/httpserver/resources/static" + path);
    if (Files.exists(altPath)) {
        return altPath;
    }
    
    // 3. Busca en el directorio de compilación
    Path prodPath = Paths.get("target/classes/static" + path);
    if (Files.exists(prodPath)) {
        return prodPath;
    }
    
    // 4. Último recurso: busca en directorio actual
    Path currentPath = Paths.get("static" + path);
    if (Files.exists(currentPath)) {
        return currentPath;
    }
    
    return null;
}

    private static String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            return path.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private static String buildBinaryResponse(int statusCode, String contentType, byte[] content) {
        String statusText = getStatusText(statusCode);
        
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");
        response.append("Content-Type: ").append(contentType).append("\r\n");
        response.append("Content-Length: ").append(content.length).append("\r\n");
        response.append("Connection: close\r\n");
        response.append("\r\n");
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(response.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.write(content);
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            return buildResponse(500, "text/plain", "Error building binary response");
        }
    }

    private static String buildResponse(int statusCode, String contentType, String content) {
        String statusText = getStatusText(statusCode);
        
        return "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
               "Content-Type: " + contentType + "\r\n" +
               "Content-Length: " + content.length() + "\r\n" +
               "Connection: close\r\n" +
               "\r\n" +
               content;
    }

    private static String getStatusText(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            default: return "Unknown Status";
        }
    }
}
