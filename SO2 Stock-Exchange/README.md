## Online Stock Exchange System

### Description
This project was developed individually as part of the **Operating Systems II** course during my Bachelor's degree.

It is an online stock exchange system implemented in C#, composed of a central server ("exchange"), multiple clients, and a separate "board" module for real-time market visualization.

Communication between clients and the server was handled through **named pipes**. The server managed user authentication, account registration, stock portfolios, transaction control, and real-time price updates.

The "board" module accessed shared data to display the most valuable stocks in real time.

### Technologies Used
- C#  
- Named Pipes  
- Shared Memory (in the board module)  
- Multithreading
