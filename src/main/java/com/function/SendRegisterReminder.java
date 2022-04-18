package com.function;

import java.util.*;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.microsoft.azure.functions.annotation.*;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.function.pojos.Email;
import com.google.gson.Gson;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class SendRegisterReminder {
    /**
     * This function listens at endpoint "/api/SendRegisterReminder". Two ways to
     * invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/SendRegisterReminder
     * 2. curl {your host}/api/SendRegisterReminder?name=HTTP%20Query
     * 
     * @throws AddressException
     * 
     * @throws MessagingException
     */
    @FunctionName("sendRegisterReminder")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws AddressException, MessagingException {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        // String query = request.getQueryParameters().get("email");
        String data = request.getBody().orElse(null);

        if (data == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass an object in the request body").build();
        } else {

            Email email = new Gson().fromJson(data, Email.class);

            sendMail(email.getRecipient(), email.getBody(), email.getSubject());
            return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + email).build();
        }
    }

    public void sendMail(String recipient, String body, String subject) throws AddressException, MessagingException {

        ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder()
                .build();

        SecretClient client = new SecretClientBuilder()
                .vaultUrl("https://fda-groundservices-kv.vault.azure.net/")
                .credential(managedIdentityCredential)
                .buildClient();

        KeyVaultSecret port = client.getSecret("MailServerPort");
        KeyVaultSecret host = client.getSecret("MailServerHost");
        KeyVaultSecret user = client.getSecret("MailServerAuthUsername");
        KeyVaultSecret pass = client.getSecret("MailServerAuthPassword");

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", host.getValue());
        prop.put("mail.smtp.port", port.getValue());
        prop.put("mail.smtp.ssl.trust", host.getValue());

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user.getValue(), pass.getValue());
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress("noreply@flitedeckadvisor.com"));
        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);

        String msg = body;

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(msg, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        message.setContent(multipart);

        Transport.send(message);
    }
}
