package com.bootsync.common.time;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank
    private String timezone = "Asia/Seoul";

    private final Allowance allowance = new Allowance();
    private final AccountDeletion accountDeletion = new AccountDeletion();
    private final Audit audit = new Audit();
    private final Security security = new Security();
    private final Monitoring monitoring = new Monitoring();
    private final Operations operations = new Operations();
    private final RecoveryEmail recoveryEmail = new RecoveryEmail();
    private final Training training = new Training();

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Allowance getAllowance() {
        return allowance;
    }

    public AccountDeletion getAccountDeletion() {
        return accountDeletion;
    }

    public Audit getAudit() {
        return audit;
    }

    public Security getSecurity() {
        return security;
    }

    public Monitoring getMonitoring() {
        return monitoring;
    }

    public Operations getOperations() {
        return operations;
    }

    public RecoveryEmail getRecoveryEmail() {
        return recoveryEmail;
    }

    public Training getTraining() {
        return training;
    }

    public static class Allowance {

        @Min(0)
        private int dailyAllowanceAmount = 15_800;

        @Min(1)
        private int payableDayCap = 20;

        public int getDailyAllowanceAmount() {
            return dailyAllowanceAmount;
        }

        public void setDailyAllowanceAmount(int dailyAllowanceAmount) {
            this.dailyAllowanceAmount = dailyAllowanceAmount;
        }

        public int getPayableDayCap() {
            return payableDayCap;
        }

        public void setPayableDayCap(int payableDayCap) {
            this.payableDayCap = payableDayCap;
        }

        public int getMaximumAllowanceAmount() {
            return Math.max(0, dailyAllowanceAmount * payableDayCap);
        }
    }

    public static class Security {

        private boolean trustForwardedHeaders = false;

        public boolean isTrustForwardedHeaders() {
            return trustForwardedHeaders;
        }

        public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
            this.trustForwardedHeaders = trustForwardedHeaders;
        }
    }

    public static class Monitoring {

        @NotBlank
        private String prometheusScrapeToken = "";

        public String getPrometheusScrapeToken() {
            return prometheusScrapeToken;
        }

        public void setPrometheusScrapeToken(String prometheusScrapeToken) {
            this.prometheusScrapeToken = prometheusScrapeToken;
        }
    }

    public static class Audit {

        private String requestIpHmacSecret = "";
        @Min(1)
        private int requestIpHmacRetentionDays = 30;
        private boolean requestIpHmacPruneEnabled = true;

        @NotBlank
        private String requestIpHmacPruneCron = "0 25 3 * * *";

        public String getRequestIpHmacSecret() {
            return requestIpHmacSecret;
        }

        public void setRequestIpHmacSecret(String requestIpHmacSecret) {
            this.requestIpHmacSecret = requestIpHmacSecret;
        }

        public int getRequestIpHmacRetentionDays() {
            return requestIpHmacRetentionDays;
        }

        public void setRequestIpHmacRetentionDays(int requestIpHmacRetentionDays) {
            this.requestIpHmacRetentionDays = requestIpHmacRetentionDays;
        }

        public boolean isRequestIpHmacPruneEnabled() {
            return requestIpHmacPruneEnabled;
        }

        public void setRequestIpHmacPruneEnabled(boolean requestIpHmacPruneEnabled) {
            this.requestIpHmacPruneEnabled = requestIpHmacPruneEnabled;
        }

        public String getRequestIpHmacPruneCron() {
            return requestIpHmacPruneCron;
        }

        public void setRequestIpHmacPruneCron(String requestIpHmacPruneCron) {
            this.requestIpHmacPruneCron = requestIpHmacPruneCron;
        }
    }

    public static class AccountDeletion {

        private boolean purgeEnabled = false;

        @NotBlank
        private String purgeCron = "0 15 3 * * *";

        private final PurgeRunOnce purgeRunOnce = new PurgeRunOnce();

        public boolean isPurgeEnabled() {
            return purgeEnabled;
        }

        public void setPurgeEnabled(boolean purgeEnabled) {
            this.purgeEnabled = purgeEnabled;
        }

        public String getPurgeCron() {
            return purgeCron;
        }

        public void setPurgeCron(String purgeCron) {
            this.purgeCron = purgeCron;
        }

        public PurgeRunOnce getPurgeRunOnce() {
            return purgeRunOnce;
        }
    }

    public static class PurgeRunOnce {

        private boolean enabled = false;
        private String actor = "";
        private String reason = "";
        private boolean closeContextAfterRun = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getActor() {
            return actor;
        }

        public void setActor(String actor) {
            this.actor = actor;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public boolean isCloseContextAfterRun() {
            return closeContextAfterRun;
        }

        public void setCloseContextAfterRun(boolean closeContextAfterRun) {
            this.closeContextAfterRun = closeContextAfterRun;
        }
    }

    public static class RecoveryEmail {

        @NotBlank
        private String publicBaseUrl = "http://localhost:8080";

        @NotBlank
        private String fromAddress = "no-reply@bootsync.local";

        private boolean mailEnabled = false;
        private boolean developmentPreviewEnabled = false;

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }

        public boolean isMailEnabled() {
            return mailEnabled;
        }

        public void setMailEnabled(boolean mailEnabled) {
            this.mailEnabled = mailEnabled;
        }

        public boolean isDevelopmentPreviewEnabled() {
            return developmentPreviewEnabled;
        }

        public void setDevelopmentPreviewEnabled(boolean developmentPreviewEnabled) {
            this.developmentPreviewEnabled = developmentPreviewEnabled;
        }
    }

    public static class Operations {

        private final DeletionCancel deletionCancel = new DeletionCancel();
        private final PasswordReset passwordReset = new PasswordReset();

        public DeletionCancel getDeletionCancel() {
            return deletionCancel;
        }

        public PasswordReset getPasswordReset() {
            return passwordReset;
        }
    }

    public static class DeletionCancel {

        private boolean enabled = false;
        private String username = "";
        private String actor = "";
        private String reason = "";
        private boolean closeContextAfterRun = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getActor() {
            return actor;
        }

        public void setActor(String actor) {
            this.actor = actor;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public boolean isCloseContextAfterRun() {
            return closeContextAfterRun;
        }

        public void setCloseContextAfterRun(boolean closeContextAfterRun) {
            this.closeContextAfterRun = closeContextAfterRun;
        }
    }

    public static class PasswordReset {

        private boolean enabled = false;
        private String username = "";
        private String temporaryPassword = "";
        private String temporaryPasswordFile = "";
        private String actor = "";
        private String reason = "";
        private boolean closeContextAfterRun = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getTemporaryPassword() {
            return temporaryPassword;
        }

        public void setTemporaryPassword(String temporaryPassword) {
            this.temporaryPassword = temporaryPassword;
        }

        public String getTemporaryPasswordFile() {
            return temporaryPasswordFile;
        }

        public void setTemporaryPasswordFile(String temporaryPasswordFile) {
            this.temporaryPasswordFile = temporaryPasswordFile;
        }

        public String getActor() {
            return actor;
        }

        public void setActor(String actor) {
            this.actor = actor;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public boolean isCloseContextAfterRun() {
            return closeContextAfterRun;
        }

        public void setCloseContextAfterRun(boolean closeContextAfterRun) {
            this.closeContextAfterRun = closeContextAfterRun;
        }
    }

    public static class Training {

        @NotNull
        private LocalDate courseStartDate = LocalDate.of(2025, 9, 23);

        @NotNull
        private LocalDate courseEndDate = LocalDate.of(2026, 3, 27);

        @Min(1)
        @Max(100)
        private int attendanceThresholdPercent = 80;

        private List<LocalDate> holidays = new ArrayList<>(List.of(
            LocalDate.of(2025, 10, 3),
            LocalDate.of(2025, 10, 6),
            LocalDate.of(2025, 10, 7),
            LocalDate.of(2025, 10, 8),
            LocalDate.of(2025, 10, 9),
            LocalDate.of(2025, 10, 10),
            LocalDate.of(2025, 12, 25),
            LocalDate.of(2025, 12, 26),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 2),
            LocalDate.of(2026, 2, 16),
            LocalDate.of(2026, 2, 17),
            LocalDate.of(2026, 2, 18),
            LocalDate.of(2026, 3, 2)
        ));

        public LocalDate getCourseStartDate() {
            return courseStartDate;
        }

        public void setCourseStartDate(LocalDate courseStartDate) {
            this.courseStartDate = courseStartDate;
        }

        public LocalDate getCourseEndDate() {
            return courseEndDate;
        }

        public void setCourseEndDate(LocalDate courseEndDate) {
            this.courseEndDate = courseEndDate;
        }

        public int getAttendanceThresholdPercent() {
            return attendanceThresholdPercent;
        }

        public void setAttendanceThresholdPercent(int attendanceThresholdPercent) {
            this.attendanceThresholdPercent = attendanceThresholdPercent;
        }

        public List<LocalDate> getHolidays() {
            return holidays;
        }

        public void setHolidays(List<LocalDate> holidays) {
            this.holidays = holidays == null ? new ArrayList<>() : new ArrayList<>(holidays);
        }
    }
}
