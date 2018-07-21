package com.music.concertoplayer.service.notification;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import android.text.TextUtils;

import com.music.concertoplayer.R;
import com.music.concertoplayer.activity.MainActivity;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.service.MusicPlayerService;

import static com.music.concertoplayer.service.MusicPlayerService.ACTION_REWIND;
import static com.music.concertoplayer.service.MusicPlayerService.ACTION_SKIP;
import static com.music.concertoplayer.service.MusicPlayerService.ACTION_TOGGLE_PAUSE;


public class PlayingNotificationImpl24 extends PlayingNotification {

    @Override
    public synchronized void update() {
        stopped = false;

        final MusicInfo song = service.getCurrentSong();

        final String name = song.getName();
        final String folderName = song.getFolderName();
        final boolean isPlaying = service.isPlaying();
        final String text = TextUtils.isEmpty(name)
                ? folderName : folderName + " - " + name;

        final int playButtonResId = isPlaying
                ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;

        Intent action = new Intent(service, MainActivity.class);
        action.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final PendingIntent clickIntent = PendingIntent.getActivity(service, 0, action, 0);

        final ComponentName serviceName = new ComponentName(service, MusicPlayerService.class);
        Intent intent = new Intent(MusicPlayerService.ACTION_QUIT);
        intent.setComponent(serviceName);
        final PendingIntent deleteIntent = PendingIntent.getService(service, 0, intent, 0);
        Bitmap bitmap = BitmapFactory.decodeResource(service.getResources(), R.drawable.default_album_art);
        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(playButtonResId,
                service.getString(R.string.action_play_pause),
                retrievePlaybackAction(ACTION_TOGGLE_PAUSE));
        NotificationCompat.Action previousAction = new NotificationCompat.Action(R.drawable.ic_skip_previous_white_24dp,
                service.getString(R.string.action_previous),
                retrievePlaybackAction(ACTION_REWIND));
        NotificationCompat.Action nextAction = new NotificationCompat.Action(R.drawable.ic_skip_next_white_24dp,
                service.getString(R.string.action_next),
                retrievePlaybackAction(ACTION_SKIP));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(bitmap)
                .setContentIntent(clickIntent)
                .setDeleteIntent(deleteIntent)
                .setContentTitle(song.getName())
                .setContentText(text)
                .setOngoing(isPlaying)
                .setShowWhen(false)
                .addAction(previousAction)
                .addAction(playPauseAction)
                .addAction(nextAction);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setStyle(new android.support.v7.app.NotificationCompat.MediaStyle().setMediaSession(service.getMediaSession().getSessionToken()).setShowActionsInCompactView(0, 1, 2))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O)
                builder.setColor( Color.GRAY);
        }

        if (stopped)
            return; // notification has been stopped before loading was finished
        updateNotifyModeAndPostNotification(builder.build());

    }


    private PendingIntent retrievePlaybackAction(final String action) {
        final ComponentName serviceName = new ComponentName(service, MusicPlayerService.class);
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);
        return PendingIntent.getService(service, 0, intent, 0);
    }
}
