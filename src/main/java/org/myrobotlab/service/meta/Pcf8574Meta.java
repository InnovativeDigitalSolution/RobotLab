package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Pcf8574Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Pcf8574Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * @param name n
   * 
   */
  public Pcf8574Meta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("Pcf8574 i2c 8 pin I/O extender");
    addCategory("shield", "sensors");
    setSponsor("Mats");

  }

}
