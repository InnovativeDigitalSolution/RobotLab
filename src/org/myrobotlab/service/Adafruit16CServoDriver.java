/*
 * 
 *   Adafruit16CServoDriver
 *   
 *   TODO - test with Steppers & Motors - switches on board - interface accepts motor control
 *
 */

package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.PID2.PIDData;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * AdaFruit 16-Channel PWM / Servo Driver
 * 
 * @author Mats
 * 
 *         References : http://www.ladyada.net/make/mshield/use.html
 *         https://learn.adafruit.com/16-channel-pwm-servo-driver
 */

public class Adafruit16CServoDriver extends Service implements I2CControl, ServoController {
	/** version of the library */
	static public final String VERSION = "0.9";

	private static final long serialVersionUID = 1L;

	// Depending on your servo make, the pulse width min and max may vary, you
	// want these to be as small/large as possible without hitting the hard stop
	// for max range. You'll have to tweak them as necessary to match the servos
	// you have!
	//
	public final static int SERVOMIN = 150; // this is the 'minimum' pulse
	// length count (out of 4096)
	public final static int SERVOMAX = 600; // this is the 'maximum' pulse
	// length count (out of 4096)

	transient public I2CController controller;

	// Constant for default PWM freqency
	private static int pwmFreq = 60;

	// List of possible addresses. Used by the GUI.
	public List<String> deviceAddressList = Arrays.asList("0x40", "0x41", "0x42", "0x43", "0x44", "0x45", "0x46", "0x47", "0x48", "0x49", "0x4A", "0x4B", "0x4C", "0x4D", "0x4E",
			"0x4F", "0x50", "0x51", "0x52", "0x53", "0x54", "0x55", "0x56", "0x57", "0x58", "0x59", "0x5A", "0x5B", "0x5C", "0x5D", "0x5E", "0x5F");
	// Default address
	public String deviceAddress = "0x40";
	/**
	 * This address is to address all Adafruit16CServoDrivers on the i2c bus Don't
	 * use this address for any other device on the i2c bus since it will cause
	 * collisions.
	 */
	public String broadcastAddress = "0x70";

	public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
	public String deviceBus = "1";

	public transient final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriver.class.getCanonicalName());

	public static final int PCA9685_MODE1 = 0x00; // Mode 1 register
	public static final byte PCA9685_SLEEP = 0x10; // Set sleep mode, before
	// changing prescale value
	public static final byte PCA9685_AUTOINCREMENT = 0x20; // Set autoincrement to
																													// be able to write
																													// more than one byte
																													// in sequence

	public static final byte PCA9685_PRESCALE = (byte) 0xFE; // PreScale
	// register

	// Pin PWM addresses 4 bytes repeats for each pin so I only define pin 0
	// The rest of the addresses are calculated based on pin numbers
	public static final int PCA9685_LED0_ON_L = 0x06; // First LED address Low
	public static final int PCA9685_LED0_ON_H = 0x07; // First LED address High
	public static final int PCA9685_LED0_OFF_L = 0x08; // First LED address Low
	public static final int PCA9685_LED0_OFF_H = 0x08; // First LED address High

	// public static final int PWM_FREQ = 60; // default frequency for servos
	public static final int osc_clock = 25000000; // clock frequency of the
	// internal clock
	public static final int precision = 4096; // pwm_precision

	// i2c controller
	public ArrayList<String> controllers;
	public String controllerName;
	public boolean isControllerSet = false;

	/**
	 * @Mats - added by GroG - was wondering if this would help, probably you need
	 *       a reverse index too ?
	 * @GroG - I only need servoNameToPin yet. To be able to sweep some more
	 *         values may be needed
	 */
  class ServoData {
  	int pin;
  	boolean pwmFreqSet = false;
  	int pwmFreq;
  	float sweepMin = 0;
  	float sweepMax = 180;
  	float sweepDelay = 1;
  	int sweepStep = 1;
  	boolean isSweeping = false;
  	boolean sweepOneWay = false;
  }
  
	HashMap<String, ServoData> servoMap = new HashMap<String, ServoData>();

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Adafruit16CServoDriver driver = (Adafruit16CServoDriver) Runtime.start("pwm", "Adafruit16CServoDriver");
		log.info("Driver {}", driver);

	}

	public Adafruit16CServoDriver(String n) {
		super(n);
		subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
	}

	
	public void onRegistered(ServiceInterface s) {
		refreshControllers();
		broadcastState();
	}

	/*
	 * Refresh the list of running services that can be selected in the GUI
	 */
	public ArrayList<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(I2CController.class);
		return controllers;
	}

	// ----------- AFMotor API End --------------
	// TODO
	// Implement MotorController
	//
	/**
	 * This set of methods is used to set i2c parameters
	 * 
	 * @param controllerName
	 *          = The name of the i2c controller
	 * @param deviceBus
	 *          = i2c bus Should be "1" for Arduino and RasPi 
	 *          										"0"-"7" for I2CMux
	 * @param deviceAddress
	 *          = The i2c address of the PCA9685 ( "0x40" - "0x5F")
	 * @return
	 */
	// @Override
	public boolean setController(String controllerName, String deviceBus, String deviceAddress) {
		return setController((I2CController) Runtime.getService(controllerName), deviceBus, deviceAddress);
	}

	public boolean setController(String controllerName) {
		return setController((I2CController) Runtime.getService(controllerName), this.deviceBus, this.deviceAddress);
	}

	@Override
	public boolean setController(I2CController controller) {
		return setController(controller, this.deviceBus, this.deviceAddress);
	}

	@Override
	public void setController(DeviceController controller) {
		setController(controller);
	}

	public boolean setController(I2CController controller, String deviceBus, String deviceAddress) {
		if (controller == null) {
			error("setting null as controller");
			return false;
		}

		controllerName = controller.getName();
		log.info(String.format("%s setController %s", getName(), controllerName));

		controllerName = controller.getName();
		this.controller = controller;
		this.deviceBus = deviceBus;
		this.deviceAddress = deviceAddress;
		
		createDevice();
		isControllerSet = true;
		broadcastState();
		return true;
	}

	@Override
	public void unsetController() {
		controller = null;
		this.deviceBus = null;
		this.deviceAddress = null;
		isControllerSet = false;
		broadcastState();
	}

	@Override
	public void setDeviceBus(String deviceBus) {
		this.deviceBus = deviceBus;
		broadcastState();
	}

	@Override
	public void setDeviceAddress(String deviceAddress) {
		if (controller != null) {
			if (this.deviceAddress != deviceAddress) {
				controller.releaseI2cDevice(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress));
				controller.createI2cDevice(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress));
			}
		}
		log.info(String.format("Setting device address to %s", deviceAddress));
		this.deviceAddress = deviceAddress;
	}
	
	/**
	 * This method creates the i2c device
	 */
	boolean createDevice() {
		if (controller != null) {
				controller.releaseI2cDevice(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress));
				controller.createI2cDevice(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress));
		}

		log.info(String.format("Creating device on bus: %s address %s", deviceBus, deviceAddress));
		return true;
	}
	
	public void begin() {
		byte[] buffer = { PCA9685_MODE1, 0x0 };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	// @Override
	public boolean isAttached() {
		return controller != null;
	}

	/**
	 * Set the PWM pulsewidth
	 * 
	 * @param pin
	 * @param pulseWidthOn
	 * @param pulseWidthOff
	 */
	public void setPWM(Integer pin, Integer pulseWidthOn, Integer pulseWidthOff) {

		byte[] buffer = { (byte) (PCA9685_LED0_ON_L + (pin * 4)), (byte) (pulseWidthOn & 0xff), (byte) (pulseWidthOn >> 8), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	/**
	 * Set the PWM frequency i.e. the frequency between positive pulses.
	 * 
	 * @param hz
	 */
	public void setPWMFreq(Integer hz) { // Analog servos run at ~60 Hz updates
		log.info(String.format("servoPWMFreq %s hz", hz));

		int prescale_value = Math.round(osc_clock / precision / hz) - 1;
		// Set sleep mode before changing PWM freqency
		byte[] writeBuffer = { PCA9685_MODE1, PCA9685_SLEEP };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writeBuffer, writeBuffer.length);

		// Wait 1 millisecond until the oscillator has stabilized
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			if (Thread.interrupted()) { // Clears interrupted status!
			}
		}

		// Write the PWM frequency value
		byte[] buffer2 = { PCA9685_PRESCALE, (byte) prescale_value };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer2, buffer2.length);

		// Leave sleep mode, set autoincrement to be able to write several
		// bytes
		// in sequence
		byte[] buffer3 = { PCA9685_MODE1, PCA9685_AUTOINCREMENT };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer3, buffer3.length);

		// Wait 1 millisecond until the oscillator has stabilized
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			if (Thread.interrupted()) { // Clears interrupted status!
			}
		}
	}

	public void setServo(Integer pin, Integer pulseWidthOff) {
		// since pulseWidthOff can be larger than > 256 it needs to be
		// sent as 2 bytes
		log.info(String.format("setServo %s deviceAddress %S pin %s pulse %s", pin, deviceAddress, pin, pulseWidthOff));
		byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (pin * 4)), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	/**
	 * this would have been nice to have Java 8 and a default implmentation in
	 * this interface which does Servo sweeping in the Servo (already implmented)
	 * and only if the controller can does it do sweeping on the "controller"
	 * 
	 * For example MrlComm can sweep internally (or it used to be implemented)
	 */
	@Override
	public void servoSweepStart(ServoControl servo) {
		log.info("Adafruit16C can not do sweeping on the controller - sweeping must be done in ServoControl");
	}

	@Override
	public void servoSweepStop(ServoControl servo) {
		log.info("Adafruit16C can not do sweeping on the controller - sweeping must be done in ServoControl");
	}

	@Override
	public void servoWrite(ServoControl servo) {
    ServoData servoData = servoMap.get(servo.getName());
		if (!servoData.pwmFreqSet) {
			setPWMFreq(servoData.pwmFreq);
		}
		log.info(String.format("servoWrite %s deviceAddress %s targetOutput %d", servo.getName(), deviceAddress, servo.getTargetOutput()));
		int pulseWidthOff = SERVOMIN + (int) (servo.getTargetOutput() * (int) ((float) SERVOMAX - (float) SERVOMIN) / (float) (180));
		setServo(servo.getPin(), pulseWidthOff);
	}

	@Override
	public void servoWriteMicroseconds(ServoControl servo, int uS) {
    ServoData servoData = servoMap.get(servo.getName());
		if (!servoData.pwmFreqSet) {
			setPWMFreq(servoData.pwmFreq);
		}
		
		int pin = servo.getPin();
		// 1000 ms => 150, 2000 ms => 600
		int pulseWidthOff = (int) (uS * 0.45) - 300;
		// since pulseWidthOff can be larger than > 256 it needs to be
		// sent as 2 bytes
		log.info(String.format("servoWriteMicroseconds %s deviceAddress x%02X pin %s pulse %d", servo.getName(), deviceAddress, pin, pulseWidthOff));

		byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (pin * 4)), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	@Override
	public boolean servoEventsEnabled(ServoControl servo, boolean enabled) {
		// @GroG. What is this method supposed to do ?
		// return arduino.servoEventsEnabled(servo, enabled);
		// Grog says - if you want feedback from the microcontroller to say when a
		// servo has stopped
		// when its moving at sub speed ...
		// perhaps cannot do this with Adafruit16CServoDriver
		// Mats says - We don't have any feedback from the servos, but to send
		// an event when the sweep stops should not be a problem

		return enabled;
	}

	@Override
	public void servoSetSpeed(ServoControl servo) {
		// TODO Auto-generated method stub.
		// perhaps cannot do this with Adafruit16CServoDriver
		// Mats says. It can be done in this service. But not by the board.
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

		ServiceType meta = new ServiceType(Adafruit16CServoDriver.class.getCanonicalName());
		meta.addDescription("Adafruit 16-Channel PWM/Servo Driver");
		meta.addCategory("shield", "servo & pwm");
		meta.setSponsor("Mats");
		/*
		 * meta.addPeer("arduino", "Arduino", "our Arduino"); meta.addPeer("raspi",
		 * "RasPi", "our RasPi");
		 */
		return meta;
	}

	@Override
	public DeviceController getController() {
		return controller;
	}

	/**
	 * Device attach - this should be creating the I2C device on MRLComm for the
	 * "first" servo if not already created - Since this does not use the Arduino
	 * <Servo.h> servos - it DOES NOT need to create "Servo" devices in MRLComm.
	 * It will need to keep track of the "pin" to I2C address, and whenever a
	 * ServoControl.moveTo(79) - the Servo will tell this controller its name &
	 * location to move.
	 * Mats says. The board has a single i2c address that doesn't change.
	 * The Arduino only needs to keep track of the i2c bs, not all devices that can
	 * communicate thru it. I.e. This service should keep track of servos,
	 * not the Arduino or the Raspi. 
	 * 
	 * 
	 * This service will translate the name & location to an I2C address & value
	 * write request to the MRLComm device.
	 * 
	 * Mats comments on the above MRLComm should not know anything about the
	 * servos in this case. This service keeps track of the servos. MRLComm should
	 * not know anything about what addresses are used on the i2c bus MRLComm
	 * should initiate the i2c bus when it receives the first i2c write or read
	 * This service knows nothing about other i2c devices that can be on the same
	 * bus. And most important. This service knows nothing about MRLComm at all.
	 * I.e except for this bunch of comments :-)
	 * 
	 * It implements the methods defined in the ServoController and translates the
	 * servo requests to i2c writes defined in the I2CControl interface
	 * 
	 */

	/**
	 * if your device controller can provided several {Type}Controller interfaces,
	 * there might be commonality between all of them. e.g. initialization of data
	 * structures, preparing communication, sending control and config messages,
	 * etc.. - if there is commonality, it could be handled here - where Type
	 * specific methods call this method
	 * 
	 * This is a software representation of a board that uses the i2c protocol.
	 * It uses the methods defined in the I2CController interface to write
	 * servo-commands.
	 * The I2CControl interface defines the common methods for all devices that use the
	 * i2c protocol. 
	 * In most services I wiil define addition <device>Control methods, but this
	 * service is a "middle man" so it implements the ServoController methods and
	 * should not have any "own" methods. 
	 *
	 * After our explanation of the roles of <device>Control and <device>Controller
	 * it's clear to me that any device that uses the i2c protocol needs to implement
	 * to <device>Control methods:
	 * 		I2CControl that is the generic interface for any i2c device
	 * 		<device>Control, that defines the specific methods for that device.
	 *    For example the MPU6050 should implement both I2CControl and MPU6050Control
	 *    or perhaps a AccGyroControl interface that would define the common methods
	 *    that a Gyro/Accelerometer/Magnetometer device should implement.
	 */

	// FIXME how many do we want to support ??
	// this device attachment is overloaded on the Arduino side ...
	// Currently its only Servo, but it's also possible to implement
	// MotorController and any device that requires pwm, like a LED dimmer.

	@Override
	public void deviceAttach(DeviceControl device, Object... conf) throws Exception {
		// only need to handle servos :)
		// this is easy
		ServoControl servo = (ServoControl) device;
		// servo.setController(this); Do not set any "ServoControl" data like this
		// Not necessary
		// should initial pos be a requirement ?
		// This will fail because the pin data has not yet been set in Servo
		// servoNameToPin.put(servo.getName(), servo.getPin());
		ServoData servoData = new ServoData();
	  servoData.pin = (int)conf[0];	
	  servoData.pwmFreqSet = false;
	  servoData.pwmFreq = pwmFreq;
		servoMap.put(servo.getName(), servoData);
	}

	@Override 
	public void deviceDetach(DeviceControl servo) {
		servoDetach((ServoControl) servo);
		servoMap.remove(servo.getName());
	}
	
	/**
	 * Start sending pulses to the servo
	 * 
	 */
	@Override
	public void servoAttach(ServoControl servo, int pin) {
		servoWrite(servo);
	}

	/**
	 * Stop sending pulses to the servo, relax
	 */
	@Override
	public void servoDetach(ServoControl servo) {
		int pin = servo.getPin();
		setPWM(pin, 4096, 0);
	}
}