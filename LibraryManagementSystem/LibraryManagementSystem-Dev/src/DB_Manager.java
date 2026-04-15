
import java.sql.*;
import java.io.*;
import javax.swing.JOptionPane;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author DASH-
 */
public class DB_Manager {
    // MySQL JDBC 드라이버와 연결 URL 정보로 변경
    String strDriver = "com.mysql.cj.jdbc.Driver";
    String strURL = "jdbc:mysql://localhost:3306/LMS?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"; // MySQL의 포트와 데이터베이스명(LMS)을 지정
    String strUser = "root"; // MySQL 사용자 이름
    String strPWD = "rootpw"; // MySQL 비밀번호
    
    Connection DB_con;
    Statement DB_stmt;
    ResultSet DB_rs;
    
public void dbOpen() {
    try {
        // 연결 설정
        DB_con = DriverManager.getConnection(strURL, strUser, strPWD);
        DB_stmt = DB_con.createStatement();
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "DB 연결 중 오류 발생: " + e.getMessage());
    }
}

public void dbClose() {
    try {
        if (DB_rs != null) DB_rs.close();
        if (DB_stmt != null) DB_stmt.close();
        if (DB_con != null) DB_con.close();
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "DB 종료 중 오류 발생: " + e.getMessage());
    }
}

    
    // user_id와 password로 user_pin 가져오는 메서드
//    public int getUserPin(String userId, String password) {
//        int userPin = -1; // 로그인 실패 시 -1 반환
//        try {
//            dbOpen();
//            String sql = "SELECT user_pin, role FROM users WHERE user_id = ? AND password = ?";
//            PreparedStatement pstmt = DB_con.prepareStatement(sql);
//            pstmt.setString(1, userId);
//            pstmt.setString(2, password);
//
//            DB_rs = pstmt.executeQuery();
//            if (DB_rs.next()) {
//                userPin = DB_rs.getInt("user_pin"); // 사용자 식별 번호 가져오기
//            }
//
//            DB_rs.close();
//            dbClose();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return userPin;
//    }
    
    public String[] getUserDetails(String userId, String password) {
        String[] userDetails = new String[2]; // [0]: user_pin, [1]: role
        try {
            dbOpen();
            String sql = "SELECT user_pin, role FROM users WHERE user_id = ? AND password = ?";
            PreparedStatement pstmt = DB_con.prepareStatement(sql);
            pstmt.setString(1, userId);   // userId = txtId를 통해 전달받은 user_id 값
            pstmt.setString(2, password); // password = txtPass를 통해 전달받은 password 값

            DB_rs = pstmt.executeQuery();
            if (DB_rs.next()) {
                userDetails[0] = String.valueOf(DB_rs.getInt("user_pin")); // user_pin
                userDetails[1] = DB_rs.getString("role");                  // role
            }
            DB_rs.close();
            dbClose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userDetails; // user_pin과 role을 반환
    }
}

