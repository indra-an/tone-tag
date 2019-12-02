package id.codify.tone_tag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.PluginRequestCodes;
import com.tonetag.tone.SoundPlayer;
import com.tonetag.tone.SoundRecorder;
import com.tonetag.tone.TTUtils;
import com.tonetag.tone.ToneTagManager;

import java.security.InvalidKeyException;

@NativePlugin(
        permissions={
                Manifest.permission.RECORD_AUDIO
        },
        requestCodes={12345}
)
public class ToneTag extends Plugin implements SoundRecorder.TTOnDataFoundListener, SoundPlayer.TTOnPlaybackFinishedListener {
    static final int PERMISSION_RECORD_AUDIO = 12345;

    private static final String TAG = ToneTag.class.getSimpleName();
    private static final String KEY = "068f0aaa2f7d1c222a68d75b972ed318d9ec98ce3d6ac8856972fb80a20256a6";
    private StringBuilder strBuilder = new StringBuilder();
    private int count = 0;
    private SoundPlayer mSoundPlayer;
    private boolean mIsChannelADataReceived, mIsChannelCDataReceived;
    private SoundRecorder mSoundRecorder;
    private Context mContext;

    private ToneTagManager mToneTagManager = null;
    private ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
    private int pval = 100;

    @PluginMethod()
    public void echo(PluginCall call) {
        String value = call.getString("value");
        JSObject ret = new JSObject();
        ret.put("value", value);
        call.success(ret);
    }

    @PluginMethod()
    public void initSdk(PluginCall call) {
        mContext = this.getBridge().getContext();
        saveCall(call);

        if (!hasRequiredPermissions()) {
            pluginRequestPermissions(new String[] {
                    Manifest.permission.RECORD_AUDIO
            }, PERMISSION_RECORD_AUDIO);
        } else {
            try {
                initToneTagSDK();
            } catch (InvalidKeyException e) {
                displayOnUI(e.getLocalizedMessage());
            }
        }
    }

    /**
     * Method to play Sound player with max input 10 digits data
     */
    @PluginMethod()
    public void playUltraTone(PluginCall call) {

        String playCount = call.getString("play_count");
        String token = call.getString("token").trim();
        String ticket = call.getString("ticket").trim();

        if (count < Integer.parseInt(playCount.trim())) {
            mSoundPlayer.setDeviceVolume(pval);

            if (!TextUtils.isEmpty(token) && TextUtils.isEmpty(ticket)) {
                mSoundPlayer.TTPlay10USString(token, 0);

            } else if (!TextUtils.isEmpty(ticket) && TextUtils.isEmpty(token)) {
                mSoundPlayer.TTPlay10USString(ticket, 2);

            } else if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(ticket)) {
                mSoundPlayer.TTPlayMixed10USString(new String[]{token, ticket}, new int[]{0, 2});
            }
        }
        saveCall(call);

        JSObject ret = new JSObject();
        ret.put("play_count", playCount);
        ret.put("token", token);
        ret.put("ticket", ticket);
        call.success(ret);
    }

//    private void togglePlayer(Button btn, final int status) {
//        if (btn == mBtnPlayUltraTone) {
//            switch (status) {
//                case 0:
//                    mEtToken.setEnabled(false);
//                    mEtTicket.setEnabled(false);
//                    mBtnPlayUltraTone.setText("Stop Play");
//                    break;
//
//                case 1:
//                    mEtToken.setEnabled(true);
//                    mEtTicket.setEnabled(true);
//                    mBtnPlayUltraTone.setText("Play");
//                    break;
//            }
//        }
//    }

    /**
     * Method to stop Sound player
     */
    @PluginMethod()
    public void stopPlaying(PluginCall call) {
        if (mSoundPlayer != null) {
            if (mSoundPlayer.isPlaying()) {
                mSoundPlayer.TTStopPlaying();
            }
        }

        JSObject ret = new JSObject();
        ret.put("message", "success stop play");
        call.success(ret);
    }

    /**
     * Method to initiate Tonetag SDK
     */
    @PluginMethod()
    private void initToneTagSDK() throws InvalidKeyException {
        PluginCall savedCall = getSavedCall();

        mToneTagManager = new ToneTagManager(mContext, KEY);
        String expiryDate = ToneTagManager.getKeyExpiryDate(KEY).toString();
        Log.e(TAG, "Expiry Date : " + expiryDate);
        Log.e(TAG, "Build : " + ToneTagManager.getToneTagLibBuildInfo());
        mSoundPlayer = mToneTagManager.getSoundPlayerInstance();
        mSoundPlayer.setTTOnPlaybackFinishedListener(this);

        JSObject ret = new JSObject();
        ret.put("message", "success init SDK");
        savedCall.success(ret);
    }

    @PluginMethod()
    private void displayOnUI(final String message) {
        this.getBridge().getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PluginCall savedCall = getSavedCall();

                JSObject ret = new JSObject();
                ret.put("message", message);
                savedCall.success(ret);
            }
        });
    }


    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("request_permission", "handling request perms result");
        PluginCall savedCall = getSavedCall();
        if (savedCall == null) {
            Log.d("request_permission", "No stored plugin call for permissions request result");
            return;
        }

        for(int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                savedCall.error("User denied permission");
                return;
            }
        }

        if (requestCode == PERMISSION_RECORD_AUDIO) {
            // We got the permission
            initSdk(savedCall);
        }
    }

    /**
     * Method will be called when the tone id decoded from ToneTag SDK
     *
     * @param receivedData = decoded tone data
     * @param recorderType = decoded recorder type
     * @param ch           = differs token and ticket decoded data
     */
    @Override
    public void TTOnDataFound(String receivedData, TTUtils.TTRecorderDataType recorderType, int ch) {
        final PluginCall savedCall = getSavedCall();
        Log.e(TAG, receivedData);
        String receivedDataString = "";
        if (recorderType == TTUtils.TTRecorderDataType.REC_ULTRASONIC_MULTI) {
            switch (ch) {
                case 0:
                    if(!mIsChannelADataReceived) {
                        receivedDataString = "Token :" + receivedData;
                        strBuilder.append(receivedDataString).append("\n");
                        mIsChannelADataReceived = true;
                        Log.e(TAG, "Received Channel A data");
                    }
                    break;

                case 2:
                    if(!mIsChannelCDataReceived) {
                        receivedDataString = "Ticket :" + receivedData;
                        strBuilder.append(receivedDataString).append("\n");
                        mIsChannelCDataReceived = true;
                        Log.e(TAG, "Received Channel C data");
                    }
                    break;
                default:
                    break;
            }
        }

        String str = strBuilder.toString();
        Log.e(TAG, str);

        this.getBridge().getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mIsChannelADataReceived && mIsChannelCDataReceived) {
                    mIsChannelADataReceived = false;
                    mIsChannelCDataReceived = false;
                    displayOnUI("Received data: \n" + strBuilder.toString());
                    try {
                        toneG.startTone(ToneGenerator.TONE_DTMF_9, 60);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
//                    toggleReceiver(mBtnReceive, 1);
                    stopRecording(savedCall);
                }
            }
        });

    }

    @PluginMethod()
    public void startRecording(PluginCall call) {
        mSoundRecorder.TTStartRecording(0);
        saveCall(call);

//        JSObject ret = new JSObject();
//        ret.put("message", "success start recording");
//        call.success(ret);
    }


    /**
     * Method to stop the recorder
     */
    @PluginMethod()
    public void stopRecording(PluginCall call) {
        if (mSoundRecorder != null) {
            if (mSoundRecorder.isRecordingOn()) {
                mSoundRecorder.TTStopRecording();
            }
        }

        JSObject ret = new JSObject();
        ret.put("message", "success stop recording");
        call.success(ret);
    }

//    private void toggleReceiver(Button btn, final int status) {
//        if (btn == mBtnReceive) {
//            switch (status) {
//                case 0:
//                    this.getBridge().getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mBtnReceive.setText("Stop Receiving");
//                        }
//                    });
//
//                    break;
//                case 1:
//                    this.getBridge().getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mBtnReceive.setText("Start Receiving");
//                        }
//                    });
//                    break;
//            }
//        }
//    }

    @Override
    public void TTOnRecorderError(int code, String message) {
        PluginCall savedCall = getSavedCall();
        savedCall.error("TTOnRecorderError: " + code + " " + message);
        Log.e(TAG, "TTOnRecorderError: " + code + " " + message);
    }

    /**
     * Method will be called when the input data is been played.
     *
     * @param playedData = Array of input data played from tone tag SDK
     * @param playerType = type of player on which data is played
     * @param ch         = Array of channel by which means of data is played
     */
    @Override
    public void TTOnPlaybackFinished(String[] playedData, int playerType, int[] ch) {
        final PluginCall savedCall = getSavedCall();

        count++;
        String playedDataString = "";
        for (int i = 0; i < playedData.length; i++) {
            Log.e(TAG, "Played data is: " + playedData[i]);
            switch (ch[i]) {
                case 0:
                    playedDataString = "Token :" + playedData[0];
                    strBuilder.append(playedDataString).append("\n");
                    break;
                case 2:
                    if (playedData.length > 1 && playedData[1] != null) {
                        playedDataString = "Ticket:" + playedData[1];
                    } else if (playedData.length == 1 && playedData[0] != null) {
                        playedDataString = "Ticket:" + playedData[0];
                    }
                    strBuilder.append(playedDataString).append("\n");
                    break;
                default:
                    break;
            }
        }

        String str = strBuilder.toString();
        displayOnUI("Played data: \n" + str);
//        doValidateStartOrStopPlayer();

    }

//    private void doValidateStartOrStopPlayer() {
//        if (count == Integer.parseInt(mEtCount.getText().toString().trim())) {
//            count = 0;
//            togglePlayer(mBtnPlayUltraTone, 1);
//            stopPlaying();
//        } else {
//            final Handler mHandler = new Handler();
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    playUltraTone();
//                }
//            }, 1000);
//        }
//    }

    /**
     * Method will be called when the input data is been played.
     *
     * @param statusCode = player status code
     * @param message    = Player error message
     */
    @Override
    public void TTOnPlaybackError(int statusCode, String message) {
        PluginCall savedCall = getSavedCall();
        savedCall.error("TTOnRecorderError: " + statusCode + " " + message);
        Log.e(TAG, "TTOnPlaybackError: " + statusCode + " " + message);

        count = 0;
    }

}
