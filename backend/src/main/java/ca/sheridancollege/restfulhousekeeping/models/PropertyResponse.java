package ca.sheridancollege.restfulhousekeeping.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponse {

    private Long id;
    private String name;
    private String street;
    private String unit;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private String accessInstructions;
    private String propertyPicturePath;
    private LocalTime checkoutTime;
    private LocalTime checkinTime;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean airbnbCalendarConnected;

}
