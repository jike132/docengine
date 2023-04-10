package com.nsfocus.reportengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

import java.sql.*;


@SpringBootApplication( exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class } )

public class ReportengineApplication {

    public static void main(String[] args) {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            Class.forName("org.postgresql.Driver");
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        //方便测试添加数据
        //operate database
        String url = "jdbc:mysql://10.66.21.151/sales?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "Nsf0cus.";
        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            Statement statement = connection.createStatement();
            int num = statement.executeUpdate("INSERT INTO orders(ordertime,total,count,name,telephone,address,state,s_pid) VALUES('2022-11-14 17:25:00',4100,2,'渣渣辉','15512342345','AI创新中心A区',2,6)");
            num = statement.executeUpdate("INSERT INTO orders(ordertime,total,count,name,telephone,address,state,s_pid) VALUES('2022-11-14 17:25:00',4100,2,'渣渣辉','15512342345','AI创新中心A区',2,6)");
            num = statement.executeUpdate("INSERT INTO orders(ordertime,total,count,name,telephone,address,state,s_pid) VALUES('2022-11-14 17:26:00',4100,2,'渣渣辉','15512342345','AI创新中心A区',2,6)");
            num = statement.executeUpdate("INSERT INTO orders(ordertime,total,count,name,telephone,address,state,s_pid) VALUES('2022-11-14 17:27:00',4100,2,'渣渣辉','15512342345','AI创新中心A区',2,6)");
            num = statement.executeUpdate("INSERT INTO orders(ordertime,total,count,name,telephone,address,state,s_pid) VALUES('2022-11-14 17:28:00',4100,2,'渣渣辉','15512342345','AI创新中心A区',2,6)");
            num = statement.executeUpdate("INSERT INTO orders(ordertime,total,count,name,telephone,address,state,s_pid) VALUES('2022-11-14 17:29:00',4100,2,'渣渣辉','15512342345','AI创新中心A区',2,6)");
            num = statement.executeUpdate("INSERT INTO orders(ordertime,total,count,name,telephone,address,state,s_pid) VALUES('2022-11-24 17:29:00',4100,2,'渣渣辉','15512342345','AI创新中心A区',2,6)");
            num = statement.executeUpdate("INSERT INTO orders(ordertime,total,count,name,telephone,address,state,s_pid) VALUES('2022-11-24 17:29:00',4100,2,'渣渣辉','15512342345','AI创新中心A区',2,6)");
        }catch(SQLException e){
            e.printStackTrace();
        }

        SpringApplication.run(ReportengineApplication.class, args);
    }

}
