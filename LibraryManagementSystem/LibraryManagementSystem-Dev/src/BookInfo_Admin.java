import java.awt.Dimension;
import java.awt.Image;
import javax.swing.ImageIcon;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author pc
 */
public class BookInfo_Admin extends javax.swing.JFrame {
    DB_Manager db = new DB_Manager();
    private final int userPin;
    /**
     * Creates new form BookInfo_Admin
     */
    public BookInfo_Admin(int userPin) {
        // 에러메세지 한글변환
        try { System.setOut(new java.io.PrintStream(System.out, true, "UTF-8")); } 
        catch (java.io.UnsupportedEncodingException ex) { ex.printStackTrace(); }
        this.userPin = userPin; // 로그인한 사용자의 식별 번호 저장
        
        initComponents();
        loadBookData(); // 도서 데이터 로드
        loadUserData(); // 회원 데이터 로드
        loadLoanData(); // 대여 데이터 로드
        enableTableSorting(); // 데이터 속성별 정렬
    }
    
    private void loadBookData() {
        try {
            db.dbOpen(); // DB 연결
            DefaultTableModel model = (DefaultTableModel) BookTable.getModel();
            model.setRowCount(0); // 기존 데이터 초기화
            
            String sql = "SELECT * FROM books";
            db.DB_rs = db.DB_stmt.executeQuery(sql);
            
            while (db.DB_rs.next()) {
                Object[] row = {
                    db.DB_rs.getString("title"),
                    db.DB_rs.getString("author"),
                    db.DB_rs.getString("publisher"),
                    db.DB_rs.getInt("year_published"),
                    db.DB_rs.getBoolean("is_borrowed") ? "대출 중" : "대출 가능"
                };
                model.addRow(row); // 데이터 추가
            }
            db.dbClose(); // DB 연결 종료
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadUserData() {
        try {
            db.dbOpen();
            DefaultTableModel model = (DefaultTableModel) UserTable.getModel();
            model.setRowCount(0); // 기존 데이터 초기화
            
            String sql = "SELECT * FROM users";
            db.DB_rs = db.DB_stmt.executeQuery(sql);
            
            while (db.DB_rs.next()) {
                // role 값을 읽어 권한을 문자열로 변환
            String role;
            switch (db.DB_rs.getString("role")) {
                case "ADMIN":
                    role = "관리자";
                    break;
                case "USER":
                    role = "일반 사용자";
                    break;
                default:
                    role = "알 수 없음";
            }
                Object[] row = {
                    db.DB_rs.getString("user_id"),
                    db.DB_rs.getString("username"),
                    db.DB_rs.getString("email"),
                    role
                };
                model.addRow(row); // 데이터 추가
            }
            db.dbClose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     
    private void loadLoanData() {
        try {
            db.dbOpen();
            DefaultTableModel model = (DefaultTableModel) LoanTable.getModel();
            model.setRowCount(0); // 기존 데이터 초기화
            
            String sql = "SELECT l.loan_pin, u.user_id, b.title, l.loan_date, l.due_date, l.return_date " +
                         "FROM loans l " +
                         "JOIN users u ON l.user_pin = u.user_pin " +
                         "JOIN books b ON l.book_pin = b.book_pin";
            db.DB_rs = db.DB_stmt.executeQuery(sql);
            
            while (db.DB_rs.next()) {
                Object[] row = {
                    db.DB_rs.getString("user_id"),
                    db.DB_rs.getString("title"),
                    db.DB_rs.getDate("loan_date"),
                    db.DB_rs.getDate("due_date"),
                    db.DB_rs.getDate("return_date") != null ? db.DB_rs.getDate("return_date") : "미반납"
                };
                model.addRow(row); // 데이터 추가
            }
            db.dbClose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void searchBookData() {
        String filter = BookFilterCombo.getSelectedItem().toString();
        String searchText = BookSearchTextField.getText().trim();
        String column = "";
        
        switch (filter) {
            case "도서명":
                column = "title";
                break;
            case "저자명":
                column = "author";
                break;
            case "출판사":
                column = "publisher";
                break;
            case "출판연도":
                column = "year_published";
                break;
            default:
                column = "";
        }
        
        // SQL 쿼리 작성
        String query = "SELECT * FROM books WHERE " + column + " LIKE ?";

        try {
            db.dbOpen(); // DB 연결
            PreparedStatement pstmt = db.DB_con.prepareStatement(query);
            pstmt.setString(1, "%" + searchText + "%"); // LIKE 조건에 검색어 추가

            ResultSet rs = pstmt.executeQuery();

            // 테이블 초기화
            DefaultTableModel model = (DefaultTableModel) BookTable.getModel();
            model.setRowCount(0);

            // 결과 데이터 추가
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("book_pin"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("publisher"),
                    rs.getInt("year_published"),
                    rs.getBoolean("is_borrowed") ? "대출 중" : "대출 가능"
                };
                model.addRow(row);
            }

            // 리소스 정리
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "검색 중 오류가 발생했습니다: " + e.getMessage());
        }
}

    private void addBookData() {
        String bookname = AddTitleTextField.getText().trim(); // 도서명
        String authorname = AddAuthorTextField.getText().trim(); // 저자명
        String publisher = AddPublisherTextField.getText().trim(); // 출판사
        String pubyear = AddPublishyearTextField.getText().trim(); // 출판연도

        // 입력값 검증
        if (bookname.isEmpty() || authorname.isEmpty() || publisher.isEmpty() || pubyear.isEmpty()) {
            JOptionPane.showMessageDialog(null, "모든 필드를 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            db.dbOpen(); // DB 연결

            // 가장 작은 빈 번호 찾기
            String findMissingQuery = "SELECT book_pin + 1 AS missing_pin FROM books " +
            "WHERE (book_pin + 1) NOT IN (SELECT book_pin FROM books) " +
            "ORDER BY book_pin LIMIT 1;";
            Statement stmt = db.DB_con.createStatement();
            ResultSet rs = stmt.executeQuery(findMissingQuery);

            int bookPin = 0; // book_pin 값 초기화

            // 가장 작은 빈 번호가 존재하는 경우
            if (rs.next()) {
                bookPin = rs.getInt("missing_pin");
            } else {
                // 모든 번호가 사용 중인 경우 가장 큰 번호 + 1을 사용
                String maxPinQuery = "SELECT MAX(book_pin) AS max_pin FROM books";
                ResultSet maxPinResult = stmt.executeQuery(maxPinQuery);
                if (maxPinResult.next()) {
                    bookPin = maxPinResult.getInt("max_pin") + 1;
                }
            }

            // 도서 데이터를 books 테이블에 추가
            String sql = "INSERT INTO books (book_pin, title, author, publisher, year_published) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = db.DB_con.prepareStatement(sql);

            // ?에 값 바인딩
            pstmt.setInt(1, bookPin); // 계산된 book_pin 사용
            pstmt.setString(2, bookname);
            pstmt.setString(3, authorname);
            pstmt.setString(4, publisher);
            pstmt.setString(5, pubyear);

            // 쿼리 실행
            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                JOptionPane.showMessageDialog(null, "도서가 성공적으로 추가되었습니다!");
            }

            // DB 연결 종료
            db.dbClose();

            // 입력 필드 초기화
            AddTitleTextField.setText("");
            AddAuthorTextField.setText("");
            AddPublisherTextField.setText("");
            AddPublishyearTextField.setText("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "데이터베이스 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "예상치 못한 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

    }
    
    private void updateBookData() {
        int selectedRow = BookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "수정할 도서를 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 테이블에서 필요한 데이터 가져오기
        String title = BookTable.getValueAt(selectedRow, 0).toString();

        // 수정된 데이터 가져오기
        String newTitle = UpdateTitleTextField.getText();
        String newAuthor = UpdateAuthorTextField.getText();
        String newPublisher = UpdatePublisherTextField.getText();
        int newYear = Integer.parseInt(UpdatePublishedyearTextField.getText());

        // DB 업데이트
        try {
            db.dbOpen(); // DB 연결

            String query = "UPDATE books " +
                           "SET title = ?, author = ?, publisher = ?, year_published = ? " +
                           "WHERE title = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(query);
            pstmt.setString(1, newTitle);
            pstmt.setString(2, newAuthor);
            pstmt.setString(3, newPublisher);
            pstmt.setInt(4, newYear);
            pstmt.setString(5, title);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                JOptionPane.showMessageDialog(this, "도서 정보가 성공적으로 수정되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, "도서 정보 수정에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }

            pstmt.close();
            db.dbClose();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터베이스 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
   
    private void deleteBookData() {
        int selectedRow = BookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "삭제할 도서를 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String title = BookTable.getValueAt(selectedRow, 0).toString(); // 도서명

        try {
            db.dbOpen(); // DB 연결

            String query = "DELETE FROM books WHERE title = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(query);
            pstmt.setString(1, title);

            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                JOptionPane.showMessageDialog(this, "도서가 성공적으로 삭제되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, "도서 삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }

            pstmt.close();
            db.dbClose();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터베이스 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchUserData() {
        String filter = UserFilterCombo.getSelectedItem().toString(); // 필터 콤보 박스에서 선택한 항목
        String searchText = UserSearchTextField.getText().trim(); // 검색어 입력 필드

        // SQL 쿼리 초기화
        String query = "SELECT * FROM users";
        boolean hasFilter = false;

        // 필터 조건 설정
        if (filter.equals("일반 사용자")) {
            query += " WHERE role = 'USER'";
            hasFilter = true;
        } else if (filter.equals("관리자")) {
            query += " WHERE role = 'ADMIN'";
            hasFilter = true;
        }

        // 검색어 추가 (이름 또는 아이디 검색)
        if (!searchText.isEmpty()) {
            if (hasFilter) {
                query += " AND (user_id LIKE ? OR username LIKE ?)";
            } else {
                query += " WHERE (user_id LIKE ? OR username LIKE ?)";
            }
        }

        try {
            db.dbOpen(); // DB 연결

            PreparedStatement preparedStatement = db.DB_con.prepareStatement(query);

            // 검색어가 있을 경우 PreparedStatement에 검색 조건 추가
            if (!searchText.isEmpty()) {
                preparedStatement.setString(1, "%" + searchText + "%"); // user_id LIKE 검색 조건
                preparedStatement.setString(2, "%" + searchText + "%"); // username LIKE 검색 조건
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            // 테이블 모델 초기화
            DefaultTableModel model = (DefaultTableModel) UserTable.getModel();
            model.setRowCount(0); // 기존 테이블 데이터 삭제

            // 검색 결과 테이블에 추가
            while (resultSet.next()) {
                String userId = resultSet.getString("user_id");
                String username = resultSet.getString("username");
                String email = resultSet.getString("email");
                String role = resultSet.getString("role");

                // 역할을 한국어로 변환
                String userRole = role.equals("USER") ? "일반 사용자" : "관리자";

                // 테이블에 행 추가
                model.addRow(new Object[]{
                    userId, username, email, userRole
                });
            }

            // 리소스 정리
            resultSet.close();
            preparedStatement.close();
            db.dbClose(); // DB 연결 해제
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void addUserData() {
        // 입력 필드에서 데이터 가져오기
        String userId = AddUserIdTextField.getText().trim(); // 아이디
        String username = AddUserNameTextField.getText().trim(); // 이름
        String email = AddUserEmailTextField.getText().trim(); // 이메일
        String password = AddUserPasswordTextField.getText().trim(); // 비밀번호
        String role = AddUserRoleCombo.getSelectedItem().toString().equals("관리자") ? "ADMIN" : "USER"; // Role 설정

        // 입력값 검증
        if (userId.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(null, "모든 필드를 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            db.dbOpen(); // DB 연결

            // 가장 작은 빈 user_pin 찾기
            String findMissingQuery = "SELECT user_pin + 1 AS missing_pin FROM users " +
                    "WHERE (user_pin + 1) NOT IN (SELECT user_pin FROM users) " +
                    "ORDER BY user_pin LIMIT 1;";
            Statement stmt = db.DB_con.createStatement();
            ResultSet rs = stmt.executeQuery(findMissingQuery);

            int userPin = 0; // user_pin 초기화

            // 빈 번호가 존재하면 설정, 아니면 최대값 + 1로 설정
            if (rs.next()) {
                userPin = rs.getInt("missing_pin");
            } else {
                String maxPinQuery = "SELECT MAX(user_pin) AS max_pin FROM users";
                ResultSet maxPinResult = stmt.executeQuery(maxPinQuery);
                if (maxPinResult.next()) {
                    userPin = maxPinResult.getInt("max_pin") + 1;
                }
            }

            // users 테이블에 데이터 삽입
            String sql = "INSERT INTO users (user_pin, user_id, username, email, password, role) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = db.DB_con.prepareStatement(sql);
            pstmt.setInt(1, userPin);
            pstmt.setString(2, userId);
            pstmt.setString(3, username);
            pstmt.setString(4, email);
            pstmt.setString(5, password);
            pstmt.setString(6, role);

            // 실행 및 결과 처리
            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                JOptionPane.showMessageDialog(null, "회원이 성공적으로 추가되었습니다!");
            } else {
                JOptionPane.showMessageDialog(null, "회원 추가에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }

            // 자원 해제 및 연결 종료
            rs.close();
            pstmt.close();
            db.dbClose();

            // 입력 필드 초기화
            AddUserIdTextField.setText("");
            AddUserNameTextField.setText("");
            AddUserEmailTextField.setText("");
            AddUserPasswordTextField.setText("");
            AddUserRoleCombo.setSelectedIndex(0); // 기본값으로 설정
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "데이터베이스 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "예상치 못한 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateUserData() {
        int selectedRow = UserTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "수정할 사용자를 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String userId = UserTable.getValueAt(selectedRow, 0).toString();

        // 수정된 데이터 가져오기
        String newUserId = UpdateUserIdTextField.getText();
        String newUsername = UpdateUserNameTextField.getText();
        String newEmail = UpdateUserEmailTextField.getText();
        String newPassword = new String(UpdateUserPasswordField.getPassword()).trim(); // PasswordField에서 문자열로 변환
        String newRole = UpdateUserRoleCombo.getSelectedItem().toString().equals("관리자") ? "ADMIN" : "USER";

        try {
            db.dbOpen(); // DB 연결

            String query = "UPDATE users " +
                           "SET user_id = ?, username = ?, email = ?, password = ?, role = ? " +
                           "WHERE user_id = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(query);
            pstmt.setString(1, newUserId);
            pstmt.setString(2, newUsername);
            pstmt.setString(3, newEmail);
            pstmt.setString(4, newPassword);
            pstmt.setString(5, newRole);
            pstmt.setString(6, userId);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                JOptionPane.showMessageDialog(this, "사용자 정보가 성공적으로 수정되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, "사용자 정보 수정에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }

            pstmt.close();
            db.dbClose();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터베이스 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
 
    private void deleteUserData() {
        int selectedRow = UserTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "삭제할 사용자를 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String userId = UserTable.getValueAt(selectedRow, 0).toString();

        try {
            db.dbOpen(); // DB 연결

            String query = "DELETE FROM users WHERE user_id = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(query);
            pstmt.setString(1, userId);

            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                JOptionPane.showMessageDialog(this, "사용자가 성공적으로 삭제되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, "사용자 삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }

            pstmt.close();
            db.dbClose();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터베이스 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchLoanData() {
        String filter = LoanFilterCombo.getSelectedItem().toString(); // 필터 콤보박스에서 선택된 값 가져오기
        String searchText = LoanSearchTextField.getText().trim(); // 검색어 입력 필드 값 가져오기
        String query = "SELECT l.loan_pin, u.user_id, b.title, l.loan_date, l.due_date, l.return_date " +
                        "FROM loans l " +
                        "JOIN users u ON l.user_pin = u.user_pin " +
                        "JOIN books b ON l.book_pin = b.book_pin ";

        // SQL 쿼리 초기화
        switch (filter) {
            case "반납":
                query += "WHERE l.return_date IS NOT NULL";
                break;
            case "미반납":
                query += "WHERE l.return_date IS NULL";
                break;
        }

        // 검색어가 입력된 경우 추가 조건
        if (!searchText.isEmpty()) {
            query += " AND (u.user_id LIKE ? OR b.title LIKE ?)";
        }

        try {
            db.dbOpen(); // DB 연결

            PreparedStatement pstmt = db.DB_con.prepareStatement(query);

            // 검색어가 있는 경우 PreparedStatement에 조건 추가
            if (!searchText.isEmpty()) {
                pstmt.setString(1, "%" + searchText + "%"); // 대출자 ID 조건
                pstmt.setString(2, "%" + searchText + "%"); // 도서명 조건
            }

            ResultSet rs = pstmt.executeQuery();

            // LoanTable 데이터 초기화
            DefaultTableModel model = (DefaultTableModel) LoanTable.getModel();
            model.setRowCount(0);

            // 결과 데이터를 LoanTable에 추가
            while (rs.next()) {
                Object[] row = {
                    rs.getString("user_id"),
                    rs.getString("title"),
                    rs.getDate("loan_date"),
                    rs.getDate("due_date"),
                    rs.getDate("return_date") != null ? rs.getDate("return_date") : "미반납"
                };
                model.addRow(row);
            }

            // 리소스 정리
            rs.close();
            pstmt.close();
            db.dbClose();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "검색 중 오류가 발생했습니다: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteLoanData() {
        int selectedRow = LoanTable.getSelectedRow(); // 선택된 행의 인덱스 가져오기

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "삭제할 대여 기록을 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 선택된 행의 데이터 가져오기
        String userId = LoanTable.getValueAt(selectedRow, 0).toString(); // 대출자 ID
        String bookTitle = LoanTable.getValueAt(selectedRow, 1).toString(); // 도서명

        // 확인 대화상자
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "선택된 대여 기록을 삭제하시겠습니까?",
            "삭제 확인",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                db.dbOpen(); // DB 연결

                // 삭제할 loan_pin을 가져오기 위해 JOIN으로 조회
                String findLoanQuery = "SELECT l.loan_pin FROM loans l " +
                                       "JOIN users u ON l.user_pin = u.user_pin " +
                                       "JOIN books b ON l.book_pin = b.book_pin " +
                                       "WHERE u.user_id = ? AND b.title = ?";
                PreparedStatement findStmt = db.DB_con.prepareStatement(findLoanQuery);
                findStmt.setString(1, userId);
                findStmt.setString(2, bookTitle);

                ResultSet rs = findStmt.executeQuery();

                if (rs.next()) {
                    int loanPin = rs.getInt("loan_pin");

                    // loan_pin을 사용해 대여 기록 삭제
                    String deleteQuery = "DELETE FROM loans WHERE loan_pin = ?";
                    PreparedStatement deleteStmt = db.DB_con.prepareStatement(deleteQuery);
                    deleteStmt.setInt(1, loanPin);

                    int rowsDeleted = deleteStmt.executeUpdate();
                    if (rowsDeleted > 0) {
                        // 테이블에서 행 삭제
                        DefaultTableModel model = (DefaultTableModel) LoanTable.getModel();
                        model.removeRow(selectedRow);
                        JOptionPane.showMessageDialog(this, "대여 기록이 성공적으로 삭제되었습니다.");
                    } else {
                        JOptionPane.showMessageDialog(this, "대여 기록 삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    }

                    deleteStmt.close();
                } else {
                    JOptionPane.showMessageDialog(this, "삭제할 대여 기록을 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }

                rs.close();
                findStmt.close();
                db.dbClose(); // DB 연결 종료
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "데이터베이스 오류: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "예상치 못한 오류가 발생했습니다: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "삭제가 취소되었습니다.");
        }
    }
    
    private void toggleReturnStatus() {
        int selectedRow = LoanTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "상태를 변경할 대여 기록을 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 선택된 행에서 대출자 ID와 도서명을 가져옴
        String userId = LoanTable.getValueAt(selectedRow, 0).toString(); // 대출자 ID
        String title = LoanTable.getValueAt(selectedRow, 1).toString(); // 도서명

        // 현재 상태 가져오기
        String currentStatus = LoanTable.getValueAt(selectedRow, 4).toString(); // 반납 여부
        String newStatus;
        java.sql.Date returnDate = null; // 초기화

        // 반납 여부에 따라 상태 변경 및 날짜 처리
        if (currentStatus.equals("미반납")) {
            newStatus = "반납"; // 상태를 반납으로 전환
            returnDate = new java.sql.Date(System.currentTimeMillis()); // 오늘 날짜로 설정
        } else {
            newStatus = "미반납"; // 상태를 미반납으로 전환
            returnDate = null; // 반납 날짜 초기화
        }

        try {
            db.dbOpen(); // DB 연결

            String query = "UPDATE loans l " +
                           "JOIN users u ON l.user_pin = u.user_pin " +
                           "JOIN books b ON l.book_pin = b.book_pin " +
                           "SET l.return_date = ? " +
                           "WHERE u.user_id = ? AND b.title = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(query);

            if (returnDate == null) {
                pstmt.setNull(1, java.sql.Types.DATE); // 반납 날짜 초기화
            } else {
                pstmt.setDate(1, returnDate); // 반납 날짜를 설정
            }
            pstmt.setString(2, userId);
            pstmt.setString(3, title);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                LoanTable.setValueAt(newStatus, selectedRow, 4); // 상태 업데이트
                LoanTable.setValueAt(returnDate != null ? returnDate.toString() : "미반납", selectedRow, 4); // 반납 날짜 업데이트
                JOptionPane.showMessageDialog(this, "대여 상태가 성공적으로 변경되었습니다!");
            } else {
                JOptionPane.showMessageDialog(this, "대여 상태 변경에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }

            pstmt.close();
            db.dbClose();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터베이스 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enableTableSorting() {
        // LoanTable 정렬 설정
        TableRowSorter<DefaultTableModel> loanSorter = new TableRowSorter<>((DefaultTableModel) LoanTable.getModel());
        LoanTable.setRowSorter(loanSorter);

        // LoanTable 열 인덱스: 2 = 대출 일자, 3 = 반납 기한, 4 = 반납 일자
        loanSorter.setComparator(2, (o1, o2) -> { // 대출 일자
            if (o1 == null && o2 == null) return 0;
            if (o1 == null) return 1;
            if (o2 == null) return -1;
            return java.sql.Date.valueOf(o1.toString()).compareTo(java.sql.Date.valueOf(o2.toString()));
        });
        loanSorter.setComparator(3, (o1, o2) -> { // 반납 기한
            if (o1 == null && o2 == null) return 0;
            if (o1 == null) return 1;
            if (o2 == null) return -1;
            return java.sql.Date.valueOf(o1.toString()).compareTo(java.sql.Date.valueOf(o2.toString()));
        });
        loanSorter.setComparator(4, (o1, o2) -> { // 반납 일자
            if (o1 == null && o2 == null) return 0;
            if (o1 == null) return 1;
            if (o2 == null) return -1;
            return o1.toString().compareTo(o2.toString());
        });

        // UserTable 정렬 설정
        TableRowSorter<DefaultTableModel> userSorter = new TableRowSorter<>((DefaultTableModel) UserTable.getModel());
        UserTable.setRowSorter(userSorter);

        // UserTable 열 인덱스: 3 = 권한
        userSorter.setComparator(3, (o1, o2) -> {
            String str1 = o1 != null ? o1.toString() : "";
            String str2 = o2 != null ? o2.toString() : "";
            return str1.compareTo(str2);
        });

        // BookTable 정렬 설정
        TableRowSorter<DefaultTableModel> bookSorter = new TableRowSorter<>((DefaultTableModel) BookTable.getModel());
        BookTable.setRowSorter(bookSorter);

        // BookTable 열 인덱스: 3 = 출판연도
        bookSorter.setComparator(3, (o1, o2) -> {
            Integer int1 = o1 != null ? Integer.parseInt(o1.toString()) : Integer.MIN_VALUE;
            Integer int2 = o2 != null ? Integer.parseInt(o2.toString()) : Integer.MIN_VALUE;
            return int1.compareTo(int2);
        });
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // Main 메소드 없음. 로그인 인터페이스를 통해서만 호출.
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        AddBookDialog = new javax.swing.JDialog();
        AddBookPanel = new javax.swing.JPanel();
        AddTitleLabel = new javax.swing.JLabel();
        AddAuthorLabel = new javax.swing.JLabel();
        AddPublisherLabel = new javax.swing.JLabel();
        AddPublishyearLabel = new javax.swing.JLabel();
        AddTitleTextField = new javax.swing.JTextField();
        AddAuthorTextField = new javax.swing.JTextField();
        AddPublisherTextField = new javax.swing.JTextField();
        AddPublishyearTextField = new javax.swing.JTextField();
        AddBookConfirmButton = new javax.swing.JButton();
        AddBookCancelButton = new javax.swing.JButton();
        UpdateBookDialog = new javax.swing.JDialog();
        UpdateBookPanel = new javax.swing.JPanel();
        UpdateTitleLabel = new javax.swing.JLabel();
        UpdateAuthorLabel = new javax.swing.JLabel();
        UpdatePublisherLabel = new javax.swing.JLabel();
        UpdatePublishedyearLabel = new javax.swing.JLabel();
        UpdateTitleTextField = new javax.swing.JTextField();
        UpdateAuthorTextField = new javax.swing.JTextField();
        UpdatePublisherTextField = new javax.swing.JTextField();
        UpdatePublishedyearTextField = new javax.swing.JTextField();
        UpdateBookConfirmButton = new javax.swing.JButton();
        UpdateBookCancelButton = new javax.swing.JButton();
        AddUserDialog = new javax.swing.JDialog();
        AddUserPanel = new javax.swing.JPanel();
        AddUserIdLabel = new javax.swing.JLabel();
        AddUserNameLabel = new javax.swing.JLabel();
        AddUserEmailLabel = new javax.swing.JLabel();
        AddUserPasswordLabel = new javax.swing.JLabel();
        AddUserRoleLabel = new javax.swing.JLabel();
        AddUserIdTextField = new javax.swing.JTextField();
        AddUserNameTextField = new javax.swing.JTextField();
        AddUserEmailTextField = new javax.swing.JTextField();
        AddUserPasswordTextField = new javax.swing.JTextField();
        AddUserRoleCombo = new javax.swing.JComboBox<>();
        AddUserConfirmButton = new javax.swing.JButton();
        AddUserCancelButton = new javax.swing.JButton();
        UpdateUserDialog = new javax.swing.JDialog();
        UpdateUserPanel = new javax.swing.JPanel();
        UpdateUserIdLabel = new javax.swing.JLabel();
        UpdateUserNameLabel = new javax.swing.JLabel();
        UpdateUserEmailLabel = new javax.swing.JLabel();
        UpdateUserPasswordLabel = new javax.swing.JLabel();
        UpdateUserRoleLabel = new javax.swing.JLabel();
        UpdateUserIdTextField = new javax.swing.JTextField();
        UpdateUserNameTextField = new javax.swing.JTextField();
        UpdateUserEmailTextField = new javax.swing.JTextField();
        UpdateUserPasswordField = new javax.swing.JPasswordField();
        UpdateUserRoleCombo = new javax.swing.JComboBox<>();
        UpdateUserConfirmButton = new javax.swing.JButton();
        UpdateUserCancelButton = new javax.swing.JButton();
        Admin = new javax.swing.JPanel();
        Container = new javax.swing.JPanel();
        TitleLabel = new javax.swing.JLabel();
        AdminTabbedpane = new javax.swing.JTabbedPane();
        BookPanel = new javax.swing.JPanel();
        BookTableScroll = new javax.swing.JScrollPane();
        BookTable = new javax.swing.JTable();
        BookAddButton = new javax.swing.JButton();
        BookUpdateButton = new javax.swing.JButton();
        BookDeleteButton = new javax.swing.JButton();
        BookFilterCombo = new javax.swing.JComboBox<>();
        BookSearchTextField = new javax.swing.JTextField();
        BookSearchButton = new javax.swing.JButton();
        UserPanel = new javax.swing.JPanel();
        UserTableScroll = new javax.swing.JScrollPane();
        UserTable = new javax.swing.JTable();
        UserAddButton = new javax.swing.JButton();
        UserUpdateButton = new javax.swing.JButton();
        UserDeleteButton = new javax.swing.JButton();
        UserFilterCombo = new javax.swing.JComboBox<>();
        UserSearchTextField = new javax.swing.JTextField();
        UserSearchButton = new javax.swing.JButton();
        LoanPanel = new javax.swing.JPanel();
        LoanTableScroll = new javax.swing.JScrollPane();
        LoanTable = new javax.swing.JTable();
        ToggleReturnButton = new javax.swing.JButton();
        LoanDeleteButton = new javax.swing.JButton();
        LoanFilterCombo = new javax.swing.JComboBox<>();
        LoanSearchTextField = new javax.swing.JTextField();
        LoanSearchButton = new javax.swing.JButton();

        AddBookDialog.setTitle("도서 추가");
        AddBookDialog.setPreferredSize(new java.awt.Dimension(600, 400));
        AddBookDialog.setSize(new java.awt.Dimension(600, 400));
        AddBookDialog.getContentPane().setLayout(new java.awt.GridBagLayout());

        AddBookPanel.setPreferredSize(new java.awt.Dimension(550, 350));

        AddTitleLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        AddTitleLabel.setText("도서명");

        AddAuthorLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        AddAuthorLabel.setText("저자명");

        AddPublisherLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        AddPublisherLabel.setText("출판사");

        AddPublishyearLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        AddPublishyearLabel.setText("출판연도");

        AddTitleTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        AddAuthorTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        AddPublisherTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        AddPublishyearTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        AddBookConfirmButton.setText("추가");
        AddBookConfirmButton.setPreferredSize(new java.awt.Dimension(72, 32));
        AddBookConfirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddBookConfirmButtonActionPerformed(evt);
            }
        });

        AddBookCancelButton.setText("취소");
        AddBookCancelButton.setMinimumSize(new java.awt.Dimension(72, 32));
        AddBookCancelButton.setPreferredSize(new java.awt.Dimension(72, 32));
        AddBookCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddBookCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout AddBookPanelLayout = new javax.swing.GroupLayout(AddBookPanel);
        AddBookPanel.setLayout(AddBookPanelLayout);
        AddBookPanelLayout.setHorizontalGroup(
            AddBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AddBookPanelLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(AddBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(AddAuthorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(AddTitleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(AddPublisherLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(AddBookPanelLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(AddPublishyearLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(AddBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(AddAuthorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 451, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddTitleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 451, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddPublishyearTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 451, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddPublisherTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 451, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(16, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, AddBookPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(AddBookConfirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(AddBookCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
        );
        AddBookPanelLayout.setVerticalGroup(
            AddBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AddBookPanelLayout.createSequentialGroup()
                .addGap(63, 63, 63)
                .addGroup(AddBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddTitleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddTitleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(AddBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddAuthorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddAuthorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(AddBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddPublisherLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddPublisherTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(AddBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddPublishyearLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddPublishyearTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                .addGroup(AddBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddBookCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddBookConfirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        AddBookDialog.getContentPane().add(AddBookPanel, new java.awt.GridBagConstraints());

        AddBookDialog.getAccessibleContext().setAccessibleName("도서 정보 추가");
        AddBookDialog.getAccessibleContext().setAccessibleDescription("");

        UpdateBookDialog.setTitle("도서 추가");
        UpdateBookDialog.setPreferredSize(new java.awt.Dimension(600, 400));
        UpdateBookDialog.setSize(new java.awt.Dimension(600, 400));
        UpdateBookDialog.getContentPane().setLayout(new java.awt.GridBagLayout());

        UpdateBookPanel.setPreferredSize(new java.awt.Dimension(550, 350));

        UpdateTitleLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        UpdateTitleLabel.setText("도서명");

        UpdateAuthorLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        UpdateAuthorLabel.setText("저자명");

        UpdatePublisherLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        UpdatePublisherLabel.setText("출판사");

        UpdatePublishedyearLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        UpdatePublishedyearLabel.setText("출판연도");

        UpdateTitleTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        UpdateAuthorTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        UpdatePublisherTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        UpdatePublishedyearTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        UpdateBookConfirmButton.setText("수정");
        UpdateBookConfirmButton.setPreferredSize(new java.awt.Dimension(72, 32));
        UpdateBookConfirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UpdateBookConfirmButtonActionPerformed(evt);
            }
        });

        UpdateBookCancelButton.setText("취소");
        UpdateBookCancelButton.setMinimumSize(new java.awt.Dimension(72, 32));
        UpdateBookCancelButton.setPreferredSize(new java.awt.Dimension(72, 32));
        UpdateBookCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UpdateBookCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout UpdateBookPanelLayout = new javax.swing.GroupLayout(UpdateBookPanel);
        UpdateBookPanel.setLayout(UpdateBookPanelLayout);
        UpdateBookPanelLayout.setHorizontalGroup(
            UpdateBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UpdateBookPanelLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(UpdateBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(UpdateAuthorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(UpdateTitleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(UpdatePublisherLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(UpdateBookPanelLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(UpdatePublishedyearLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(UpdateBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(UpdateAuthorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 451, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdateTitleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 451, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdatePublishedyearTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 451, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdatePublisherTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 451, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(16, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UpdateBookPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(UpdateBookConfirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(UpdateBookCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
        );
        UpdateBookPanelLayout.setVerticalGroup(
            UpdateBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UpdateBookPanelLayout.createSequentialGroup()
                .addGap(63, 63, 63)
                .addGroup(UpdateBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UpdateTitleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdateTitleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(UpdateBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UpdateAuthorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdateAuthorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(UpdateBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UpdatePublisherLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdatePublisherTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(UpdateBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UpdatePublishedyearLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdatePublishedyearTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                .addGroup(UpdateBookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UpdateBookCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdateBookConfirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        UpdateBookDialog.getContentPane().add(UpdateBookPanel, new java.awt.GridBagConstraints());

        UpdateBookDialog.getAccessibleContext().setAccessibleName("도서 정보 수정");

        AddUserDialog.setTitle("도서 추가");
        AddUserDialog.setPreferredSize(new java.awt.Dimension(600, 400));
        AddUserDialog.setSize(new java.awt.Dimension(600, 400));
        AddUserDialog.getContentPane().setLayout(new java.awt.GridBagLayout());

        AddUserPanel.setPreferredSize(new java.awt.Dimension(550, 350));

        AddUserIdLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        AddUserIdLabel.setText("아이디");

        AddUserNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        AddUserNameLabel.setText("이름");

        AddUserEmailLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        AddUserEmailLabel.setText("이메일");

        AddUserPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        AddUserPasswordLabel.setText("비밀번호");

        AddUserRoleLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        AddUserRoleLabel.setText("권한");

        AddUserIdTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        AddUserNameTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        AddUserEmailTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        AddUserPasswordTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        AddUserRoleCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "일반 사용자", "관리자" }));

        AddUserConfirmButton.setText("추가");
        AddUserConfirmButton.setPreferredSize(new java.awt.Dimension(72, 32));
        AddUserConfirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddUserConfirmButtonActionPerformed(evt);
            }
        });

        AddUserCancelButton.setText("취소");
        AddUserCancelButton.setMinimumSize(new java.awt.Dimension(72, 32));
        AddUserCancelButton.setPreferredSize(new java.awt.Dimension(72, 32));
        AddUserCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddUserCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout AddUserPanelLayout = new javax.swing.GroupLayout(AddUserPanel);
        AddUserPanel.setLayout(AddUserPanelLayout);
        AddUserPanelLayout.setHorizontalGroup(
            AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, AddUserPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(AddUserConfirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(AddUserCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
            .addGroup(AddUserPanelLayout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addGroup(AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(AddUserNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                    .addComponent(AddUserIdLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(AddUserEmailLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(AddUserPasswordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(AddUserRoleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(AddUserNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                    .addComponent(AddUserIdTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                    .addComponent(AddUserEmailTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                    .addComponent(AddUserPasswordTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                    .addComponent(AddUserRoleCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(22, Short.MAX_VALUE))
        );
        AddUserPanelLayout.setVerticalGroup(
            AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AddUserPanelLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddUserIdLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddUserIdTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddUserNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddUserNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddUserEmailLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddUserEmailTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddUserPasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddUserPasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddUserRoleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddUserRoleCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 40, Short.MAX_VALUE)
                .addGroup(AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddUserCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(AddUserConfirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        AddUserDialog.getContentPane().add(AddUserPanel, new java.awt.GridBagConstraints());

        AddUserDialog.getAccessibleContext().setAccessibleName("회원 정보 추가");

        UpdateUserDialog.setTitle("도서 추가");
        UpdateUserDialog.setPreferredSize(new java.awt.Dimension(600, 400));
        UpdateUserDialog.setSize(new java.awt.Dimension(600, 400));
        UpdateUserDialog.getContentPane().setLayout(new java.awt.GridBagLayout());

        UpdateUserPanel.setPreferredSize(new java.awt.Dimension(550, 350));

        UpdateUserIdLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        UpdateUserIdLabel.setText("아이디");

        UpdateUserNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        UpdateUserNameLabel.setText("이름");

        UpdateUserEmailLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        UpdateUserEmailLabel.setText("이메일");

        UpdateUserPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        UpdateUserPasswordLabel.setText("비밀번호");

        UpdateUserRoleLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        UpdateUserRoleLabel.setText("권한");

        UpdateUserIdTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        UpdateUserNameTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        UpdateUserEmailTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        UpdateUserRoleCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "일반 사용자", "관리자" }));

        UpdateUserConfirmButton.setText("수정");
        UpdateUserConfirmButton.setPreferredSize(new java.awt.Dimension(72, 32));
        UpdateUserConfirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UpdateUserConfirmButtonActionPerformed(evt);
            }
        });

        UpdateUserCancelButton.setText("취소");
        UpdateUserCancelButton.setMinimumSize(new java.awt.Dimension(72, 32));
        UpdateUserCancelButton.setPreferredSize(new java.awt.Dimension(72, 32));
        UpdateUserCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UpdateUserCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout UpdateUserPanelLayout = new javax.swing.GroupLayout(UpdateUserPanel);
        UpdateUserPanel.setLayout(UpdateUserPanelLayout);
        UpdateUserPanelLayout.setHorizontalGroup(
            UpdateUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UpdateUserPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(UpdateUserConfirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(UpdateUserCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
            .addGroup(UpdateUserPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(UpdateUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(UpdateUserNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
                    .addComponent(UpdateUserIdLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(UpdateUserEmailLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(UpdateUserPasswordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
                    .addComponent(UpdateUserRoleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(UpdateUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(UpdateUserNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                    .addComponent(UpdateUserIdTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                    .addComponent(UpdateUserEmailTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                    .addComponent(UpdateUserRoleCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(UpdateUserPasswordField))
                .addContainerGap(11, Short.MAX_VALUE))
        );
        UpdateUserPanelLayout.setVerticalGroup(
            UpdateUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UpdateUserPanelLayout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addGroup(UpdateUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UpdateUserIdLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdateUserIdTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(UpdateUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UpdateUserNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdateUserNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(UpdateUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UpdateUserEmailLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdateUserEmailTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(UpdateUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(UpdateUserPasswordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                    .addComponent(UpdateUserPasswordField))
                .addGap(18, 18, 18)
                .addGroup(UpdateUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(UpdateUserRoleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdateUserRoleCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                .addGroup(UpdateUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UpdateUserCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UpdateUserConfirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        UpdateUserDialog.getContentPane().add(UpdateUserPanel, new java.awt.GridBagConstraints());

        UpdateUserDialog.getAccessibleContext().setAccessibleName("회원 정보 수정");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Admin.setBackground(new java.awt.Color(204, 204, 255));
        Admin.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        Admin.setPreferredSize(new java.awt.Dimension(1200, 800));
        Admin.setLayout(new java.awt.GridBagLayout());

        Container.setBackground(new java.awt.Color(204, 204, 255));
        Container.setPreferredSize(new java.awt.Dimension(1150, 750));

        TitleLabel.setFont(new java.awt.Font("나눔바른펜", 1, 24)); // NOI18N
        TitleLabel.setForeground(new java.awt.Color(153, 153, 255));
        TitleLabel.setText("Book Management");

        AdminTabbedpane.setPreferredSize(new java.awt.Dimension(1100, 650));

        BookTableScroll.setPreferredSize(new java.awt.Dimension(430, 500));

        BookTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "도서명", "저자명", "출판사", "출판연도", "대출 가능 여부"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        BookTableScroll.setViewportView(BookTable);

        BookAddButton.setText("추가");
        BookAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BookAddButtonActionPerformed(evt);
            }
        });

        BookUpdateButton.setText("수정");
        BookUpdateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BookUpdateButtonActionPerformed(evt);
            }
        });

        BookDeleteButton.setText("삭제");
        BookDeleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BookDeleteButtonActionPerformed(evt);
            }
        });

        BookFilterCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "도서명", "저자명", "출판사", "출판연도" }));

        BookSearchButton.setText("검색");
        BookSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BookSearchButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout BookPanelLayout = new javax.swing.GroupLayout(BookPanel);
        BookPanel.setLayout(BookPanelLayout);
        BookPanelLayout.setHorizontalGroup(
            BookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BookPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(BookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(BookTableScroll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(BookPanelLayout.createSequentialGroup()
                        .addGap(0, 628, Short.MAX_VALUE)
                        .addGroup(BookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(BookPanelLayout.createSequentialGroup()
                                .addComponent(BookAddButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(BookUpdateButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(BookDeleteButton))
                            .addGroup(BookPanelLayout.createSequentialGroup()
                                .addComponent(BookFilterCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(BookSearchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(BookSearchButton)))))
                .addContainerGap())
        );
        BookPanelLayout.setVerticalGroup(
            BookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BookPanelLayout.createSequentialGroup()
                .addContainerGap(31, Short.MAX_VALUE)
                .addGroup(BookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(BookSearchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BookSearchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BookFilterCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(BookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(BookDeleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BookUpdateButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BookAddButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(7, 7, 7)
                .addComponent(BookTableScroll, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        AdminTabbedpane.addTab("도서 관리", BookPanel);

        UserTableScroll.setPreferredSize(new java.awt.Dimension(430, 500));

        UserTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "아이디", "이름", "이메일", "권한"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        UserTableScroll.setViewportView(UserTable);

        UserAddButton.setText("추가");
        UserAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UserAddButtonActionPerformed(evt);
            }
        });

        UserUpdateButton.setText("수정");
        UserUpdateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UserUpdateButtonActionPerformed(evt);
            }
        });

        UserDeleteButton.setText("삭제");
        UserDeleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UserDeleteButtonActionPerformed(evt);
            }
        });

        UserFilterCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "전체", "일반 사용자", "관리자" }));
        UserFilterCombo.setPreferredSize(new java.awt.Dimension(100, 23));

        UserSearchButton.setText("검색");
        UserSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UserSearchButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout UserPanelLayout = new javax.swing.GroupLayout(UserPanel);
        UserPanel.setLayout(UserPanelLayout);
        UserPanelLayout.setHorizontalGroup(
            UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UserPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(UserTableScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 1138, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UserPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(UserPanelLayout.createSequentialGroup()
                                .addComponent(UserFilterCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(UserSearchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(UserPanelLayout.createSequentialGroup()
                                .addComponent(UserAddButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(UserUpdateButton)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(UserDeleteButton)
                            .addComponent(UserSearchButton))))
                .addContainerGap())
        );
        UserPanelLayout.setVerticalGroup(
            UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UserPanelLayout.createSequentialGroup()
                .addContainerGap(32, Short.MAX_VALUE)
                .addGroup(UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UserSearchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UserFilterCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UserSearchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UserAddButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UserUpdateButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(UserDeleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(UserTableScroll, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        AdminTabbedpane.addTab("회원 관리", UserPanel);

        LoanTableScroll.setPreferredSize(new java.awt.Dimension(430, 500));

        LoanTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "대출자 ID", "도서명", "대출 일자", "반납 기한", "반납 일자"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        LoanTableScroll.setViewportView(LoanTable);

        ToggleReturnButton.setText("반납 / 미반납");
        ToggleReturnButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToggleReturnButtonActionPerformed(evt);
            }
        });

        LoanDeleteButton.setText("삭제");
        LoanDeleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoanDeleteButtonActionPerformed(evt);
            }
        });

        LoanFilterCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "전체", "반납", "미반납" }));
        LoanFilterCombo.setPreferredSize(new java.awt.Dimension(100, 23));

        LoanSearchButton.setText("검색");
        LoanSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoanSearchButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout LoanPanelLayout = new javax.swing.GroupLayout(LoanPanel);
        LoanPanel.setLayout(LoanPanelLayout);
        LoanPanelLayout.setHorizontalGroup(
            LoanPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LoanPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(LoanPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(LoanTableScroll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(LoanPanelLayout.createSequentialGroup()
                        .addGap(0, 628, Short.MAX_VALUE)
                        .addGroup(LoanPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LoanPanelLayout.createSequentialGroup()
                                .addComponent(ToggleReturnButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(LoanDeleteButton))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LoanPanelLayout.createSequentialGroup()
                                .addComponent(LoanFilterCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(LoanSearchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(LoanSearchButton)))))
                .addContainerGap())
        );
        LoanPanelLayout.setVerticalGroup(
            LoanPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(LoanPanelLayout.createSequentialGroup()
                .addContainerGap(32, Short.MAX_VALUE)
                .addGroup(LoanPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LoanSearchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LoanFilterCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LoanSearchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(LoanPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ToggleReturnButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LoanDeleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(LoanTableScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        AdminTabbedpane.addTab("대출 현황", LoanPanel);

        javax.swing.GroupLayout ContainerLayout = new javax.swing.GroupLayout(Container);
        Container.setLayout(ContainerLayout);
        ContainerLayout.setHorizontalGroup(
            ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ContainerLayout.createSequentialGroup()
                .addGap(0, 936, Short.MAX_VALUE)
                .addComponent(TitleLabel))
            .addComponent(AdminTabbedpane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        ContainerLayout.setVerticalGroup(
            ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ContainerLayout.createSequentialGroup()
                .addContainerGap(48, Short.MAX_VALUE)
                .addComponent(TitleLabel)
                .addGap(18, 18, 18)
                .addComponent(AdminTabbedpane, javax.swing.GroupLayout.PREFERRED_SIZE, 650, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        Admin.add(Container, new java.awt.GridBagConstraints());
        Container.getAccessibleContext().setAccessibleDescription("");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Admin, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Admin, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void BookAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BookAddButtonActionPerformed
        // TODO add your handling code here:
        AddBookDialog.setVisible(true);
    }//GEN-LAST:event_BookAddButtonActionPerformed

    private void AddBookConfirmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddBookConfirmButtonActionPerformed
        addBookData();
        AddBookDialog.dispose();
        loadBookData();
    }//GEN-LAST:event_AddBookConfirmButtonActionPerformed

    private void AddBookCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddBookCancelButtonActionPerformed
        // TODO add your handling code here:
        AddBookDialog.dispose();
    }//GEN-LAST:event_AddBookCancelButtonActionPerformed

    private void UpdateBookConfirmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UpdateBookConfirmButtonActionPerformed
        // TODO add your handling code here:
        updateBookData();
        UpdateBookDialog.dispose();
        loadBookData();
    }//GEN-LAST:event_UpdateBookConfirmButtonActionPerformed

    private void UpdateBookCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UpdateBookCancelButtonActionPerformed
        // TODO add your handling code here:
        UpdateBookDialog.dispose();
    }//GEN-LAST:event_UpdateBookCancelButtonActionPerformed

    private void AddUserConfirmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddUserConfirmButtonActionPerformed
        // TODO add your handling code here:
        addUserData();
        AddUserDialog.dispose();
        loadUserData();
    }//GEN-LAST:event_AddUserConfirmButtonActionPerformed

    private void AddUserCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddUserCancelButtonActionPerformed
        // TODO add your handling code here:
        AddUserDialog.dispose();
    }//GEN-LAST:event_AddUserCancelButtonActionPerformed

    private void UpdateUserConfirmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UpdateUserConfirmButtonActionPerformed
        // TODO add your handling code here:
        updateUserData();
        UpdateUserDialog.dispose();
        loadUserData();
    }//GEN-LAST:event_UpdateUserConfirmButtonActionPerformed

    private void UpdateUserCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UpdateUserCancelButtonActionPerformed
        // TODO add your handling code here:
        UpdateUserDialog.dispose();
    }//GEN-LAST:event_UpdateUserCancelButtonActionPerformed

    private void ToggleReturnButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToggleReturnButtonActionPerformed
        // TODO add your handling code here:
        toggleReturnStatus();
    }//GEN-LAST:event_ToggleReturnButtonActionPerformed

    private void BookUpdateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BookUpdateButtonActionPerformed
            // TODO add your handling code here:
        int selectedRow = BookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "수정할 행을 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 선택된 행의 도서명을 가져오기
        String title = BookTable.getValueAt(selectedRow, 0).toString(); // 도서명은 첫 번째 열에 있다고 가정

        // DB에서 title에 해당하는 데이터 가져오기
        try {
            db.dbOpen(); // DB 연결

            String query = "SELECT title, author, publisher, year_published FROM books WHERE title = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(query);
            pstmt.setString(1, title); // 도서명으로 검색

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // 가져온 데이터로 수정 창의 텍스트 필드 업데이트
                UpdateTitleTextField.setText(rs.getString("title"));
                UpdateAuthorTextField.setText(rs.getString("author"));
                UpdatePublisherTextField.setText(rs.getString("publisher"));
                UpdatePublishedyearTextField.setText(String.valueOf(rs.getInt("year_published")));
            } else {
                JOptionPane.showMessageDialog(this, "도서 데이터를 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }

            rs.close();
            pstmt.close();
            db.dbClose();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터 로드 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
        UpdateBookDialog.setVisible(true);
    }//GEN-LAST:event_BookUpdateButtonActionPerformed

    private void UserAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UserAddButtonActionPerformed
        // TODO add your handling code here:
        AddUserDialog.setVisible(true);
    }//GEN-LAST:event_UserAddButtonActionPerformed

    private void UserUpdateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UserUpdateButtonActionPerformed
        // TODO add your handling code here:
        int selectedRow = UserTable.getSelectedRow(); // 선택된 행의 인덱스 가져오기
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "수정할 사용자를 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 선택된 행의 user_id 가져오기
        String userId = UserTable.getValueAt(selectedRow, 0).toString(); // user_id는 첫 번째 열에 있다고 가정

        // DB에서 user_id에 해당하는 데이터 가져오기
        try {
            db.dbOpen(); // DB 연결

            String query = "SELECT * FROM users WHERE user_id = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(query);
            pstmt.setString(1, userId); // user_id로 검색

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // 가져온 데이터로 수정 창의 텍스트 필드 업데이트
                UpdateUserIdTextField.setText(rs.getString("user_id"));
                UpdateUserNameTextField.setText(rs.getString("username"));
                UpdateUserEmailTextField.setText(rs.getString("email"));
                UpdateUserPasswordField.setText("password"); // 비밀번호는 보안상 초기화
                UpdateUserRoleCombo.setSelectedItem(rs.getString("role").equals("ADMIN") ? "관리자" : "일반 사용자");
            } else {
                JOptionPane.showMessageDialog(this, "사용자 데이터를 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }

            rs.close();
            pstmt.close();
            db.dbClose();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터 로드 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }

        // 수정 창 열기
        UpdateUserDialog.setVisible(true);
    }//GEN-LAST:event_UserUpdateButtonActionPerformed

    private void BookDeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BookDeleteButtonActionPerformed
        // TODO add your handling code here:
        deleteBookData();
        loadBookData();
    }//GEN-LAST:event_BookDeleteButtonActionPerformed

    private void BookSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BookSearchButtonActionPerformed
        // TODO add your handling code here:
        searchBookData();
    }//GEN-LAST:event_BookSearchButtonActionPerformed

    private void UserSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UserSearchButtonActionPerformed
        // TODO add your handling code here:
        searchUserData();
    }//GEN-LAST:event_UserSearchButtonActionPerformed

    private void UserDeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UserDeleteButtonActionPerformed
        // TODO add your handling code here:
        deleteUserData();
    }//GEN-LAST:event_UserDeleteButtonActionPerformed

    private void LoanSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoanSearchButtonActionPerformed
        // TODO add your handling code here:
        searchLoanData();
    }//GEN-LAST:event_LoanSearchButtonActionPerformed

    private void LoanDeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoanDeleteButtonActionPerformed
        // TODO add your handling code here:
        deleteLoanData();
    }//GEN-LAST:event_LoanDeleteButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel AddAuthorLabel;
    private javax.swing.JTextField AddAuthorTextField;
    private javax.swing.JButton AddBookCancelButton;
    private javax.swing.JButton AddBookConfirmButton;
    private javax.swing.JDialog AddBookDialog;
    private javax.swing.JPanel AddBookPanel;
    private javax.swing.JLabel AddPublisherLabel;
    private javax.swing.JTextField AddPublisherTextField;
    private javax.swing.JLabel AddPublishyearLabel;
    private javax.swing.JTextField AddPublishyearTextField;
    private javax.swing.JLabel AddTitleLabel;
    private javax.swing.JTextField AddTitleTextField;
    private javax.swing.JButton AddUserCancelButton;
    private javax.swing.JButton AddUserConfirmButton;
    private javax.swing.JDialog AddUserDialog;
    private javax.swing.JLabel AddUserEmailLabel;
    private javax.swing.JTextField AddUserEmailTextField;
    private javax.swing.JLabel AddUserIdLabel;
    private javax.swing.JTextField AddUserIdTextField;
    private javax.swing.JLabel AddUserNameLabel;
    private javax.swing.JTextField AddUserNameTextField;
    private javax.swing.JPanel AddUserPanel;
    private javax.swing.JLabel AddUserPasswordLabel;
    private javax.swing.JTextField AddUserPasswordTextField;
    private javax.swing.JComboBox<String> AddUserRoleCombo;
    private javax.swing.JLabel AddUserRoleLabel;
    private javax.swing.JPanel Admin;
    private javax.swing.JTabbedPane AdminTabbedpane;
    private javax.swing.JButton BookAddButton;
    private javax.swing.JButton BookDeleteButton;
    private javax.swing.JComboBox<String> BookFilterCombo;
    private javax.swing.JPanel BookPanel;
    private javax.swing.JButton BookSearchButton;
    private javax.swing.JTextField BookSearchTextField;
    private javax.swing.JTable BookTable;
    private javax.swing.JScrollPane BookTableScroll;
    private javax.swing.JButton BookUpdateButton;
    private javax.swing.JPanel Container;
    private javax.swing.JButton LoanDeleteButton;
    private javax.swing.JComboBox<String> LoanFilterCombo;
    private javax.swing.JPanel LoanPanel;
    private javax.swing.JButton LoanSearchButton;
    private javax.swing.JTextField LoanSearchTextField;
    private javax.swing.JTable LoanTable;
    private javax.swing.JScrollPane LoanTableScroll;
    private javax.swing.JLabel TitleLabel;
    private javax.swing.JButton ToggleReturnButton;
    private javax.swing.JLabel UpdateAuthorLabel;
    private javax.swing.JTextField UpdateAuthorTextField;
    private javax.swing.JButton UpdateBookCancelButton;
    private javax.swing.JButton UpdateBookConfirmButton;
    private javax.swing.JDialog UpdateBookDialog;
    private javax.swing.JPanel UpdateBookPanel;
    private javax.swing.JLabel UpdatePublishedyearLabel;
    private javax.swing.JTextField UpdatePublishedyearTextField;
    private javax.swing.JLabel UpdatePublisherLabel;
    private javax.swing.JTextField UpdatePublisherTextField;
    private javax.swing.JLabel UpdateTitleLabel;
    private javax.swing.JTextField UpdateTitleTextField;
    private javax.swing.JButton UpdateUserCancelButton;
    private javax.swing.JButton UpdateUserConfirmButton;
    private javax.swing.JDialog UpdateUserDialog;
    private javax.swing.JLabel UpdateUserEmailLabel;
    private javax.swing.JTextField UpdateUserEmailTextField;
    private javax.swing.JLabel UpdateUserIdLabel;
    private javax.swing.JTextField UpdateUserIdTextField;
    private javax.swing.JLabel UpdateUserNameLabel;
    private javax.swing.JTextField UpdateUserNameTextField;
    private javax.swing.JPanel UpdateUserPanel;
    private javax.swing.JPasswordField UpdateUserPasswordField;
    private javax.swing.JLabel UpdateUserPasswordLabel;
    private javax.swing.JComboBox<String> UpdateUserRoleCombo;
    private javax.swing.JLabel UpdateUserRoleLabel;
    private javax.swing.JButton UserAddButton;
    private javax.swing.JButton UserDeleteButton;
    private javax.swing.JComboBox<String> UserFilterCombo;
    private javax.swing.JPanel UserPanel;
    private javax.swing.JButton UserSearchButton;
    private javax.swing.JTextField UserSearchTextField;
    private javax.swing.JTable UserTable;
    private javax.swing.JScrollPane UserTableScroll;
    private javax.swing.JButton UserUpdateButton;
    // End of variables declaration//GEN-END:variables
}
