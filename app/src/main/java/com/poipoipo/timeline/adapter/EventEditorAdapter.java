package com.poipoipo.timeline.adapter;

import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.poipoipo.timeline.R;
import com.poipoipo.timeline.data.DateMessageEvent;
import com.poipoipo.timeline.data.Event;
import com.poipoipo.timeline.data.Label;
import com.poipoipo.timeline.data.TimeMessageEvent;
import com.poipoipo.timeline.data.TimestampUtil;
import com.poipoipo.timeline.database.DatabaseHelper;
import com.poipoipo.timeline.dialog.DatePickerFragment;
import com.poipoipo.timeline.dialog.TimePickerFragment;
import com.poipoipo.timeline.ui.MainActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventEditorAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements View.OnClickListener, Serializable {
    private static final String TAG = "EventEditorAdapter";
    private static final int TYPE_NOR = 1, TYPE_HEADER = 0, TYPE_FOOTER = 2;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private Map<Integer, Integer> labels;
    private OnEventChangedListener mListener;
    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private Context context;
    private FragmentManager manager;
    private DatabaseHelper databaseHelper;
    private HeaderViewHolder headerViewHolder;
    private int key;
    private int value;
    private SpinnerAdapter adapter;

    public EventEditorAdapter(Event event, Context context, OnEventChangedListener mListener) {
        this.labels = event.getLabelsMap();
        startCalendar.setTimeInMillis(event.getStart() * 1000L);
        endCalendar.setTimeInMillis(event.getEnd() * 1000L);
        this.context = context;
        manager = ((MainActivity) context).getFragmentManager();
        this.mListener = mListener;
        databaseHelper = ((MainActivity) context).databaseHelper;
        labels = event.getLabelsMap();
        EventBus.getDefault().register(this);
        setAdapter();
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_NOR) {
            final LabelViewHolder viewHolder = (LabelViewHolder) holder;
            final int key = (int) labels.keySet().toArray()[position - 1];
            final int value = labels.get(key);
            final Label label = databaseHelper.labelMap.get(key);
            Log.d(TAG, "onBindViewHolder: key = " + key);
            Log.d(TAG, "onBindViewHolder: value = " + value);
            viewHolder.icon.setAdapter(adapter);
            viewHolder.icon.setSelection(label.position);
            viewHolder.icon.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (++i == viewHolder.icon.getAdapter().getCount()) {
                        labels.remove(key);
                        notifyItemRemoved(viewHolder.getAdapterPosition());
                    } else if (i != key) {
                        mListener.onEventChanged(i, 1);
                        labels.put(i, 1);
                        labels.remove(key);
                        notifyDataSetChanged();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            viewHolder.text.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(label.name.values())));
            if (value != 0) {
                viewHolder.text.setSelection(label.index.get(value));
            }
            viewHolder.text.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (++i != value) {
                        mListener.onEventChanged(key, i);
                        labels.put(key, i);
                        notifyItemChanged(viewHolder.getAdapterPosition());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        } else if (getItemViewType(position) == TYPE_HEADER) {
            headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.startDate.setOnClickListener(this);
            headerViewHolder.startTime.setOnClickListener(this);
            headerViewHolder.endDate.setOnClickListener(this);
            headerViewHolder.endTime.setOnClickListener(this);
            headerViewHolder.startCurrent.setOnClickListener(this);
            headerViewHolder.endCurrent.setOnClickListener(this);
            updateTime(Event.START);
            updateTime(Event.END);
        } else {
            ((FooterViewHolder) holder).button.setOnClickListener(this);
        }
    }

    private void setAdapter() {
        List<String> text = new ArrayList<>();
        List<Integer> icon = new ArrayList<>();
        for (Label label : databaseHelper.labelMap.values()) {
            text.add(label.value);
            icon.add(label.icon);
        }
        text.add("Remove");
        icon.add(R.drawable.ic_remove);
        adapter = new SpinnerAdapter(context, text, icon);
    }

    private void updateTime(int type) {
        switch (type) {
            case Event.START:
                headerViewHolder.startDate.setText(dateFormat.format(startCalendar.getTime()));
                headerViewHolder.startTime.setText(timeFormat.format(startCalendar.getTime()));
            case Event.END:
                headerViewHolder.endDate.setText(dateFormat.format(endCalendar.getTime()));
                headerViewHolder.endTime.setText(timeFormat.format(endCalendar.getTime()));
        }
        if (startCalendar.after(endCalendar) || endCalendar.equals(startCalendar)) {
            headerViewHolder.endDate.setTextColor(Color.RED);
            headerViewHolder.endTime.setTextColor(Color.RED);
            mListener.onEventChanged(Event.ERROR_TIME, 0);
        } else {
            headerViewHolder.endDate.setTextColor(Color.BLACK);
            headerViewHolder.endTime.setTextColor(Color.BLACK);
            mListener.onKeyRemoved(Event.ERROR_TIME);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.event_editor_start_date:
                DatePickerFragment.newInstance(Event.START, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH)).show(manager, "DatePicker");
                break;
            case R.id.event_editor_start_time:
                TimePickerFragment.newInstance(Event.START, startCalendar.get(Calendar.HOUR), startCalendar.get(Calendar.MINUTE)).show(manager, "TimePicker");
                break;
            case R.id.event_editor_end_date:
                DatePickerFragment.newInstance(Event.END, endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH)).show(manager, "DatePicker");
                break;
            case R.id.event_editor_end_time:
                TimePickerFragment.newInstance(Event.END, endCalendar.get(Calendar.HOUR), endCalendar.get(Calendar.MINUTE)).show(manager, "TimePicker");
                break;
            case R.id.event_editor_start_current:
                startCalendar = Calendar.getInstance();
                updateTime(Event.START);
                mListener.onEventChanged(Event.START, TimestampUtil.getTimestampByCalendar(startCalendar));
                break;
            case R.id.event_editor_end_current:
                endCalendar = Calendar.getInstance();
                updateTime(Event.END);
                mListener.onEventChanged(Event.END, TimestampUtil.getTimestampByCalendar(endCalendar));
                break;
            case R.id.event_editor_add_label:
                if (labels.size() < databaseHelper.labelMap.size()) {
                    for (Integer key : databaseHelper.labelMap.keySet()) {
                        if (!labels.containsKey(key)) {
                            labels.put(key, 1);
                            notifyItemInserted(getItemCount());
                            mListener.onEventChanged(key, 1);
                            break;
                        }
                    }
                }
        }
    }

    @Subscribe
    public void onTimeMessageEvent(TimeMessageEvent event) {
        switch (event.type) {
            case Event.START:
                startCalendar.set(Calendar.HOUR, event.hour);
                startCalendar.set(Calendar.MINUTE, event.minute);
                mListener.onEventChanged(Event.START, TimestampUtil.getTimestampByCalendar(startCalendar));
                updateTime(event.type);
                break;
            case Event.END:
                endCalendar.set(Calendar.HOUR, event.hour);
                endCalendar.set(Calendar.MINUTE, event.minute);
                mListener.onEventChanged(Event.END, TimestampUtil.getTimestampByCalendar(endCalendar));
                updateTime(event.type);
        }
    }

    @Subscribe
    public void onDateMessageEvent(DateMessageEvent event) {
        switch (event.type) {
            case Event.START:
                startCalendar.set(Calendar.YEAR, event.year);
                startCalendar.set(Calendar.MONTH, event.month);
                startCalendar.set(Calendar.DAY_OF_MONTH, event.day);
                mListener.onEventChanged(Event.START, TimestampUtil.getTimestampByCalendar(startCalendar));
                updateTime(event.type);
                break;
            case Event.END:
                endCalendar.set(Calendar.YEAR, event.year);
                endCalendar.set(Calendar.MONTH, event.month);
                endCalendar.set(Calendar.DAY_OF_MONTH, event.day);
                mListener.onEventChanged(Event.END, TimestampUtil.getTimestampByCalendar(endCalendar));
                updateTime(event.type);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        } else if (position == labels.size() + 1) {
            return TYPE_FOOTER;
        } else {
            return TYPE_NOR;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_NOR) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_label_edit, parent, false);
            return new LabelViewHolder(view);
        } else if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_footer, parent, false);
            return new FooterViewHolder(view);
        }
    }

    @Override
    public int getItemCount() {
        return labels.size() + 2;
    }

    public interface OnEventChangedListener {
        void onEventChanged(int key, int value);

        void onKeyRemoved(int key);
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        Button startDate;
        Button startTime;
        Button endDate;
        Button endTime;
        ImageButton startCurrent;
        ImageButton endCurrent;

        public HeaderViewHolder(final View view) {
            super(view);
            startDate = (Button) view.findViewById(R.id.event_editor_start_date);
            startTime = (Button) view.findViewById(R.id.event_editor_start_time);
            endDate = (Button) view.findViewById(R.id.event_editor_end_date);
            endTime = (Button) view.findViewById(R.id.event_editor_end_time);
            startCurrent = (ImageButton) view.findViewById(R.id.event_editor_start_current);
            endCurrent = (ImageButton) view.findViewById(R.id.event_editor_end_current);
        }
    }

    static class LabelViewHolder extends RecyclerView.ViewHolder {
        Spinner icon;
        Spinner text;

        public LabelViewHolder(final View view) {
            super(view);
            icon = (Spinner) view.findViewById(R.id.edit_label_icon);
            text = (Spinner) view.findViewById(R.id.edit_label_text);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        Button button;

        public FooterViewHolder(final View view) {
            super(view);
            button = (Button) view.findViewById(R.id.event_editor_add_label);
        }
    }
}
