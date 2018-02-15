package ar.arsensors;

import ar.arsensors.model.ModelObject;

public interface OnCanvasObjectClicked {

    void clickCanvasObject(ModelObject model);

    void sensorOutputResult(String result);

}
