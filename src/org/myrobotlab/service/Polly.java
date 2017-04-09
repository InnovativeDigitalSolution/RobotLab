package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.codec.digest.DigestUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.AudioListener;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.Voice;

/**
 * Amazon's cloud speech service
 * 
 * Free Tier The Amazon Polly free tier includes 5 million characters per month,
 * for the first 12 months, starting from the first request for speech.
 * 
 * Polly Pricing Pay-as-you-go $4.00 per 1 million characters (when outside the
 * free tier).
 *
 * @author gperry
 *
 */
public class Polly extends Service implements SpeechSynthesis, AudioListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Polly.class);

  transient AWSCredentials credentials;
  transient AmazonPollyClient polly;
  transient Voice awsVoice;
  transient List<Voice> awsVoices;

  // this is a peer service.
  transient AudioFile audioFile = null;

  transient Map<String, Voice> voiceMap = new HashMap<String, Voice>();
  transient Map<String, Voice> langMap = new HashMap<String, Voice>();
  String voice;

  Stack<String> audioFiles = new Stack<String>();

  private String keyIdSecret;
  private String keyId;

  transient HashMap<AudioData, String> utterances = new HashMap<AudioData, String>();

  public Polly(String n) {
    super(n);
  }

  public void setKey(String keyId, String keyIdSecret) {
    this.keyId = keyId;
    this.keyIdSecret = keyIdSecret;
  }

  @Override
  public List<String> getVoices() {
    getPolly();
    return new ArrayList<String>(voiceMap.keySet());
  }

  @Override
  public boolean setVoice(String voice) {
    getPolly();
    if (voiceMap.containsKey(voice)) {
      log.info("setting voice to {}", voice);
      awsVoice = voiceMap.get(voice);
      this.voice = voice;
      return true;
    }
    log.info("could not set voice to {}", voice);
    return false;
  }

  @Override
  public void setLanguage(String l) {
    // TODO Auto-generated method stub

  }

  @Override
  public String getLanguage() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getLanguages() {
    // TODO Auto-generated method stub
    return null;
  }

  public void cacheAmazonFile(String toSpeak, OutputFormat format) throws IOException {

    // cache it begin -----
    String localFileName = getLocalFileName(this, toSpeak, "mp3");
    // String filename = AudioFile.globalFileCacheDir + File.separator +
    // localFileName;
    if (!audioFile.cacheContains(localFileName)) {
      log.info("retrieving speech from Amazon - {}", localFileName);
      AmazonPollyClient polly = getPolly();
      SynthesizeSpeechRequest synthReq = new SynthesizeSpeechRequest().withText(toSpeak).withVoiceId(awsVoice.getId()).withOutputFormat(format);
      SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);
      InputStream data = synthRes.getAudioStream();
      byte[] b = FileIO.toByteArray(data);
      audioFile.cache(localFileName, b, toSpeak);
    } else {
      log.info("using local cached file");
    }

    // invoke("publishStartSpeaking", toSpeak);
    // audioFile.playBlocking(filename);
    // invoke("publishEndSpeaking", toSpeak);
    // log.info("Finished waiting for completion.");

  }

  @Override
  public AudioData speak(String toSpeak) throws Exception {
    cacheAmazonFile(toSpeak, OutputFormat.Mp3);
    AudioData audioData = audioFile.playCachedFile(getLocalFileName(this, toSpeak, "mp3"));
    utterances.put(audioData, toSpeak);
    return audioData;
    /*
     * InputStream speechStream = synthesize(toSpeak, OutputFormat.Mp3); //
     * create an MP3 player AdvancedPlayer player = new
     * AdvancedPlayer(speechStream,
     * javazoom.jl.player.FactoryRegistry.systemRegistry().createAudioDevice());
     * 
     * player.setPlayBackListener(new PlaybackListener() {
     * 
     * @Override public void playbackStarted(PlaybackEvent evt) {
     * System.out.println("Playback started"); System.out.println(toSpeak); }
     * 
     * @Override public void playbackFinished(PlaybackEvent evt) {
     * System.out.println("Playback finished"); } });
     * 
     * // play it! player.play();
     */

  }

  private void processVoicesRequest() {
    // Create describe voices request.
    DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

    // Synchronously ask Polly Polly to describe available TTS voices.
    DescribeVoicesResult describeVoicesResult = polly.describeVoices(describeVoicesRequest);
    awsVoices = describeVoicesResult.getVoices();
    log.info("found {} voices", awsVoices.size());
    for (int i = 0; i < awsVoices.size(); ++i) {
      Voice voice = awsVoices.get(i);
      voiceMap.put(voice.getName(), voice);
      langMap.put(voice.getLanguageCode(), voice);
      log.info("{} {} - {}", i, voice.getName(), voice.getLanguageCode());
    }

    // set default voice
    if (voice == null) {
      voice = awsVoices.get(0).getName();
      awsVoice = awsVoices.get(0);
      log.info("setting default voice to {}", voice);
    }

  }

  private AmazonPollyClient getPolly() {
    if (polly == null) {
      try {
        // AWSCredentials creds = new
        // BasicAWSCredentials("AKIAJGL6AEN37LDO3N7A", "secret-access-key");
        if (credentials == null) {
          // try credential chain - in case they have set env vars
          credentials = new BasicAWSCredentials(keyId, keyIdSecret);
        }

        // polly = (AmazonPollyClient)
        // AmazonPollyClientBuilder.standard().withCredentials(new
        // AWSStaticCredentialsProvider(credentials)).withRegion(Regions.US_EAST_1).build();
        polly = (AmazonPollyClient) AmazonPollyClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.US_WEST_2).build();
        processVoicesRequest();
      } catch (Exception e) {
        try {
          log.error("could not get client with keys supplied - trying default chain", e);
          polly = new AmazonPollyClient(new DefaultAWSCredentialsProviderChain(), new ClientConfiguration());
          // polly.setRegion(Region.getRegion(Regions.US_EAST_1));
          polly.setRegion(Region.getRegion(Regions.US_WEST_2));
          processVoicesRequest();
        } catch (Exception e2) {
          error("could not get Polly client - did you setKeys ?");
          log.error("giving up", e2);
        }
      }
    }
    return polly;
  }

  @Override
  public void onAudioStart(AudioData data) {
    log.info("onAudioStart {} {}", getName(), data.toString());
    // filters on only our speech
    if (utterances.containsKey(data)) {
      String utterance = utterances.get(data);
      invoke("publishStartSpeaking", utterance);
    }
  }

  @Override
  public void onAudioEnd(AudioData data) {
    log.info("onAudioEnd {} {}", getName(), data.toString());
    // filters on only our speech
    if (utterances.containsKey(data)) {
      String utterance = utterances.get(data);
      invoke("publishEndSpeaking", utterance);
      utterances.remove(data);
    }
  }

  @Override
  public boolean speakBlocking(String toSpeak) throws Exception {
    cacheAmazonFile(toSpeak, OutputFormat.Mp3);
    audioFile.playBlocking(AudioFile.globalFileCacheDir + File.separator + getLocalFileName(this, toSpeak, "mp3"));
    return false;
  }

  @Override
  public void setVolume(float volume) {
    audioFile.setVolume(volume);
  }

  @Override
  public float getVolume() {
    return audioFile.getVolume();
  }

  @Override
  public void interrupt() {
    // TODO Auto-generated method stub

  }

  @Override
  public String getVoice() {
    getPolly();
    return voice;
  }

  @Override
  public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType) throws UnsupportedEncodingException {
    // TODO: make this a base class sort of thing.
    getPolly();
    return provider.getClass().getSimpleName() + File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8") + File.separator + DigestUtils.md5Hex(toSpeak) + "."
        + audioFileType;
  }

  // can this be defaulted ?
  @Override
  public void addEar(SpeechRecognizer ear) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onRequestConfirmation(String text) {
    // TODO Auto-generated method stub

  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Polly.class.getCanonicalName());
    meta.addDescription("used as a general template");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addDependency("com.amazonaws.services", "1.11.118");
    meta.addCategory("speech");
    return meta;
  }

  public void startService() {
    super.startService();
    audioFile = (AudioFile) startPeer("audioFile");
    audioFile.startService();
    subscribe(audioFile.getName(), "publishAudioStart");
    subscribe(audioFile.getName(), "publishAudioEnd");
    // attach a listener when the audio file ends playing.
    audioFile.addListener("finishedPlaying", this.getName(), "publishEndSpeaking");
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Polly polly = (Polly) Runtime.start("polly", "Polly");

      // add your amazon access key & secret 
      polly.setKey("{access-key-id}", "{secret-access-key}");

      polly.setVoice("Russel");
      polly.setVoice("Nicole");
      
      polly.setVoice("Brian");
      polly.setVoice("Amy");
      polly.setVoice("Emma");
      
      polly.setVoice("Brian");
      polly.setVoice("Kimberly");
      
      polly.setVoice("Justin");
      polly.setVoice("Joey");      
      polly.setVoice("Raveena");
      polly.setVoice("Ivy");
      polly.setVoice("Kendra");

      polly.speak("this is a new thing");

      polly.speak("Hello there, i am a cloud service .. i probably sound like the echo");
      polly.speak("Here is another sentence");
      polly.speak("To be or not to be that is the question");
      polly.speakBlocking("now i am blocking my speech");
      polly.speakBlocking("put one foot in front of the other and");
      // xxx
      polly.speakBlocking("soon you'll be walking out the door");
      polly.speak("this is a new sentence");
      polly.speak("to be or not to be that is the question");
      polly.speak("weather tis nobler in the mind to suffer");
      polly.speak("the slings and arrows of ourtrageous fortune");
      polly.speak("or to take arms against a see of troubles");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
