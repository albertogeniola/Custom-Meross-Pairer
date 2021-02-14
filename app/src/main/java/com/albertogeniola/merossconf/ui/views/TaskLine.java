package com.albertogeniola.merossconf.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.albertogeniola.merossconf.R;


public class TaskLine extends LinearLayout {
    private Context context;
    private TextView taskTitleTextView;
    private ProgressBar taskLine_progressSpinner;
    private ImageView taskLine_taskIcon;

    private String taskTitle = "";
    private TaskState state = TaskState.not_started;

    public TaskLine(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public TaskLine(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,R.styleable.TaskLine,0, 0);

        try {
            taskTitle = a.getString(R.styleable.TaskLine_title);
            state = TaskState.values()[a.getInt(R.styleable.TaskLine_state,-1)];
        } finally {
            a.recycle();
        }
        init();
    }

    private void init() {
        View rootView = inflate(context, R.layout.task_line, this);
        taskTitleTextView = (TextView) rootView.findViewById(R.id.taskLine_taskTitle);
        taskLine_progressSpinner = (ProgressBar) rootView.findViewById(R.id.taskLine_progressSpinner);
        taskLine_taskIcon = (ImageView)rootView.findViewById(R.id.taskLine_taskIcon);

        taskTitleTextView.setText(taskTitle);
        taskLine_progressSpinner.setIndeterminate(true);
        setState(state);
    }

    public void setState(TaskState state) {
        this.state = state;

        switch (state) {
            case not_started:
                taskLine_taskIcon.setImageResource(R.drawable.ic_more_horiz_black_24dp);
                taskLine_taskIcon.setVisibility(VISIBLE);
                taskLine_progressSpinner.setVisibility(GONE);
                break;
            case failed:
                taskLine_taskIcon.setImageResource(R.drawable.ic_error_outline_black_24dp);
                taskLine_taskIcon.setVisibility(VISIBLE);
                taskLine_progressSpinner.setVisibility(GONE);
                break;
            case running:
                taskLine_taskIcon.setVisibility(GONE);
                taskLine_progressSpinner.setVisibility(VISIBLE);
                break;
            case skipped:
                taskLine_taskIcon.setImageResource(R.drawable.ic_priority_high_black_24dp);
                taskLine_taskIcon.setVisibility(VISIBLE);
                taskLine_progressSpinner.setVisibility(GONE);
                break;
            case completed:
                taskLine_taskIcon.setImageResource(R.drawable.ic_done_black_24dp);
                taskLine_taskIcon.setVisibility(VISIBLE);
                taskLine_progressSpinner.setVisibility(GONE);
                break;
        }
    }

    public enum TaskState {
        not_started,
        running,
        completed,
        failed,
        skipped;
    }
}
