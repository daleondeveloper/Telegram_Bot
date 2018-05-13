import com.mysql.fabric.jdbc.FabricMySQLDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DBConnection {
    private static final String url = "jdbc:mysql://localhost:3306/?????";
    private static final String name = "?????";
    private static final String password = "?????";
    Connection connection;
    DBConnection() {
        try {
            DriverManager.registerDriver(new FabricMySQLDriver());
            connection = DriverManager.getConnection(url,name,password);

        }catch (Exception e){

        }


    }
    public Connection getConnection(){
        return connection;
    }
    @Override
    public void finalize() {
        try {
            if(!connection.isClosed())
            connection.close();
        }catch (Exception e){

        }
    }
}
