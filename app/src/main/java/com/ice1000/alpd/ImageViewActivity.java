package com.ice1000.alpd;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.format.Time;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

import data.DownloadData;
import data.Poster;
import util.DownloadingActivity;
import view.ZoomImageView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ImageViewActivity extends DownloadingActivity {

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private ZoomImageView contentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            contentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    };
    private View controlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) actionBar.show();
            controlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean visible;
    private final Runnable mHideRunnable = this::hide;
    private Poster poster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        toast("refreshing");
        initViews();
        initFuncs();
    }

    private void initViews() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        visible = true;
        controlsView = findViewById(R.id.fullscreen_content_controls);
        contentView = (ZoomImageView) findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        if (contentView != null) {
            contentView.setOnClickListener(view -> toggle());
        }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        Button download = (Button) findViewById(R.id.dummy_button);
        assert download != null;
        download.setOnTouchListener((view, motionEvent) -> {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        });
        download.setOnClickListener(v -> {
            final Dialog dialog = ProgressDialog.show(ImageViewActivity.this, "保存图片", "图片正在保存中，请稍等...", true);
            new Thread(() -> {
                Message message = new Message();
                DownloadData data = new DownloadData();
                data.dialog = dialog;
                try {
                    contentView.buildDrawingCache();
                    Time time = new Time("GMT+8");
                    time.setToNow();
                    saveFile(
                            poster.getBitMap(),
                            String.format(
                                    "%s.png",
                                    time.toString()
                            )
                    );
                    data.msg = "saved!";
                } catch (IOException e) {
                    data.msg = "failed to save";
                    e.printStackTrace();
                }
                message.obj = data;
                message.what = IMAGE_SAVE;
                handler.sendMessage(message);
            }).start();
        });
        getImage(getIntent().getIntExtra(NUMBER, 1), false);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (visible)
            hide();
        else
            show();
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        controlsView.setVisibility(View.GONE);
        visible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        contentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        visible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void addView(Poster poster) {
        contentView.setImage(poster.getBitMap());
        this.poster = poster;
    }
}
