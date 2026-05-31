package finalsprojectsystem;

import java.sql.SQLException;
import javax.swing.UIManager;
import java.awt.Color;

public class Main {
    public static void main(String[] args) throws SQLException {

        UIManager.put("Button.focus", new Color(0, 0, 0, 0));

        DBconnection.getConnection(); 
            
        java.awt.EventQueue.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}