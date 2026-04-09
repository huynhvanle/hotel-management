CREATE DATABASE IF NOT EXISTS hotel_management_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE hotel_management_db;

CREATE TABLE Hotel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    starLevel INT,
    address VARCHAR(255),
    description VARCHAR(500)
) ENGINE=InnoDB;

CREATE TABLE Room (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255),
    name VARCHAR(255),
    price DECIMAL(19,2),
    description VARCHAR(500),
    hotelID INT,
    CONSTRAINT fk_room_hotel FOREIGN KEY (hotelID) REFERENCES Hotel(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Client (
    id INT AUTO_INCREMENT PRIMARY KEY,
    idCardNumber INT,
    fullName VARCHAR(255),
    address VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(15),
    description VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE User (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    fullName VARCHAR(255),
    position VARCHAR(255),
    mail VARCHAR(255),
    description VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE Service (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    unit VARCHAR(255),
    price DECIMAL(19,2)
) ENGINE=InnoDB;

CREATE TABLE Booking (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bookingDate DATE,
    discount DECIMAL(19,2) DEFAULT 0,
    note VARCHAR(500),
    clientID INT,
    userID INT,
    CONSTRAINT fk_booking_client FOREIGN KEY (clientID) REFERENCES Client(id),
    CONSTRAINT fk_booking_user FOREIGN KEY (userID) REFERENCES User(id)
) ENGINE=InnoDB;

CREATE TABLE BookedRoom (
    id INT AUTO_INCREMENT PRIMARY KEY,
    checkin DATE,
    checkout DATE,
    discount DECIMAL(19,2) DEFAULT 0,
    isCheckedIn INT DEFAULT 0,
    note VARCHAR(500),
    bookingID INT,
    roomID VARCHAR(255),
    CONSTRAINT fk_booked_booking FOREIGN KEY (bookingID) REFERENCES Booking(id) ON DELETE CASCADE,
    CONSTRAINT fk_booked_room FOREIGN KEY (roomID) REFERENCES Room(id)
) ENGINE=InnoDB;

CREATE TABLE UsedService (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quantity INT,
    discount DECIMAL(19,2) DEFAULT 0,
    serviceID INT,
    bookedRoomID INT,
    CONSTRAINT fk_used_service FOREIGN KEY (serviceID) REFERENCES Service(id),
    CONSTRAINT fk_used_bookedroom FOREIGN KEY (bookedRoomID) REFERENCES BookedRoom(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Bill (
    id INT AUTO_INCREMENT PRIMARY KEY,
    paymentDate DATE,
    paymentAmount DECIMAL(19,2),
    paymentType INT,
    note VARCHAR(500),
    bookingID INT,
    userID INT,
    CONSTRAINT fk_bill_booking FOREIGN KEY (bookingID) REFERENCES Booking(id),
    CONSTRAINT fk_bill_user FOREIGN KEY (userID) REFERENCES User(id)
) ENGINE=InnoDB;
