/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrftoolhax.uart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import no.nordicsemi.android.nrftoolhax.R;
import no.nordicsemi.android.nrftoolhax.uart.domain.Command;
import no.nordicsemi.android.nrftoolhax.uart.domain.UartConfiguration;

public class UARTControlFragment extends Fragment implements GridView.OnItemClickListener, UARTActivity.ConfigurationListener {
	private final static String TAG = "UARTControlFragment";
	private final static String SIS_EDIT_MODE = "sis_edit_mode";
    private final static int    PACKETS_PER_ACK = 5;

    /*
    public static final String BROADCAST_UART_TX = "no.nordicsemi.android.nrftoolhax.uart.BROADCAST_UART_TX";
    public static final String BROADCAST_UART_RX = "no.nordicsemi.android.nrftoolhax.uart.BROADCAST_UART_RX";
    public static final String EXTRA_DATA = "no.nordicsemi.android.nrftoolhax.uart.EXTRA_DATA";
    */

	private UartConfiguration mConfiguration;
	private UARTButtonAdapter mAdapter;
	private boolean mEditMode;

	private Context mContext;
    /*
	private boolean mIsRunning;
	private UARTInterface mUart;
	private InputStream mAudioSample;
	private Timer mTimer = new Timer();
	private Handler mHandler = new Handler(Looper.getMainLooper());
	private LinkedList<byte[]> mAudioFrameBuffer = new LinkedList<byte[]>();
    private boolean mWaitingForAck = false;
    private int     mPacketsUntilAck = PACKETS_PER_ACK;
    private BroadcastReceiver mUARTRXBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra(EXTRA_DATA);
            Log.d("NrFToolhax", "Received data: " + data);
        }
    };
    */

	@Override
	public void onAttach(final Context context) {
		super.onAttach(context);

		mContext = context;
        //mContext.registerReceiver(mUARTRXBroadcastReceiver, new IntentFilter(BROADCAST_UART_RX));

		try {
			((UARTActivity)context).setConfigurationListener(this);
		} catch (final ClassCastException e) {
			Log.e(TAG, "The parent activity must implement EditModeListener");
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			mEditMode = savedInstanceState.getBoolean(SIS_EDIT_MODE);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		((UARTActivity)getActivity()).setConfigurationListener(null);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		outState.putBoolean(SIS_EDIT_MODE, mEditMode);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_feature_uart_control, container, false);

		final GridView grid = (GridView) view.findViewById(R.id.grid);
		grid.setAdapter(mAdapter = new UARTButtonAdapter(mConfiguration));
		grid.setOnItemClickListener(this);
		mAdapter.setEditMode(mEditMode);

		return view;
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		if (mEditMode) {
			Command command = mConfiguration.getCommands()[position];
			if (command == null)
				mConfiguration.getCommands()[position] = command = new Command();
			final UARTEditDialog dialog = UARTEditDialog.getInstance(position, command);
			dialog.show(getChildFragmentManager(), null);
		} else {
			final String command = ((Command)mAdapter.getItem(position)).getCommand();

			//if (!mIsRunning) {

				int resourceID = 0;
				String sampleName = "";

				if (command.contains("1")){
					sampleName = "sample 1";
					resourceID = R.raw.sample_1_1khz;
				}
				else if (command.contains("2")){
					sampleName = "sample 2";
					resourceID = R.raw.sample_2_dagsnytt_atten_intro;
				}
				else if (command.contains("3")){
					sampleName = "sample 3";
					resourceID = R.raw.sample_3_bbc_news_intro;
				}
				else if (command.contains("4")){
					sampleName = "sample 4";
					resourceID = R.raw.sample_4_fire_it_up;
				}
				else if (command.contains("5")){
					sampleName = "sample 5";
					resourceID = R.raw.sample_5_pzeyes03;
				}
				else if (command.contains("6")){
					sampleName = "sample 6";
					resourceID = R.raw.sample_6_ppwrdown;
				}
				else {
					Toast.makeText(mContext, "Sample not found", Toast.LENGTH_SHORT).show();
					return;
				}

                UARTInterface uartInterface = (UARTInterface) getActivity();
                uartInterface.sendResourceFile(resourceID, sampleName);

                return;

                /*
				mAudioSample     = getResources().openRawResource(resourceID);

                mWaitingForAck   = false;
                mPacketsUntilAck = PACKETS_PER_ACK;
				Toast.makeText(mContext, "Playing " + sampleName, Toast.LENGTH_SHORT).show();

				mUart = (UARTInterface) getActivity();

				mTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						int byteCount = 0;
						byte[] frame = new byte[20];

						if (mWaitingForAck) {
                            return;
                        }

						try {
							byteCount = mAudioSample.read(frame);
						} catch (IOException e) {
							e.printStackTrace();
						}

						if (byteCount == 20) {
							mAudioFrameBuffer.add(frame);
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									mUart.send(mAudioFrameBuffer.remove());
                                    mPacketsUntilAck -= 1;
                                    if (mPacketsUntilAck == 0) {
                                        mWaitingForAck = true;
                                    }
								}
							});
						}
						else {
							cancel();
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									try {
										byte[] msg = new byte[4];
										msg[0] = 'S';
										msg[1] = 't';
										msg[2] = 'o';
										msg[3] = 'p';
										mUart.send(msg);
										Thread.sleep(10);
										mUart.send(msg);
										Thread.sleep(20);
										mUart.send(msg);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									mIsRunning = false;
								}
							});
						}
					}
				}, 10, 10);
			}
			else {
				Toast.makeText(mContext, "Please wait", Toast.LENGTH_SHORT).show();
			}
			*/
		}
	}

	@Override
	public void onConfigurationModified() {
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onConfigurationChanged(final UartConfiguration configuration) {
		mConfiguration = configuration;
		mAdapter.setConfiguration(configuration);
	}

	@Override
	public void setEditMode(final boolean editMode) {
		mEditMode = editMode;
		mAdapter.setEditMode(mEditMode);
	}
}
