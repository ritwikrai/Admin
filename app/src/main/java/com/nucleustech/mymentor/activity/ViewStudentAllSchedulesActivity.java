package com.nucleustech.mymentor.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.nucleustech.mymentor.R;
import com.nucleustech.mymentor.adapter.ScheduleAdapter;
import com.nucleustech.mymentor.model.Schedule;
import com.nucleustech.mymentor.model.UserClass;
import com.nucleustech.mymentor.util.AlertDialogCallBack;
import com.nucleustech.mymentor.util.Util;
import com.nucleustech.mymentor.volley.ServerResponseCallback;
import com.nucleustech.mymentor.volley.VolleyTaskManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by knowalladmin on 24/12/17.
 */

public class ViewStudentAllSchedulesActivity extends AppCompatActivity implements ServerResponseCallback {

    private TextView tv_scheduleChat_dateTime;
    private Context mContext;
    VolleyTaskManager volleyTaskManager;
    DatePickerDialog datePickerDialog;
    TimePickerDialog timePickerDialog;
    int changedYear, changedMonth, changedDay, changedHour, changedMinute, changedSecond;
    private boolean isScheduleService = false;
    private ArrayList<Schedule> scheduleArrayList;
    private ListView lv_schedules;
    private String studentID = "";

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
        setContentView(R.layout.activity_view_all_schedule);
        mContext = ViewStudentAllSchedulesActivity.this;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Schedules");
        studentID = getIntent().getStringExtra("studentId");

        tv_scheduleChat_dateTime = (TextView) findViewById(R.id.tv_scheduleChat_dateTime);
        lv_schedules = (ListView) findViewById(R.id.lv_schedules);

        volleyTaskManager = new VolleyTaskManager(mContext);

        Calendar newCalendar = Calendar.getInstance();
        UserClass userClass = Util.fetchUserClass(mContext);
        HashMap<String, String> requestMap = new HashMap<>();
        requestMap.put("userID", "" + studentID);
        volleyTaskManager.doPostFetchSchedule(requestMap, true);
        //volleyTaskManager.doPostFetchSchedules(new HashMap<String, String>(), true);
    }


    @Override
    public void onSuccess(JSONObject resultJsonObject) {


        if (resultJsonObject.optString("code").equalsIgnoreCase("200")) {
            try {
                scheduleArrayList = new ArrayList<>();
                JSONArray schedules = resultJsonObject.optJSONArray("schedules");

                if (schedules != null && schedules.length() > 0) {

                    for (int i = 0; i < schedules.length(); i++) {
                        JSONObject userSchedule = schedules.optJSONObject(i);
                        Schedule schedule = new Schedule();
                        schedule.userID = userSchedule.optString("userID");
                        schedule.name = userSchedule.optString("name");
                        schedule.scheduledDate = userSchedule.optString("scheduleDate");
                        schedule.scheduledTime = userSchedule.optString("scheduleTime");
                        schedule.meetingId = userSchedule.optString("meetingID");
                        scheduleArrayList.add(schedule);
                    }

                    // Load Adapter
                    ScheduleAdapter scheduleAdapter = new ScheduleAdapter(mContext, scheduleArrayList);
                    lv_schedules.setAdapter(scheduleAdapter);
                    lv_schedules.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                            Schedule schedule = scheduleArrayList.get(i);
                            Intent intent = new Intent(mContext, ViewUserScheduleActivity.class);
                            intent.putExtra("schedule", schedule);
                            startActivity(intent);
                            finish();
                        }
                    });
                }
                else{
                    Util.showCallBackMessageWithOkCallback(mContext, "No schedules found.", new AlertDialogCallBack() {
                        @Override
                        public void onSubmit() {
                            finish();
                        }

                        @Override
                        public void onCancel() {

                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace(); Util.showCallBackMessageWithOkCallback(mContext, "Something went wrong.", new AlertDialogCallBack() {
                    @Override
                    public void onSubmit() {
                        finish();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
            }
        }

    }

    @Override
    public void onError() {

    }


}
