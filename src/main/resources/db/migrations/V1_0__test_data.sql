CREATE TABLE `customer` (
    `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    `active` tinyint(1) unsigned NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`)
);

INSERT INTO `customer` VALUES
(1,'Big News Media Corp',1),
(2,'Online Mega Store',1),
(3,'Nachoroo Delivery',0),
(4,'Euro Telecom Group',1);


CREATE TABLE `ip_blacklist` (
    `address` bigint(11) unsigned NOT NULL,
    `netmask_bits` int(11) NOT NULL,
    PRIMARY KEY (`address`, `netmask_bits`)
);

INSERT INTO `ip_blacklist` VALUES
(0, 32), (2130706433, 16), (4294967295, 32);

CREATE TABLE `ua_blacklist` (
    `ua` varchar(255) NOT NULL,
    PRIMARY KEY (`ua`)
);

INSERT INTO `ua_blacklist` VALUES
('A6-Indexer'), ('Googlebot-News'), ('Googlebot');


CREATE TABLE `hourly_stats` (
    `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
    `customer_id` int(11) unsigned NOT NULL,
    `time` timestamp NOT NULL,
    `request_count` bigint(20) unsigned NOT NULL DEFAULT '0',
    `invalid_count` bigint(20) unsigned NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_customer_time` (`customer_id`,`time`),
    KEY `customer_idx` (`customer_id`),
    CONSTRAINT `hourly_stats_customer_id` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
);