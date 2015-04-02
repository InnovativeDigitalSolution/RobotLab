/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

// http://stackoverflow.com/questions/11515072/how-to-identify-optimal-parameters-for-cvcanny-for-polygon-approximation
package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_core.cvCircle;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvPutText;

import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.Point2Df;
import org.slf4j.Logger;
import org.bytedeco.javacpp.opencv_core.CvFont;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_features2d.KeyPoint;
import org.bytedeco.javacpp.opencv_features2d.KeyPointVectorVector;
import org.bytedeco.javacpp.opencv_features2d.SimpleBlobDetector;

public class OpenCVFilterSimpleBlobDetector extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSimpleBlobDetector.class.getCanonicalName());

	public ArrayList<Point2Df> pointsToPublish = new ArrayList<Point2Df>();
	transient CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN);
	
	public OpenCVFilterSimpleBlobDetector()  {
		super();
	}
	
	public OpenCVFilterSimpleBlobDetector(String name)  {
		super(name);
	}
	

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		if (image == null) {
			log.error("image is null");
		}
		
		// TODO: track an array of blobs , not just one.
		SimpleBlobDetector o = new SimpleBlobDetector();
		KeyPoint point = new KeyPoint();

		// TODO: i'd like to detect all the points at once..  
		// can i pass an array or something like that?  hmm.
		// TODO: this is null?! we blow up! (after javacv upgrade)
		o.detect(new Mat(image), point);
		
		//System.out.println(point.toString());
		float x = point.pt().x();
		float y = point.pt().y();
		if (x == 0 && y == 0) {
			// ignore the zero / zero point
			return image;
		}
		// pointsToPublish.clear();
		// min distance to an existing point ?
		// up to 25 pixels away?
		double minDist = 20.0;
		// Is this a new blob? or an old blob?
		boolean dupPoint = false;
		for (Point2Df p : pointsToPublish) {
			double dist = Math.sqrt((p.x-x)*(p.x-x) + (p.y-y)*(p.y-y));
			if (dist < minDist) {
				// we already have this point ?
				dupPoint = true;
				break;
			}
		}

		if (!dupPoint) {
			pointsToPublish.add(new Point2Df(x, y));
			System.out.println("There are " + pointsToPublish.size() + " blobs.");
		}
		return image;
	}

	@Override
	public IplImage display(IplImage frame, OpenCVData data) {
		float x, y;
		int xPixel, yPixel;
		for (int i = 0; i < pointsToPublish.size(); ++i) {
			Point2Df point = pointsToPublish.get(i);
			x = point.x;
			y = point.y;
			// graphics.setColor(Color.red);
			//if (useFloatValues) {
			//	xPixel = (int) (x * width);
			//	yPixel = (int) (y * height);
			//} else {
				xPixel = (int) x;
				yPixel = (int) y;
			//}
			cvCircle(frame, cvPoint(xPixel, yPixel), 5, CvScalar.GREEN, -1, 8, 0);
		}
		//cvPutText(frame, String.format("valid %d", pointsToPublish.size()), cvPoint(10,10), font, CvScalar.GREEN);
		//cvPutText(frame, String.format("valid %d", pointsToPublish.size()), cvPoint(10,10), font, CvScalar.GREEN);
		log.info("cvPutText is no worky yet, until JavaCV upgrade is done..");
		return frame;
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub
		
	}
	
	public void clearPoints() {
		pointsToPublish.clear();
	}

}
