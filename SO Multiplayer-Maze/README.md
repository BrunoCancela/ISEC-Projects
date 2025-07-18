## Multiplayer Maze Game 

### Description
This project was developed as part of the **Operating Systems** course during my Bachelor's degree.

It consisted of building a multiplayer platform in C for Unix systems, where players navigate dynamic mazes with moving and temporary obstacles.

The project was split into client and server components. I was responsible for implementing the **server**, which handled all core game logic: managing the global game state, processing player input, and controlling obstacle behavior.

Shared memory was used to store the global maze state and player positions. Communication between client and server was handled through **named pipes (FIFOs)**, and **Unix signals** were used for asynchronous notifications.

### Technologies Used
- C  
- Shared Memory  
- Named Pipes (FIFOs)  
- Unix Signals
