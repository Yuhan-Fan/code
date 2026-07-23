DROP TABLE IF EXISTS Listings;
DROP TABLE IF EXISTS Block;
DROP TABLE IF EXISTS Section_pricetier;
DROP TABLE IF EXISTS Review;
DROP TABLE IF EXISTS Feature;
DROP TABLE IF EXISTS General_tickets;
DROP TABLE IF EXISTS Reserved_tickets;
DROP TABLE IF EXISTS Tickets;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Seats;
DROP TABLE IF EXISTS Rows;
DROP TABLE IF EXISTS General_sections;
DROP TABLE IF EXISTS Reserved_sections;
DROP TABLE IF EXISTS Sections;
DROP TABLE IF EXISTS Price_tiers;
DROP TABLE IF EXISTS Performances;
DROP TABLE IF EXISTS Events;
DROP TABLE IF EXISTS Venues;
DROP TABLE IF EXISTS Genres;
DROP TABLE IF EXISTS Organizers;
DROP TABLE IF EXISTS Customers;
DROP TABLE IF EXISTS Segments;
DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS Artists;

CREATE TABLE Artists (
    aid            INT              AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255)     NOT NULL
);

CREATE TABLE Users (
    uid            INT              AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255)     NOT NULL,
    address        VARCHAR(255)     NOT NULL,
    email          VARCHAR(255)     NOT NULL,
    date_of_birth DATE              NOT NULL
);

CREATE TABLE Segments (
    segment        VARCHAR(255)     PRIMARY KEY
);

CREATE TABLE Customers (
    uid            INT              PRIMARY KEY,
    payment_card   VARCHAR(16)      NOT NULL,
    FOREIGN KEY (uid) REFERENCES Users(uid)
        ON UPDATE CASCADE
);

CREATE TABLE Organizers (
    uid            INT              PRIMARY KEY,
    FOREIGN KEY (uid) REFERENCES Users(uid)
        ON UPDATE CASCADE
);

CREATE TABLE Genres (
    genre          VARCHAR(255)     PRIMARY KEY,
    segment        VARCHAR(255)     NOT NULL,
    FOREIGN KEY (segment) REFERENCES Segments(segment)
        ON UPDATE CASCADE
);

CREATE TABLE Venues (
    name           VARCHAR(255)     PRIMARY KEY,
    latitude       DECIMAL(9, 6)    NOT NULL,
    longitude      DECIMAL(9, 6)    NOT NULL,
    postal_code    VARCHAR(6)      NOT NULL,
    city           VARCHAR(255)     NOT NULL,
    country        VARCHAR(255)     NOT NULL
);

CREATE TABLE Events (
    eid            INT              AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255)     NOT NULL,
    resale_cap     DECIMAL(5, 2)    NOT NULL,
    uid            INT              NOT NULL,
    genre          VARCHAR(255)     NOT NULL,
    FOREIGN KEY (uid) REFERENCES Organizers(uid)
        ON UPDATE CASCADE,
    FOREIGN KEY (genre) REFERENCES Genres(genre)
        ON UPDATE CASCADE
);

CREATE TABLE Performances (
    pid            INT              AUTO_INCREMENT PRIMARY KEY,
    eid            INT              NOT NULL,
    cancelled      BOOLEAN          NOT NULL DEFAULT FALSE,
    datetime       DATETIME         NOT NULL,
    venue_name     VARCHAR(255)     NOT NULL,
    FOREIGN KEY (eid) REFERENCES Events(eid)
        ON UPDATE CASCADE,
    FOREIGN KEY (venue_name) REFERENCES Venues(name)
        ON UPDATE CASCADE
);

CREATE TABLE Price_tiers (
    name           VARCHAR(255)     NOT NULL,
    pid            INT              NOT NULL,
    price          DECIMAL(10, 2)   NOT NULL,
    PRIMARY KEY (name, pid),
    FOREIGN KEY (pid) REFERENCES Performances(pid)
        ON UPDATE CASCADE
);

CREATE TABLE Sections (
    name           VARCHAR(255)     NOT NULL,
    venue_name     VARCHAR(255)     NOT NULL,
    PRIMARY KEY (name, venue_name),
    FOREIGN KEY (venue_name) REFERENCES Venues(name)
        ON UPDATE CASCADE
);

CREATE TABLE Reserved_sections (
    name           VARCHAR(255)     NOT NULL,
    venue_name     VARCHAR(255)     NOT NULL,
    PRIMARY KEY (name, venue_name),
    FOREIGN KEY (name, venue_name) REFERENCES Sections(name, venue_name)
        ON UPDATE CASCADE
);

CREATE TABLE General_sections (
    name           VARCHAR(255)     NOT NULL,
    venue_name     VARCHAR(255)     NOT NULL,
    total_capacity INT             NOT NULL,
    PRIMARY KEY (name, venue_name),
    FOREIGN KEY (name, venue_name) REFERENCES Sections(name, venue_name)
        ON UPDATE CASCADE
);

CREATE TABLE Rows (
    row            INT              NOT NULL,
    section_name   VARCHAR(255)     NOT NULL,
    venue_name     VARCHAR(255)     NOT NULL,
    PRIMARY KEY (row, section_name, venue_name),
    FOREIGN KEY (section_name, venue_name) REFERENCES Reserved_sections(name, venue_name)
        ON UPDATE CASCADE
);

CREATE TABLE Seats (
    seat           INT              NOT NULL,
    row            INT              NOT NULL,
    section_name   VARCHAR(255)     NOT NULL,
    venue_name     VARCHAR(255)     NOT NULL,
    PRIMARY KEY (seat, row, section_name, venue_name),
    FOREIGN KEY (row, section_name, venue_name) REFERENCES Rows(row, section_name, venue_name)
        ON UPDATE CASCADE
);

CREATE TABLE Orders (
    oid            INT              PRIMARY KEY,
    uid            INT              NOT NULL,
    pid            INT              NOT NULL,
    datetime       DATETIME         NOT NULL,
    FOREIGN KEY (uid) REFERENCES Customers(uid)
        ON UPDATE CASCADE,
    FOREIGN KEY (pid) REFERENCES Performances(pid)
        ON UPDATE CASCADE
);

CREATE TABLE Tickets (
    tid            INT              AUTO_INCREMENT PRIMARY KEY,
    face_value     DECIMAL(10, 2)   NOT NULL,
    cancel_datetime DATETIME,
    oid            INT              NOT NULL,
    FOREIGN KEY (oid) REFERENCES Orders(oid)
        ON UPDATE CASCADE
);

CREATE TABLE Reserved_tickets (
    tid            INT              PRIMARY KEY,
    seat           INT              NOT NULL,
    row            INT              NOT NULL,
    section_name   VARCHAR(255)     NOT NULL,
    venue_name     VARCHAR(255)     NOT NULL,
    FOREIGN KEY (tid) REFERENCES Tickets(tid)
        ON UPDATE CASCADE,
    FOREIGN KEY (seat, row, section_name, venue_name) REFERENCES Seats(seat, row, section_name, venue_name)
        ON UPDATE CASCADE
);

CREATE TABLE General_tickets (
    tid            INT              PRIMARY KEY,
    section_name   VARCHAR(255)     NOT NULL,
    venue_name     VARCHAR(255)     NOT NULL,
    FOREIGN KEY (tid) REFERENCES Tickets(tid)
        ON UPDATE CASCADE,
    FOREIGN KEY (section_name, venue_name) REFERENCES General_sections(name, venue_name)
        ON UPDATE CASCADE
);

CREATE TABLE Feature (
    aid            INT              NOT NULL,
    eid            INT              NOT NULL,
    billing_order  VARCHAR(255)     NOT NULL,
    PRIMARY KEY (aid, eid),
    FOREIGN KEY (aid) REFERENCES Artists(aid)
        ON UPDATE CASCADE,
    FOREIGN KEY (eid) REFERENCES Events(eid)
        ON UPDATE CASCADE
);

CREATE TABLE Review (
    uid            INT              NOT NULL,
    pid            INT              NOT NULL,
    comment        TEXT             NOT NULL,
    event_score    INT              NOT NULL,
    venue_score    INT              NOT NULL,
    PRIMARY KEY (uid, pid),
    FOREIGN KEY (uid) REFERENCES Customers(uid)
        ON UPDATE CASCADE,
    FOREIGN KEY (pid) REFERENCES Performances(pid)
        ON UPDATE CASCADE,
    CHECK (event_score >= 1 AND event_score <= 5),
    CHECK (venue_score >= 1 AND venue_score <= 5)
);

CREATE TABLE Section_pricetier (
    section_name   VARCHAR(255)     NOT NULL,
    pid            INT              NOT NULL,
    pricetier_name VARCHAR(255)     NOT NULL,
    PRIMARY KEY (section_name, pid),
    FOREIGN KEY (pricetier_name, pid) REFERENCES Price_tiers(name, pid)
        ON UPDATE CASCADE
);

CREATE TABLE Block (
    pid            INT              NOT NULL,
    seat           INT              NOT NULL,
    row            INT              NOT NULL,
    section_name   VARCHAR(255)     NOT NULL,
    PRIMARY KEY (pid, seat, row, section_name),
    FOREIGN KEY (pid) REFERENCES Performances(pid)
        ON UPDATE CASCADE
);

CREATE TABLE Listings (
    lid            INT              AUTO_INCREMENT PRIMARY KEY,
    tid            INT              NOT NULL,
    seller_id      INT              NOT NULL,
    list_datetime  DATETIME         NOT NULL,
    price          DECIMAL(10, 2)   NOT NULL,
    withdraw_datetime DATETIME,
    buyer_id       INT,
    trans_datetime DATETIME,
    FOREIGN KEY (tid) REFERENCES Tickets(tid)
        ON UPDATE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES Customers(uid)
        ON UPDATE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES Customers(uid)
        ON UPDATE CASCADE,
    CHECK (
        (buyer_id IS NOT NULL AND trans_datetime IS NOT NULL AND withdraw_datetime IS NULL)
        OR (buyer_id IS NULL AND trans_datetime IS NULL)
    )
);