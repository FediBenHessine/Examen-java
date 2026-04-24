package Model;
public enum RoomType {
    PUBLIC("Public - No Password"),
    PUBLIC_PASSWORD("Public - Password Required"),
    PRIVATE("Private - IP + Password Required");

    private final String label;
    RoomType(String label) { this.label = label; }
    @Override public String toString() { return label; }
}