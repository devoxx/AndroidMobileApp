package com.devoxx.devoxx_pl.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class NfcTagAdapter {
	private final Activity activity;
	private final NfcAdapter adapter;
	private final PendingIntent pendingIntent;
	private final IntentFilter writeTagFilters[];

	public NfcTagAdapter(Activity activity) {
		this.activity = activity;
		adapter = NfcAdapter.getDefaultAdapter(activity);
		pendingIntent = PendingIntent.getActivity(activity, 0, new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
		writeTagFilters = new IntentFilter[]{tagDetected};
	}

	public void disableForegroundDispatch() {
		if (isNfcEnabled()) {
			adapter.disableForegroundDispatch(activity);
		}
	}

	public void enableForegroundDispatch() {
		if (isNfcEnabled()) {
			adapter.enableForegroundDispatch(activity, pendingIntent, writeTagFilters, null);
		}
	}

	public void cancelPendingIntent() {
		pendingIntent.cancel();
	}

	public String read(Parcelable[] rawMsgs) {
		if (rawMsgs != null) {
			NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
			for (int i = 0; i < rawMsgs.length; i++) {
				msgs[i] = (NdefMessage) rawMsgs[i];
			}
			try {
				return new String(msgs[0].getRecords()[0].getPayload(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void write(String text, Tag tag) throws IOException, FormatException {
		NdefRecord[] records = {createRecord(text)};
		NdefMessage message = new NdefMessage(records);
		Ndef ndef = Ndef.get(tag);
		ndef.connect();
		ndef.writeNdefMessage(message);
		ndef.close();
	}

	public String byteArrayToHexString(byte[] inarray) { // converts byte arrays to string
		int i, j, in;
		String[] hex = {
				"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"
		};
		String out = "";

		for (j = 0; j < inarray.length; ++j) {
			in = inarray[j] & 0xff;
			i = (in >> 4) & 0x0f;
			out += hex[i];
			i = in & 0x0f;
			out += hex[i];
		}
		return out;
	}

	private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
		byte[] textBytes = text.getBytes("UTF-8");
		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], textBytes);
	}

	public boolean isNfcEnabled() {
		return adapter != null;
	}
}
