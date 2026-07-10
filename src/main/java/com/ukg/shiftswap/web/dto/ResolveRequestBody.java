package com.ukg.shiftswap.web.dto;

import jakarta.validation.constraints.Size;

public record ResolveRequestBody(@Size(max = 500) String resolutionNote) {
}
