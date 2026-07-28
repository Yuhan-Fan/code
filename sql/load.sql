LOAD DATA LOCAL INFILE 'data/Artists.txt'
INTO TABLE Artists
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(aid, name);


LOAD DATA LOCAL INFILE 'data/Users.txt'
INTO TABLE Users
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(uid, name, address, email, date_of_birth, deleted);


LOAD DATA LOCAL INFILE 'data/Segments.txt'
INTO TABLE Segments
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(segment);


LOAD DATA LOCAL INFILE 'data/Customers.txt'
INTO TABLE Customers
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(uid, payment_card);


LOAD DATA LOCAL INFILE 'data/Organizers.txt'
INTO TABLE Organizers
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(uid);


LOAD DATA LOCAL INFILE 'data/Genres.txt'
INTO TABLE Genres
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(genre, segment);


LOAD DATA LOCAL INFILE 'data/Venues.txt'
INTO TABLE Venues
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(name, latitude, longitude, postal_code, city, country);


LOAD DATA LOCAL INFILE 'data/Events.txt'
INTO TABLE Events
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(eid, name, resale_cap, uid, genre, segment);


LOAD DATA LOCAL INFILE 'data/Performances.txt'
INTO TABLE Performances
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(pid, eid, cancel_datetime, datetime, venue_name);


LOAD DATA LOCAL INFILE 'data/Price_tiers.txt'
INTO TABLE Price_tiers
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(name, pid, price);


LOAD DATA LOCAL INFILE 'data/Sections.txt'
INTO TABLE Sections
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(name, venue_name);


LOAD DATA LOCAL INFILE 'data/Reserved_sections.txt'
INTO TABLE Reserved_sections
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(name, venue_name);


LOAD DATA LOCAL INFILE 'data/General_sections.txt'
INTO TABLE General_sections
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(name, venue_name, total_capacity);


LOAD DATA LOCAL INFILE 'data/Rows.txt'
INTO TABLE `Rows`
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(`row`, section_name, venue_name);


LOAD DATA LOCAL INFILE 'data/Seats.txt'
INTO TABLE Seats
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(seat, `row`, section_name, venue_name);


LOAD DATA LOCAL INFILE 'data/Orders.txt'
INTO TABLE Orders
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(oid, uid, pid, datetime);


LOAD DATA LOCAL INFILE 'data/Tickets.txt'
INTO TABLE Tickets
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(tid, face_value, cancel_datetime, oid);


LOAD DATA LOCAL INFILE 'data/Reserved_tickets.txt'
INTO TABLE Reserved_tickets
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(tid, seat, `row`, section_name, venue_name);


LOAD DATA LOCAL INFILE 'data/General_tickets.txt'
INTO TABLE General_tickets
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(tid, section_name, venue_name);


LOAD DATA LOCAL INFILE 'data/Feature.txt'
INTO TABLE Feature
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(aid, eid, billing_order);


LOAD DATA LOCAL INFILE 'data/Review.txt'
INTO TABLE Review
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(uid, pid, comment, event_score, venue_score);


LOAD DATA LOCAL INFILE 'data/Section_pricetier.txt'
INTO TABLE Section_pricetier
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(section_name, pid, pricetier_name);


LOAD DATA LOCAL INFILE 'data/Block.txt'
INTO TABLE Block
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(pid, seat, `row`, section_name);


LOAD DATA LOCAL INFILE 'data/Listings.txt'
INTO TABLE Listings
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(lid, tid, seller_id, list_datetime, price, withdraw_datetime, buyer_id, trans_datetime);