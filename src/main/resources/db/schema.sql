CREATE DATABASE IF NOT EXISTS hotel_management_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE hotel_management_db;

CREATE TABLE User (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE, 
    password VARCHAR(255),
    fullName VARCHAR(255) NOT NULL,
    idCardNumber VARCHAR(20) UNIQUE,
    address VARCHAR(255),
    mail VARCHAR(255),
    phone VARCHAR(15),
    role ENUM('ADMIN', 'RECEPTIONIST', 'MANAGER', 'CLIENT') DEFAULT 'CLIENT',
    description VARCHAR(255),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE Hotel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    starLevel INT,
    address VARCHAR(255),
    description VARCHAR(500)
) ENGINE=InnoDB;

CREATE TABLE Room (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(255),
    name VARCHAR(255),
    price DECIMAL(19,2),
    description VARCHAR(500),
    status ENUM('AVAILABLE', 'OCCUPIED', 'CLEANING', 'MAINTENANCE') DEFAULT 'AVAILABLE',
    hotelID INT,
    CONSTRAINT fk_room_hotel FOREIGN KEY (hotelID) REFERENCES Hotel(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Service (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    unit VARCHAR(255),
    price DECIMAL(19,2),
    hotelID INT, 
    CONSTRAINT fk_service_hotel FOREIGN KEY (hotelID) REFERENCES Hotel(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Booking (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bookingDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    discount DECIMAL(19,2) DEFAULT 0,
    note VARCHAR(500),
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED') DEFAULT 'PENDING',
    clientID INT NOT NULL, 
    userID INT,   
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_client FOREIGN KEY (clientID) REFERENCES User(id),
    CONSTRAINT fk_booking_staff FOREIGN KEY (userID) REFERENCES User(id)
) ENGINE=InnoDB;

CREATE TABLE BookedRoom (
    id INT AUTO_INCREMENT PRIMARY KEY,
    checkin DATE,
    checkout DATE,
    discount DECIMAL(19,2) DEFAULT 0,
    isCheckIn TINYINT DEFAULT 0,
    note VARCHAR(500),
    bookingID INT,
    roomID INT,
    CONSTRAINT fk_booked_booking FOREIGN KEY (bookingID) REFERENCES Booking(id) ON DELETE CASCADE,
    CONSTRAINT fk_booked_room FOREIGN KEY (roomID) REFERENCES Room(id)
) ENGINE=InnoDB;

CREATE TABLE UsedService (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quantity INT,
    unitPrice DECIMAL(19,2), 
    serviceID INT,
    bookedRoomID INT,
    usedTime DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_used_service FOREIGN KEY (serviceID) REFERENCES Service(id),
    CONSTRAINT fk_used_bookedroom FOREIGN KEY (bookedRoomID) REFERENCES BookedRoom(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Bill (
    id INT AUTO_INCREMENT PRIMARY KEY,
    paymentDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    paymentAmount DECIMAL(19,2),
    paymentType ENUM('CASH', 'CARD', 'TRANSFER') DEFAULT 'CASH',
    note VARCHAR(500),
    bookingID INT,
    userID INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bill_booking FOREIGN KEY (bookingID) REFERENCES Booking(id),
    CONSTRAINT fk_bill_staff FOREIGN KEY (userID) REFERENCES User(id)
) ENGINE=InnoDB;