package com.mobilestore.mobile_store.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDto {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private boolean active;
    private long orderCount;
    private LocalDateTime createdAt;
}
