public enum SessionAction {

    LOGIN("logged in"),
    LOGOUT("logged out");

    private final String message;

    SessionAction(String mes) {
        message = mes;
    }

    public String message() {
        return message;
    }

}