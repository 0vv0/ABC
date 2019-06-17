package com.example.addressbookcleaner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private Timer timer;
    private Helper helper = new Helper();

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerDown();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manualDeleteAllContactsUp();
        timerUp();
        addContactUp();
    }

    public void manualDeleteAllContactsUp() {
        Button btnDelete = findViewById(R.id.delete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helper.deleteContacts();
            }
        });
    }

    public void timerDown() {
        if (timer != null) {
            timer.cancel();
        }
    }

    public void timerUp() {
        Button startTimer = findViewById(R.id.startTimer);
        startTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timerDown();
                view.setSelected(!view.isSelected());
                if (view.isSelected()) {
                    view.setBackgroundColor(Color.DKGRAY);
                    timer = new Timer("AddressBookCleaner", false);
                    timer.schedule(getTaskWithCurrentData(), 0, 3000);
                } else {
                    view.setBackgroundColor(Color.LTGRAY);
                }

            }
        });
    }

    public void addContactUp() {
        Button addContact = findViewById(R.id.add);
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> phones= new ArrayList<>();
                phones.add("123");
                phones.add("124");
                phones.add("1111111111");
                helper.addPhoneNumbers(phones);
            }
        });
    }

    public MyTimerTask getTaskWithCurrentData() {
        String in = ((TextView) findViewById(R.id.in)).getText().toString();
        String viber = ((TextView) findViewById(R.id.viber)).getText().toString();
//        String out = ((TextView) findViewById(R.id.out)).getText().toString() + File.separator + Calendar.getInstance().getTime().getTime();
        String out = ((TextView) findViewById(R.id.out)).getText().toString() + File.separator + "viber_db";
        return new MyTimerTask(in, viber, out);
    }

    private long getCurrentPeriod() {
        return Long.parseLong(((EditText) findViewById(R.id.period)).getText().toString());
    }

    public void showError(Throwable throwable) {
        ((TextView) findViewById(R.id.errors)).setText(throwable.getMessage());
    }

    public void showError(String eMessage) {
        ((TextView) findViewById(R.id.errors)).setText(eMessage);
    }

    private class Helper {
        void deleteContacts() {
            ContentResolver cr = getContentResolver();
            try (Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)) {
                if (cur != null) {
                    while (cur.moveToNext()) {
                        try {
                            String lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                            runOnUiThread(new ShowError("The uri is " + uri.toString()));
                            cr.delete(uri, null, null);
                        } catch (Exception e) {
                            runOnUiThread(new ShowError(e));
                        }
                    }
                }
            } catch (Exception e) {
                runOnUiThread(new ShowError(e));
            }
        }

        List<String> readPhoneNumbers(File file) {
            List<String> numbers = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String buffer;
                while ((buffer = reader.readLine()) != null) {
                    numbers.add(buffer);
                }
            } catch (IOException e) {
                runOnUiThread(new ShowError(e));
            }
            return numbers;
        }

        void addPhoneNumbers(List<String> phones) {
            for (int i = 0; i < phones.size(); i++) {
                Long id = insertEmptyContact();
                if(id!=null){
                    insertContactDisplayName(ContactsContract.Data.CONTENT_URI, id, phones.get(i));
                    insertContactPhoneNumber(ContactsContract.Data.CONTENT_URI, id, phones.get(i));
                }
            }

        }

        Long insertEmptyContact(){
            // Inser an empty contact.
            ContentValues contentValues = new ContentValues();
            Uri rawContactUri = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, contentValues);
            // Get the newly created contact raw id.
            return rawContactUri!=null?ContentUris.parseId(rawContactUri):null;
        }

        void insertContactDisplayName(Uri addContactsUri, long rawContactId, String displayName)
        {
            ContentValues contentValues = new ContentValues();

            contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);

            // Each contact must has an mime type to avoid java.lang.IllegalArgumentException: mimetype is required error.
            contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);

            // Put contact display name value.
            contentValues.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, displayName);

            getContentResolver().insert(addContactsUri, contentValues);

        }

        private void insertContactPhoneNumber(Uri addContactsUri, long rawContactId, String phoneNumber)
        {
            // Create a ContentValues object.
            ContentValues contentValues = new ContentValues();

            // Each contact must has an id to avoid java.lang.IllegalArgumentException: raw_contact_id is required error.
            contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);

            // Each contact must has an mime type to avoid java.lang.IllegalArgumentException: mimetype is required error.
            contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

            // Put phone number value.
            contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);

            // Put phone type value.
            contentValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);

            // Insert new contact data into phone contact list.
            getContentResolver().insert(addContactsUri, contentValues);

        }

        void copyViberFile(File from, File to) {
            try {
                copyFiles(from, to);
            } catch (IOException e) {
                runOnUiThread(new ShowError(e));
            }
        }

        void copyFiles(final File from, final File to) throws IOException {
            try (BufferedReader fr = new BufferedReader(new FileReader(from));
                 BufferedWriter fw = new BufferedWriter(new FileWriter(to))) {

                String buffer;
                while ((buffer = fr.readLine()) != null) {
                    fw.write(buffer);
                }
                fw.flush();
            }
        }
    }

    class MyTimerTask extends TimerTask {
        private final File inFile;
        private final File viberFile;
        private final File outFile;

        MyTimerTask(String in, String viber, String out) {
            inFile = new File(in);
            viberFile = new File(viber);
            outFile = new File(out);
        }

        @Override
        public void run() {
            helper.copyViberFile(viberFile, outFile);
            helper.deleteContacts();
            helper.addPhoneNumbers(helper.readPhoneNumbers(inFile));
        }


    }

    class ShowError implements Runnable {
        private final String eMessage;

        ShowError(Throwable e) {
            eMessage = e.getMessage();
        }

        ShowError(String eMessage) {
            this.eMessage = eMessage;
        }

        @Override
        public void run() {
            showError(eMessage);
        }
    }
}
