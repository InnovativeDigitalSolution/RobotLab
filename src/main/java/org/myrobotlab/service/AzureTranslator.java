/**
 * Azure Translator by Microsoft - Service
 * 
 * @author Giovanni Mirulla (Papaouitai), thanks GroG and kwatters
 * moz4r updated 10/5/17
 * 
 *         References : https://github.com/boatmeme/microsoft-translator-java-api 
 */

package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

import io.github.firemaples.detect.Detect;
import io.github.firemaples.language.Language;
import io.github.firemaples.translate.Translate;

public class AzureTranslator extends Service implements TextListener, TextPublisher {

  private static final long serialVersionUID = 1L;

  String toLanguage = "it";
  String fromLanguage = null;
  public final static Logger log = LoggerFactory.getLogger(AzureTranslator.class);

  public static void main(String[] args) throws Exception {
    LoggingFactory.init(Level.INFO);
    try {

      AzureTranslator translator = (AzureTranslator) Runtime.start("translator", "AzureTranslator");
      Runtime.start("gui", "SwingGui");
      log.info("Translator service instance: {}", translator);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public AzureTranslator(String n, String id) {
    super(n, id);
  }

  public String translate(String toTranslate) throws Exception {
    String translatedText = null;
    if (fromLanguage == null) {
      translatedText = Translate.execute(toTranslate, Language.AUTO_DETECT, Language.fromString(toLanguage));
    } else {
      translatedText = Translate.execute(toTranslate, Language.fromString(fromLanguage), Language.fromString(toLanguage));
    }
    return translatedText;
  }

  public Language detectLanguage(String toDetect) throws Exception {
    Language detectedLanguage = Detect.execute(toDetect);
    return detectedLanguage;
  }

  public void setCredentials(String clientSecret) {

    // Translate.setKey(clientID);
    Translate.setSubscriptionKey(clientSecret);
    // Detect.setKey(clientID);
    Detect.setSubscriptionKey(clientSecret);
  }

  public void fromLanguage(String from) {
    fromLanguage = from;
  }

  public void toLanguage(String to) {
    toLanguage = to;
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName(), "onText");
  }

  @Override
  public void attachTextListener(TextListener service) {
    attachTextListener(service.getName());
  }

  @Override
  public void onText(String text) {
    String cleanText;
    try {
      cleanText = translate(text);
      invoke("publishText", cleanText);
    } catch (Exception e) {
      log.error("Unable to translate text! {} {}", text, e);
    }
  }

  @Override
  public void attachTextPublisher(TextPublisher service) {
    if (service == null) {
      log.warn("{}.attachTextPublisher(null)");
      return;
    }
    subscribe(service.getName(), "publishText");
  }

  @Override
  public void attachTextListener(String name) {
    addListener("publishText", name);
  }

}
