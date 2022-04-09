#include "pch.h"

#include "tc_4l6x.h"

Gm4l6xTransmissionController gm4l6xTransmissionController;

void Gm4l6xTransmissionController::init() {
    for (size_t i = 0; i < efi::size(engineConfiguration->tcu_solenoid); i++) {
        enginePins.tcuSolenoids[i].initPin("Transmission Solenoid", engineConfiguration->tcu_solenoid[i], &engineConfiguration->tcu_solenoid_mode[i]);
    }
}

void Gm4l6xTransmissionController::update(gear_e gear) {
    for (size_t i = 0; i < efi::size(engineConfiguration->tcu_solenoid); i++) {
#if ! EFI_UNIT_TEST
    	enginePins.tcuSolenoids[i].setValue(config->tcuSolenoidTable[static_cast<int>(gear) + 1][i]);
#endif
    }
    setCurrentGear(gear);
    postState();

#if EFI_TUNER_STUDIO
    if (engineConfiguration->debugMode == DBG_TCU) {
        engine->outputChannels.debugIntField1 = config->tcuSolenoidTable[static_cast<int>(gear) + 1][0];
        engine->outputChannels.debugIntField2 = config->tcuSolenoidTable[static_cast<int>(gear) + 1][1];
        engine->outputChannels.debugIntField3 = config->tcuSolenoidTable[static_cast<int>(gear) + 1][2];
        engine->outputChannels.debugIntField4 = config->tcuSolenoidTable[static_cast<int>(gear) + 1][3];
        engine->outputChannels.debugIntField5 = config->tcuSolenoidTable[static_cast<int>(gear) + 1][4];
    }
#endif
}

Gm4l6xTransmissionController* getGm4l6xTransmissionController() {
	return &gm4l6xTransmissionController;
}