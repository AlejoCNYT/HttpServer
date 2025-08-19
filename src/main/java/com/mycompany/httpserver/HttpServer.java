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
        MIME_TYPES.put("ong", "image/png"); // por si la imagen quedó con extensión .ong (typo)
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

            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream())) {
                
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

                byte[] responseBytes;
                if (requesturi == null) {
                    responseBytes = buildResponse(400, "text/plain", "Bad Request").getBytes(StandardCharsets.UTF_8);
                } else if (requesturi.getPath().startsWith("/app/hello")) {
                    responseBytes = handleHelloRequest(requesturi).getBytes(StandardCharsets.UTF_8);
                } else if (requesturi.getPath().startsWith("/stocks")) {
                    responseBytes = handleStockRequest(requesturi).getBytes(StandardCharsets.UTF_8);
                } else if (requesturi.getPath().startsWith("/static/")) {
                    responseBytes = handleFileResponseBytes(requesturi);
                } else if (requesturi.getPath().equals("/")) {
                    responseBytes = handleFileResponseBytes(new URI("/static/index.html"));
                } else {
                    responseBytes = handleFileResponseBytes(new URI("/static" + requesturi.getPath()));
                }
                dataOut.write(responseBytes);
                dataOut.flush();
            } catch (Exception e) {
                System.err.println("Error handling client: " + e.getMessage());
                try {
                    clientSocket.getOutputStream().write(
                        buildResponse(500, "text/plain", "Internal Server Error").getBytes(StandardCharsets.UTF_8)
                    );
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
                symbol = requesturi.getQuery().split("=")[1];
            }

            String url = String.format(
                "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s",
                symbol, API_KEY
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
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

    // === Nuevo: respuesta de archivos binaria/segura ===
    private static byte[] handleFileResponseBytes(URI requesturi) throws IOException {
        String path = requesturi.getPath().replaceFirst("^/static", "");
        if (path.isEmpty() || path.equals("/")) path = "/index.html";

        path = path.replaceAll("\\.\\.", "").replaceAll("//", "/");

        System.out.println("Buscando archivo en (bytes): " + path);

        Path filePath = tryResolveFile(path);
        if (filePath == null || !Files.exists(filePath)) {
            return buildResponse(404, "text/plain", "File not found: " + path)
                   .getBytes(StandardCharsets.UTF_8);
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            String extension = getFileExtension(path);
            contentType = MIME_TYPES.getOrDefault(extension, "application/octet-stream");
        }

        System.out.println("Sirviendo archivo (bytes): " + filePath + " como " + contentType);
        byte[] fileContent = Files.readAllBytes(filePath);

        return buildBinaryResponseBytes(200, contentType, fileContent);
    }

    private static Path tryResolveFile(String path) {
        // Normaliza la ruta para seguridad
        path = path.replaceAll("\\.\\.", "").replaceAll("//", "/");
        
        // Lista de posibles ubicaciones a verificar
        String[] possibleLocations = {
            "src/main/resources/static",  // Desarrollo
            "target/classes/static",      // Producción después de compilar (mvn package)
            "static",                     // Directorio raíz
            "resources/static"            // Algunos IDEs
        };
        
        for (String location : possibleLocations) {
            Path filePath = Paths.get(location + path);
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                System.out.println("Found file at: " + filePath.toAbsolutePath());
                return filePath;
            }
        }
        
        System.out.println("File not found in any standard location: " + path);
        return null;
    }

    private static String getFileExtension(String path) {
        int dotIndex = path.lastIndexOf('.');
        return (dotIndex == -1) ? "" : path.substring(dotIndex + 1).toLowerCase();
    }

    // --- RESPUESTAS HTTP ---
    private static byte[] buildBinaryResponseBytes(int statusCode, String contentType, byte[] content) {
        String statusText = getStatusText(statusCode);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(("HTTP/1.1 " + statusCode + " " + statusText + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
            outputStream.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
            outputStream.write(("Content-Length: " + content.length + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
            outputStream.write("Connection: close\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            outputStream.write(content);
            return outputStream.toByteArray();
        } catch (IOException e) {
            return buildResponse(500, "text/plain", "Error building binary response: " + e.getMessage())
                   .getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String buildResponse(int statusCode, String contentType, String content) {
        String statusText = getStatusText(statusCode);
        
        return "HTTP/1.1 " + statusCode + " " + statusText + "\r\n"
               + "Content-Type: " + contentType + "\r\n"
               + "Content-Length: " + content.getBytes(StandardCharsets.UTF_8).length + "\r\n"
               + "Connection: close\r\n"
               + "\r\n"
               + content;
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
