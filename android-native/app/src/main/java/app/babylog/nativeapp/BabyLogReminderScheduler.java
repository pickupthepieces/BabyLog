package app.babylog.nativeapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class BabyLogReminderScheduler {
    public static final String OPEN_ROUTE_EXTRA = "app.babylog.nativeapp.OPEN_ROUTE";
    private static final String ACTION_REMINDER = "app.babylog.nativeapp.REMINDER";
    private static final String EXTRA_REMINDER_ID = "reminder_id";
    private static final String CHANNEL_ID = "babylog_reminders";
    private static final String WORK_NAME = "babylog_reminder_refresh";

    private BabyLogReminderScheduler() {
    }

    public static void refreshAndSchedule(Context context) {
        BabyLogRepository repository = new BabyLogRepository(context);
        BabyLogReminderStore store = new BabyLogReminderStore(context);
        BabyLogDomain.ChildProfile profile = repository.loadChildProfile();
        List<BabyLogDomain.BabyLogEvent> events = repository.listEvents();
        try {
            store.syncSystemReminders(profile, events);
        } catch (JSONException ignored) {
            // Reminder generation is best-effort; the reminder center remains readable.
        }
        scheduleAll(context, store.listReminders(), profile);
        enqueuePeriodicRefresh(context);
    }

    public static void enqueuePeriodicRefresh(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderRefreshWorker.class,
                6,
                TimeUnit.HOURS
        ).build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void scheduleAll(
            Context context,
            List<BabyLogReminderStore.Reminder> reminders,
            BabyLogDomain.ChildProfile profile
    ) {
        if (reminders == null) {
            return;
        }
        boolean systemMuted = BabyLogReminderStore.isSystemMuted(profile);
        for (BabyLogReminderStore.Reminder reminder : reminders) {
            if (reminder == null) {
                continue;
            }
            if (!BabyLogReminderStore.isActionable(reminder)
                    || (systemMuted && BabyLogReminderStore.SOURCE_SYSTEM.equals(reminder.source))) {
                cancel(context, reminder.id);
                continue;
            }
            schedule(context, reminder);
        }
    }

    public static boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static void schedule(Context context, BabyLogReminderStore.Reminder reminder) {
        long dueAt = BabyLogFormatters.parseIsoMillis(reminder.dueAtIso);
        if (dueAt <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        if (dueAt <= now - TimeUnit.HOURS.toMillis(1)) {
            return;
        }
        long triggerAt = dueAt <= now ? now + 5_000L : dueAt;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(reminder.id),
                new Intent(context, ReminderAlarmReceiver.class)
                        .setAction(ACTION_REMINDER)
                        .putExtra(EXTRA_REMINDER_ID, reminder.id),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) {
                scheduleInexact(alarmManager, triggerAt, pendingIntent);
                return;
            }
            if (Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (SecurityException ignored) {
            scheduleInexact(alarmManager, triggerAt, pendingIntent);
        }
    }

    private static void scheduleInexact(AlarmManager alarmManager, long triggerAt, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private static void cancel(Context context, String reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(reminderId),
                new Intent(context, ReminderAlarmReceiver.class).setAction(ACTION_REMINDER),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static int requestCode(String id) {
        return id == null ? 0 : id.hashCode();
    }

    public static final class ReminderAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String reminderId = intent == null ? "" : intent.getStringExtra(EXTRA_REMINDER_ID);
            BabyLogRepository repository = new BabyLogRepository(context);
            BabyLogDomain.ChildProfile profile = repository.loadChildProfile();
            BabyLogReminderStore store = new BabyLogReminderStore(context);
            BabyLogReminderStore.Reminder reminder = store.findReminder(reminderId);
            if (!BabyLogReminderStore.isActionable(reminder)
                    || (BabyLogReminderStore.isSystemMuted(profile) && BabyLogReminderStore.SOURCE_SYSTEM.equals(reminder.source))
                    || !hasNotificationPermission(context)) {
                return;
            }
            showNotification(context, reminder);
        }
    }

    public static final class ReminderRefreshWorker extends Worker {
        public ReminderRefreshWorker(Context context, WorkerParameters params) {
            super(context, params);
        }

        @Override
        public Result doWork() {
            refreshAndSchedule(getApplicationContext());
            return Result.success();
        }
    }

    public static final class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null ? "" : intent.getAction();
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                    || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
                refreshAndSchedule(context);
            }
        }
    }

    private static void showNotification(Context context, BabyLogReminderStore.Reminder reminder) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "栗记提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent(context, ComposeMainActivity.class)
                .putExtra(OPEN_ROUTE_EXTRA, "tools/reminder-center")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                requestCode(reminder.id),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(reminder.title)
                .setContentText(reminder.note)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
        manager.notify(requestCode(reminder.id), builder.build());
    }
}
