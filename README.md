# Indoor Court Booking System (Java Swing)

The Indoor Court Booking System is a simple GUI-based desktop application developed using Core Java and Swing. The system allows users to book indoor sports courts by selecting a date, court, and available time slot. It provides real-time availability updates, validates user input, and stores booking data locally using file handling.

## Features

* User-friendly graphical interface using Java Swing
* Multiple court selection (Badminton, Basketball, Tennis, Futsal)
* Real-time slot availability tracking
* Built-in calendar for date selection
* Form validation for all required fields
* Booking dashboard with live statistics
* Cancel selected booking option
* Clear all bookings option
* Persistent storage using CSV file

## Technologies Used

* Core Java
* Java Swing and AWT
* File Handling (CSV-based storage)

## How the System Works

1. The user selects a date using the calendar dialog.
2. A court is chosen from the dropdown list.
3. Available time slots are displayed dynamically.
4. The user enters required details such as name, register number, email, and phone number.
5. After confirmation, the booking is saved and reflected in the dashboard instantly.

The system automatically prevents duplicate bookings and ensures slots cannot be booked in the past.

## Dashboard Overview

The dashboard displays:

* Total bookings for the selected court
* Today’s bookings
* Number of available slots

Users can also cancel individual bookings or clear all bookings.

## Project Structure

IndoorCourtBookingSwing.java — Main application source code
court_bookings.csv — Automatically generated file for storing bookings

## How to Run

Step 1: Compile the program
javac IndoorCourtBookingSwing.java

Step 2: Run the program
java IndoorCourtBookingSwing

## Data Storage

All booking records are stored locally in a CSV file named:

court_bookings.csv

This ensures bookings remain saved even after closing the application.

## Learning Outcomes

This project demonstrates practical implementation of:

* Java Swing GUI design
* Event handling
* File handling
* Input validation
* Object-oriented programming concepts

## Author

Sharon M Varghese

## Note

This project was developed as a basic academic application for learning Core Java concepts and GUI development.
