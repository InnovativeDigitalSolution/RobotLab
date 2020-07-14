package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class LidarVlp16Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(LidarVlp16Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public LidarVlp16Meta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("used as a general template");
    setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // addDependency("org.coolproject", "1.0.0");
    setAvailable(false);
    addCategory("sensors", "lidar");

  }

}