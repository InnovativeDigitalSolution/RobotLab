angular.module('mrlapp.service.ServoGui', []).controller('ServoGuiCtrl', ['$timeout', '$scope', 'mrl', 'statusSvc', function($timeout, $scope, mrl, statusSvc) {
    console.info('ServoGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.mrl = mrl

    // mode is either "status" or "control"
    // in status mode we take updates by the servo and its events
    // in control mode we take updates by the user
    // $scope.mode = "status"; now statusControlMode

    // useful for initialization of components
    var firstTime = true

    // init
    $scope.min = 0
    $scope.max = 180

    $scope.possibleControllers = null
    $scope.testTime = 300
    $scope.sliderEnabled = false
    $scope.speedDisplay = 0

    $scope.speed = null
    $scope.lockInputOutput = true

    $scope.activeTabIndex = 0

    $scope.speedSlider = {
        value: 501,
        options: {
            floor: 1,
            ceil: 501,
            minLimit: 1,
            maxLimit: 501,
            hideLimitLabels: true,
            onStart: function() {},
            onChange: function() {
                if ($scope.sliderEnabled) {
                    if ($scope.speedSlider.value == 501) {
                        msg.send('fullSpeed')
                    } else {
                        msg.send('setSpeed', $scope.speedSlider.value)
                    }
                }
            },
            onEnd: function() {}
        }
    }

    $scope.autoDisable = null

    $scope.autoDisableSlider = {
        value: 3,
        options: {
            floor: 1,
            ceil: 10,
            minLimit: 1,
            maxLimit: 10,
            hideLimitLabels: true,
            onStart: function() {},
            onChange: function() {
                if ($scope.sliderEnabled) {
                    msg.send('setIdleTimeout', $scope.autoDisableSlider.value * 1000)
                }
            },
            onEnd: function() {}
        }
    }

    // TODO - should be able to build this based on
    // current selection of controller
    $scope.pinList = []
    for (let i = 0; i < 58; ++i) {
        $scope.pinList.push(i + '')
        // make strings 
    }

    //slider config with callbacks
    $scope.pos = {
        value: 90,
        options: {
            floor: 0,
            ceil: 180,
            minLimit: 0,
            maxLimit: 180,
            onStart: function() {},
            onChange: function() {
                if ($scope.sliderEnabled) {
                    msg.send('moveTo', $scope.pos.value)
                }
            },
            onEnd: function() {}
        }
    }

    $scope.restSlider = {
        value: 0,
        options: {
            floor: 0,
            ceil: 180,
            step: 1,
            showTicks: false,
            hideLimitLabels: true,
            onStart: function() {},
            /* - changing only on mouse up event - look in ServoGui.html - cannot do this !!! - sliding to the end an letting go doesnt do what you expect */
            onChange: function() {
                // $scope.setMinMax()
                msg.send('setRest', $scope.restSlider.value)
            },
            onEnd: function() {}
        }
    }

    $scope.inputSlider = {
        minValue: 0,
        maxValue: 180,
        options: {
            floor: 0,
            ceil: 180,
            step: 1,
            showTicks: false,
            hideLimitLabels: true,
            noSwitching: true,
            onStart: function() {},
            /* - changing only on mouse up event - look in ServoGui.html - cannot do this !!! - sliding to the end an letting go doesnt do what you expect */
            onChange: function() {
                if ($scope.lockInputOutput) {
                    $scope.outputSlider.minValue = $scope.inputSlider.minValue
                    $scope.outputSlider.maxValue = $scope.inputSlider.maxValue
                }
                msg.send('map', $scope.inputSlider.minValue, $scope.inputSlider.maxValue, $scope.outputSlider.minValue, $scope.outputSlider.maxValue)
            },
            onEnd: function() {}
        }
    }

    $scope.inputFieldMin = function() {
        if ($scope.lockInputOutput) {
            $scope.outputSlider.minValue = $scope.inputSlider.minValue
        }
        msg.send('map', $scope.inputSlider.minValue, $scope.inputSlider.maxValue, $scope.outputSlider.minValue, $scope.outputSlider.maxValue)
    }
    $scope.inputFieldMax = function() {
        if ($scope.lockInputOutput) {
            $scope.outputSlider.maxValue = $scope.inputSlider.maxValue
        }
        msg.send('map', $scope.inputSlider.minValue, $scope.inputSlider.maxValue, $scope.outputSlider.minValue, $scope.outputSlider.maxValue)
    }
    $scope.outputFieldMin = function() {
        if ($scope.lockInputOutput) {
            $scope.inputSlider.minValue = $scope.outputSlider.minValue
        }
        msg.send('map', $scope.inputSlider.minValue, $scope.inputSlider.maxValue, $scope.outputSlider.minValue, $scope.outputSlider.maxValue)
    }
    $scope.outputFieldMax = function() {
        if ($scope.lockInputOutput) {
            $scope.inputSlider.maxValue = $scope.outputSlider.maxValue
        }
        msg.send('map', $scope.inputSlider.minValue, $scope.inputSlider.maxValue, $scope.outputSlider.minValue, $scope.outputSlider.maxValue)
    }
    $scope.restField = function() {
        msg.send('setRest', $scope.restSlider.value)
    }

    $scope.outputSlider = {
        minValue: 0,
        maxValue: 180,
        options: {
            floor: 0,
            ceil: 180,
            step: 1,
            showTicks: false,
            hideLimitLabels: true,
            noSwitching: true,
            onStart: function() {},
            /* - changing only on mouse up event - look in ServoGui.html - cannot do this !!! - sliding to the end an letting go doesnt do what you expect */
            onChange: function() {
                if ($scope.lockInputOutput) {
                    $scope.inputSlider.minValue = $scope.outputSlider.minValue
                    $scope.inputSlider.maxValue = $scope.outputSlider.maxValue
                }
                msg.send('map', $scope.inputSlider.minValue, $scope.inputSlider.maxValue, $scope.outputSlider.minValue, $scope.outputSlider.maxValue)
            },
            onEnd: function() {}
        }
    }

    $scope.toggleLock = function() {
        $scope.lockInputOutput = !$scope.lockInputOutput
    }

    $scope.setMinMax = function(min, max) {
        console.log('setMinMax ', min, max)
        msg.send('setMinMax', min, max)
        /*
        if ($scope.statusControlMode == 'control') {
            msg.send('setMinMax', $scope.limits.minValue, $scope.limits.maxValue)
        }
        */
    }

    $scope.setSpeed = function(speed) {
        if (speed == null || ((typeof speed == 'string') && (speed.trim().length == 0))) {
            msg.send("unsetSpeed")
        } else {
            msg.send("setSpeed", speed)
        }
    }

    $scope.refreshSlider = function() {
        setTimeout(function() {
            $scope.$broadcast('rzSliderForceRender');
        }, 20)
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.selectedController = service.controller

        $scope.autoDisable = service.autoDisable

        // done correctly - speedDisplay is a 'status' display !
        // its NOT used to set 'control' speed - control is sent
        // from the ui interface - but the ui component does not display what it sent
        // speedDisplay displays what was recieved - and is currently set
        if (service.speed) {
            $scope.speedDisplay = service.speed
        } else {
            $scope.speedDisplay = 'Max'
        }

        $scope.pos.options.minLimit = service.mapper.minX
        $scope.pos.options.maxLimit = service.mapper.maxX

        $scope.restSlider.options.minLimit = service.mapper.minX
        $scope.restSlider.options.maxLimit = service.mapper.maxX
        if ($scope.restSlider.value < $scope.inputSlider.minValue) {
            $scope.restSlider.value = $scope.inputSlider.minValue
            msg.send('setRest', $scope.restSlider.value)
        }
        if ($scope.restSlider.value > $scope.inputSlider.maxValue) {
            $scope.restSlider.value = $scope.inputSlider.maxValue
            msg.send('setRest', $scope.restSlider.value)
        }

        // ui initialization - good idea !
        // first time is 'status' - otherwise control
        if (firstTime) {

            $scope.restSlider.value = service.rest

            $scope.pos.value = service.currentOutputPos
            $scope.sliderEnabled = true

            $scope.activeTabIndex = service.controller == null ? 0 : 1

            $scope.inputSlider.minValue = service.mapper.minX
            $scope.inputSlider.maxValue = service.mapper.maxX
            $scope.outputSlider.minValue = service.mapper.minY
            $scope.outputSlider.maxValue = service.mapper.maxY

            // init ui components
            if (service.speed) {
                $scope.speedSlider.value = service.speed
            } else {
                $scope.speedSlider.value = 501
                // ui max limit
            }

            firstTime = false

            // BELOW NECESSARY?
            $timeout(function() {
                $scope.$broadcast('rzSliderForceRender')
            })
        }

        // set min/max mapper slider BAD IDEA !!!! control "OR" status NEVER BOTH !!!!
        // $scope.pinList = service.pinList
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
            // servo event in the past 
            // meant feedback from MRLComm.c
            // but perhaps its come to mean
            // feedback from the service.moveTo
        case 'onRefreshControllers':
//            $scope.possibleControllers = data
            $scope.$apply()
            break

        case 'onServoController':
            $scope.possibleControllers = data
            $scope.$apply()
            break

        case 'onEncoderData':
            $scope.service.currentOutputPos = data.angle
            $scope.$apply()
            break

        case 'onStatus':
            break

        case 'onServoEvent':
            console.info("ServoEvent")
            console.info(data)
            /*  FIXME - use this to display servo 'intention' move stop etc
            if ($scope.statusControlMode == 'status') {
                $scope.service.currentOutputPos = data.pos
                $scope.$apply()
            }
        */
            break
        case 'onMoveTo':
            // FIXME - whole servo is sent ? - maybe not a bad thing, but there should probably be more
            // granularity and selectiveness on what data is published when ...

            break
        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }
    }

    $scope.setPin = function() {
        msg.send('setPin', $scope.service.pin)
    }

    $scope.setAutoDisable = function() {
        msg.send("setIdleTimeout", $scope.service.idleTimeout)
        msg.send("setAutoDisable", $scope.service.autoDisable)
    }

    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P
    $scope.sweep = function() {
        msg.send('sweep')
    }

    $scope.attachController = function(controller, pin) {
        console.info("attachController")
        if (!pin){
            statusSvc.error("pin must be set")
            return
        }

        // FIXME - there needs to be some updates to handle the complexity of taking updates from the servo vs
        // taking updates from the UI ..  some of this would be clearly solved with a (control/status) button

        let pos = $scope.pos.value;
        // currently taken from the slider's value :P - not good if the slider's value is not good :(
        if ($scope.speedSlider.value == 501) {
            msg.send('attach', controller, pin, pos)
        } else {
            msg.send('attach', controller, pin, pos, $scope.speedSlider.value)
        }

        // $scope.rest) <-- previously used rest which is (not good)
        // msg.attach($scope.controller, $scope.pin, 90)
    }

    // msg.subscribe("publishMoveTo")
    msg.subscribe("publishServoEvent")
    //    msg.subscribe("publishEncoderData")
    // msg.subscribe("refreshControllers")
    msg.subscribe(this)
    // msg.send('refreshControllers')
    msg.sendTo('runtime', 'requestAttachMatrix')
}
])
