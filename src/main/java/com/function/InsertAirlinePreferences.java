package com.function;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.function.pojos.AirlinePreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class InsertAirlinePreferences {
    /**
     * This function listens at endpoint "/api/InsertAirlinePreferences". Two ways
     * to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/InsertAirlinePreferences
     * 2. curl {your host}/api/InsertAirlinePreferences?name=HTTP%20Query
     */
    @FunctionName("insertAirlinePreferences")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String airline = request.getQueryParameters().get("airline");
        String updatedBy = request.getQueryParameters().get("updatedBy");
        String data = request.getBody().orElse(null);

        Gson gson = new Gson();
        Type preferencesListType = new TypeToken<ArrayList<AirlinePreferences>>() {
        }.getType();
        List<AirlinePreferences> preferences = gson.fromJson(data, preferencesListType);

        String conn = System.getenv("DB_CONNECTION_STRING");

        try (Connection connection = DriverManager.getConnection(conn);
                Statement statement = connection.createStatement();) {

            PreparedStatement pstmt = connection
                    .prepareStatement("INSERT INTO airline_preferences values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            for (AirlinePreferences preference : preferences) {
                pstmt.setString(1, airline);
                pstmt.setString(2, preference.getPreference());
                pstmt.setString(3, preference.getAirlineKey());
                pstmt.setBoolean(4, preference.getEnabled());
                pstmt.setString(5, preference.getDescription());
                pstmt.setBoolean(6, preference.getDisplay());
                pstmt.setBoolean(7, preference.getChoicePilot());
                pstmt.setBoolean(8, preference.getChoiceFocal());
                pstmt.setBoolean(9, preference.getChoiceCheckAirman());
                pstmt.setBoolean(10, preference.getChoiceMaintenance());
                pstmt.setBoolean(11, preference.getChoiceEFBAdmin());
                pstmt.setString(12, updatedBy);
                pstmt.setTimestamp(13, new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis()));
                pstmt.executeUpdate();
            }

            context.getLogger().info("Airline preferences for " + airline + " inserted by " + updatedBy);

        } catch (Exception e) {
            context.getLogger().info("Error: " + e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inserting airline preferences").build();

        }
        return request.createResponseBuilder(HttpStatus.OK).body("Airlines preferences inserted successfully").build();
    }

}
