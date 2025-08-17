# 🌐 Servidor Web en Java — Taller de Arquitectura de Aplicaciones Distribuidas

En este taller se explora la **arquitectura de las aplicaciones distribuidas**, con especial enfoque en **servidores web** y el **protocolo HTTP** sobre el que están soportados.

## 📋 Descripción del reto

Se implementó un **servidor web en Java puro** (sin frameworks como Spark o Spring) que:
- Soporta **múltiples solicitudes seguidas** (no concurrentes).
- Lee archivos desde el **disco local** y retorna:
  - Páginas HTML
  - Archivos JavaScript
  - Archivos CSS
  - Imágenes (PNG, JPG, etc.)
- Incluye una **aplicación web de prueba** con HTML, CSS y JavaScript.
- Implementa **comunicación asíncrona** con servicios REST en el backend.

---

## 📂 Estructura del proyecto

```
src/
└── main/
    ├── java/
    │   └── com/mycompany/httpserver/
    │       ├── HttpServer.java
    │       ├── URLReader.java
    │       ├── HttpConnectionExample.java
    │       ├── EchoClient.java
    │       └── EchoServer.java
    └── resources/
        └── static/
            ├── index.html
            ├── css/
                ├── styles.css            
            ├── js/
                ├── app.js
            └── images/
                └── logo.png
```

---

## ⚙️ Instalación

### Requisitos previos
- **Java 8+** (recomendado Java 17 o superior)
- **Apache Maven** (para compilar y ejecutar)
- Editor recomendado: Apache NetBeans o IntelliJ IDEA

### Clonar el repositorio
```bash
git clone https://github.com/usuario/HttpServer.git
cd HttpServer
```

### Compilar
```bash
mvn clean package
```

---

## ▶️ Ejecución

Desde la carpeta raíz del proyecto:
```bash
java -cp target/classes com.mycompany.httpserver.HttpServer
```

Por defecto el servidor escucha en:
```
http://localhost:35000
```

Archivos estáticos disponibles:
```
http://localhost:35000/static/index.html

http://localhost:35000/static/css/styles.css

http://localhost:35000/static/js/app.js

http://localhost:35000/static/images/logo.png

```

---

## 🏗 Arquitectura del prototipo

- **Servidor HTTP propio** en Java:
  - Usa `ServerSocket` y `Socket` para gestionar conexiones.
  - Lee peticiones HTTP, parsea la ruta y devuelve el archivo solicitado.
  - Soporta rutas `/static/` para archivos estáticos.
  - Soporta endpoints REST `/hello` y `/hellopost`.

- **Aplicación web frontend**:
  - HTML + CSS para interfaz.
  - JavaScript para lógica y comunicación con backend.
  - Ejemplo de llamadas asíncronas usando:
    - `XMLHttpRequest` (GET)
    - `fetch` (POST)

---

## 🔌 Ejemplo de comunicación asíncrona

**GET desde el cliente JS:**
```javascript
function loadGetMsg() {
    let nameVar = document.getElementById("name").value;
    const xhttp = new XMLHttpRequest();
    xhttp.onload = function() {
        document.getElementById("getrespmsg").innerHTML = this.responseText;
    }
    xhttp.open("GET", "/hello?name=" + nameVar);
    xhttp.send();
}
```

**POST desde el cliente JS:**
```javascript
function loadPostMsg(name){
    let url = "/hellopost?name=" + name.value;
    fetch(url, {method: 'POST'})
    .then(x => x.text())
    .then(y => document.getElementById("postrespmsg").innerHTML = y);
}
```

---

## ✅ Evaluación y pruebas realizadas

Se probaron:
- **Carga de página principal**: `index.html` carga correctamente con estilos y JS.
- **Carga de recursos estáticos**: CSS, JS e imágenes se sirven sin problemas.
- **Solicitudes GET/POST**: Endpoints `/hello` y `/hellopost` responden según parámetros.
- **Pruebas con imágenes**: Acceso a `/static/images/logo.png` y otras imágenes añadidas.
- **Resistencia a solicitudes seguidas**: Se enviaron múltiples peticiones desde el navegador y `curl` para validar que no haya caídas.

---

## 📜 Licencia
Este proyecto se distribuye bajo licencia MIT. Consulta el archivo `LICENSE` para más información.
