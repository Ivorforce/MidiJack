package mmmlabs.com.mididroid;

// Features.
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.media.midi.MidiDeviceInfo;
import android.os.Bundle;

// Unity
import com.example.android.common.midi.MidiConstants;
import com.example.android.common.midi.MidiFramer;
import com.unity3d.player.UnityPlayer;

// Debug
import android.os.Looper;
import android.util.Log;

// MIDI
import android.media.midi.MidiManager;
import android.media.midi.MidiDevice;
import android.media.midi.*;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

public class MidiDroid extends Fragment {

    // Constants.
    public static final String TAG = "MidiDROID";

    // Singleton instance.
    public static MidiDroid instance;

    MidiCallback midiCallback;

    MidiManager manager;
    BluetoothManager btManager;

    // Receiver that parses raw data into complete messages.
    MidiFramer connectFramer = new MidiFramer(new MyReceiver());

    MidiDevice current;
    private MidiOutputPort mOutputPort;

    public static void start()
    {
        // Instantiate and add to Unity Player Activity.
        Log.i(TAG, "Starting MidiDROID");
        instance = new MidiDroid();
        UnityPlayer.currentActivity.getFragmentManager().beginTransaction().add(instance, MidiDroid.TAG).commit();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // Retain between configuration changes (like device rotation)

        Context context = UnityPlayer.currentActivity.getApplicationContext();
        manager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
        btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        manager.registerDeviceCallback(new MidiManager.DeviceCallback() {
            @Override
            public void onDeviceRemoved(MidiDeviceInfo device) {
                super.onDeviceRemoved(device);

                reset();
            }

            @Override
            public void onDeviceAdded(MidiDeviceInfo device) {
                super.onDeviceAdded(device);

                reset();
            }
        }, null);

        reset();
    }

    public void reset() {
        for (BluetoothDevice bondedDevice : btManager.getAdapter().getBondedDevices()) {
            connect(bondedDevice);
        }
    }

    public void connect(BluetoothDevice device) {
        manager.openBluetoothDevice(device, new MidiManager.OnDeviceOpenedListener() {
            @Override
            public void onDeviceOpened(MidiDevice midiDevice) {
                mOutputPort = midiDevice.openOutputPort(0);
                if (mOutputPort == null) {
                    return;
                }
                current = midiDevice;

                //      mOutputPort.connect(new LogReceiver());
                mOutputPort.connect(connectFramer);
            }
        }, null);
    }

    private class MyReceiver extends MidiReceiver {
        @Override
        public void onSend(byte[] data, int offset, int count, long timestamp)
                throws IOException {
                if(midiCallback != null){
                    midiCallback.midiJackMessage(current.getInfo().getId(), data[0], data[1], data[2]);
                }
        }
    }

    public void setMidiCallback(MidiCallback callback){
        midiCallback = callback;
    }
}
