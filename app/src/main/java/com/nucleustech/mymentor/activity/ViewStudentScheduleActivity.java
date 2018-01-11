package com.nucleustech.mymentor.activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.nucleustech.mymentor.R;
import com.nucleustech.mymentor.model.UserClass;
import com.nucleustech.mymentor.util.Util;
import com.nucleustech.mymentor.volley.ServerResponseCallback;
import com.nucleustech.mymentor.volley.VolleyTaskManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by knowalladmin on 24/12/17.
 */

public class ViewStudentScheduleActivity extends AppCompatActivity implements ServerResponseCallback {

    private TextView tv_scheduleChat_dateTime;
    private Context mContext;
    VolleyTaskManager volleyTaskManager;
    DatePickerDialog datePickerDialog;
    TimePickerDialog timePickerDialog;
    int changedYear, changedMonth, changedDay, changedHour, changedMinute, changedSecond;
    private boolean isScheduleService = false;
    private String studentID = "";
    private String studentName = "";
    private String scheduledChatTimeStamp="";

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_schedule);
        mContext = ViewStudentScheduleActivity.this;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Schedule Meet");
        tv_scheduleChat_dateTime = (TextView) findViewById(R.id.tv_scheduleChat_dateTime);

        volleyTaskManager = new VolleyTaskManager(mContext);
        studentName = getIntent().getStringExtra("name");

        studentID = getIntent().getStringExtra("studentId");
        Calendar newCalendar = Calendar.getInstance();
        //UserClass userClass = Util.fetchUserClass(mContext);
        //tv_scheduleChat_dateTime.setText(userClass.scheduledChatTimeStamp);
        datePickerDialog = new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                changedYear = year;
                changedMonth = monthOfYear;
                changedDay = dayOfMonth;
                timePickerDialog.show();
                // tv_scheduleChat_dateTime.setText(dateFormatter.format(newDate.getTime()));

            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));


        timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        changedHour = hourOfDay;
                        changedMinute = minute;

                        tv_scheduleChat_dateTime.setText(changedDay + "/" + changedMonth + "/" + changedYear + "    " + changedHour + ":" + changedMinute + ":" + "00");
                        tv_scheduleChat_dateTime.setClickable(false);
                        callScheduleChatService();
                    }
                }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), true);

        HashMap<String, String> requestMap = new HashMap<>();
        requestMap.put("userID", "" +studentID);
        volleyTaskManager.doPostFetchSchedule(requestMap, true);
    }

    public void onRescheduleClick(View view) {
        setScheduleForAdminChat();
    }


    @Override
    public void onSuccess(JSONObject resultJsonObject) {

        if (isScheduleService) {
            isScheduleService = false;
            if (resultJsonObject.optString("code").trim().equalsIgnoreCase("200")) {
                Toast.makeText(mContext, "Chat Session Scheduled.", Toast.LENGTH_LONG).show();


            } else {
                Toast.makeText(mContext, "Something went wrong please try again.", Toast.LENGTH_LONG).show();
            }
        } else {
            if (resultJsonObject.optString("code").equalsIgnoreCase("200")) {
                try {
                    JSONArray schedules = resultJsonObject.optJSONArray("schedules");
                    for (int i = 0; i < schedules.length(); i++) {
                        JSONObject userSchedule = schedules.optJSONObject(i);
                        String date = userSchedule.optString("scheduleDate");
                        String time = userSchedule.optString("scheduleTime");
                        String timeStamp = date+ "   " + time;
                        if (timeStamp == null || timeStamp.trim().equalsIgnoreCase("")) {
                            tv_scheduleChat_dateTime.setText("SCHEDULE CHAT");
                            tv_scheduleChat_dateTime.setClickable(true);
                            tv_scheduleChat_dateTime.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    setScheduleForAdminChat();
                                }
                            });
                        } else {
                            tv_scheduleChat_dateTime.setText("" + timeStamp);
                            tv_scheduleChat_dateTime.setClickable(false);
                        }
                        scheduledChatTimeStamp= timeStamp;
                    }

                } catch (Exception e) {
                    e.printStackTrace();

                    tv_scheduleChat_dateTime.setText("SCHEDULE CHAT");
                    tv_scheduleChat_dateTime.setClickable(true);
                    tv_scheduleChat_dateTime.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            setScheduleForAdminChat();
                        }
                    });

                }
            }
        }
    }

    @Override
    public void onError() {

    }

    private void setScheduleForAdminChat() {

        datePickerDialog.show();
    }

    private void callScheduleChatService() {

        HashMap<String, String> requestMap = new HashMap<>();
        requestMap.put("userID", "" + Util.fetchUserClass(mContext).getUserId());
        requestMap.put("scheduleDate", "" + changedDay + "/" + changedMonth + "/" + changedYear);
        requestMap.put("scheduleTime", "" + changedHour + ":" + changedMinute);
        isScheduleService = true;
        volleyTaskManager.doPostAdminChatSchedule(requestMap, true);
    }

    /**
     * Cancel scheduled Chat
     */
    public void onCancelScheduleClick(View view) {

        if (scheduledChatTimeStamp != null && scheduledChatTimeStamp.trim().length() > 0) {

            final Dialog dialog = new Dialog(mContext);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

            dialog.setContentView(R.layout.dialog_cancel_meeting);
            dialog.setCancelable(true);
            Button btn_submit = (Button) dialog.findViewById(R.id.btn_submit);
            final TextView tv_meetDateTime = (TextView) dialog.findViewById(R.id.tv_meetDateTime);


            btn_submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();

                    tv_scheduleChat_dateTime.setText("SCHEDULE CHAT");
                }
            });
            dialog.show();
        }

    }
}
