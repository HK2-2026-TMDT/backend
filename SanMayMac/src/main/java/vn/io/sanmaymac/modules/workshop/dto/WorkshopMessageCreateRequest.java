package vn.io.sanmaymac.modules.workshop.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkshopMessageCreateRequest(@NotBlank String content) {
}