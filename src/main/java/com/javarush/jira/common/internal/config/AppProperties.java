package com.javarush.jira.common.internal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties("app")
@Validated
public class AppProperties {

    /**
     * Host url
     */
    @NonNull
    private String hostUrl;

    /**
     * Test email
     */
    @NonNull
    private String testMail;

    /**
     * Interval for update templates
     */
    @NonNull
    private Duration templatesUpdateCache;

    @NonNull
    private MailSendingProps mailSendingProps;

    @NonNull
    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(@NonNull String hostUrl) {
        this.hostUrl = hostUrl;
    }

    @NonNull
    public String getTestMail() {
        return testMail;
    }

    public void setTestMail(@NonNull String testMail) {
        this.testMail = testMail;
    }

    @NonNull
    public Duration getTemplatesUpdateCache() {
        return templatesUpdateCache;
    }

    public void setTemplatesUpdateCache(@NonNull Duration templatesUpdateCache) {
        this.templatesUpdateCache = templatesUpdateCache;
    }

    @NonNull
    public MailSendingProps getMailSendingProps() {
        return mailSendingProps;
    }

    public void setMailSendingProps(@NonNull MailSendingProps mailSendingProps) {
        this.mailSendingProps = mailSendingProps;
    }

    //    https://stackoverflow.com/a/29588215/548473
    @Setter
    public static class MailSendingProps {
        int corePoolSize;
        int maxPoolSize;
    }
}
