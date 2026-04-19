# Distributed Collaborative Text Editor (Java RMI)

## Overview
A multi-user collaborative text editor built using Java RMI, supporting concurrent editing with synchronization mechanisms.

## Features
- Multi-client editing via client-server architecture
- Line-level locking to prevent race conditions
- Polling-based synchronization with version control
- Fault tolerance via file replication (primary + backup)
- GUI built with Java Swing

## Tech Stack
- Java
- Java RMI
- Swing (GUI)
- ConcurrentHashMap (locking)

## How to Run

### Start Server
```bash
javac *.java
java DocumentServerImpl
