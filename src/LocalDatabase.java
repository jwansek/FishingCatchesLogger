import java.math.BigInteger;
import java.security.MessageDigest;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;

public class LocalDatabase {

    private final Path dbfolder;
    private final Path dbpath;
    private final String connectionString;

    public static class FishingUser {
        private final String username;
        private String passwordHash;

        public FishingUser(String username) {
            this.username = username;
        }

        public FishingUser(String username, String plainPassword) {
            this.username = username;
            this.passwordHash = hashPassword(plainPassword);
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        public String getUsername() {
            return this.username;
        }

        public String getPasswordHash() {
            return this.passwordHash;
        }

        public static String hashPassword(String password) {
            //https://www.geeksforgeeks.org/md5-hash-in-java/
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");     //md5 is bad, use SHA
                byte[] bytes = messageDigest.digest(password.getBytes());
                BigInteger bigInt = new BigInteger(1, bytes);

                String hash = bigInt.toString(16);
                while (hash.length() < 32) {
                    hash = "0" + hash;
                }
                return hash;

            } catch (java.security.NoSuchAlgorithmException e) {
                System.out.println(e);
                return null;
            }
        }
    }

    public static class UserNotFoundException extends Exception {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class IncorrectPasswordException extends Exception {
        public IncorrectPasswordException(String message) {
            super(message);
        }
    }

    public LocalDatabase() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        dbfolder = Paths.get(System.getenv("APPDATA"), "FishingCatchesLogger");
        dbpath = Paths.get(dbfolder.toString(), "localDatabase.db");
        connectionString = String.format("jdbc:sqlite:%s", dbpath);

        if (Files.notExists(dbfolder)) {
            try {
                Files.createDirectory(dbfolder);
                createDatabase();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection(connectionString);
        Statement statement = connection.createStatement();
        statement.executeUpdate(
                "CREATE TABLE users (" +
                        "user_id INT PRIMARY KEY," +
                        "username TEXT NOT NULL," +
                        "passwordHash TEXT NOT NULL" +
                    ");"
        );
        statement.executeUpdate(
                "CREATE TABLE catches (" +
                        "catch_id INT PRIMARY KEY," +
                        "user_id INT," +
                        "latitude REAL NOT NULL," +
                        "longitude REAL NOT NULL," +
                        "weight REAL NOT NULL," +
                        "revenue REAL NOT NULL," +
                        "FOREIGN KEY (user_id) REFERENCES users (user_id)" +
                    ");"
        );
        statement.close();
        connection.close();
    }

    public void addUser(FishingUser newUser) throws SQLException {
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "INSERT INTO users (username, passwordHash) VALUES (?, ?);";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, newUser.getUsername());
        statement.setString(2, newUser.getPasswordHash());
        statement.executeUpdate();
        connection.close();
    }

    public FishingUser getUser(String username, String plainPassword) throws SQLException, UserNotFoundException, IncorrectPasswordException {
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "SELECT * FROM users WHERE username = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, username);
        statement.execute();
        ResultSet results = statement.getResultSet();
        FishingUser user;

        if (results.next()) {
            user = new FishingUser(results.getString(2));
            user.setPasswordHash(results.getString(3));
        } else {
            connection.close();
            throw new UserNotFoundException("A user with the specified username was not found in the database.");
        }

        if (user.getPasswordHash().equals(FishingUser.hashPassword(plainPassword))) {
            return user;
        } else {
            throw new IncorrectPasswordException("The incorrect password was provided for the specified user");
        }
    }

    public static void main(String[] args) {

        try {
            LocalDatabase db = new LocalDatabase();
            db.getUser("eden", "password");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
