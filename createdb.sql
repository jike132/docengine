-- create database sales character set utf8;
CREATE DATABASE `sales`;
USE `sales`;

DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `pid` int unsigned AUTO_INCREMENT PRIMARY KEY,    -- 商品id
  `pname` varchar(50) , -- 商品名称 
  `price` double, -- 商品价格
  `pdesc` varchar(255), -- 商品描述
  `pflag` int(11) -- 商品状态 1 上架 ,0 下架
)ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('小米6',2200,'小米 移动联通电信4G手机 双卡双待',0);
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('华为Mate9',2599,'华为 双卡双待 高清大屏',0);
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('OPPO11',3000,'移动联通 双4G手机',1);
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('华为荣耀',1499,'3GB内存标准版 黑色 移动4G手机',1);
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('华硕台式电脑',5000,'爆款直降，满千减百',1);
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('MacBook',6688,'128GB 闪存',1);
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('ThinkPad',4199,'轻薄系列1)',1);
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('联想小新',4499,'14英寸超薄笔记本电脑',1);
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('李宁音速6',500,'实战篮球鞋',0);
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('AJ11',3300,'乔丹实战系列',0);
INSERT INTO `product`(`pname`,`price`,`pdesc`,`pflag`) VALUES('AJ1',5800,'精神小伙系列',1);

DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `oid` int unsigned AUTO_INCREMENT PRIMARY KEY, -- 订单id
  `ordertime` datetime ,    -- 下单时间 
  `total` double , -- 总金额
  `count` int unsigned,  -- 数量
  `name` varchar(20), -- 收货人姓名
  `telephone` varchar(20) , -- 电话
  `address` varchar(30) , -- 地址
  `state` int(11),  -- 订单状态
  `s_pid` int unsigned not null,  -- 关联商品表
  FOREIGN KEY(s_pid) REFERENCES product(pid)
)ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;
INSERT INTO `orders`(`ordertime`,`total`,`count`,`name`,`telephone`,`address`,`state`,`s_pid`) VALUES('2019-10-11',4100,2,'渣渣辉','15512342345','AI创新中心A区',2,6);
INSERT INTO `orders`(`ordertime`,`total`,`count`,`name`,`telephone`,`address`,`state`,`s_pid`) VALUES('2020-10-11',3100,1,'药水哥','13533334444','AI创新中心B区',2,2);
INSERT INTO `orders`(`ordertime`,`total`,`count`,`name`,`telephone`,`address`,`state`,`s_pid`) VALUES('2021-10-11',1100,2,'大明白','13544445555','AI创新中心C区',2,3);
INSERT INTO `orders`(`ordertime`,`total`,`count`,`name`,`telephone`,`address`,`state`,`s_pid`) VALUES('2021-10-11',4160,1,'长海','13566667777','AI创新中心D区',2,4);
INSERT INTO `orders`(`ordertime`,`total`,`count`,`name`,`telephone`,`address`,`state`,`s_pid`) VALUES('2022-10-11',700,1,'乔杉','13588889999','AI创新中心A区',2,5);