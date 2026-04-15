
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author pc
 */
public class SignUp extends javax.swing.JFrame {
    private String captchaText; // 생성된 캡챠 텍스트
    /**
     * Creates new form SignIn
     */
    public SignUp() {
        try { System.setOut(new java.io.PrintStream(System.out, true, "UTF-8")); } 
        catch (java.io.UnsupportedEncodingException ex) { ex.printStackTrace(); }
        
        initComponents();
        generateCaptcha(); // 캡챠 생성 및 표시
        
        IdTextField.setPreferredSize(new Dimension(75, 25));
        PasswordField.setPreferredSize(new Dimension(75, 25));
        NameTextField.setPreferredSize(new Dimension(75, 25));
        EmailTextField.setPreferredSize(new Dimension(75, 25));
        SignupButton.setPreferredSize(new Dimension(50, 20));
    }
    
    private void generateCaptcha() {
        captchaText = generateCaptchaText();
        CaptchaLabel.setIcon(new ImageIcon(createCaptchaImage(captchaText)));
    }

    private String generateCaptchaText() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"; // 문자열 내의 캐릭터들로 랜덤 생성
        StringBuilder captcha = new StringBuilder();
        Random rand = new Random();
        // 5자리의 랜덤 문자열 생성
        while (captcha.length() < 5) {
            int index = (int) (rand.nextFloat() * chars.length());
            captcha.append(chars.charAt(index));
        }
        return captcha.toString();
    }

    private BufferedImage createCaptchaImage(String captchaText) {
        BufferedImage captchaImage = new BufferedImage(120, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = captchaImage.createGraphics();
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 120, 50);
        g2d.setColor(Color.BLACK);
        g2d.drawString(captchaText, 20, 35);
        g2d.dispose();
        return captchaImage;
    }
    
    private boolean validateInputs() {
        // 1. 비밀번호 검증
        String password = new String(PasswordField.getPassword()).trim();
        String passwordConfirm = new String(PasswordConfirmField.getPassword()).trim();
        if (password.length() < 8 || !password.matches(".*[A-Z].*") || 
            !password.matches(".*[a-z].*") || !password.matches(".*\\d.*") || 
            !password.matches(".*[!@#$%^&*()].*")) {
            WarningLabel.setText("비밀번호는 8자 이상, 대소문자, 숫자, 특수문자를 포함해야 합니다.");
            return false;
        }
        if (!password.equals(passwordConfirm)) {
            WarningLabel.setText("비밀번호 확인이 일치하지 않습니다.");
            return false;
        }

        // 2. 아이디 검증
        String userId = IdTextField.getText().trim();
        if (userId.length() < 4 || userId.length() > 20 || !userId.matches("^[a-zA-Z0-9]+$")) {
            WarningLabel.setText("아이디는 4자 이상 20자 이하, 영문자와 숫자만 포함해야 합니다.");
            return false;
        }

        // 3. 이름 검증
        String name = NameTextField.getText().trim();
        if (name.length() < 2 || name.length() > 50 || !name.matches("^[가-힣a-zA-Z]+$")) {
            WarningLabel.setText("이름은 2자 이상 50자 이하, 한글 또는 영문자만 포함해야 합니다.");
            return false;
        }

        // 4. 이메일 검증
        String email = EmailTextField.getText().trim();
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            WarningLabel.setText("유효한 이메일 형식이 아닙니다.");
            return false;
        }

        // 5. 캡챠 검증
        String enteredCaptcha = CaptchaTextField.getText().trim();
        if (!enteredCaptcha.equalsIgnoreCase(captchaText)) {
            WarningLabel.setText("캡챠가 일치하지 않습니다. 다시 시도하세요.");
            generateCaptcha(); // 캡챠 재생성
            return false;
        }

        // 모든 검증을 통과했으면 true 반환
        return true;
    }
    
    private void saveUserToDatabase(String userId, String password, String username, String email) {
        String strURL = "jdbc:mysql://localhost:3306/LMS?characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false"; // MySQL의 포트와 데이터베이스명(LMS)을 지정
        String strUser = "root"; // MySQL 사용자 이름
        String strPWD = "rootpw"; // MySQL 비밀번호

        try (Connection conn = DriverManager.getConnection(strURL, strUser, strPWD)) {
            String sql = "INSERT INTO users (user_id, password, username, email) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);   // txtId를 통해 전달받은 user_id 값
            pstmt.setString(2, password); // txtPass를 통해 전달받은 password 값
            pstmt.setString(3, username); // txtName를 통해 전달받은 username 값
            pstmt.setString(4, email);    // txtEmail를 통해 전달받은 email 값

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                JOptionPane.showMessageDialog(this, "회원가입이 완료되었습니다!");
                redirectToLogin(); // 회원가입 성공 시 로그인 창으로 돌아감
            } else {
                WarningLabel.setText("회원가입에 실패했습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            WarningLabel.setText("데이터베이스 연결 오류가 발생했습니다.");
        }
    }

    private void redirectToLogin() {
        LogIn loginWindow = new LogIn();
        loginWindow.setVisible(true);
        this.dispose(); // 현재 회원가입 창을 닫음
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Signup = new javax.swing.JPanel();
        Container = new javax.swing.JPanel();
        TitleLabel = new javax.swing.JLabel();
        IdLabel = new javax.swing.JLabel();
        PasswordLabel = new javax.swing.JLabel();
        NameLabel = new javax.swing.JLabel();
        EmailLabel = new javax.swing.JLabel();
        IdTextField = new javax.swing.JTextField();
        PasswordField = new javax.swing.JPasswordField();
        NameTextField = new javax.swing.JTextField();
        EmailTextField = new javax.swing.JTextField();
        CaptchaLabel = new javax.swing.JLabel();
        CaptchaTextField = new javax.swing.JTextField();
        WarningLabel = new javax.swing.JLabel();
        SignupButton = new javax.swing.JButton();
        PasswordConfirmLabel = new javax.swing.JLabel();
        PasswordConfirmField = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Signup.setBackground(new java.awt.Color(204, 204, 255));
        Signup.setPreferredSize(new java.awt.Dimension(1200, 800));
        Signup.setLayout(new java.awt.GridBagLayout());

        Container.setBackground(new java.awt.Color(204, 204, 255));
        Container.setFocusCycleRoot(true);
        Container.setPreferredSize(new java.awt.Dimension(600, 500));

        TitleLabel.setFont(new java.awt.Font("나눔바른펜", 1, 24)); // NOI18N
        TitleLabel.setForeground(new java.awt.Color(153, 153, 255));
        TitleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        TitleLabel.setText("SignUp");

        IdLabel.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        IdLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        IdLabel.setText("아이디");

        PasswordLabel.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        PasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        PasswordLabel.setText("비밀번호");

        NameLabel.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        NameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        NameLabel.setText("이름");
        NameLabel.setPreferredSize(new java.awt.Dimension(28, 32));

        EmailLabel.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        EmailLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        EmailLabel.setText("이메일");

        IdTextField.setMinimumSize(new java.awt.Dimension(326, 32));
        IdTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        PasswordField.setMinimumSize(new java.awt.Dimension(326, 32));
        PasswordField.setPreferredSize(new java.awt.Dimension(326, 32));

        NameTextField.setMinimumSize(new java.awt.Dimension(326, 32));
        NameTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        EmailTextField.setMinimumSize(new java.awt.Dimension(326, 32));
        EmailTextField.setPreferredSize(new java.awt.Dimension(326, 32));

        SignupButton.setBackground(new java.awt.Color(153, 153, 255));
        SignupButton.setFont(new java.awt.Font("나눔스퀘어", 0, 12)); // NOI18N
        SignupButton.setText("회원가입");
        SignupButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        SignupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SignupButtonActionPerformed(evt);
            }
        });

        PasswordConfirmLabel.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        PasswordConfirmLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        PasswordConfirmLabel.setText("비밀번호 확인");

        PasswordConfirmField.setMinimumSize(new java.awt.Dimension(326, 32));
        PasswordConfirmField.setPreferredSize(new java.awt.Dimension(326, 32));

        javax.swing.GroupLayout ContainerLayout = new javax.swing.GroupLayout(Container);
        Container.setLayout(ContainerLayout);
        ContainerLayout.setHorizontalGroup(
            ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ContainerLayout.createSequentialGroup()
                .addGap(69, 69, 69)
                .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ContainerLayout.createSequentialGroup()
                        .addComponent(PasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(PasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(ContainerLayout.createSequentialGroup()
                        .addComponent(IdLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(IdTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(ContainerLayout.createSequentialGroup()
                        .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(ContainerLayout.createSequentialGroup()
                                .addComponent(CaptchaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(EmailTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(SignupButton, javax.swing.GroupLayout.PREFERRED_SIZE, 464, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(WarningLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 464, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(ContainerLayout.createSequentialGroup()
                                    .addComponent(EmailLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(CaptchaTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(TitleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ContainerLayout.createSequentialGroup()
                                        .addComponent(NameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(NameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(ContainerLayout.createSequentialGroup()
                                        .addComponent(PasswordConfirmLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(PasswordConfirmField, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE)))))
                        .addGap(67, 67, 67))))
        );
        ContainerLayout.setVerticalGroup(
            ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ContainerLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(TitleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(IdLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(IdTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(PasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(PasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(PasswordConfirmLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(PasswordConfirmField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CaptchaTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(EmailLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(ContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ContainerLayout.createSequentialGroup()
                        .addComponent(CaptchaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ContainerLayout.createSequentialGroup()
                        .addComponent(EmailTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(45, 45, 45)))
                .addComponent(WarningLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SignupButton, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        Signup.add(Container, new java.awt.GridBagConstraints());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Signup, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Signup, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void SignupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SignupButtonActionPerformed
        if (validateInputs()) {
            String userId = IdTextField.getText().trim();
            String password = new String(PasswordField.getPassword()).trim();
            String name = NameTextField.getText().trim();
            String email = EmailTextField.getText().trim();

            saveUserToDatabase(userId, password, name, email);
        }
    }//GEN-LAST:event_SignupButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SignUp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SignUp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SignUp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SignUp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SignUp().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel CaptchaLabel;
    private javax.swing.JTextField CaptchaTextField;
    private javax.swing.JPanel Container;
    private javax.swing.JLabel EmailLabel;
    private javax.swing.JTextField EmailTextField;
    private javax.swing.JLabel IdLabel;
    private javax.swing.JTextField IdTextField;
    private javax.swing.JLabel NameLabel;
    private javax.swing.JTextField NameTextField;
    private javax.swing.JPasswordField PasswordConfirmField;
    private javax.swing.JLabel PasswordConfirmLabel;
    private javax.swing.JPasswordField PasswordField;
    private javax.swing.JLabel PasswordLabel;
    private javax.swing.JPanel Signup;
    private javax.swing.JButton SignupButton;
    private javax.swing.JLabel TitleLabel;
    private javax.swing.JLabel WarningLabel;
    // End of variables declaration//GEN-END:variables
}
