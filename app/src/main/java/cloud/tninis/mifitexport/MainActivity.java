package cloud.tninis.mifitexport;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.BoringLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {

    Button exportBtn;
    String folder;
    TextView infoTextView;
    EditText dateTo;
    EditText dateFrom;
    RadioGroup radioGroup;
    private static DecimalFormat df = new DecimalFormat(".#");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        folder=Environment.getExternalStorageDirectory()+"/Android/data/"+getResources().getString(R.string.AppFolder);
        exportBtn=(Button)findViewById(R.id.btnExport);
        radioGroup=(RadioGroup)findViewById(R.id.radioGroup) ;
        infoTextView=(TextView)findViewById(R.id.exportInfo);
        dateFrom=(EditText)findViewById(R.id.date_from);
        dateTo=(EditText)findViewById(R.id.date_to);
        EnableDisableDates(false);
        exportBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},1);
            }
        });



        dateFrom.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showSetDate(dateFrom);
            }
        });

        dateTo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showSetDate(dateTo);

            }
        });
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {

            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                switch(checkedId)
                {
                    case R.id.all_dates:
                        EnableDisableDates(false);

                        break;
                    case R.id.specif_dates:
                        EnableDisableDates(true);
                        break;
                }
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(CreateAppFolder()) {
                        ClearAppFolderData();
                        (new Startup()).setContext(this).execute();
                    }
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private class Startup extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = null;
        private Context context = null;
        private List<String> suResult = null;
        private Boolean exportResult=false;

        public Startup setContext(Context context) {
            this.context = context;
            return this;
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(context);
            dialog.setTitle(getResources().getString(R.string.DialogTitle));
            dialog.setMessage(getResources().getString(R.string.DialogMes));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Shell.SU.available()) {
                suResult = Shell.SU.run(new String[] {
                        "cd "+ getResources().getString(R.string.MiFitDatabaseFolder),
                        "find -name \"*origin*\" -not -name \"*journal\" -exec cp {} "+folder+" \\;"
                });
                exportResult=ExportToJson();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();

            if(exportResult)
                ((TextView)findViewById(R.id.exportInfo)).setText(getResources().getString(R.string.ExportSucces));
            else
                ((TextView)findViewById(R.id.exportInfo)).setText(getResources().getString(R.string.ExportFailed));
        }
    }



    public Boolean CreateAppFolder(){
        Boolean result;
        File appFolder = new File(folder);
        if(!appFolder.exists())
        {
            appFolder.mkdirs();
            result=true;
        }
        else
        {
            result=true;
        }
        return result;

    }

    public void ClearAppFolderData(){
        File appFolder = new File(folder);

        if(appFolder.exists())
        {
            for (File file :appFolder.listFiles())
            {
                file.delete();
            }

        }

    }

    public String GetDatabasePath(){
        String path=null;
        File appFolder = new File(folder);

        if(appFolder.exists())
        {
            for (File file :appFolder.listFiles())
            {
                path= file.getPath();
            }
        }
        return path;
    }

    public boolean ExportToJson(){

        if(GetDatabasePath()!=null) {
            try
            {
                SQLiteDatabase db = SQLiteDatabase.openDatabase(GetDatabasePath(), null, 0);
                Cursor cursor=null;
                String Name="";
                int Height=0;
                double Weight=0;
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ssZ");
                TimeZone tes=TimeZone.getDefault();
                sdf.setTimeZone(tes);
                String exportDate = sdf.format(new Date());

                cursor  = db.rawQuery("Select NAME,HEIGHT,WEIGHT from USER_INFOS ", null);
                cursor.moveToFirst();
                Name=cursor.getString(0);
                Height=cursor.getInt(1);
                Weight=cursor.getDouble(2);
                cursor.close();

                cursor  = db.rawQuery("Select SUMMARY from DATE_DATA ", null);
                JSONObject jsonData = new JSONObject();
                jsonData.put("Name", Name);
                jsonData.put("Height", Height);
                jsonData.put("Weight", df.format(Weight));
                jsonData.put("ExportDate", exportDate);
                JSONArray jsonArr = new JSONArray();
                if (cursor.moveToFirst()) {
                    do {
                        String test = cursor.getString(0);
                        JSONObject data = new JSONObject(test);
                        data.getJSONObject("stp").remove("stage");
                        data.getJSONObject("slp").remove("stage");
                        jsonArr.put(data);
                    } while (cursor.moveToNext());
                }
                cursor.close();
                jsonData.put("FullData",jsonArr);
                String teaa=jsonData.toString();
                WriteToFile(teaa);

            }
            catch(JSONException ex)
            {
                Log.e("MiFitExport", "JSONException", ex);
                return false;
            }
            catch(Exception ex)
            {
                Log.e("MiFitExport", "Exception", ex);
                return false;
            }

        }
        return true;
    }
    private void WriteToFile(String data) {
        FileOutputStream stream=null;
        try {
            File file = new File(folder, "export.json");
            stream = new FileOutputStream(file);
            stream.write(data.getBytes());
            stream.close();
        }
        catch (IOException ex) {
            Log.e("MiFitExport", "exception", ex);
        }
    }

    private void EnableDisableDates(Boolean mode)
    {
        if(mode)
        {
            dateTo.setEnabled(true);
            dateTo.setBackground(getDrawable(R.drawable.rect_btn));

            dateFrom.setEnabled(true);
            dateFrom.setBackground(getDrawable(R.drawable.rect_btn));
        }
        else
        {
            dateTo.setText("");
            dateTo.setEnabled(false);
            dateTo.setBackground(getDrawable(R.drawable.rect_btn_disable));

            dateFrom.setText("");
            dateFrom.setEnabled(false);
            dateFrom.setBackground(getDrawable(R.drawable.rect_btn_disable));
        }

    }

    private void showSetDate(final EditText dateText)
    {
        Calendar mcurrentDate=Calendar.getInstance();
        int year=mcurrentDate.get(Calendar.YEAR);
        int month=mcurrentDate.get(Calendar.MONTH);
        int day=mcurrentDate.get(Calendar.DAY_OF_MONTH);

        final DatePickerDialog mDatePicker =new DatePickerDialog(this,R.style.DialogTheme, new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker datepicker, int selectedyear, int selectedmonth, int selectedday)
            {
                dateText.setText(new StringBuilder().append(selectedday).append("/").append(selectedmonth+1).append("/").append(selectedyear));
            }
        },year, month, day);

        long maxMillis,minMillis ;
        String minDate="01/05/"+year+" 00:00:00";
        String maxDate="31/12/"+year+" 00:00:00";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        try {

            Date date= sdf.parse(minDate);
            minMillis = date.getTime();
            date=sdf.parse(maxDate);
            maxMillis= date.getTime();
            mDatePicker.getDatePicker().setMinDate(minMillis);
            mDatePicker.getDatePicker().setMaxDate(maxMillis);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        finally {
            mDatePicker.show();
        }
    }
}
