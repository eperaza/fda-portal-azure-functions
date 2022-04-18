package com.function.pojos;

public class Email {
    
    String recipient;
    String body;
    String subject;

    
    public Email(String recipient, String body, String subject) {
        this.recipient = recipient;
        this.body = body;
        this.subject = subject;
    }

    public Email() {
        
    }

    public String getRecipient() {
        return recipient;
    }
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }
}
