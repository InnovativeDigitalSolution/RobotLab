package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.AbstractSpeechSynthesisMeta;
import org.slf4j.Logger;

public class VoiceRssMeta extends AbstractSpeechSynthesisMeta {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(VoiceRssMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public VoiceRssMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("VoiceRss speech synthesis service.");
    addCategory("speech");
    setSponsor("moz4r");
    addCategory("speech", "cloud");
    addTodo("test speak blocking - also what is the return type and AudioFile audio track id ?");
    setCloudService(true);
    addDependency("com.voicerss", "tts", "1.0");

  }

}
