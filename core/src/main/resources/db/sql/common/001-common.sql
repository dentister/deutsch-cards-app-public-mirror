CREATE SEQUENCE security_seq
    START WITH 1000
    INCREMENT BY 1
    CACHE 1;
    
CREATE TABLE groups (
    id bigint DEFAULT nextval('security_seq') NOT NULL PRIMARY KEY,
    group_name varchar(100) NOT NULL
);

CREATE TABLE users (
    id bigint DEFAULT nextval('security_seq') NOT NULL PRIMARY KEY,
    username varchar(50) NOT NULL,
    password varchar(100) NOT NULL,
    enabled boolean NOT NULL
);