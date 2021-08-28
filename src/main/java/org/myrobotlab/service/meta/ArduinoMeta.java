package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ArduinoMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ArduinoMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @param name
   *          n
   */
  public ArduinoMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("controls an Arduino microcontroller as a slave, which allows control of all the devices the Arduino is attached to, such as servos, motors and sensors");
    addCategory("microcontroller");
    addPeer("serial", "Serial", "serial device for this Arduino");

  }

}
