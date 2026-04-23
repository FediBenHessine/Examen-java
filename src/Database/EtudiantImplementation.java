package Database;

import java.sql.*;

public class EtudiantImplementation implements EtudiantDAO{
    Connection con;
    public EtudiantImplementation() {
        con=Singleton.getConnection();
    }

    @Override
    public int insertEtudiant(int cin, String nom, String prenom, Double moyenne) {
        String request_insertion= "insert into etudiant values("+cin+",'"+nom+"','"+prenom+"',"+moyenne+");";

        if(con!=null){
            try {
                Statement st= con.createStatement();
                int a= st.executeUpdate(request_insertion);
                if (a>0){
                    System.out.println("inserted successfully");
                    return a;
                }
                else{
                    System.out.println("insertion failed");
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return 0;

    }

    @Override
    public int modifierEtudiant(int cin, String nom, String prenom, Double moyenne) {
        String request_update= "update etudiant set nom = '"+nom+"' ,prenom ='"+prenom+"' , moyenne ="+moyenne+" where cin = "+cin+" ;";

        if(con!=null){
            try {
                Statement st= con.createStatement();
                int a= st.executeUpdate(request_update);
                if (a>0){
                    System.out.println("update successfully");
                    return a;
                }
                else{
                    System.out.println("update failed");
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return 0;
    }

    @Override
    public int deleteEtudiant(int cin) {
        String request_deletion= "delete from etudiant where cin = "+cin+";";

        if(con!=null){
            try {
                Statement st= con.createStatement();
                int a= st.executeUpdate(request_deletion);
                if (a>0){
                    System.out.println("deleted successfully");
                    return a;
                }
                else{
                    System.out.println("deletion failed");
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return 0;
    }

    @Override
    public ResultSet selectionnerEtudiant(String query) {


        if(con!=null){
            try {
                Statement st = con.createStatement();
                return st.executeQuery(query);



            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public void afficherEtudiant(ResultSet rs) {

        try {
            ResultSetMetaData rsmd=rs.getMetaData();
            int nbcol = rsmd.getColumnCount();

        while (rs.next()){
//                    System.out.println(res.getInt("cin")+" "+res.getString("nom")+" "+res.getString("prenom")+" "+res.getFloat("moyenne"));
            for (int i = 0; i < nbcol; i++) {
                System.out.print(rsmd.getColumnName(i+1)+" : "+rs.getObject(i+1)+"\t\t");

            }
            System.out.println();
        }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        EtudiantImplementation em =new EtudiantImplementation();
//        System.out.println(em.insertEtudiant(1236,"klali","firas",17.25));
        em.modifierEtudiant(123,"klaliii","firas",19.0);
        String request_selection = "select * from etudiant;";
        em.afficherEtudiant(em.selectionnerEtudiant(request_selection));
        em.deleteEtudiant(1236);


    }
}
