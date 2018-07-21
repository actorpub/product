package com.music.concertoplayer.service.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.music.concertoplayer.App;
import com.music.concertoplayer.R;
import com.music.concertoplayer.activity.MainActivity;
import com.music.concertoplayer.entity.FolderInfo;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.service.MusicPlayerService;


public class PlayingNotificationImpl extends PlayingNotification {

    // private Target<BitmapPaletteWrapper> target;
    private RemoteViews notificationLayout;
    private boolean isPlaying;
    private Notification notification;

    @Override
    public synchronized void update() {
        stopped = false;

        final MusicInfo song = service.getCurrentSong();

        isPlaying = service.isPlaying();

        notificationLayout = new RemoteViews(service.getPackageName(), R.layout.notification);
        if (TextUtils.isEmpty(song.getName()) && TextUtils.isEmpty(song.getFolderName())) {
            notificationLayout.setViewVisibility(R.id.media_titles, View.INVISIBLE);
        } else {
            notificationLayout.setViewVisibility(R.id.media_titles, View.VISIBLE);
            notificationLayout.setTextViewText(R.id.title, song.getName());
            notificationLayout.setTextViewText(R.id.text, song.getFolderName());
        }

        linkButtons(notificationLayout);

        Intent action = new Intent(service, MainActivity.class);
        action.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final PendingIntent clickIntent = PendingIntent.getActivity(service, 0, action, 0);
        final PendingIntent deleteIntent = buildPendingIntent(service, MusicPlayerService.ACTION_QUIT, null);

       notification = new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(clickIntent)
                .setDeleteIntent(deleteIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContent(notificationLayout)
                .setOngoing(isPlaying)
                .build();

        FolderInfo info = App.getDbManager().getFolderNameByName(song.getFolderName());
        update(info.getBitmap());
    }

    private void update(@Nullable Bitmap bitmap) {
        if (bitmap != null) {
            notificationLayout.setImageViewBitmap(R.id.image, bitmap);
        } else {
            notificationLayout.setImageViewResource(R.id.image, R.drawable.default_album_art);
        }
        setBackgroundColor(Color.GRAY);
        setNotificationContent();

        if (stopped)
            return; // notification has been stopped before loading was finished
        updateNotifyModeAndPostNotification(notification);
    }

    private void setBackgroundColor(int color) {
        notificationLayout.setInt(R.id.root, "setBackgroundColor", color);
    }

    private void setNotificationContent() {

       /* Bitmap prev = createBitmap(Util.getTintedVectorDrawable(service, R.drawable.ic_skip_previous_white_24dp, Color.BLACK), 1.5f);
        Bitmap next = createBitmap(Util.getTintedVectorDrawable(service, R.drawable.ic_skip_next_white_24dp, Color.BLACK), 1.5f);
        Bitmap playPause = createBitmap(Util.getTintedVectorDrawable(service, isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp, Color.BLACK), 1.5f);
*/
        Bitmap prev = BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_notif_pre);
        Bitmap next = BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_notif_next);
        Bitmap playPause = BitmapFactory.decodeResource(service.getResources(),isPlaying ? R.drawable.ic_notif_pause : R.drawable.ic_notif_play );
        notificationLayout.setTextColor(R.id.title, Color.BLACK);
        notificationLayout.setTextColor(R.id.text, Color.BLACK);
        notificationLayout.setImageViewBitmap(R.id.action_prev, prev);
        notificationLayout.setImageViewBitmap(R.id.action_next, next);
        notificationLayout.setImageViewBitmap(R.id.action_play_pause, playPause);

    }
    private void linkButtons(final RemoteViews notificationLayout) {
        PendingIntent pendingIntent;

        final ComponentName serviceName = new ComponentName(service, MusicPlayerService.class);

        // Previous track
        pendingIntent = buildPendingIntent(service, MusicPlayerService.ACTION_REWIND, serviceName);
        notificationLayout.setOnClickPendingIntent(R.id.action_prev, pendingIntent);

        // Play and pause
        pendingIntent = buildPendingIntent(service, MusicPlayerService.ACTION_TOGGLE_PAUSE, serviceName);
        notificationLayout.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent);

        // Next track
        pendingIntent = buildPendingIntent(service, MusicPlayerService.ACTION_SKIP, serviceName);
        notificationLayout.setOnClickPendingIntent(R.id.action_next, pendingIntent);
    }

    private PendingIntent buildPendingIntent(Context context, final String action, final ComponentName serviceName) {
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    private static Bitmap createBitmap(Drawable drawable, float sizeMultiplier) {
        Bitmap bitmap = Bitmap.createBitmap((int) (drawable.getIntrinsicWidth() * sizeMultiplier), (int) (drawable.getIntrinsicHeight() * sizeMultiplier), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        drawable.setBounds(0, 0, c.getWidth(), c.getHeight());
        drawable.draw(c);
        return bitmap;
    }
}
