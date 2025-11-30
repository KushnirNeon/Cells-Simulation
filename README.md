# Cells Simulation (Java Concurrency Project)

This project contains two Java implementations (**Cells0** and **Cells1**) of a simple concurrent simulation.  
A number of *atoms* (threads) randomly walk across a 1-dimensional array of cells. Each cell holds an integer count of how many atoms currently occupy it.

The goal of the project is to demonstrate:
- Multithreading in Java  
- Race conditions  
- Synchronization strategies  
- Performance impact of different locking mechanisms  

## Overview

Each atom starts in cell `0` and repeatedly:

1. Generates a random number.
2. Moves left or right depending on probability `p`.
3. Updates the shared `cells[]` array.
4. Sleeps for 1 ms.
5. Continues until the simulation ends.

Snapshots are printed every second for 60 seconds.  
After the run, the program verifies that the total number of atoms is preserved.

## Project Structure

### **Cells0**
A minimal version **without synchronization**, used to demonstrate race conditions.

Characteristics:
- Concurrent updates without locks  
- Potentially corrupted totals  
- Educational example of data races  

### **Cells1**
A synchronized version with two locking modes:

#### 1. **Array Lock Mode** (`-Dlock=array`)
Uses `synchronized(cells)` to guard all movements.

Pros: simple  
Cons: low concurrency  

#### 2. **Per-Cell Lock Mode** (`-Dlock=cell`)
Each cell has its own lock. Movement locks only the two involved cells (`pos` and `next`), always in sorted order to prevent deadlocks.

Pros: high concurrency  
Cons: slightly more complex  
```sh
javac Cells0.java Cells1.java
