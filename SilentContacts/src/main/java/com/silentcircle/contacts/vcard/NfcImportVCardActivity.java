/*
Copyright © 2013-2014, Silent Circle, LLC.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal 
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the names of its contributors may 
      be used to endorse or promote products derived from this software 
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL SILENT CIRCLE, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/*
 * This  implementation is edited version of original Android sources.
 */

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.silentcircle.contacts.vcard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.silentcircle.contacts.R;
import com.silentcircle.silentcontacts.ScContactsContract.RawContacts;
import com.silentcircle.vcard.VCardEntry;
import com.silentcircle.vcard.VCardEntryCounter;
import com.silentcircle.vcard.VCardParser;
import com.silentcircle.vcard.VCardParser_V21;
import com.silentcircle.vcard.VCardParser_V30;
import com.silentcircle.vcard.VCardSourceDetector;
import com.silentcircle.vcard.exception.VCardException;
import com.silentcircle.vcard.exception.VCardNestedException;
import com.silentcircle.vcard.exception.VCardVersionException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class NfcImportVCardActivity extends Activity implements ServiceConnection, VCardImportExportListener {
    private static final String TAG = "NfcImportVCardActivity";

    private static final int SELECT_ACCOUNT = 1;

    private NdefRecord mRecord;

    class ImportTask extends AsyncTask<VCardService, Void, ImportRequest> {
        @Override
        public ImportRequest doInBackground(VCardService... services) {
            ImportRequest request = createImportRequest();
            if (request == null) {
                return null;
            }

            ArrayList<ImportRequest> requests = new ArrayList<ImportRequest>();
            requests.add(request);
            services[0].handleImportRequest(requests, NfcImportVCardActivity.this);
            return request;
        }

        @Override
        public void onCancelled() {
            unbindService(NfcImportVCardActivity.this);
        }

        @Override
        public void onPostExecute(ImportRequest request) {
            unbindService(NfcImportVCardActivity.this);
        }
    }

    ImportRequest createImportRequest() {
        VCardParser parser;
        VCardEntryCounter counter = null;
        VCardSourceDetector detector = null;
        int vcardVersion = ImportVCardActivity.VCARD_VERSION_V21;
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(mRecord.getPayload());
            is.mark(0);
            parser = new VCardParser_V21();
            try {
                counter = new VCardEntryCounter();
                detector = new VCardSourceDetector();
                parser.addInterpreter(counter);
                parser.addInterpreter(detector);
                parser.parse(is);
            } catch (VCardVersionException e1) {
                is.reset();
                vcardVersion = ImportVCardActivity.VCARD_VERSION_V30;
                parser = new VCardParser_V30();
                try {
                    counter = new VCardEntryCounter();
                    detector = new VCardSourceDetector();
                    parser.addInterpreter(counter);
                    parser.addInterpreter(detector);
                    parser.parse(is);
                } catch (VCardVersionException e2) {
                    return null;
                }
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed reading vcard data", e);
            return null;
        } catch (VCardNestedException e) {
            Log.w(TAG, "Nested Exception is found (it may be false-positive).");
            // Go through without throwing the Exception, as we may be able to detect the
            // version before it
        } catch (VCardException e) {
            Log.e(TAG, "Error parsing vcard", e);
            return null;
        }

        return new ImportRequest(null, mRecord.getPayload(), null,
                getString(R.string.nfc_vcard_file_name), detector.getEstimatedType(),
                detector.getEstimatedCharset(), vcardVersion, counter.getCount());
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        VCardService service = ((VCardService.MyBinder) binder).getService();
        new ImportTask().execute(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Do nothing
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Intent intent = getIntent();
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Log.w(TAG, "Unknowon intent " + intent);
            finish();
            return;
        }

        String type = intent.getType();
        if (type == null || (!RawContacts.CONTENT_VCARD_TYPE.equals(type))) {
            Log.w(TAG, "Not a vcard");
            //setStatus(getString(R.string.fail_reason_not_supported));
            finish();
            return;
        }
        NdefMessage msg = (NdefMessage) intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)[0];
        mRecord = msg.getRecords()[0];
        startImport();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SELECT_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                startImport();
            } else {
                finish();
            }
        }
    }

    private void startImport() {
        // We don't want the service finishes itself just after this connection.
        Intent intent = new Intent(this, VCardService.class);
        startService(intent);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onImportProcessed(ImportRequest request, int jobId, int sequence) {
        // do nothing
    }

    @Override
    public void onImportParsed(ImportRequest request, int jobId, VCardEntry entry, int currentCount,
            int totalCount) {
        // do nothing
    }

    @Override
    public void onImportFinished(ImportRequest request, int jobId, Uri uri) {
        if (isFinishing()) {
            Log.i(TAG, "Late import -- ignoring");
            return;
        }
        if (uri != null) {
            Uri contactUri = uri;
            Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onImportFailed(ImportRequest request) {
        if (isFinishing()) {
            Log.i(TAG, "Late import failure -- ignoring");
            return;
        }
        // TODO: report failure
    }

    @Override
    public void onImportCanceled(ImportRequest request, int jobId) {
        // do nothing
    }

    @Override
    public void onExportProcessed(ExportRequest request, int jobId) {
        // do nothing
    }

    @Override
    public void onExportFailed(ExportRequest request) {
        // do nothing
    }

    @Override
    public void onCancelRequest(CancelRequest request, int type) {
        // do nothing
    }

    @Override
    public void onComplete() {
        // do nothing
    }
}
