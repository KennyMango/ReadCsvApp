import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.opencsv.CSVReader;
import java.util.List;
import org.apache.commons.lang3.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;

public class Main {

    // Database Insert Query
    private static final String INSERT_QUERY = "INSERT INTO PPE_DATA (TERMINAL, DEPARTMENT, DATE_TIME, EMP_NUMBER, LAST_NAME, FIRST_NAME, GCT_PART_NUMBER, ITEM_ID, ITEM_NAME, QUANTITY) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    // Initialized Logging Class
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();

        // Read configuration file --config.properties
        try (InputStream inputStream = Files.newInputStream(Paths.get("config.properties"))) {
            props.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Variables from config.properties
        final String CSV_FILE_PATH = props.getProperty("CSV_FILE_PATH");
        final String JDBC_URL = props.getProperty("JDBC_URL");
        final String JDBC_USERNAME = props.getProperty("JDBC_USERNAME");
        final String JDBC_PASSWORD = props.getProperty("JDBC_PASSWORD");

        // Encrypts SQL password
        final String Encrypt_JDBC_PASSWORD = PasswordEncryptor.encrypt(JDBC_PASSWORD);
        // Decrypts SQL password to be used for SQL connection
        final String Decrypt_JDBC_PASSWORD = PasswordEncryptor.decrypt(Encrypt_JDBC_PASSWORD);
        // Writes Encrypted password back to config.properties
        props.setProperty("JDBC_PASSWORD", Encrypt_JDBC_PASSWORD);

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("config.properties"))) {
            props.store(outputStream, "Updated Encrypted password");
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {

            // Add CSV files to list if multiple
            File dir = new File (CSV_FILE_PATH);
            File[] files = dir.listFiles();

            // Error/Info Logging
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
            String logFolder = "logs";
            File folder = new File(logFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }
            String logFilePath = logFolder + "/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".log";
            LOGGER.setLevel(Level.INFO);
            FileHandler handler = new FileHandler(logFilePath);
            handler.setLevel(Level.INFO);
            LOGGER.addHandler(handler);

            // Intialize Database connection
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USERNAME, Decrypt_JDBC_PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(INSERT_QUERY);


            // Loops through CSV files in the list and read line by line and inserts values to the prepared statement
            for (File file : files) {
                if (file.isFile()){

                    CSVReader reader = new CSVReader(new FileReader(file));

                    int i = 0;

                    List<String[]> lines = reader.readAll();

                    for (String[] values : lines) {

                        if(i == 0){
                            i++;
                            continue;
                        }

                        // Parsing needs to be done here to remove comma from column 9 itemname
                        String ItemName = values[8].replaceAll(",", "");
                        // Date Time Parsing for timestamp in DB
                        Date parsedDate = dateFormat.parse(values[2]);
                        Timestamp timestamp = new Timestamp(parsedDate.getTime());
                        // Parsing String to Int, so it's accepted in to DB for Quantity
                        int qty = Integer.parseInt(values[9]);

                        // Preparing SQL statement
                        pstmt.setString(1, values[0]);
                        pstmt.setString(2, values[1]);
                        pstmt.setTimestamp(3, timestamp);
                        pstmt.setString(4, values[3]);
                        pstmt.setString(5, values[4]);
                        pstmt.setString(6, values[5]);
                        pstmt.setString(7, values[6]);
                        pstmt.setString(8, values[7]);
                        pstmt.setString(9, ItemName);
                        pstmt.setInt(10, qty);

                        LOGGER.info("Executing DB update");

                        // Executes SQL update
                        pstmt.executeUpdate();


                        // Logging each update statement per line in CSV
                        int k = 0;
                        StringBuilder FinalinsertLog = new StringBuilder("Inserted record: ");

                        for (String value : values) {

                            if(k == 9){
                                k++;
                                String strQTY = String.valueOf(value);
                                FinalinsertLog.append(strQTY);
                                continue;
                            }
                            FinalinsertLog.append(value);
                            k++;

                        }

                        LOGGER.info(FinalinsertLog.toString());
                    }
                    LOGGER.info("All records inserted successfully. Closing DB connection..");
                    reader.close();
                    pstmt.close();
                    conn.close();
                    LOGGER.info("DB connection closed");
                }
            }


        }

        catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "File not found " + e.getMessage(), e);
        }

        catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, "Bad File Path " + e.getMessage(), e);
        }

        catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inserting records: " + e.getMessage(), e);
        }

    }

}