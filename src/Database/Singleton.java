package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static Database.DatabaseConfig.*;

public class Singleton {

    private static final Connection con;
    static {
        try{
            Class.forName(NOM_DRIVER);
            con= DriverManager.getConnection(URL_DB,USERNAME,PASSWORD);
            System.out.println("Connected!");
        }catch (ClassNotFoundException | SQLException e){
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection(){
        return con;
    }

    public static void main(String[] args) {
        System.out.println(Singleton.getConnection());

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
