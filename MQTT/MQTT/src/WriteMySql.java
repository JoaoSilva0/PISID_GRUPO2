import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class WriteMySql {
    static JTextArea documentLabel = new JTextArea("\n");
    static Connection connTo;
    static String sql_database_connection_to = "";
    static String sql_database_password_to = "";
    static String sql_database_user_to = "";
    static String sql_table_to = "";

    private static void createWindow() {
        JFrame frame = new JFrame("Data Bridge");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel textLabel = new JLabel("Data : ", SwingConstants.CENTER);
        textLabel.setPreferredSize(new Dimension(600, 30));
        JScrollPane scroll = new JScrollPane(documentLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setPreferredSize(new Dimension(600, 200));
        JButton b1 = new JButton("Stop the program");
        frame.getContentPane().add(textLabel, BorderLayout.PAGE_START);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(b1, BorderLayout.PAGE_END);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);
        b1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        createWindow();
        try {
            Properties p = new Properties();
            p.load(new FileInputStream("WriteMysql.ini"));
            sql_table_to = p.getProperty("sql_table_to");
            sql_database_connection_to = p.getProperty("sql_database_connection_to");
            sql_database_password_to = p.getProperty("sql_database_password_to");
            sql_database_user_to = p.getProperty("sql_database_user_to");
        } catch (Exception e) {
            System.out.println("Error reading WriteMysql.ini file " + e);
            JOptionPane.showMessageDialog(null, "The WriteMysql ini file wasn't found.", "Data Migration", JOptionPane.ERROR_MESSAGE);
        }
        connectDatabase_to();
        //ReadData(); // Não há necessidade de chamar ReadData() aqui
    }

    public static void connectDatabase_to() {
        try {
            Properties p = new Properties();
            p.load(new FileInputStream("WriteMysql.ini"));
            sql_table_to = p.getProperty("sql_table_to");
            sql_database_connection_to = p.getProperty("sql_database_connection_to");
            sql_database_password_to = p.getProperty("sql_database_password_to");
            sql_database_user_to = p.getProperty("sql_database_user_to");
            Class.forName("org.mariadb.jdbc.Driver");
            connTo = DriverManager.getConnection(sql_database_connection_to, sql_database_user_to, sql_database_password_to);
            //System.err.println(connTo.getMetaData());
            documentLabel.append("SQl Connection:" + sql_database_connection_to + "\n");
            documentLabel.append("Connection To MariaDB Destination " + sql_database_connection_to + " Suceeded" + "\n");
        } catch (Exception e) {
            System.out.println("Mysql Server Destination down, unable to make the connection. " + e);
        }
    }

    public static void WriteToMySQL(String c) {
        String convertedjson = c;
        String fields = "";
        String values = "";
        String SqlCommando = "";
        String column_database = "";
        fields = "";
        values = "";
        column_database = " ";
        String x = convertedjson.toString();
        String[] splitArray = x.split(",");
        for (int i = 0; i < splitArray.length; i++) {
            String[] splitArray2 = splitArray[i].split(" :");
            if (i == 0) fields = splitArray2[0];
            else fields = fields + ", " + splitArray2[0];
            if (i == 0) values = splitArray2[1];
            else values = values + ", " + splitArray2[1];
        }
        fields = fields.replace("\"", "");
        SqlCommando = "Insert into " + sql_table_to + " (" + fields.substring(7, fields.length()) + ") values (" + values.substring(11, values.length() - 1) + ");";
        try {
            documentLabel.append(SqlCommando.toString() + "\n");
        } catch (Exception e) {
            System.out.println(e);
        }
        try {
            Statement s = connTo.createStatement();
            int result = new Integer(s.executeUpdate(SqlCommando));
            s.close();
        } catch (Exception e) {
            System.out.println("Error Inserting in the database . " + e);
            System.out.println(SqlCommando);
        }
    }
}