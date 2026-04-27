package Database;

public class DatabaseConfig {
    public static final String NOM_DRIVER="com.mysql.cj.jdbc.Driver";
    public static final String IPServeur ="localhost";
    public static final String PORT= "3306";
    public static final String DataBaseName="Whiteboard_DB";
    public static final String USERNAME="root";
    public static final String PASSWORD="";

    // Method to get URL for remote host
    public static String getRemoteURL(String hostIP) {
        return "jdbc:mysql://" + hostIP + ":" + PORT + "/" + DataBaseName;
    }

    // Default local URL
    public static final String URL_DB = getRemoteURL(IPServeur);
}
