import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class LocalDatabase {

    private final Path dbfolder;
    private final Path dbpath;
    private final String connectionString;
    public FishingUser currentUser = null;

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

    /**
     * Method: createDatabase()
     *
     * Description: Builds a new SQLite database in the user's %APPDATA% folder
     * Throws a generic SQLException if there's an error
     *
     * Author: Edward Attenborough
     * Date: 07/06/2021
     */
    private void createDatabase() throws SQLException {
        System.out.println("Building new database at: " + dbpath);

        Connection connection = DriverManager.getConnection(connectionString);
        Statement statement = connection.createStatement();
        statement.executeUpdate(
                "CREATE TABLE users (" +
                        "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT NOT NULL," +
                        "email TEXT NOT NULL," +
                        "passwordHash TEXT NOT NULL" +
                    ");"
        );
        statement.executeUpdate(
                "CREATE TABLE records (" +
                        "record_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "user_id INT," +
                        "weight REAL NOT NULL," +
                        "datetime TEXT NOT NULL," + //SQLite does not have a datetime format
                        "FOREIGN KEY (user_id) REFERENCES users (user_id)" +
                    ");"
        );
        statement.executeUpdate(
                "CREATE TABLE catches (" +
                        "catch_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "record_id INTEGER," +
                        "latitude REAL NOT NULL," +
                        "longitude REAL NOT NULL," +
                        "FOREIGN KEY (record_id) REFERENCES records (record_id)" +
                    ");"
        );
        statement.executeUpdate(
                "CREATE TABLE sells (" +
                        "sell_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "record_id INTEGER," +
                        "revenue REAL NOT NULL," +
                        "FOREIGN KEY (record_id) REFERENCES records (record_id)" +
                    ");"
        );
        statement.close();
        connection.close();
    }

    /**
     * Method: addUser(String username, String email, String plainPassword)
     *
     * Description: Adds a new user to the database. Takes a username, email address, and plaintext
     * password, which will be hashed in the database with a SHA-256 algorithm
     * Throws a generic SQLException if there's an error
     *
     * Author: Edward Attenborough
     * Date: 07/06/2021
     */
    public void addUser(String username, String email, String plainPassword) throws SQLException {
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "INSERT INTO users (username, email, passwordHash) VALUES (?, ?, ?);";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, username);
        statement.setString(2, email);
        statement.setString(3, FishingUser.hashPassword(plainPassword));
        statement.executeUpdate();
        connection.close();
    }

    /**
     * Method: changeUser(String username, String plainPassword)
     *
     * Description: Changes the current user, and returns it. Takes a username and plaintext
     * password.
     * Throws a generic SQLException if there's an error. Throws a UserNotFoundException exception if
     * the given username was not found. Throws a IncorrectPasswordException if the provided password hash
     * does not match the hash in the database.
     *
     * Author: Edward Attenborough
     * Date: 07/06/2021
     */
    public FishingUser changeUser(String username, String plainPassword) throws SQLException, UserNotFoundException, IncorrectPasswordException {
        FishingUser user = searchForUser(username);

        if (user.getPasswordHash().equals(FishingUser.hashPassword(plainPassword))) {
            currentUser = user;
            return user;
        } else {
            throw new IncorrectPasswordException("The incorrect password was provided for the specified user");
        }
    }

    /**
     * Method: searchForUser(String username)
     *
     * Description: Returns a user. Takes a username only.
     * password.
     * Throws a generic SQLException if there's an error. Throws a UserNotFoundException exception if
     * the given username was not found. Throws a IncorrectPasswordException if the provided password hash
     * does not match the hash in the database.
     *
     * Author: Edward Attenborough
     * Date: 08/06/2021
     */
    public FishingUser searchForUser(String username) throws  SQLException, UserNotFoundException {
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "SELECT * FROM users WHERE username = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, username);
        statement.execute();
        ResultSet results = statement.getResultSet();
        FishingUser user;

        if (results.next()) {
            user = new FishingUser(
                    results.getString(2),
                    results.getString(3),
                    results.getInt(1)
            );
            user.setPasswordHash(results.getString(4));
        } else {
            connection.close();
            throw new UserNotFoundException("A user with the specified username was not found in the database.");
        }

        connection.close();
        return user;
    }

    private int generateRecord(LocalDateTime dateTime, double weight) throws SQLException {
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "INSERT INTO records (user_id, weight, datetime) VALUES (?, ?, ?);";
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, currentUser.getUser_id());
        statement.setDouble(2, weight);
        statement.setString(3, dateTime.toString());
        statement.executeUpdate();

        int pkey;
        ResultSet results = statement.getGeneratedKeys();
        if (results.next()) {
            pkey = results.getInt(1);
        } else {
            throw new SQLException("Couldn't get generated index");
        }
        connection.close();
        return pkey;
    }

    /**
     * Method: inputSellData(LocalDateTime dateTime, double weight, double revenue)
     *
     * Description: Adds a selling record to the database. Takes a LocalDateTime, weight, and revenue
     * as arguments.
     * Throws a generic SQLException if there's an error.
     *
     * Author: Edward Attenborough
     * Date: 08/06/2021
     */
    public void inputSellData(LocalDateTime dateTime, double weight, double revenue) throws SQLException {
        int record_id = generateRecord(dateTime, weight);
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "INSERT INTO sells (record_id, revenue) VALUES (?, ?);";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, record_id);
        statement.setDouble(2, revenue);
        statement.executeUpdate();
        connection.close();
    }

    /**
     * Method: inputCatchData(LocalDateTime dateTime, double weight, double latitude, double longitude)
     *
     * Description: Adds catch record to the database. Takes a LocalDatetime, weight, latitude and longitude as arguments.
     * Throws a generic SQLException if there's an error.
     *
     * Author: Edward Attenborough
     * Date: 08/06/2021
     */
    public void inputCatchData(LocalDateTime dateTime, double weight, double latitude, double longitude) throws SQLException {
        int record_id = generateRecord(dateTime, weight);
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "INSERT INTO catches (record_id, latitude, longitude) VALUES (?, ?, ?);";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, record_id);
        statement.setDouble(2, latitude);
        statement.setDouble(3, longitude);
        statement.executeUpdate();
        connection.close();
    }

     /**
     * Method: getAllSellRecords()
     *
     * Description: Returns all the selling records associated with the current user. Returns an ArrayList of SellRecord
      * objects
     * Throws a generic SQLException if there's an error.
     *
     * Author: Edward Attenborough
     * Date: 08/06/2021
     */
    public ArrayList<SellRecord> getAllSellRecords() throws SQLException {
        ArrayList<SellRecord> records = new ArrayList<>();

        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "SELECT records.record_id, datetime, weight, revenue FROM records INNER JOIN sells ON records.record_id = sells.record_id WHERE user_id = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, currentUser.getUser_id());
        statement.execute();
        ResultSet results = statement.getResultSet();

        while (results.next()) {
            records.add(new SellRecord(
                    results.getInt(1),
                    LocalDateTime.parse(results.getString(2)),
                    results.getDouble(3),
                    results.getDouble(4)
            ));
        }

        connection.close();
        return records;
    }

    /**
     * Method:  getAllCatchRecords()
     *
     * Description: Returns all of the catch records associated with the current user. Returns as an ArrayList
     * of CatchRecord objects.
     * Throws a generic SQLException if there's an error.
     *
     * Author: Edward Attenborough
     * Date: 08/06/2021
     */
    public ArrayList<CatchRecord> getAllCatchRecords() throws SQLException {
        ArrayList<CatchRecord> records = new ArrayList<>();

        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "SELECT records.record_id, datetime, weight, latitude, longitude FROM records INNER JOIN catches ON records.record_id = catches.record_id WHERE user_id = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, currentUser.getUser_id());
        statement.execute();
        ResultSet results = statement.getResultSet();

        while (results.next()) {
            records.add(new CatchRecord(
                    results.getInt(1),
                    LocalDateTime.parse(results.getString(2)),
                    results.getDouble(3),
                    results.getDouble(4),
                    results.getDouble(5)
            ));
        }
        connection.close();
        return records;
    }

    /**
     * Method: getAllRecords()
     *
     * Description: Returns all of the records (both catch records and selling records) associated with the current
     * user. Returns an ArrayList of Record objects (which can be casted back to their original type)
     * Throws a generic SQLException if there's an error.
     *
     * Author: Edward Attenborough
     * Date: 08/06/2021
     */
    public ArrayList<Record> getAllRecords() throws SQLException {
       ArrayList<Record> records = new ArrayList<>();

       for (Record r : getAllCatchRecords()) {
           records.add(r);
       }
       for (Record r : getAllSellRecords()) {
           records.add(r);
       }
       return records;
    }

    /**
     * Method: getAllRecords()
     *
     * Description: Returns a record (catch record or selling record) with a given record_id and associated with the
     * current user. Returns a Record object (which can be casted) or null if no record is found.
     * Throws a generic SQLException if there's an error.
     *
     * Author: Edward Attenborough
     * Date: 08/06/2021
     */
    public Record getRecordById(int record_id) throws SQLException {
        Record toReturn = null;
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "SELECT records.record_id, datetime, weight, latitude, longitude FROM records INNER JOIN catches ON records.record_id = catches.record_id WHERE user_id = ? AND records.record_id = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, currentUser.getUser_id());
        statement.setInt(2, record_id);
        statement.execute();
        ResultSet results = statement.getResultSet();

        if (results.next()) {
            toReturn = new CatchRecord(
                    results.getInt(1),
                    LocalDateTime.parse(results.getString(2)),
                    results.getDouble(3),
                    results.getDouble(4),
                    results.getDouble(5)
            );
        } else {
            sql = "SELECT records.record_id, datetime, weight, revenue FROM records INNER JOIN sells ON records.record_id = sells.record_id WHERE user_id = ? AND records.record_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setInt(1, currentUser.getUser_id());
            statement.setInt(2, record_id);
            statement.execute();
            results = statement.getResultSet();
            if (results.next()) {
                toReturn = new SellRecord(
                        results.getInt(1),
                        LocalDateTime.parse(results.getString(2)),
                        results.getDouble(3),
                        results.getDouble(4)
                );
            }
        }

        connection.close();
        return toReturn;
    }

    /**
     * Method: getRecordsByDate(LocalDateTime dateTime)
     *
     * Description: Returns records (catch records or sell records) with a given datetime associated with the current user.
     * Throws an SQLException if there's an SQL-related error.
     *
     * Author: Edward Attenborough
     * Date: 09/06/2021
     */
    public ArrayList<Record> getRecordsByDate(LocalDateTime dateTime) throws SQLException {
        ArrayList<Record> records = new ArrayList<>();

        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "SELECT records.record_id, datetime, weight, latitude, longitude FROM records INNER JOIN catches ON records.record_id = catches.record_id WHERE user_id = ? AND datetime = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, currentUser.getUser_id());
        statement.setString(2, dateTime.toString());
        statement.execute();
        ResultSet results = statement.getResultSet();

        while (results.next()) {
            records.add(new CatchRecord(
                    results.getInt(1),
                    LocalDateTime.parse(results.getString(2)),
                    results.getDouble(3),
                    results.getDouble(4),
                    results.getDouble(5)
            ));
        }

        sql = "SELECT records.record_id, datetime, weight, revenue FROM records INNER JOIN sells ON records.record_id = sells.record_id WHERE user_id = ? AND datetime = ?;";
        statement = connection.prepareStatement(sql);
        statement.setInt(1, currentUser.getUser_id());
        statement.setString(2, dateTime.toString());
        statement.execute();
        results = statement.getResultSet();

        if (results.next()) {
            records.add(new SellRecord(
                    results.getInt(1),
                    LocalDateTime.parse(results.getString(2)),
                    results.getDouble(3),
                    results.getDouble(4)
            ));
        }
        connection.close();
        return records;
    }

    /**
     * Method: getRecordsByWeight(double weight)
     *
     * Description: Returns records (catch records or sell records) with a given weight associated with the current user.
     * Throws an SQLException if there's an SQL-related error.
     *
     * Author: Edward Attenborough
     * Date: 09/06/2021
     */
    public ArrayList<Record> getRecordsByWeight(double weight) throws SQLException {
        ArrayList<Record> records = new ArrayList<>();

        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "SELECT records.record_id, datetime, weight, latitude, longitude FROM records INNER JOIN catches ON records.record_id = catches.record_id WHERE user_id = ? AND weight = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, currentUser.getUser_id());
        statement.setDouble(2, weight);
        statement.execute();
        ResultSet results = statement.getResultSet();

        while (results.next()) {
            records.add(new CatchRecord(
                    results.getInt(1),
                    LocalDateTime.parse(results.getString(2)),
                    results.getDouble(3),
                    results.getDouble(4),
                    results.getDouble(5)
            ));
        }

        sql = "SELECT records.record_id, datetime, weight, revenue FROM records INNER JOIN sells ON records.record_id = sells.record_id WHERE user_id = ? AND weight = ?;";
        statement = connection.prepareStatement(sql);
        statement.setInt(1, currentUser.getUser_id());
        statement.setDouble(2, weight);
        statement.execute();
        results = statement.getResultSet();

        if (results.next()) {
            records.add(new SellRecord(
                    results.getInt(1),
                    LocalDateTime.parse(results.getString(2)),
                    results.getDouble(3),
                    results.getDouble(4)
            ));
        }
        connection.close();
        return records;
    }

    /**
     * Method: getRecordsByRevenue(double revenue)
     *
     * Description: Returns sell records associated with the current user that have a given revenue.
     * Throws an SQLException if there's an SQL-related error.
     *
     * Author: Edward Attenborough
     * Date: 10/06/2021
     */
    public ArrayList<SellRecord> getRecordsByRevenue(double revenue) throws SQLException {
        ArrayList<SellRecord> records = new ArrayList<>();

        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "SELECT records.record_id, datetime, weight, revenue FROM records INNER JOIN sells ON records.record_id = sells.record_id WHERE user_id = ? AND revenue  = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, currentUser.getUser_id());
        statement.setDouble(2, revenue);
        statement.execute();
        ResultSet results = statement.getResultSet();

        if (results.next()) {
            records.add(new SellRecord(
                    results.getInt(1),
                    LocalDateTime.parse(results.getString(2)),
                    results.getDouble(3),
                    results.getDouble(4)
            ));
        }
        connection.close();
        return records;
    }

    /**
     * Method: getRecordsByRevenue(double revenue)
     *
     * Description: Returns catch records associated with the current user that have a given location. Takes latitude
     * and longitude as arguments.
     * Throws an SQLException if there's an SQL-related error.
     *
     * Author: Edward Attenborough
     * Date: 10/06/2021
     */
    public ArrayList<CatchRecord> getRecordsByLocation(double latitude, double longitude) throws SQLException {
        ArrayList<CatchRecord> records = new ArrayList<>();

        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "SELECT records.record_id, datetime, weight, latitude, longitude FROM records INNER JOIN catches ON records.record_id = catches.record_id WHERE user_id = ? AND latitude = ? AND longitude = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, currentUser.getUser_id());
        statement.setDouble(2, latitude);
        statement.setDouble(3, longitude);
        statement.execute();
        ResultSet results = statement.getResultSet();

        while (results.next()) {
            records.add(new CatchRecord(
                    results.getInt(1),
                    LocalDateTime.parse(results.getString(2)),
                    results.getDouble(3),
                    results.getDouble(4),
                    results.getDouble(5)
            ));
        }
        connection.close();
        return records;
    }

    public static void main(String[] args) {
        try {
            LocalDatabase db = new LocalDatabase();
//            db.addUser("eden", "gae19jtu@uea.ac.uk", "password");
            System.out.println(db.changeUser("eden", "password"));
//            db.inputSellData(LocalDateTime.now(), 420.69, 92.42);
//            db.inputCatchData(LocalDateTime.now(), 423.2, 52.639337, 1.246151);
//            for (Record r : db.getAllRecords()) {
//                System.out.println(r);
//            }
//            CatchRecord record = (CatchRecord)db.getRecordById(3);
//            System.out.println(record);
//            record.editLocation(5.21312, 63.51234);
//            System.out.println(record);
//            SellRecord record = (SellRecord) db.getRecordById(1);
//            System.out.println(record);
//            record.editRevenue(52.3);
//            System.out.println(record);
//            for (Record r : db.getRecordsByDate(LocalDateTime.parse("2021-06-08T13:55:36.287800200"))) {
//                System.out.println(r);
//            }
//            for (Record r : db.getRecordsByWeight(43.2)) {
//                System.out.println(r);
//            }
//            for (SellRecord r : db.getRecordsByRevenue(52.3)) {
//                System.out.println(r);
//            }
            for (CatchRecord r : db.getRecordsByLocation(5.21312, 63.51234)) {
                System.out.println(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class FishingUser {
        private final String username;
        private final String email;
        private final int user_id;
        private String passwordHash;

        public FishingUser(String username, String email, int user_id) {
            this.username = username;
            this.email = email;
            this.user_id = user_id;
        }

        public FishingUser(String username, String email, int user_id, String plainPassword) {
            this.username = username;
            this.email = email;
            this.user_id = user_id;
            this.passwordHash = hashPassword(plainPassword);
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        public String getUsername() {
            return this.username;
        }

        public int getUser_id() {
            return this.user_id;
        }

        public String getEmail() {
            return this.email;
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

        public String toString() {
            return String.format("%d\t%s\t%s", user_id, username, email);
        }
    }

    abstract class Record {
        public LocalDateTime date;
        public double weight;
        protected int record_id;

        public void editDate(LocalDateTime newDate) throws SQLException {
            Connection connection = DriverManager.getConnection(connectionString);
            String sql = "UPDATE records SET datetime = ? WHERE record_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, newDate.toString());
            statement.setInt(2, record_id);
            statement.execute();
            this.date = newDate;
        }

        public void editWeight(double newWeight) throws SQLException {
            Connection connection = DriverManager.getConnection(connectionString);
            String sql = "UPDATE records SET weight = ? WHERE record_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setDouble(1, newWeight);
            statement.setInt(2, record_id);
            statement.execute();
            this.weight = newWeight;
        }

        public double getWeight() {
            return weight;
        }

        public SimpleDoubleProperty weightProperty(){

            return new SimpleDoubleProperty(weight);
        }
        public SimpleStringProperty dateTimeProperty(){

            return new SimpleStringProperty(String.valueOf(date));
        }
    }

    public class CatchRecord extends Record {
        public double latitude;
        public double longitude;

        public CatchRecord(int record_id, LocalDateTime dateTime, double weight, double latitude, double longitude) {
            this.record_id = record_id;
            this.date = dateTime;
            this.weight = weight;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public void editLocation(double latitude, double longitude) throws SQLException {
            Connection connection = DriverManager.getConnection(connectionString);
            String sql = "UPDATE catches SET latitude = ?, longitude = ? WHERE record_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setDouble(1, latitude);
            statement.setDouble(2, longitude);
            statement.setInt(3, record_id);
            statement.execute();
            connection.close();
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String toString() {
            return String.format("%fkg catch at %f %f @ %s", weight, latitude, longitude, date.toString());
        }

        public SimpleDoubleProperty latitudeProperty(){
            return new SimpleDoubleProperty(latitude);
        }
        public SimpleDoubleProperty longitudeProperty(){

            return new SimpleDoubleProperty(longitude);
        }

    }

    public class SellRecord extends Record {
        public double revenue;

        public SellRecord(int record_id, LocalDateTime date, double weight, double revenue) {
            this.record_id = record_id;
            this.date = date;
            this.weight = weight;
            this.revenue = revenue;
        }

        public void editRevenue(double newRevenue) throws SQLException {
            Connection connection = DriverManager.getConnection(connectionString);
            String sql = "UPDATE sells SET revenue = ? WHERE record_id = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setDouble(1, newRevenue);
            statement.setInt(2, record_id);
            statement.execute();
            connection.close();
            this.revenue = newRevenue;
        }
        public SimpleDoubleProperty priceProperty(){

            return new SimpleDoubleProperty(revenue);
        }

        public String toString() {
            return String.format("Sold %f for $%f @ %s", weight, revenue, date.toString());
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
    
        public void deleteData(int record_id) throws SQLException {
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "DELETE FROM records WHERE record_id = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, record_id);
        statement.execute();
        String sql1 = "DELETE FROM catches WHERE record_id = ?";
        PreparedStatement statement1 = connection.prepareStatement(sql1);
        statement.setInt(1, record_id);
        statement1.execute();
        connection.close();
    }
    
        public void exportCatches(String filePath) throws SQLException, IOException{
            Connection connection = DriverManager.getConnection(connectionString);
            String sql = "SELECT * FROM records";

            Statement statement = connection.createStatement();

            ResultSet result = statement.executeQuery(sql);

            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filePath));

            fileWriter.write("record_id, user_id, weight, datetime");

            while (result.next()) {
                int recordID = result.getInt("record_id");
                int userID = result.getInt("user_id");
                double weight = result.getDouble("weight");
                String datetime = result.getString("datetime");

                String line = String.format("%i, %i, %d, %s", recordID, userID, weight, datetime);

                fileWriter.newLine();
                fileWriter.write(line);
            }
            statement.close();

            String sql1 = "SELECT * FROM sales";

            Statement statement1 = connection.createStatement();

            ResultSet result1 = statement1.executeQuery(sql1);

            fileWriter.write("sale_id, record_id, revenue");

            while (result1.next()) {
                int saleID = result1.getInt("sale_id");
                int recordID = result1.getInt("record_id");
                double revenue = result.getDouble("revenue");

                String line = String.format("%i, %i, %d");
                fileWriter.newLine();
                fileWriter.write(line);
            }
            statement.close();
            fileWriter.close();
    }
}
