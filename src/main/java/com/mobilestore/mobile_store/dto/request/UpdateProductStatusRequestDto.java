package com.mobilestore.mobile_store.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductStatusRequestDto {

    @NotNull(message = "Active flag is required")
    private Boolean active;
}
