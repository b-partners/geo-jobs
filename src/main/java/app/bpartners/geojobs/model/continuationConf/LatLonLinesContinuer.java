package app.bpartners.geojobs.model.continuationConf;

import lombok.Getter;

@Getter
public enum LatLonLinesContinuer {
    DEFAULT_Z(17),
    DEFAULT_IMG_SIZE(1_024),
    DEFAULT_NEIGHBOURHOOD(10);

    private final int value;

    LatLonLinesContinuer(int value) {
        this.value = value;
    }
}
