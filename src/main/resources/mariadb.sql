
CREATE database qiuqiu;
use qiuqiu;

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for account
-- ----------------------------
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) DEFAULT NULL,
  `balance` int(11) DEFAULT NULL,
  `row_update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of account
-- ----------------------------
INSERT INTO `account` VALUES ('1', 'user001', '1', '2016-12-27 17:31:15');
INSERT INTO `account` VALUES ('2', 'user002', '1', '2016-12-27 17:31:16');
INSERT INTO `account` VALUES ('3', 'user003', '1', '2016-12-27 17:31:16');
INSERT INTO `account` VALUES ('4', 'user004', '1', '2016-12-27 17:31:20');
INSERT INTO `account` VALUES ('5', 'user005', '1', '2016-12-27 17:31:21');
INSERT INTO `account` VALUES ('6', 'user006', '1', '2016-12-27 17:31:21');
INSERT INTO `account` VALUES ('7', 'user007', '1', '2016-12-27 17:31:26');
INSERT INTO `account` VALUES ('8', 'user008', '1', '2016-12-27 17:31:26');
INSERT INTO `account` VALUES ('9', 'user009', '1', '2016-12-27 17:31:26');
INSERT INTO `account` VALUES ('10', 'user010', '1', '2016-12-27 17:31:25');
INSERT INTO `account` VALUES ('11', 'user011', '1', '2016-12-27 17:31:25');
INSERT INTO `account` VALUES ('12', 'user012', '1', '2016-12-27 17:31:24');
INSERT INTO `account` VALUES ('13', 'user013', '1', '2016-12-27 17:31:24');
INSERT INTO `account` VALUES ('14', 'user014', '1', '2016-12-27 17:31:24');
INSERT INTO `account` VALUES ('15', 'user015', '1', '2016-12-27 17:31:19');
INSERT INTO `account` VALUES ('16', 'user016', '1', '2016-12-27 17:31:18');
INSERT INTO `account` VALUES ('17', 'user017', '1', '2016-12-27 17:31:14');
INSERT INTO `account` VALUES ('18', 'user018', '1', '2016-12-27 17:31:14');
INSERT INTO `account` VALUES ('19', 'user019', '1', '2016-12-27 17:31:14');
INSERT INTO `account` VALUES ('20', 'user020', '1', '2016-12-27 17:31:13');
