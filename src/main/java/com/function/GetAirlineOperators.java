package com.function;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.function.pojos.AirlineOperator;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class GetAirlineOperators {
    /**
     * This function listens at endpoint "/api/GetAirlineOperators". Two ways to
     * invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/GetAirlineOperators
     * 2. curl {your host}/api/GetAirlineOperators?name=HTTP%20Query
     * @throws SQLException
     */
    @FunctionName("getAirlineOperators")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws SQLException {
        context.getLogger().info("Java HTTP trigger processed a request.");

        String conn = System.getenv("ANALYTICS_DB_CONNECTION_STRING");

        ResultSet resultSet = null;
        List<AirlineOperator> operators = new ArrayList<AirlineOperator>();

        try (Connection connection = DriverManager.getConnection(conn);
                Statement statement = connection.createStatement();) {

            // Create and execute a SELECT SQL statement.
            String selectSql = "SELECT * FROM AirlineOperator";
            context.getLogger().info(selectSql);

            resultSet = statement.executeQuery(selectSql);
            context.getLogger().info(resultSet.toString());

            // Print results from select statement
            while (resultSet.next()) {
                AirlineOperator operator = new AirlineOperator();
                operator.setAirlineID(resultSet.getString("AIRLINEID"));
                operator.setIataCode(resultSet.getString("IATACODE"));
                operator.setIcaoCode(resultSet.getString("ICAOCODE"));
                operator.setBoeingCode(resultSet.getString("BOEINGCODE"));
                operator.setName(resultSet.getString("NAME"));
                operators.add(operator);
            }

            return request.createResponseBuilder(HttpStatus.OK).body(operators).build();

        }
    }
}
