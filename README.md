# Distributed Matrix Multiplication System Using Java RMI

## Overview

This project implements a distributed matrix multiplication system using Java Remote Method Invocation (RMI). It follows a master-worker architecture where computational tasks are distributed across multiple worker nodes to improve performance and demonstrate concepts of distributed computing.

## Features

* Distributed matrix multiplication
* Java RMI-based communication
* Master-worker architecture
* Concurrent task processing
* Fault tolerance and worker monitoring
* Dynamic task distribution

## Technologies Used

* Java
* Java RMI
* Multithreading
* Distributed Systems Concepts

## Project Structure

* **Master Node:** Distributes matrix multiplication tasks.
* **Worker Nodes:** Perform computations on assigned matrix segments.
* **Shared Interfaces:** Define remote communication methods.

## How to Run

1. Clone the repository.
2. Compile all Java files.
3. Start the RMI registry.
4. Launch the worker nodes.
5. Start the master node to begin matrix multiplication.

## Learning Objectives

This project demonstrates:

* Remote Method Invocation (RMI)
* Distributed computing principles
* Parallel processing
* Fault tolerance mechanisms
* Client-server communication in Java

## Author

Developed by Isaiah as part of a distributed systems learning project.
# matrix_distributed_system
A distributed matrix multiplication system built in Java using Remote Method Invocation (RMI). The project implements a master-worker architecture with task distribution, worker health monitoring, fault tolerance, and concurrent processing for efficient distributed computation.
