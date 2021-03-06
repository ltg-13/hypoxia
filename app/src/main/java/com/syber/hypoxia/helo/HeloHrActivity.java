package com.syber.hypoxia.helo;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.syber.base.BaseActivity;
import com.syber.base.BaseViewHolder;
import com.syber.base.view.ViewPost;
import com.syber.hypoxia.IApplication;
import com.syber.hypoxia.R;
import com.syber.hypoxia.bt.FlowExtra;
import com.syber.hypoxia.data.HeartHistoryResponse;
import com.syber.hypoxia.data.IRequester;
import com.syber.hypoxia.widget.HoloCircularProgressBar;

import java.util.ArrayList;
import java.util.Date;

public class HeloHrActivity extends BaseActivity implements BTManager.RequestListener {
    private int pul;
    private BTManager bleHelper;
    private ConnectHeloFragment connectHeloFragment;
    private ViewHolder viewHolder;
    private Bus bus = new Bus();
    private ArrayList<HeartHistoryResponse.HistoryItem> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helo_hr);
        initAppBar();
        connectHeloFragment = new ConnectHeloFragment();
        viewHolder = new ViewHolder();
        ViewPost.postOnAnimation(getWindow().getDecorView(), new Runnable() {
            @Override
            public void run() {
                startFlow();
            }
        });
    }

    private void startFlow() {
        bleHelper = new BTManager();
        bleHelper.setRequestListener(this);
        connectHeloFragment.show(getSupportFragmentManager(), "connect");
        bleHelper.startHeloHR(HeloHrActivity.this);
        viewHolder.state.setText("测量中");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!bleHelper.handleEnableResult(requestCode, resultCode, data)) {
            finish();
        } else {
            bleHelper.start(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleHelper.stop();
        bleHelper.setRequestListener(null);
        bleHelper = null;
    }

    @Override
    public void onRequestConfirm(int request, Intent data) {
        if (FlowExtra.REQUEST_MATCHED == request) {
            connectHeloFragment.dismiss();
            bleHelper.setRequestConfirmed(request, FlowExtra.CONFIRM_OK);
            viewHolder.start.setStart(true);
            new Timer().start();
        } else if (FlowExtra.REQUEST_BIND == request) {
            showToast("发现设备,准备绑定");
            bleHelper.setRequestConfirmed(request, FlowExtra.CONFIRM_OK);
        } else if (FlowExtra.REQUEST_BINDED_OTHER == request) {
            bleHelper.stop();
            viewHolder.start.setClickable(true);
            connectHeloFragment.dismiss();
            new HeloBindedOtherFragment().show(getSupportFragmentManager(), "connected_other");
        } else if (FlowExtra.RESULT_HR == request) {
            pul = data.getIntExtra(FlowExtra.KEY_PUL, 0);
        }
    }

    private class ViewHolder extends BaseViewHolder {
        TextView countDown, state;
        RecyclerView recyclerView;
        HoloCircularProgressBar start;

        public ViewHolder() {
            super(findViewById(R.id.view_holder));
            countDown = get(R.id.count_down);
            state = get(R.id.state);
            recyclerView = get(R.id.list);
            recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext(), LinearLayoutManager.VERTICAL, false));
            recyclerView.setAdapter(new HistoryAdapter());
            start = get(R.id.start);
            start.setOnClickListener(this);
            start.setClickable(false);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (R.id.start == id) {
                v.setClickable(false);
                startFlow();
            }
        }
    }

    private class Timer extends CountDownTimer {
        public Timer() {
            super(40 * 1000, 500);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if (isFinishing()) {
                cancel();
                return;
            }
            viewHolder.countDown.setText(String.format("%d″", (millisUntilFinished + 500) / 1000));
        }

        @Override
        public void onFinish() {
            if (isFinishing()) return;
            viewHolder.start.setClickable(true);
            viewHolder.start.setStart(false);
            bleHelper.stop();
            viewHolder.countDown.setText("测量");
            if (pul > 0) {
                viewHolder.state.setText("测量完成");
                String start = IApplication.dateFormat.format(new Date());
                IRequester.getInstance().addBP(bus, start, 0, 0, pul);
                HeartHistoryResponse.HistoryItem item = new HeartHistoryResponse.HistoryItem();
                item.heartrate = pul;
                item.time_test = start;
                data.add(0, item);
                viewHolder.recyclerView.getAdapter().notifyItemInserted(0);
            } else {
                viewHolder.state.setText("测量失败");
            }
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<AdapterHolder> {

        @Override
        public AdapterHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new AdapterHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_heart_rate_history, parent, false));
        }

        @Override
        public void onBindViewHolder(AdapterHolder holder, int position) {
            HeartHistoryResponse.HistoryItem item = data.get(position);
            holder.date.setText(item.time_test);
            holder.rate.setText("心率" + item.heartrate);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class AdapterHolder extends RecyclerView.ViewHolder {
        TextView date, rate;
        View abnormal;

        public AdapterHolder(View itemView) {
            super(itemView);
            date = BaseViewHolder.get(itemView, R.id.date);
            rate = BaseViewHolder.get(itemView, R.id.rate);
            abnormal = BaseViewHolder.get(itemView, R.id.state);
        }
    }

}
