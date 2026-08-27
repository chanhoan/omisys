package com.omisys.user_dto.infrastructure;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
public class AddressDto {

    private Long addressId;
    private Long userId;
    private String alias;
    private String recipient;
    private String phoneNumber;
    private String zipcode;
    private String address;
    private Boolean isDefault;

}
