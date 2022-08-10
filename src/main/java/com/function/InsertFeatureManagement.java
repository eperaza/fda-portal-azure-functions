package com.function;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.function.pojos.FeatureManagement;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class InsertFeatureManagement {
    /**
     * This function listens at endpoint "/api/InsertAirlinePreferences". Two ways
     * to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/InsertAirlinePreferences
     * 2. curl {your host}/api/InsertAirlinePreferences?name=HTTP%20Query
     */
    @FunctionName("insertFeatureManagement")
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
        Type preferencesListType = new TypeToken<ArrayList<FeatureManagement>>() {
        }.getType();
        List<FeatureManagement> preferences = gson.fromJson(data, preferencesListType);

        String conn = System.getenv("DB_CONNECTION_STRING");

        try (Connection connection = DriverManager.getConnection(conn);
                Statement statement = connection.createStatement();) {

            PreparedStatement pstmt = connection
                    .prepareStatement("INSERT INTO feature_management values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            for (FeatureManagement preference : preferences) {
                pstmt.setString(1, airline);
                pstmt.setString(2, preference.getTitle());
                pstmt.setString(3, preference.getFeatureKey());
                pstmt.setBoolean(4, preference.getEnabled());
                pstmt.setString(5, preference.getDescription());
                pstmt.setBoolean(6, preference.getChoicePilot());
                pstmt.setBoolean(7, preference.getChoiceFocal());
                pstmt.setBoolean(8, preference.getChoiceCheckAirman());
                pstmt.setBoolean(9, preference.getChoiceMaintenance());
                pstmt.setBoolean(10, preference.getChoiceEFBAdmin());
                pstmt.setString(11, updatedBy);
                pstmt.setTimestamp(12, new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis()));
                pstmt.executeUpdate();
            }

            context.getLogger().info("Feature management for " + airline + " inserted by " + updatedBy);

        } catch (Exception e) {
            context.getLogger().info("Error: " + e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inserting feature management").build();

        }
        return request.createResponseBuilder(HttpStatus.OK).body("Feature management inserted successfully").build();
    }

}
