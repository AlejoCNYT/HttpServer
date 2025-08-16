/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.httpserver;

import java.net.*;
import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpServer {

    private static final String API_KEY = "Q1QZFVJQ21K7C6XM";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws IOException, URISyntaxException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(35001);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }
        
        System.out.println("Server running on port 35000...");
        System.out.println("Try these endpoints:");
        System.out.println("http://localhost:35000/app/hello?name=John");
        System.out.println("http://localhost:35000/stocks?symbol=fb");
        System.out.println("http://localhost:35000/");

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

                while ((inputLine = in.readLine()) != null) {
                    if (isFirstLine && !inputLine.isEmpty()) {
                        String[] requestParts = inputLine.split(" ");
                        if (requestParts.length > 1) {
                            requesturi = new URI(requestParts[1]);
                            System.out.println("Requested Path: " + requesturi.getPath());
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
                } else {
                    outputLine = getDefaultHTML();
                }

                out.println(outputLine);
            } catch (Exception e) {
                System.err.println("Error handling client: " + e.getMessage());
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

    private static String getDefaultHTML() {
        String html = "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<title>HTTP Server Example</title>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<style>"
                + "body { font-family: Arial, sans-serif; margin: 40px; }"
                + ".container { max-width: 800px; margin: 0 auto; }"
                + "form { margin: 20px 0; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }"
                + "input, button { margin: 5px 0; padding: 8px; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class=\"container\">"
                + "<h1>HTTP Server Demo</h1>"
                
                + "<h2>Hello Service</h2>"
                + "<form onsubmit=\"event.preventDefault(); loadGetMsg()\">"
                + "<label for=\"name\">Name:</label><br>"
                + "<input type=\"text\" id=\"name\" name=\"name\" value=\"John\"><br>"
                + "<button type=\"submit\">Say Hello</button>"
                + "</form>"
                + "<div id=\"getrespmsg\"></div>"
                
                + "<h2>Stock Data Lookup</h2>"
                + "<form onsubmit=\"event.preventDefault(); fetchStockData()\">"
                + "<label for=\"stockSymbol\">Stock Symbol:</label><br>"
                + "<input type=\"text\" id=\"stockSymbol\" value=\"fb\"><br>"
                + "<button type=\"submit\">Get Stock Data</button>"
                + "</form>"
                + "<pre id=\"stockData\"></pre>"
                
                + "<script>"
                + "function loadGetMsg() {"
                + "  const name = document.getElementById('name').value;"
                + "  fetch('/app/hello?name=' + name)"
                + "    .then(response => response.json())"
                + "    .then(data => document.getElementById('getrespmsg').innerHTML = data.message);"
                + "}"
                
                + "function fetchStockData() {"
                + "  const symbol = document.getElementById('stockSymbol').value;"
                + "  fetch('/stocks?symbol=' + symbol)"
                + "    .then(response => response.json())"
                + "    .then(data => document.getElementById('stockData').innerHTML = JSON.stringify(data, null, 2))"
                + "    .catch(err => document.getElementById('stockData').innerHTML = 'Error: ' + err);"
                + "}"
                + "</script>"
                + "</div>"
                + "</body>"
                + "</html>";
        
        return buildResponse(200, "text/html", html);
    }

    private static String buildResponse(int statusCode, String contentType, String content) {
        String statusText = "";
        switch (statusCode) {
            case 200: statusText = "OK"; break;
            case 400: statusText = "Bad Request"; break;
            case 500: statusText = "Internal Server Error"; break;
            case 502: statusText = "Bad Gateway"; break;
        }
        
        return "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
               "Content-Type: " + contentType + "\r\n" +
               "Connection: close\r\n" +
               "\r\n" +
               content;
    }
}
