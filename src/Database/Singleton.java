package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static Database.DatabaseConfig.*;

public class Singleton {





    static {
        try {
            Class.forName(NOM_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    // Creates a NEW connection per call (thread-safe for Swing EDT + Network threads)
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL_DB,USERNAME,PASSWORD);
    }

//    // Creates a connection to a remote host's database
//    public static Connection getRemoteConnection(String hostIP) throws SQLException {
//        return DriverManager.getConnection(DatabaseConfig.getRemoteURL(hostIP), USERNAME, PASSWORD);
//    }

    public static void main(String[] args) {
//        System.out.println(Singleton.getConnection());

        String request_insertion= "insert into etudiant values(65468776,'test','user',15);";

//        if(con!=null){
//            try {
//                Statement st= con.createStatement();
//                int a= st.executeUpdate(request_insertion);
//                if (a>0){
//                    System.out.println("inserted successfully");
//                }
//                else{
//                    System.out.println("insertion failed");
//                }
//
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
//        }

//        String request_selection = "select * from etudiant;";
//
//        if(con!=null){
//            try {
//                Statement st = con.createStatement();
//                ResultSet res=st.executeQuery(request_selection);
//                ResultSetMetaData rsmd=res.getMetaData();
//                int nbcol=rsmd.getColumnCount();
//                while (res.next()){
////                    System.out.println(res.getInt("cin")+" "+res.getString("nom")+" "+res.getString("prenom")+" "+res.getFloat("moyenne"));
//                    for (int i = 0; i < nbcol; i++) {
//                        System.out.print(rsmd.getColumnName(i+1)+" : "+res.getObject(i+1)+"\t\t");
//
//                    }
//                    System.out.println();
//                }
//
//
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
//        }
    }
}
