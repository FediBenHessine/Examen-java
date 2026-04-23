package Database;

import java.sql.ResultSet;

public interface EtudiantDAO {
    int insertEtudiant(int cin, String nom,String prenom,Double moyenne);
    int modifierEtudiant(int cin, String nom,String prenom,Double moyenne);
    int deleteEtudiant(int cin);
    ResultSet selectionnerEtudiant(String query);
    void afficherEtudiant(ResultSet rs);

}
