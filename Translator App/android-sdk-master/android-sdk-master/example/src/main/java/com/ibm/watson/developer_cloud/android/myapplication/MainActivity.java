
package com.ibm.watson.developer_cloud.android.myapplication;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.language_translator.v3.LanguageTranslator;
import com.ibm.watson.language_translator.v3.model.TranslateOptions;
import com.ibm.watson.language_translator.v3.model.TranslationResult;
import com.ibm.watson.language_translator.v3.util.Language;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.speech_to_text.v1.websocket.RecognizeCallback;
import com.ibm.watson.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.text_to_speech.v1.model.SynthesizeOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements android.widget.AdapterView.OnItemSelectedListener {
  DatabaseHelper mydb;
  private final String TAG = "MainActivity";

  private EditText input;
  private ImageButton mic;
  private Button translate;
  private Button addData;
  private ImageButton play;
  private TextView translatedText;

  private SpeechToText speechService;
  private TextToSpeech textService;
  private LanguageTranslator translationService;
  private String selectedTargetLanguage = Language.SPANISH;

  private StreamPlayer player = new StreamPlayer();

  private MicrophoneHelper microphoneHelper;

  private MicrophoneInputStream capture;
  private boolean listening = false;
  private Object AdapterView;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mydb = new DatabaseHelper(this);


    microphoneHelper = new MicrophoneHelper(this);

    speechService = initSpeechToTextService();
    textService = initTextToSpeechService();
    translationService = initLanguageTranslatorService();

    input = findViewById(R.id.input);
    mic = findViewById(R.id.mic);
    translate = findViewById(R.id.translate);
    play = findViewById(R.id.play);
    translatedText = findViewById(R.id.translated_text);
    addData = findViewById(R.id.btn_add);
    Spinner targetLanguage = (Spinner) findViewById(R.id.spinner);
    targetLanguage.setOnItemSelectedListener(this);

    List<String> languages = new ArrayList<String>();
    languages.addAll(Arrays.asList( "Arabic","Bengali","Bulgarian", "Chinese (Simplified)","Chinese (Traditional)", "Czech", "Danish", "Dutch", "English",  "Estonian", "Finnish", "French", "German", "Greek", "Gujarati", "Hebrew", "Hindi", "Hungarian", "Indonesian",
            "Italian", "Japanese", "Korean", "Latvian", "Lithuanian", "Malayalam", "Norwegian Bokmal",  "Polish", "Portuguese", "Romanian", "Russian",
            "Slovakian",  "Spanish", "Swedish", "Tamil", "Telugu", "Turkish", "Ukrainian", "Urdu", "Vietnamese"));


    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, languages);
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    targetLanguage.setAdapter(dataAdapter);


    input.addTextChangedListener(new EmptyTextWatcher() {
      @Override
      public void onEmpty(boolean empty) {
        if (empty) {
          translate.setEnabled(false);
        } else {
          translate.setEnabled(true);
        }
      }
    });

    mic.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!listening) {
          // Update the icon background
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mic.setBackgroundColor(Color.GREEN);
            }
          });
          capture = microphoneHelper.getInputStream(true);
          new Thread(new Runnable() {
            @Override
            public void run() {
              try {
                speechService.recognizeUsingWebSocket(getRecognizeOptions(capture),
                        new MicrophoneRecognizeDelegate());
              } catch (Exception e) {
                showError(e);
              }
            }
          }).start();

          listening = true;
        } else {
          // Update the icon background
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mic.setBackgroundColor(Color.LTGRAY);
            }
          });
          microphoneHelper.closeInputStream();
          listening = false;
        }
      }
    });

    translate.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        new TranslationTask().execute(input.getText().toString());
      }
    });

    translatedText.addTextChangedListener(new EmptyTextWatcher() {
      @Override
      public void onEmpty(boolean empty) {
        if (empty) {
          play.setEnabled(false);
        } else {
          play.setEnabled(true);
        }
      }
    });

    play.setEnabled(false);

    play.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new SynthesisTask().execute(translatedText.getText().toString());
      }
    });

    AddData();
  }

  public void AddData() {
    addData.setOnClickListener(
            new View.OnClickListener() {

              @Override
              public void onClick(View view) {
                boolean isInserted = mydb.insertData(input.getText().toString(), translatedText.getText().toString(), selectedTargetLanguage.toString());
                if (isInserted == true) {
                  Toast.makeText(MainActivity.this, "Data Inserted", Toast.LENGTH_LONG).show();
                } else
                  Toast.makeText(MainActivity.this, "Data is not  Inserted", Toast.LENGTH_LONG).show();

              }
            }
    );
  }


  private void showTranslation(final String translation) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        translatedText.setText(translation);
      }
    });
  }

  private void showError(final Exception e) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        e.printStackTrace();
        // Update the icon background
        mic.setBackgroundColor(Color.LTGRAY);
      }
    });
  }

  private void showMicText(final String text) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        input.setText(text);
      }
    });
  }

  private void enableMicButton() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mic.setEnabled(true);
      }
    });
  }

  private SpeechToText initSpeechToTextService() {
    Authenticator authenticator = new IamAuthenticator(getString(R.string.speech_text_apikey));
    SpeechToText service = new SpeechToText(authenticator);
    service.setServiceUrl(getString(R.string.speech_text_url));
    return service;
  }

  private TextToSpeech initTextToSpeechService() {
    Authenticator authenticator = new IamAuthenticator(getString(R.string.text_speech_apikey));
    TextToSpeech service = new TextToSpeech(authenticator);
    service.setServiceUrl(getString(R.string.text_speech_url));
    return service;
  }

  private LanguageTranslator initLanguageTranslatorService() {
    Authenticator authenticator
            = new IamAuthenticator(getString(R.string.language_translator_apikey));
    LanguageTranslator service = new LanguageTranslator("2018-05-01", authenticator);
    service.setServiceUrl(getString(R.string.language_translator_url));
    return service;
  }

  private RecognizeOptions getRecognizeOptions(InputStream captureStream) {
    return new RecognizeOptions.Builder()
            .audio(captureStream)
            .contentType(ContentType.OPUS.toString())
            .model("en-US_BroadbandModel")
            .interimResults(true)
            .inactivityTimeout(2000)
            .build();
  }

  @Override
  public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
    String item = parent.getItemAtPosition(position).toString();
    switch (item) {
      case "Arabic":
        selectedTargetLanguage = Language.ARABIC;
        return;
      case "Bengali":
        selectedTargetLanguage = Language.BENGALI;
        return;
      case "Bulgarian":
        selectedTargetLanguage = Language.BULGARIAN;
        return;
      case "Chinese (Simplified)":
        selectedTargetLanguage = Language.CHINESE;
        return;
      case "Chinese (Traditional)":
        selectedTargetLanguage = Language.TRADITIONAL_CHINESE;
        return;
      case "Czech":
        selectedTargetLanguage = Language.CZECH;
        return;
      case "Danish":
        selectedTargetLanguage = Language.DANISH;
        return;
      case "Dutch":
        selectedTargetLanguage = Language.DUTCH;
        return;
      case "English":
        selectedTargetLanguage = Language.ENGLISH;
        return;
      case "Estonian":
        selectedTargetLanguage = Language.ESTONIAN;
        return;
      case "Finnish":
        selectedTargetLanguage = Language.FINNISH;
        return;
      case "French":
        selectedTargetLanguage = Language.FRENCH;
        return;
      case "German":
        selectedTargetLanguage = Language.GERMAN;
        return;
      case "Greek":
        selectedTargetLanguage = Language.GREEK;
        return;
      case "Gujarati":
        selectedTargetLanguage = Language.GUJARATI;
        return;
      case "Hebrew":
        selectedTargetLanguage = Language.HEBREW;
        return;
      case "Hindi":
        selectedTargetLanguage = Language.HINDI;
        return;
      case "Hungarian":
        selectedTargetLanguage = Language.HUNGARIAN;
        return;
      case "Indonesian":
        selectedTargetLanguage = Language.INDONESIAN;
        return;
      case "Italian":
        selectedTargetLanguage = Language.ITALIAN;
        return;
      case "Japanese":
        selectedTargetLanguage = Language.JAPANESE;
        return;
      case "Korean":
        selectedTargetLanguage = Language.KOREAN;
        return;
      case "Latvian":
        selectedTargetLanguage = Language.LATVIAN;
        return;
      case "Lithuanian":
        selectedTargetLanguage = Language.LITHUANIAN;
        return;
      case "Malayalam":
        selectedTargetLanguage = Language.MALAYALAM;
        return;
      case "Norwegian Bokmal":
        selectedTargetLanguage = Language.NORWEGIAN_BOKMAL;
        return;

      case "Polish":
        selectedTargetLanguage = Language.POLISH;
        return;
      case "Portuguese":
        selectedTargetLanguage = Language.PORTUGUESE;
        return;

      case "Romanian":
        selectedTargetLanguage = Language.ROMANIAN;
        return;
      case "Russian":
        selectedTargetLanguage = Language.RUSSIAN;
        return;
      case "Slovakian":
        selectedTargetLanguage = Language.SLOVAKIAN;
        return;

      case "Spanish":
        selectedTargetLanguage = Language.SPANISH;
        return;
      case "Swedish":
        selectedTargetLanguage = Language.SWEDISH;
        return;
      case "Tamil":
        selectedTargetLanguage = Language.TAMIL;
        return;
      case "Telugu":
        selectedTargetLanguage = Language.TELUGU;
        return;
      case "Turkish":
        selectedTargetLanguage = Language.TURKISH;
        return;
      case "Urdu":
        selectedTargetLanguage = Language.URDU;
        return;
      case "Vietnamese":
        selectedTargetLanguage = Language.VIETNAMESE;
        return;
      default:
        return;


    }

  }

  @Override
  public void onNothingSelected(android.widget.AdapterView<?> parent) {

  }

  private abstract class EmptyTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    // assumes text is initially empty
    private boolean isEmpty = true;

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      if (s.length() == 0) {
        isEmpty = true;
        onEmpty(true);
      } else if (isEmpty) {
        isEmpty = false;
        onEmpty(false);
      }
    }

    @Override
    public void afterTextChanged(Editable s) {}

    public abstract void onEmpty(boolean empty);
  }

  private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback implements RecognizeCallback {
    @Override
    public void onTranscription(SpeechRecognitionResults speechResults) {
      System.out.println(speechResults);
      if (speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
        String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
        showMicText(text);
      }
    }

    @Override
    public void onError(Exception e) {
      try {
        capture.close();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      showError(e);
      enableMicButton();
    }

    @Override
    public void onDisconnected() {
      enableMicButton();
    }
  }

  private class TranslationTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
      TranslateOptions translateOptions = new TranslateOptions.Builder()
              .addText(params[0])
              .source(Language.ENGLISH)
              .target(selectedTargetLanguage)
              .build();
      TranslationResult result
              = translationService.translate(translateOptions).execute().getResult();
      String firstTranslation = result.getTranslations().get(0).getTranslation();
      showTranslation(firstTranslation);
      return "Did translate";
    }
  }

  private class SynthesisTask extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String... params) {
      SynthesizeOptions synthesizeOptions = new SynthesizeOptions.Builder()
              .text(params[0])
              .voice(SynthesizeOptions.Voice.EN_US_LISAV3VOICE)
              .accept(HttpMediaType.AUDIO_WAV)
              .build();
      player.playStream(textService.synthesize(synthesizeOptions).execute().getResult());
      return "Did synthesize";
    }
  }


  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String[] permissions,
                                         int[] grantResults) {
    switch (requestCode) {

      case MicrophoneHelper.REQUEST_PERMISSION: {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show();
        }
      }
    }
  }


}
