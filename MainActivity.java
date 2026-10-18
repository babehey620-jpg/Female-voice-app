package com.example.femalevoicestudio;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private EditText textInput, backendInput, tokenInput, voiceIdInput, cloneNameInput;
    private Spinner languageSpinner, systemVoiceSpinner, modelSpinner;
    private SeekBar rateBar, pitchBar;
    private TextView status, sampleStatus;
    private CheckBox consentBox;
    private final List<Voice> systemVoices = new ArrayList<>();
    private Uri sampleUri;
    private MediaRecorder recorder;
    private File recordingFile, generatedFile;
    private MediaPlayer player;
    private static final int PICK_AUDIO=101, CREATE_MP3=102, RECORD_PERMISSION=103;
    private byte[] pendingMp3;

    private final String[] langs={"English (United States)","English (United Kingdom)","Bangla","Hindi","Urdu","Arabic","French","Spanish"};
    private final String[] tags={"en-US","en-GB","bn-BD","hi-IN","ur-PK","ar-SA","fr-FR","es-ES"};
    private final String[] models={"eleven_multilingual_v2","eleven_flash_v2_5","eleven_v3"};

    @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();tts=new TextToSpeech(this,this);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xFFF3EEFF);t.setPadding(0,dp(7),0,dp(7));return t;}
    private Button btn(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setTextColor(0xFFFFFFFF);b.setBackgroundTintList(ColorStateList.valueOf(0xFF7655F6));b.setOnClickListener(v->r.run());return b;}
    private Spinner spinner(String[] values){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));return s;}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(18),dp(18),dp(18),dp(26));page.setBackgroundColor(0xFF171126);scroll.addView(page);
        TextView title=label("♫ Female Voice Studio AI",27);title.setTypeface(null,1);page.addView(title);
        page.addView(label("Free device voices + premium AI voices + authorized voice cloning",14));
        page.addView(label("FREE MODE",18));
        textInput=new EditText(this);textInput.setTextColor(0xFFF8F5FF);textInput.setHintTextColor(0xFFB7AACF);textInput.setBackgroundTintList(ColorStateList.valueOf(0xFF9C83FF));textInput.setHint("Type text to speak or generate…");textInput.setMinLines(4);textInput.setGravity(android.view.Gravity.TOP);textInput.setText("Hello! Welcome to Female Voice Studio.");page.addView(textInput,new LinearLayout.LayoutParams(-1,dp(120)));
        page.addView(label("Language / accent",15));languageSpinner=spinner(langs);page.addView(languageSpinner);
        page.addView(label("Device voice",15));systemVoiceSpinner=spinner(new String[]{"Loading voices"});page.addView(systemVoiceSpinner);
        page.addView(btn("▶ Speak with free device voice",this::speakFree));
        page.addView(btn("■ Stop speech",()->{if(tts!=null)tts.stop();status("Stopped.");}));

        page.addView(label("PREMIUM AI MODE",18));
        page.addView(label("Backend HTTPS URL (deploy the included backend first)",14));
        backendInput=new EditText(this);backendInput.setTextColor(0xFFF8F5FF);backendInput.setHintTextColor(0xFFB7AACF);backendInput.setBackgroundTintList(ColorStateList.valueOf(0xFF9C83FF));backendInput.setSingleLine(true);backendInput.setHint("https://your-backend.example.com");page.addView(backendInput);
        page.addView(label("App access token (your backend secret)",14));
        tokenInput=new EditText(this);tokenInput.setTextColor(0xFFF8F5FF);tokenInput.setHintTextColor(0xFFB7AACF);tokenInput.setBackgroundTintList(ColorStateList.valueOf(0xFF9C83FF));tokenInput.setSingleLine(true);tokenInput.setHint("APP_ACCESS_TOKEN");page.addView(tokenInput);
        page.addView(btn("Load provider voice library",this::loadProviderVoices));
        page.addView(label("Voice ID",14));voiceIdInput=new EditText(this);voiceIdInput.setTextColor(0xFFF8F5FF);voiceIdInput.setHintTextColor(0xFFB7AACF);voiceIdInput.setBackgroundTintList(ColorStateList.valueOf(0xFF9C83FF));voiceIdInput.setSingleLine(true);voiceIdInput.setHint("Paste a licensed voice ID from the library");page.addView(voiceIdInput);
        page.addView(label("AI model",14));modelSpinner=spinner(models);page.addView(modelSpinner);
        page.addView(btn("✨ Generate premium speech + save MP3",this::generatePremium));
        page.addView(btn("▶ Play last generated MP3",this::playGenerated));

        page.addView(label("VOICE CLONING — CONSENT REQUIRED",18));
        page.addView(label("Only clone your own voice or a voice you have explicit permission to use. Selected samples are uploaded to the configured voice provider through your backend.",14));
        cloneNameInput=new EditText(this);cloneNameInput.setTextColor(0xFFF8F5FF);cloneNameInput.setHintTextColor(0xFFB7AACF);cloneNameInput.setBackgroundTintList(ColorStateList.valueOf(0xFF9C83FF));cloneNameInput.setSingleLine(true);cloneNameInput.setHint("Name for your authorized voice");page.addView(cloneNameInput);
        page.addView(btn("Choose voice sample audio",()->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("audio/*");startActivityForResult(Intent.createChooser(i,"Choose authorized audio sample"),PICK_AUDIO);}));
        page.addView(btn("Record my voice",this::startRecording));
        page.addView(btn("Stop recording",this::stopRecording));
        sampleStatus=label("No sample selected.",13);page.addView(sampleStatus);
        consentBox=new CheckBox(this);consentBox.setTextColor(0xFFF3EEFF);consentBox.setButtonTintList(ColorStateList.valueOf(0xFF9C83FF));consentBox.setText("I own this voice or have explicit permission to clone it.");page.addView(consentBox);
        page.addView(btn("Create authorized voice clone",this::cloneVoice));
        page.addView(label("Voice library note: choose licensed voices or original synthetic voices. Celebrity impersonation is not provided. Provider access, costs, and cloning permissions depend on your account.",13));
        status=label("Free mode ready. Premium mode requires backend setup.",13);page.addView(status);
        setContentView(scroll);
    }

    @Override public void onInit(int result){if(result==TextToSpeech.SUCCESS){tts.setLanguage(Locale.US);refreshSystemVoices();status("Ready. Free voices depend on the Android TTS engine.");}else status("Could not initialize Android TTS.");}
    private void refreshSystemVoices(){
        if(tts==null||systemVoiceSpinner==null)return;int i=languageSpinner.getSelectedItemPosition();if(i<0)i=0;Locale wanted=Locale.forLanguageTag(tags[i]);systemVoices.clear();
        if(tts.getVoices()!=null)for(Voice v:tts.getVoices())if(v.getLocale()!=null&&v.getLocale().getLanguage().equals(wanted.getLanguage())&&(wanted.getCountry().isEmpty()||v.getLocale().getCountry().equalsIgnoreCase(wanted.getCountry()))&&!v.getFeatures().contains("notInstalled"))systemVoices.add(v);
        Collections.sort(systemVoices,Comparator.comparing(Voice::getName));List<String> names=new ArrayList<>();for(Voice v:systemVoices)names.add(v.getName()+(v.isNetworkConnectionRequired()?" (online)":" (device)"));if(names.isEmpty())names.add("No matching voice installed");systemVoiceSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
    }
    private void speakFree(){
        if(tts==null||textInput.getText().toString().trim().isEmpty()){status("Type text first.");return;}int i=languageSpinner.getSelectedItemPosition();if(i<0)i=0;
        int v=systemVoiceSpinner.getSelectedItemPosition();if(v>=0&&v<systemVoices.size())tts.setVoice(systemVoices.get(v));tts.setLanguage(Locale.forLanguageTag(tags[i]));
        if(Build.VERSION.SDK_INT>=21)tts.speak(textInput.getText().toString(),TextToSpeech.QUEUE_FLUSH,null,"fvs-free");else tts.speak(textInput.getText().toString(),TextToSpeech.QUEUE_FLUSH,null);status("Speaking with device voice.");
    }
    private String base(){return backendInput.getText().toString().trim().replaceAll("/+$","");}
    private String token(){return tokenInput.getText().toString().trim();}
    private boolean configOk(){if(base().isEmpty()||token().isEmpty()){status("Enter your deployed backend HTTPS URL and app access token.");return false;}return true;}
    private void loadProviderVoices(){
        if(!configOk())return;
        new Thread(()->{HttpURLConnection c=null;try{
            c=(HttpURLConnection)new URL(base()+"/api/voices").openConnection();c.setRequestMethod("GET");c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("x-app-token",token());
            String body=read(c);if(c.getResponseCode()<200||c.getResponseCode()>=300)throw new Exception(body);
            JSONObject json=new JSONObject(body);JSONArray a=json.getJSONArray("voices");StringBuilder list=new StringBuilder();
            for(int i=0;i<a.length();i++){JSONObject v=a.getJSONObject(i);String name=v.optString("name");String id=v.optString("voice_id");JSONObject labels=v.optJSONObject("labels");String gender=labels==null?"":labels.optString("gender","");String accent=labels==null?"":labels.optString("accent","");list.append(name).append(" | ").append(gender).append(" | ").append(accent).append("\nID: ").append(id).append("\n\n");}
            final String result=list.length()==0?"No voices returned.":list.toString();
            runOnUiThread(()->new android.app.AlertDialog.Builder(this).setTitle("Provider voice library").setMessage(result).setPositiveButton("OK",null).show());
        }catch(Exception e){showError("Could not load voices",e);}finally{if(c!=null)c.disconnect();}}).start();
    }
    private void generatePremium(){
        if(!configOk())return;String text=textInput.getText().toString().trim();String id=voiceIdInput.getText().toString().trim();
        if(text.isEmpty()||id.isEmpty()){status("Enter text and a provider voice ID.");return;}
        status("Generating premium audio…");
        new Thread(()->{HttpURLConnection c=null;try{
            c=(HttpURLConnection)new URL(base()+"/api/tts").openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(20000);c.setReadTimeout(120000);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("x-app-token",token());
            JSONObject j=new JSONObject();j.put("text",text);j.put("voiceId",id);j.put("modelId",models[Math.max(0,modelSpinner.getSelectedItemPosition())]);
            try(OutputStream out=c.getOutputStream()){out.write(j.toString().getBytes(StandardCharsets.UTF_8));}
            int code=c.getResponseCode();if(code<200||code>=300)throw new Exception(read(c));
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);pendingMp3=out.toByteArray();}
            runOnUiThread(()->{status("Generated MP3 ("+(pendingMp3.length/1024)+" KB). Choose where to save it.");Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("audio/mpeg");i.putExtra(Intent.EXTRA_TITLE,"female-voice-studio.mp3");startActivityForResult(i,CREATE_MP3);});
        }catch(Exception e){showError("Premium generation failed",e);}finally{if(c!=null)c.disconnect();}}).start();
    }
    private void playGenerated(){
        if(generatedFile==null||!generatedFile.exists()){status("Generate and save an MP3 first.");return;}
        try{if(player!=null){player.release();player=null;}player=new MediaPlayer();player.setDataSource(generatedFile.getAbsolutePath());player.setOnCompletionListener(mp->{mp.release();player=null;});player.prepare();player.start();status("Playing generated MP3.");}catch(Exception e){status("Could not play MP3.");}
    }
    private void cloneVoice(){
        if(!configOk())return;if(!consentBox.isChecked()){status("Confirm you have permission to clone this voice.");return;}
        if(cloneNameInput.getText().toString().trim().isEmpty()){status("Enter a name for the voice.");return;}
        if(sampleUri==null&&recordingFile==null){status("Choose or record a voice sample first.");return;}
        status("Uploading authorized sample for cloning…");
        new Thread(()->{HttpURLConnection c=null;String boundary="----FVS"+UUID.randomUUID().toString().replace("-","");
        try{
            c=(HttpURLConnection)new URL(base()+"/api/clone").openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(20000);c.setReadTimeout(120000);c.setRequestProperty("x-app-token",token());c.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
            try(OutputStream out=c.getOutputStream()){
                part(out,boundary,"name",cloneNameInput.getText().toString().trim(),null,null);
                part(out,boundary,"consent","true",null,null);
                String filename="voice-sample.m4a";String mime="audio/mp4";byte[] audio;
                if(sampleUri!=null){try(InputStream in=getContentResolver().openInputStream(sampleUri)){audio=bytes(in);}String mt=getContentResolver().getType(sampleUri);if(mt!=null)mime=mt;}
                else {try(InputStream in=new FileInputStream(recordingFile)){audio=bytes(in);}}
                part(out,boundary,"sample",filename,mime,audio);
                out.write(("--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            String body=read(c);if(c.getResponseCode()<200||c.getResponseCode()>=300)throw new Exception(body);
            JSONObject result=new JSONObject(body);String id=result.optString("voice_id","");runOnUiThread(()->{if(!id.isEmpty())voiceIdInput.setText(id);status("Voice clone created. Voice ID: "+id+". Provider verification may be required.");});
        }catch(Exception e){showError("Voice cloning failed",e);}finally{if(c!=null)c.disconnect();}}).start();
    }
    private void part(OutputStream out,String b,String name,String value,String mime,byte[] bytes)throws IOException{
        out.write(("--"+b+"\r\n").getBytes(StandardCharsets.UTF_8));
        if(bytes==null){out.write(("Content-Disposition: form-data; name=\""+name+"\"\r\n\r\n"+value+"\r\n").getBytes(StandardCharsets.UTF_8));}
        else{out.write(("Content-Disposition: form-data; name=\""+name+"\"; filename=\""+value+"\"\r\nContent-Type: "+mime+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(bytes);out.write("\r\n".getBytes(StandardCharsets.UTF_8));}
    }
    private byte[] bytes(InputStream in)throws IOException{try(ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private String read(HttpURLConnection c)throws IOException{InputStream in=(c.getResponseCode()>=200&&c.getResponseCode()<300)?c.getInputStream():c.getErrorStream();return new String(bytes(in),StandardCharsets.UTF_8);}
    private void showError(String title,Exception e){String msg=e.getMessage()==null?"Unknown error":e.getMessage();runOnUiThread(()->status(title+": "+msg));}
    private void status(String s){if(status!=null)status.setText(s);}
    private void startRecording(){
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},RECORD_PERMISSION);return;}
        try{if(recorder!=null){status("Already recording.");return;}recordingFile=new File(getCacheDir(),"voice-sample.m4a");recorder=Build.VERSION.SDK_INT>=31?new MediaRecorder(this):new MediaRecorder();recorder.setAudioSource(MediaRecorder.AudioSource.MIC);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setOutputFile(recordingFile.getAbsolutePath());recorder.prepare();recorder.start();sampleUri=null;sampleStatus.setText("Recording your voice…");status("Recording… tap Stop recording when finished.");}
        catch(Exception e){status("Could not start recording.");releaseRecorder();}
    }
    private void stopRecording(){if(recorder==null){status("No recording active.");return;}try{recorder.stop();sampleStatus.setText("Recorded sample ready for authorized cloning.");}catch(Exception e){status("Recording was too short or failed.");}releaseRecorder();}
    private void releaseRecorder(){if(recorder!=null){try{recorder.reset();recorder.release();}catch(Exception ignored){}recorder=null;}}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);
        if(req==PICK_AUDIO&&result==RESULT_OK&&data!=null){sampleUri=data.getData();recordingFile=null;sampleStatus.setText("Selected sample: "+sampleUri.getLastPathSegment());}
        if(req==CREATE_MP3&&result==RESULT_OK&&data!=null&&pendingMp3!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(pendingMp3);generatedFile=new File(getCacheDir(),"last-generated.mp3");try(FileOutputStream f=new FileOutputStream(generatedFile)){f.write(pendingMp3);}status("MP3 saved successfully.");}catch(Exception e){status("Could not save MP3.");}}
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==RECORD_PERMISSION&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startRecording();else status("Microphone permission is required to record your voice.");}
    @Override protected void onDestroy(){if(recorder!=null)stopRecording();if(tts!=null){tts.stop();tts.shutdown();}if(player!=null)player.release();super.onDestroy();}
}
