package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SegmentDisplayMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(SegmentDisplayMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public SegmentDisplayMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("used as a general template");
    setAvailable(false); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // addDependency("org.coolproject", "1.0.0");
    addCategory("general");

  }

}
