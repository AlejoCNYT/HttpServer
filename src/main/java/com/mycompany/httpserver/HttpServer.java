/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.httpserver;

import java.net.*;
import java.io.*;

public class HttpServer {

    public static void main(String[] args) throws IOException, URISyntaxException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(35000);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }
        Socket clientSocket = null;

        boolean running = true;
        while (running) {

            try {
                System.out.println("Listo para recibir ...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));
            String inputLine, outputLine;

            boolean isFirstLine = true;
            URI requesturi = null;

            while ((inputLine = in.readLine()) != null) {
                if (isFirstLine) {
                    requesturi = new URI(inputLine.split(" ")[1]);
                    System.out.println("Path " + requesturi.getPath());
                    isFirstLine = false;
                }
                System.out.println("Received: " + inputLine);
                if (!in.ready()) {
                    break;
                }
            }
            if (requesturi.getPath().startsWith("/app/hello")) {
                outputLine = helloService(requesturi);
            } else {
                outputLine = "HTTP/1.1 200 OK\n\r"
                        + "content-type: text/html\n\r"
                        + "\n\r"
                        + "<!DOCTYPE html>"
                        + "<html>\n"
                        + "<head>\n"
                        + "<title>Form Example</title>\n"
                        + "<meta charset=\"UTF-8\">\n"
                        + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                        + "</head>\n"
                        + "<body>\n"
                        + "<h1>Form with GET</h1>\n"
                        + "<form action=\"/hello\">\n"
                        + "<label for=\"name\">Name:</label><br>\n"
                        + "<input type=\"text\" id=\"name\" name=\"name\" value=\"John\"><br><br>\n"
                        + "<input type=\"button\" value=\"Submit\" onclick=\"loadGetMsg()\">\n"
                        + "</form>\n"
                        + "<div id=\"getrespmsg\"></div>\n"
                        + " \n"
                        + "<script>\n"
                        + "function loadGetMsg() {\n"
                        + "let nameVar = document.getElementById(\"name\").value;\n"
                        + "const xhttp = new XMLHttpRequest();\n"
                        + "xhttp.onload = function() {\n"
                        + "document.getElementById(\"getrespmsg\").innerHTML =\n"
                        + "this.responseText;\n"
                        + "}\n"
                        + "xhttp.open(\"GET\", \"/app/hello?name=\"+nameVar);\n"
                        + "xhttp.send();\n"
                        + "}\n"
                        + "</script>\n"
                        + " \n"
                        + "<h1>Form with POST</h1>\n"
                        + "<form action=\"/hellopost\">\n"
                        + "<label for=\"postname\">Name:</label><br>\n"
                        + "<input type=\"text\" id=\"postname\" name=\"name\" value=\"John\"><br><br>\n"
                        + "<input type=\"button\" value=\"Submit\" onclick=\"loadPostMsg(postname)\">\n"
                        + "</form>\n"
                        + " \n"
                        + "<div id=\"postrespmsg\"></div>\n"
                        + " \n"
                        + "<script>\n"
                        + "function loadPostMsg(name){\n"
                        + "let url = \"/hellopost?name=\" + name.value;\n"
                        + " \n"
                        + "fetch (url, {method: 'POST'})\n"
                        + ".then(x => x.text())\n"
                        + ".then(y => document.getElementById(\"postrespmsg\").innerHTML = y);\n"
                        + "}\n"
                        + "</script>\n"
                        + "</body>\n"
                        + "</html>";
            }
            out.println(outputLine);

            out.close();
            in.close();
            clientSocket.close();
        }
        serverSocket.close();
    }

    private static String helloService(URI requesturi) {
        String response = "HTTP/1.1 200 OK\n\r"
                + "content-type: application/json\n\r"
                + "\n\r"
                + "<!DOCTYPE html>";
        String name = requesturi.getQuery().split("=")[1]; //name=jhon

        response = response + "{\"mensaje\"}: \"Hola" + name + "\"}"  ;
        return response;
    }

}
