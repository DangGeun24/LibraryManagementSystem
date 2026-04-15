
import java.awt.Dimension;
import java.awt.Image;
import javax.swing.ImageIcon;
import java.sql.*;
import java.text.SimpleDateFormat;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.util.Date;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author pc
 */
public class BookInfo_User extends javax.swing.JFrame {
    DB_Manager db = new DB_Manager(); // DB_Manager 클래스 인스턴스 생성
    private final int userPin;
    /**
     * Creates new form BookInfo
     */   
    public BookInfo_User(int userPin) {
        // 에러메세지 한글변환
        try { System.setOut(new java.io.PrintStream(System.out, true, "UTF-8")); } 
        catch (java.io.UnsupportedEncodingException ex) { ex.printStackTrace(); }
        
        this.userPin = userPin; // 로그인한 사용자의 식별 번호 저장
        initComponents();
        loadBookData(); // 도서 데이터 로드
        loadLoanData(); // 대출 데이터 로드
        loadUserData(); // 유저 데이터 로드
        checkUnreturnedBooks(); // 미반납 도서 확인
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
    
    private void loadLoanData() {
        try {
            db.dbOpen();
            DefaultTableModel model = (DefaultTableModel) LoanTable.getModel();
            model.setRowCount(0); // 기존 데이터 초기화

            String sql = "SELECT b.title, b.author, b.publisher, b.year_published, l.due_date, l.return_date " +
                         "FROM loans l " +
                         "JOIN books b ON l.book_pin = b.book_pin " +
                         "WHERE l.user_pin = ? " +
                         "AND l.return_date IS NULL"; // 특정 사용자의 미반납 대출 현황만 조회
            PreparedStatement pstmt = db.DB_con.prepareStatement(sql);
            pstmt.setInt(1, this.userPin); // 사용자 ID에 해당하는 값으로 설정
            db.DB_rs = pstmt.executeQuery();

            while (db.DB_rs.next()) {
                Object[] row = {
                    db.DB_rs.getString("title"),
                    db.DB_rs.getString("author"),
                    db.DB_rs.getString("publisher"),
                    db.DB_rs.getInt("year_published"),
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
    
    private void loadUserData() {
        try {
            db.dbOpen(); // DB 연결
            String sql = "SELECT username, email FROM users WHERE user_pin = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(sql);
            pstmt.setInt(1, userPin); // 현재 사용자 ID
            db.DB_rs = pstmt.executeQuery();

            if (db.DB_rs.next()) {
                // 사용자 정보 입력 필드에 설정
                NameTextField.setText(db.DB_rs.getString("username"));
                EmailTextField.setText(db.DB_rs.getString("email"));
            }
            db.dbClose(); // DB 연결 종료
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
    
    private void checkUnreturnedBooks() {
        try {
            db.dbOpen(); // DB 연결
            String sql = "SELECT b.title, b.author, l.due_date FROM loans l " +
                         "JOIN books b ON l.book_pin = b.book_pin " +
                         "WHERE l.user_pin = ? AND l.return_date IS NULL"; // 반납되지 않은 도서만 조회
            PreparedStatement pstmt = db.DB_con.prepareStatement(sql);
            pstmt.setInt(1, this.userPin); // 현재 사용자 ID
            db.DB_rs = pstmt.executeQuery();

            // 미반납 도서 목록 생성
            StringBuilder unreturnedBooks = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            boolean hasOverdue = false;

            while (db.DB_rs.next()) {
                String title = db.DB_rs.getString("title");
                String author = db.DB_rs.getString("author");
                Date dueDate = db.DB_rs.getDate("due_date");

                // 현재 날짜와 비교하여 연체 여부 확인
                Date currentDate = new Date(); // 오늘 날짜
                boolean isOverdue = dueDate.before(currentDate);

                if (isOverdue) {
                    hasOverdue = true;
                    unreturnedBooks.append("- ").append(title).append(" (").append(author).append(") - **연체됨** (반납기한: ").append(sdf.format(dueDate)).append(")\n");
                } else {
                    unreturnedBooks.append("- ").append(title).append(" (").append(author).append(") (반납기한: ").append(sdf.format(dueDate)).append(")\n");
                }
            }
            db.dbClose();

            // 미반납 도서가 있는 경우 팝업 표시
            if (unreturnedBooks.length() > 0) {
                String message = "미반납 도서가 있습니다:\n\n" + unreturnedBooks.toString();
                if (hasOverdue) {
                    JOptionPane.showMessageDialog(this, 
                        message, 
                        "미반납 및 연체 도서 알림", 
                        JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        message, 
                        "미반납 도서 알림", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isValidEmail(String email) {
        // 이메일 정규식 패턴
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email.matches(emailRegex);
    }

    private boolean isValidPassword(String password) {
        // 비밀번호 정규식 패턴: 최소 8자, 대문자, 소문자, 숫자, 특수문자 포함
        String passwordRegex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordRegex);
    }

    private void validatePassword() {
        String currentPassword = JOptionPane.showInputDialog(this, "현재 비밀번호를 입력하세요:", "비밀번호 확인", JOptionPane.PLAIN_MESSAGE);
        if (currentPassword == null || currentPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "비밀번호를 입력해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            db.dbOpen(); // DB 연결
            String sql = "SELECT password FROM users WHERE user_pin = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(sql);
            pstmt.setInt(1, userPin);
            db.DB_rs = pstmt.executeQuery();

            if (db.DB_rs.next() && db.DB_rs.getString("password").equals(currentPassword)) {
                db.dbClose();
                loadUserData(); // 사용자 정보 로드
                UserupdateDialog.setVisible(true); // 다이얼로그 표시
            } else {
                db.dbClose();
                JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
      
    private void updateUserInfo() {
        // 입력값 가져오기
        String newName = NameTextField.getText().trim();
        String newEmail = EmailTextField.getText().trim();
        String newPassword = new String(NewPasswordField.getPassword()).trim(); // PasswordField에서 문자열로 변환
        String confirmPassword = new String(ConfirmNewPasswordField.getPassword()).trim();


        // 1. 필수 입력값 확인
        if (newName.isEmpty() || newEmail.isEmpty()) {
            JOptionPane.showMessageDialog(UserupdateDialog, "이름과 이메일은 필수 입력값입니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. 이메일 형식 검증
        if (!isValidEmail(newEmail)) {
            JOptionPane.showMessageDialog(UserupdateDialog, "올바른 이메일 형식을 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. 새 비밀번호가 입력된 경우 비밀번호 형식 검증
        if (!newPassword.isEmpty() && !isValidPassword(newPassword)) {
            JOptionPane.showMessageDialog(UserupdateDialog, "비밀번호는 최소 8자, 대문자, 소문자, 숫자, 특수문자를 포함해야 합니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. 새 비밀번호와 확인 비밀번호 일치 여부 확인
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(UserupdateDialog, "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            db.dbOpen(); // DB 연결

            // 사용자 정보 업데이트
            String updateSql = "UPDATE users SET username = ?, email = ?, password = ? WHERE user_pin = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(updateSql);
            pstmt.setString(1, newName);
            pstmt.setString(2, newEmail);
            pstmt.setString(3, newPassword.isEmpty() ? null : newPassword); // 새 비밀번호가 없으면 null
            pstmt.setInt(4, userPin);

            int result = pstmt.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(UserupdateDialog, "회원정보가 성공적으로 수정되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                UserupdateDialog.dispose(); // 다이얼로그 닫기
            } else {
                JOptionPane.showMessageDialog(UserupdateDialog, "회원정보 수정에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
            db.dbClose(); // DB 연결 종료
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void borrowBook() {
    int selectedRow = BookTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "대출할 도서를 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // 선택한 도서 정보 가져오기
    String bookTitle = BookTable.getValueAt(selectedRow, 0).toString(); // 도서명
    String isBorrowed = BookTable.getValueAt(selectedRow, 4).toString(); // 대출 가능 여부

    if ("대출 중".equals(isBorrowed)) {
        JOptionPane.showMessageDialog(this, "선택한 도서는 이미 대출 중입니다.", "오류", JOptionPane.ERROR_MESSAGE);
        return;
    }

    try {
        db.dbOpen(); // DB 연결

        // 도서 정보를 업데이트하여 대출 처리
        String borrowSql = "UPDATE books SET is_borrowed = 1 WHERE title = ?"; // 대출 상태 업데이트
        PreparedStatement borrowPstmt = db.DB_con.prepareStatement(borrowSql);
        borrowPstmt.setString(1, bookTitle);
        int rowsUpdated = borrowPstmt.executeUpdate();

        if (rowsUpdated > 0) {
            // 대출 기록 추가
            String insertLoanSql = "INSERT INTO loans (user_pin, book_pin, loan_date, due_date) " +
                                   "SELECT ?, book_pin, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY) " +
                                   "FROM books WHERE title = ?";
            PreparedStatement insertLoanPstmt = db.DB_con.prepareStatement(insertLoanSql);
            insertLoanPstmt.setInt(1, userPin); // 로그인한 사용자의 user_pin
            insertLoanPstmt.setString(2, bookTitle);
            insertLoanPstmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "도서 대출이 완료되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
            loadBookData(); // 테이블 데이터 새로고침
        } else {
            JOptionPane.showMessageDialog(this, "도서 대출에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }

        db.dbClose(); // DB 연결 종료
    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "대출 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
    }
}
    
    private void returnBook() {
    int selectedRow = LoanTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "반납할 도서를 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // 선택한 도서 정보 가져오기
    String bookTitle = LoanTable.getValueAt(selectedRow, 0).toString(); // 도서명
    String returnDate = LoanTable.getValueAt(selectedRow, 5).toString(); // 반납 날짜

    if (!"미반납".equals(returnDate)) {
        JOptionPane.showMessageDialog(this, "선택한 도서는 이미 반납되었습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        return;
    }

    try {
        db.dbOpen(); // DB 연결

        // 반납 처리: 대출 기록에 반납 날짜 설정
        String updateLoanSql = "UPDATE loans l " +
                               "JOIN books b ON l.book_pin = b.book_pin " +
                               "SET l.return_date = CURRENT_DATE, b.is_borrowed = 0 " +
                               "WHERE l.user_pin = ? AND b.title = ?";
        PreparedStatement updateLoanPstmt = db.DB_con.prepareStatement(updateLoanSql);
        updateLoanPstmt.setInt(1, userPin); // 로그인한 사용자의 user_pin
        updateLoanPstmt.setString(2, bookTitle);
        int rowsUpdated = updateLoanPstmt.executeUpdate();

        if (rowsUpdated > 0) {
            JOptionPane.showMessageDialog(this, "도서 반납이 완료되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
            loadLoanData(); // 대출 데이터 새로고침
            loadBookData(); // 도서 데이터 새로고침
        } else {
            JOptionPane.showMessageDialog(this, "도서 반납에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }

        db.dbClose(); // DB 연결 종료
    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "반납 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
    }
}
    
    private void extendLoanPeriod() {
    int selectedRow = LoanTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "연장할 도서를 선택하세요.", "오류", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // 선택한 도서 정보 가져오기
    String bookTitle = LoanTable.getValueAt(selectedRow, 0).toString(); // 도서명
    String returnDate = LoanTable.getValueAt(selectedRow, 5).toString(); // 반납 날짜

    if (!"미반납".equals(returnDate)) {
        JOptionPane.showMessageDialog(this, "이미 반납된 도서는 연장할 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        return;
    }

    try {
        db.dbOpen(); // DB 연결

        // 연장 가능 여부 확인
        String checkExtensionSql = "SELECT l.extension_count, l.due_date FROM loans l " +
                                   "JOIN books b ON l.book_pin = b.book_pin " +
                                   "WHERE l.user_pin = ? AND b.title = ? AND l.return_date IS NULL";
        PreparedStatement checkPstmt = db.DB_con.prepareStatement(checkExtensionSql);
        checkPstmt.setInt(1, userPin);
        checkPstmt.setString(2, bookTitle);
        db.DB_rs = checkPstmt.executeQuery();

        if (db.DB_rs.next()) {
            int extensionCount = db.DB_rs.getInt("extension_count");
            if (extensionCount >= 2) {
                JOptionPane.showMessageDialog(this, " 최대 2번까지만 연장할 수 있습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                db.dbClose();
                return;
            }

            // 반납 기한 연장
            String extendSql = "UPDATE loans l " +
                               "JOIN books b ON l.book_pin = b.book_pin " +
                               "SET l.due_date = DATE_ADD(l.due_date, INTERVAL 7 DAY), l.extension_count = l.extension_count + 1 " +
                               "WHERE l.user_pin = ? AND b.title = ? AND l.return_date IS NULL";
            PreparedStatement extendPstmt = db.DB_con.prepareStatement(extendSql);
            extendPstmt.setInt(1, userPin);
            extendPstmt.setString(2, bookTitle);
            int rowsUpdated = extendPstmt.executeUpdate();

            if (rowsUpdated > 0) {
                JOptionPane.showMessageDialog(this, "반납 기한이 일주일 연장되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                loadLoanData(); // 대출 데이터 새로고침
            } else {
                JOptionPane.showMessageDialog(this, "반납 기한 연장에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "연장할 수 있는 대출 정보가 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }

        db.dbClose(); // DB 연결 종료
    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "반납 기한 연장 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
    }
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

        UserupdateDialog = new javax.swing.JDialog();
        UserupdateContainer = new javax.swing.JPanel();
        NameLabel = new javax.swing.JLabel();
        NameTextField = new javax.swing.JTextField();
        EmailLabel = new javax.swing.JLabel();
        EmailTextField = new javax.swing.JTextField();
        CancelButton = new javax.swing.JButton();
        UpdateButton = new javax.swing.JButton();
        NewPasswordLabel = new javax.swing.JLabel();
        NewPasswordConfirmLabel = new javax.swing.JLabel();
        ConfirmNewPasswordField = new javax.swing.JPasswordField();
        NewPasswordField = new javax.swing.JPasswordField();
        User = new javax.swing.JPanel();
        Container = new javax.swing.JPanel();
        TitleLabel = new javax.swing.JLabel();
        UserTabbedpane = new javax.swing.JTabbedPane();
        BookPanel = new javax.swing.JPanel();
        BookTableScroll = new javax.swing.JScrollPane();
        BookTable = new javax.swing.JTable();
        BookSearchTextField = new javax.swing.JTextField();
        BookSearchButton = new javax.swing.JButton();
        BookFilterCombo = new javax.swing.JComboBox<>();
        BookLoanButton = new javax.swing.JButton();
        LoanPanel = new javax.swing.JPanel();
        LoanTableScroll = new javax.swing.JScrollPane();
        LoanTable = new javax.swing.JTable();
        ReturnButton = new javax.swing.JButton();
        ExpandButton = new javax.swing.JButton();
        UserPanel = new javax.swing.JPanel();
        PasswordLabel = new javax.swing.JLabel();
        ConfirmButton = new javax.swing.JButton();
        PasswordField = new javax.swing.JPasswordField();

        UserupdateDialog.setSize(new java.awt.Dimension(600, 500));
        UserupdateDialog.getContentPane().setLayout(new java.awt.GridBagLayout());

        UserupdateContainer.setPreferredSize(new java.awt.Dimension(550, 450));

        NameLabel.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        NameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        NameLabel.setText("이름");

        EmailLabel.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        EmailLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        EmailLabel.setText("이메일");

        CancelButton.setText("취소");
        CancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CancelButtonActionPerformed(evt);
            }
        });

        UpdateButton.setText("수정");
        UpdateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UpdateButtonActionPerformed(evt);
            }
        });

        NewPasswordLabel.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        NewPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        NewPasswordLabel.setText("새 비밀번호");

        NewPasswordConfirmLabel.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        NewPasswordConfirmLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        NewPasswordConfirmLabel.setText("비밀번호 확인");

        javax.swing.GroupLayout UserupdateContainerLayout = new javax.swing.GroupLayout(UserupdateContainer);
        UserupdateContainer.setLayout(UserupdateContainerLayout);
        UserupdateContainerLayout.setHorizontalGroup(
            UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UserupdateContainerLayout.createSequentialGroup()
                .addContainerGap(35, Short.MAX_VALUE)
                .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UserupdateContainerLayout.createSequentialGroup()
                        .addComponent(CancelButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(UpdateButton)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UserupdateContainerLayout.createSequentialGroup()
                        .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(UserupdateContainerLayout.createSequentialGroup()
                                .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(EmailLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                                    .addComponent(NameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(EmailTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)
                                    .addComponent(NameTextField)))
                            .addGroup(UserupdateContainerLayout.createSequentialGroup()
                                .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(NewPasswordConfirmLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                                    .addComponent(NewPasswordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(ConfirmNewPasswordField)
                                    .addComponent(NewPasswordField, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE))))
                        .addGap(83, 83, 83))))
        );
        UserupdateContainerLayout.setVerticalGroup(
            UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UserupdateContainerLayout.createSequentialGroup()
                .addGap(139, 139, 139)
                .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(EmailTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(EmailLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(NewPasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NewPasswordField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NewPasswordConfirmLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ConfirmNewPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 124, Short.MAX_VALUE)
                .addGroup(UserupdateContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UpdateButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        UserupdateDialog.getContentPane().add(UserupdateContainer, new java.awt.GridBagConstraints());

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        User.setBackground(new java.awt.Color(204, 204, 255));
        User.setPreferredSize(new java.awt.Dimension(1200, 800));
        User.setLayout(new java.awt.GridBagLayout());

        Container.setBackground(new java.awt.Color(204, 204, 255));
        Container.setPreferredSize(new java.awt.Dimension(1150, 750));

        TitleLabel.setFont(new java.awt.Font("나눔바른펜", 1, 24)); // NOI18N
        TitleLabel.setForeground(new java.awt.Color(153, 153, 255));
        TitleLabel.setText("Welcome!");
        TitleLabel.setAlignmentY(0.0F);
        TitleLabel.setAutoscrolls(true);

        UserTabbedpane.setPreferredSize(new java.awt.Dimension(1100, 650));

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

        BookSearchButton.setText("검색");
        BookSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BookSearchButtonActionPerformed(evt);
            }
        });

        BookFilterCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "도서명", "저자명", "출판사", "출판연도", "대출 가능 여부" }));
        BookFilterCombo.setPreferredSize(new java.awt.Dimension(100, 23));

        BookLoanButton.setText("대출");
        BookLoanButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BookLoanButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout BookPanelLayout = new javax.swing.GroupLayout(BookPanel);
        BookPanel.setLayout(BookPanelLayout);
        BookPanelLayout.setHorizontalGroup(
            BookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(BookPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(BookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(BookTableScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 1126, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BookPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(BookFilterCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BookSearchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BookSearchButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BookLoanButton)
                        .addGap(1, 1, 1)))
                .addContainerGap())
        );
        BookPanelLayout.setVerticalGroup(
            BookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(BookPanelLayout.createSequentialGroup()
                .addContainerGap(91, Short.MAX_VALUE)
                .addGroup(BookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(BookFilterCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BookSearchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BookSearchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BookLoanButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(BookTableScroll, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        UserTabbedpane.addTab("도서 검색", BookPanel);

        LoanPanel.setPreferredSize(new java.awt.Dimension(1138, 615));

        LoanTableScroll.setPreferredSize(new java.awt.Dimension(430, 500));

        LoanTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "도서명", "저자명", "출판사", "출판연도", "반납기한", "반납여부"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        LoanTableScroll.setViewportView(LoanTable);

        ReturnButton.setText("반납");
        ReturnButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReturnButtonActionPerformed(evt);
            }
        });

        ExpandButton.setText("연장");
        ExpandButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExpandButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout LoanPanelLayout = new javax.swing.GroupLayout(LoanPanel);
        LoanPanel.setLayout(LoanPanelLayout);
        LoanPanelLayout.setHorizontalGroup(
            LoanPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(LoanPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(LoanPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(LoanTableScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 1126, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LoanPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(ExpandButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ReturnButton)))
                .addContainerGap())
        );
        LoanPanelLayout.setVerticalGroup(
            LoanPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(LoanPanelLayout.createSequentialGroup()
                .addContainerGap(92, Short.MAX_VALUE)
                .addGroup(LoanPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ReturnButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ExpandButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(LoanTableScroll, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        UserTabbedpane.addTab("내 대출 현황", LoanPanel);

        PasswordLabel.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        PasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        PasswordLabel.setText("비밀번호");

        ConfirmButton.setText("확인");
        ConfirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ConfirmButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout UserPanelLayout = new javax.swing.GroupLayout(UserPanel);
        UserPanel.setLayout(UserPanelLayout);
        UserPanelLayout.setHorizontalGroup(
            UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UserPanelLayout.createSequentialGroup()
                .addGap(363, 363, 363)
                .addGroup(UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(ConfirmButton)
                    .addGroup(UserPanelLayout.createSequentialGroup()
                        .addComponent(PasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(PasswordField, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)))
                .addGap(363, 363, 363))
        );
        UserPanelLayout.setVerticalGroup(
            UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UserPanelLayout.createSequentialGroup()
                .addGap(275, 275, 275)
                .addGroup(UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(PasswordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                    .addComponent(PasswordField))
                .addGap(18, 18, 18)
                .addComponent(ConfirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(279, Short.MAX_VALUE))
        );

        UserTabbedpane.addTab("내 회원 정보", UserPanel);

        javax.swing.GroupLayout ContainerLayout = new javax.swing.GroupLayout(Container);
        Container.setLayout(ContainerLayout);
        ContainerLayout.setHorizontalGroup(
            ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ContainerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(UserTabbedpane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ContainerLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(TitleLabel)))
                .addContainerGap())
        );
        ContainerLayout.setVerticalGroup(
            ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ContainerLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(TitleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(UserTabbedpane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        User.add(Container, new java.awt.GridBagConstraints());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(User, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(User, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ConfirmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ConfirmButtonActionPerformed
        String Password = new String(PasswordField.getPassword()).trim(); // PasswordField에서 문자열로 변환

        // 비밀번호 일치 여부 확인
        try {
            db.dbOpen(); // DB 연결
            String sql = "SELECT password FROM users WHERE user_pin = ?";
            PreparedStatement pstmt = db.DB_con.prepareStatement(sql);
            pstmt.setInt(1, userPin);
            db.DB_rs = pstmt.executeQuery();
            
            if (db.DB_rs.next() && db.DB_rs.getString("password").equals(Password)) {
                db.dbClose();
                loadUserData(); // 사용자 정보 로드
                UserupdateDialog.setVisible(true); // 비밀번호 일치 시 다이얼로그 표시
            } else {
                db.dbClose();
                JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (!Password.equals(Password)) {
//            JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.", "오류", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
    }//GEN-LAST:event_ConfirmButtonActionPerformed

    private void CancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CancelButtonActionPerformed
        UserupdateDialog.dispose(); // 다이얼로그 닫기
    }//GEN-LAST:event_CancelButtonActionPerformed

    private void UpdateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UpdateButtonActionPerformed
        updateUserInfo();
    }//GEN-LAST:event_UpdateButtonActionPerformed

    private void BookSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BookSearchButtonActionPerformed
        // TODO add your handling code here:
        searchBookData();
    }//GEN-LAST:event_BookSearchButtonActionPerformed

    private void BookLoanButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BookLoanButtonActionPerformed
        // TODO add your handling code here:
        borrowBook();
        loadLoanData();
    }//GEN-LAST:event_BookLoanButtonActionPerformed

    private void ReturnButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReturnButtonActionPerformed
        // TODO add your handling code here:
        returnBook();
    }//GEN-LAST:event_ReturnButtonActionPerformed

    private void ExpandButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExpandButtonActionPerformed
        // TODO add your handling code here:
        extendLoanPeriod();
    }//GEN-LAST:event_ExpandButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> BookFilterCombo;
    private javax.swing.JButton BookLoanButton;
    private javax.swing.JPanel BookPanel;
    private javax.swing.JButton BookSearchButton;
    private javax.swing.JTextField BookSearchTextField;
    private javax.swing.JTable BookTable;
    private javax.swing.JScrollPane BookTableScroll;
    private javax.swing.JButton CancelButton;
    private javax.swing.JButton ConfirmButton;
    private javax.swing.JPasswordField ConfirmNewPasswordField;
    private javax.swing.JPanel Container;
    private javax.swing.JLabel EmailLabel;
    private javax.swing.JTextField EmailTextField;
    private javax.swing.JButton ExpandButton;
    private javax.swing.JPanel LoanPanel;
    private javax.swing.JTable LoanTable;
    private javax.swing.JScrollPane LoanTableScroll;
    private javax.swing.JLabel NameLabel;
    private javax.swing.JTextField NameTextField;
    private javax.swing.JLabel NewPasswordConfirmLabel;
    private javax.swing.JPasswordField NewPasswordField;
    private javax.swing.JLabel NewPasswordLabel;
    private javax.swing.JPasswordField PasswordField;
    private javax.swing.JLabel PasswordLabel;
    private javax.swing.JButton ReturnButton;
    private javax.swing.JLabel TitleLabel;
    private javax.swing.JButton UpdateButton;
    private javax.swing.JPanel User;
    private javax.swing.JPanel UserPanel;
    private javax.swing.JTabbedPane UserTabbedpane;
    private javax.swing.JPanel UserupdateContainer;
    private javax.swing.JDialog UserupdateDialog;
    // End of variables declaration//GEN-END:variables
}