package com.bluewave.civicconnect.profile;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Address {


    private String city;
    private String country;
    private String state;
    private String pincode;
    private String addressLine;

}
