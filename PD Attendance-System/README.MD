## Distributed Attendance Registration System

### Description
This group project was developed as part of the **Distributed Programming** course and was divided into two main phases.

**Phase 1:**  
A distributed system was implemented with a **main server**, a **backup server**, and multiple clients. The main server handled all business logic, including user registration and authentication, event creation and management, attendance code generation, and CSV report export.

The backup server maintained a synchronized replica of the SQLite database, updated via **RMI callbacks**.  
Clients communicated with the main server through **TCP sockets**, including support for **asynchronous notifications** to update user interfaces in real time.

**Phase 2:**  
A **REST API** was integrated into the main server using **Spring Boot**, allowing clients to communicate over HTTP. REST controllers were developed, **JWT-based authentication** was implemented, and the client application was adapted to consume all services via the new API.

### Technologies Used
- Java  
- TCP Sockets  
- RMI  
- SQLite  
- Spring Boot  
- JWT (JSON Web Tokens)
