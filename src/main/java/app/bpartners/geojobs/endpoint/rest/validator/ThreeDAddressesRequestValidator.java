package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.endpoint.rest.model.ThreeDAddressesRequest;
import app.bpartners.geojobs.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;

@Component
public class ThreeDAddressesRequestValidator implements Consumer<ThreeDAddressesRequest> {
    @Override
    public void accept(ThreeDAddressesRequest threeDRequest) {
        if (threeDRequest == null
                || threeDRequest.getAddresses() == null
                || threeDRequest.getAddresses().isEmpty()) {
            throw new BadRequestException("Addresses can not be null or empty");
        }
        if (threeDRequest.getDelimitationType() != null
                && !PARCEL_FREE_DELIMITATION.equals(threeDRequest.getDelimitationType())) {
            throw new BadRequestException(
                    "Only PARCEL_FREE_DELIMITATION is supported to request 3D model on addresses for now");
        }
    }
}
