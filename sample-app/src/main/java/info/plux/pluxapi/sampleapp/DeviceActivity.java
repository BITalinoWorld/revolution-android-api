package info.plux.pluxapi.sampleapp;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.*;
import info.plux.pluxapi.Communication;
import info.plux.pluxapi.bitalino.*;
import info.plux.pluxapi.bitalino.bth.OnBITalinoDataAvailable;

import java.util.ArrayList;
import java.util.List;

import static info.plux.pluxapi.Constants.*;

public class DeviceActivity extends Activity implements OnBITalinoDataAvailable, View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();

    public final static String EXTRA_DEVICE = "info.plux.pluxapi.sampleapp.DeviceActivity.EXTRA_DEVICE";
    public final static String FRAME = "info.plux.pluxapi.sampleapp.DeviceActivity.Frame";

    private BluetoothDevice bluetoothDevice;

    private BITalinoCommunication bitalino;
    private boolean isBITalino2 = false;


    private Handler handler;

    private States currentState = States.DISCONNECTED;

    private boolean isUpdateReceiverRegistered = false;

    /*
     * UI elements
     */
    private TextView nameTextView;
    private TextView addressTextView;
    private TextView elapsedTextView;
    private TextView stateTextView;

    private Button connectButton;
    private Button disconnectButton;
    private Button startButton;
    private Button stopButton;

    private LinearLayout bitalinoLinearLayout;
    private Button stateButton;
    private RadioButton digital1RadioButton;
    private RadioButton digital2RadioButton;
    private RadioButton digital3RadioButton;
    private RadioButton digital4RadioButton;
    private Button triggerButton;
    private SeekBar batteryThresholdSeekBar;
    private Button batteryThresholdButton;
    private SeekBar pwmSeekBar;
    private Button pwmButton;
    private TextView resultsTextView;

    private boolean isDigital1RadioButtonChecked = false;
    private boolean isDigital2RadioButtonChecked = false;

    private float alpha = 0.25f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().hasExtra(EXTRA_DEVICE)){
            bluetoothDevice = getIntent().getParcelableExtra(EXTRA_DEVICE);
        }

        setContentView(R.layout.activity_main);

        initView();
        setUIElements();

        handler = new Handler(getMainLooper()){
          @Override
          public void handleMessage(Message msg) {
              Bundle bundle = msg.getData();
              Parcelable frame = bundle.getParcelable(FRAME);

              if(frame.getClass().equals(BITalinoFrame.class)){ //BITalino
                  resultsTextView.setText(frame.toString());
              }
          }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(updateReceiver, makeUpdateIntentFilter());
        isUpdateReceiverRegistered = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(isUpdateReceiverRegistered) {
            unregisterReceiver(updateReceiver);
        }
    }

    /*
     * UI elements
     */
    private void initView(){
        nameTextView = (TextView) findViewById(R.id.device_name_text_view);
        addressTextView = (TextView) findViewById(R.id.mac_address_text_view);
        elapsedTextView = (TextView) findViewById(R.id.elapsed_time_Text_view);
        stateTextView = (TextView) findViewById(R.id.state_text_view);

        connectButton = (Button) findViewById(R.id.connect_button);
        disconnectButton = (Button) findViewById(R.id.disconnect_button);
        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        //bitalino ui elements
        bitalinoLinearLayout = (LinearLayout) findViewById(R.id.bitalino_linear_layout);
        stateButton = (Button) findViewById(R.id.state_button);
        digital1RadioButton = (RadioButton) findViewById(R.id.digital_1_radio_button);
        digital2RadioButton = (RadioButton) findViewById(R.id.digital_2_radio_button);
        digital3RadioButton = (RadioButton) findViewById(R.id.digital_3_radio_button);
        digital4RadioButton = (RadioButton) findViewById(R.id.digital_4_radio_button);
        triggerButton = (Button) findViewById(R.id.trigger_button);
        batteryThresholdSeekBar = (SeekBar) findViewById(R.id.battery_threshold_seek_bar);
        batteryThresholdButton = (Button) findViewById(R.id.battery_threshold_button);
        pwmSeekBar = (SeekBar) findViewById(R.id.pwm_seek_bar);
        pwmButton = (Button) findViewById(R.id.pwm_button);
        resultsTextView = (TextView) findViewById(R.id.results_text_view);
    }

    private void setUIElements(){
        if(bluetoothDevice.getName() == null){
            nameTextView.setText("BITalino");
        }
        else {
            nameTextView.setText(bluetoothDevice.getName());
        }
        addressTextView.setText(bluetoothDevice.getAddress());
        stateTextView.setText(currentState.name());

        Communication communication = Communication.getById(bluetoothDevice.getType());
        Log.d(TAG, "Communication: " + communication.name());
        if(communication.equals(Communication.DUAL)){
            communication = Communication.BLE;
        }

        bitalino = new BITalinoCommunicationFactory().getCommunication(communication,this, this);

        connectButton.setOnClickListener(this);
        disconnectButton.setOnClickListener(this);
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        stateButton.setOnClickListener(this);
        digital1RadioButton.setOnClickListener(this);
        digital2RadioButton.setOnClickListener(this);
        digital3RadioButton.setOnClickListener(this);
        digital4RadioButton.setOnClickListener(this);
        triggerButton.setOnClickListener(this);
        batteryThresholdButton.setOnClickListener(this);
        pwmButton.setOnClickListener(this);
    }

    /*
     * Local Broadcast
     */
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(ACTION_STATE_CHANGED.equals(action)){
                String identifier = intent.getStringExtra(IDENTIFIER);
                States state = States.getStates(intent.getIntExtra(EXTRA_STATE_CHANGED, 0));

                Log.i(TAG, identifier + " -> " + state.name());

                stateTextView.setText(state.name());

                switch (state){
                    case NO_CONNECTION:
                        break;
                    case LISTEN:
                        break;
                    case CONNECTING:
                        break;
                    case CONNECTED:
                        break;
                    case ACQUISITION_TRYING:
                        break;
                    case ACQUISITION_OK:
                        break;
                    case ACQUISITION_STOPPING:
                        break;
                    case DISCONNECTED:
                        break;
                    case ENDED:
                        break;

                }
            }
            else if(ACTION_DATA_AVAILABLE.equals(action)){
                if(intent.hasExtra(EXTRA_DATA)){
                    Parcelable parcelable = intent.getParcelableExtra(EXTRA_DATA);
                    if(parcelable.getClass().equals(BITalinoFrame.class)){ //BITalino
                        resultsTextView.setText(parcelable.toString());
                    }
                }
            }
            else if(ACTION_COMMAND_REPLY.equals(action)){
                String identifier = intent.getStringExtra(IDENTIFIER);

                if(intent.hasExtra(EXTRA_COMMAND_REPLY) && (intent.getParcelableExtra(EXTRA_COMMAND_REPLY) != null)){
                    Parcelable parcelable = intent.getParcelableExtra(EXTRA_COMMAND_REPLY);
                    if(parcelable.getClass().equals(BITalinoState.class)){ //BITalino
                        Log.d(TAG, ((BITalinoState)parcelable).toString());
                        resultsTextView.setText(parcelable.toString());
                    }
                    else if(parcelable.getClass().equals(BITalinoDescription.class)){ //BITalino
                        isBITalino2 = ((BITalinoDescription)parcelable).isBITalino2();
                        resultsTextView.setText("isBITalino2: " + isBITalino2 + "; FwVersion: " + String.valueOf(((BITalinoDescription)parcelable).getFwVersion()));
                    }
                }
            }
        }
    };

    private IntentFilter makeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_STATE_CHANGED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ACTION_EVENT_AVAILABLE);
        intentFilter.addAction(ACTION_DEVICE_READY);
        intentFilter.addAction(ACTION_COMMAND_REPLY);
        return intentFilter;
    }

    /*
     * Callbacks
     */

    @Override
    public void onBITalinoDataAvailable(BITalinoFrame bitalinoFrame) {
        Message message = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putParcelable(FRAME, bitalinoFrame);
        message.setData(bundle);
        handler.sendMessage(message);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.connect_button:
                try {
                    bitalino.connect(bluetoothDevice.getAddress());
                } catch (BITalinoException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.disconnect_button:
                try {
                    bitalino.disconnect();
                } catch (BITalinoException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.start_button:
                try {
                    bitalino.start(new int[]{0,1,2,3,4,5}, 100);
                } catch (BITalinoException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.stop_button:
                try {
                    bitalino.stop();
                } catch (BITalinoException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.state_button:
                try {
                    bitalino.state();
                } catch (BITalinoException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.trigger_button:
                int[] digitalChannels;
                if(isBITalino2){
                    digitalChannels = new int[2];
                }
                else{
                    digitalChannels = new int[4];
                }

                digitalChannels[0] = (digital1RadioButton.isChecked()) ? 1 : 0;
                digitalChannels[1] = (digital2RadioButton.isChecked()) ? 1 : 0;

                if(!isBITalino2){
                    digitalChannels[2] = (digital3RadioButton.isChecked()) ? 1 : 0;
                    digitalChannels[4] = (digital4RadioButton.isChecked()) ? 1 : 0;
                }

                try {
                    bitalino.trigger(digitalChannels);
                } catch (BITalinoException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.digital_1_radio_button:
                if(isDigital1RadioButtonChecked){
                    digital1RadioButton.setChecked(false);
                }
                else{
                    digital1RadioButton.setChecked(true);
                }
                isDigital1RadioButtonChecked = digital1RadioButton.isChecked();
                break;
            case R.id.digital_2_radio_button:
                if(isDigital2RadioButtonChecked){
                    digital2RadioButton.setChecked(false);
                }
                else{
                    digital2RadioButton.setChecked(true);
                }
                isDigital2RadioButtonChecked = digital2RadioButton.isChecked();
                break;
            case R.id.digital_3_radio_button:
                if(digital3RadioButton.isChecked()){
                    digital3RadioButton.setChecked(false);
                }
                else{
                    digital3RadioButton.setChecked(true);
                }
                break;
            case R.id.digital_4_radio_button:
                if(digital4RadioButton.isChecked()){
                    digital4RadioButton.setChecked(false);
                }
                else{
                    digital4RadioButton.setChecked(true);
                }
                break;
            case R.id.battery_threshold_button:
                try {
                    bitalino.battery(batteryThresholdSeekBar.getProgress());
                } catch (BITalinoException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.pwm_button:
                try {
                    bitalino.pwm(pwmSeekBar.getProgress());
                } catch (BITalinoException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
