package com.urbanclean.controller;

import com.urbanclean.dto.request.CountryRequest;
import com.urbanclean.dto.response.CountryResponse;
import com.urbanclean.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for country management operations
 */
@Tag(name = "Countries", description = "Endpoints for managing country configurations")
@RestController
@RequestMapping("/api/admin/countries")
@RequiredArgsConstructor
@Slf4j
public class CountryController {

    private final CountryService countryService;

    /**
     * Get all countries
     * GET /api/admin/countries
     * Accessible by ADMIN only
     */
    @Operation(
        summary = "Get all countries",
        description = "Retrieve a list of all configured countries (enabled and disabled)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Countries retrieved successfully",
            content = @Content(schema = @Schema(implementation = CountryResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required"
        )
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CountryResponse>> getAllCountries() {
        log.info("Get all countries request");
        List<CountryResponse> countries = countryService.getAllCountries();
        return ResponseEntity.ok(countries);
    }

    /**
     * Get enabled countries
     * GET /api/admin/countries/enabled
     * Accessible by all authenticated users
     */
    @Operation(
        summary = "Get enabled countries",
        description = "Retrieve a list of all enabled countries",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Enabled countries retrieved successfully",
            content = @Content(schema = @Schema(implementation = CountryResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        )
    })
    @GetMapping("/enabled")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'CIUDADANO')")
    public ResponseEntity<List<CountryResponse>> getEnabledCountries() {
        log.info("Get enabled countries request");
        List<CountryResponse> countries = countryService.getEnabledCountries();
        return ResponseEntity.ok(countries);
    }

    /**
     * Get default country
     * GET /api/admin/countries/default
     * Accessible by all authenticated users
     */
    @Operation(
        summary = "Get default country",
        description = "Retrieve the default country configuration",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Default country retrieved successfully",
            content = @Content(schema = @Schema(implementation = CountryResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No default country configured"
        )
    })
    @GetMapping("/default")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'CIUDADANO')")
    public ResponseEntity<CountryResponse> getDefaultCountry() {
        log.info("Get default country request");
        CountryResponse country = countryService.getDefaultCountry();
        return ResponseEntity.ok(country);
    }

    /**
     * Get country by ID
     * GET /api/admin/countries/{id}
     * Accessible by ADMIN only
     */
    @Operation(
        summary = "Get country by ID",
        description = "Retrieve a specific country by its unique identifier",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Country retrieved successfully",
            content = @Content(schema = @Schema(implementation = CountryResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Country not found"
        )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> getCountry(
            @Parameter(description = "Country ID", required = true)
            @PathVariable UUID id) {
        log.info("Get country by id request: {}", id);
        CountryResponse country = countryService.getCountryById(id);
        return ResponseEntity.ok(country);
    }

    /**
     * Create a new country
     * POST /api/admin/countries
     * Accessible by ADMIN only
     */
    @Operation(
        summary = "Create a new country",
        description = "Create a new country configuration with geofencing boundaries",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Country created successfully",
            content = @Content(schema = @Schema(implementation = CountryResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors or invalid boundaries"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Country with same code or name already exists"
        )
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> createCountry(
            @Parameter(description = "Country data", required = true)
            @Valid @RequestBody CountryRequest request) {
        log.info("Create country request: {}", request.getName());
        CountryResponse country = countryService.createCountry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(country);
    }

    /**
     * Update an existing country
     * PUT /api/admin/countries/{id}
     * Accessible by ADMIN only
     */
    @Operation(
        summary = "Update a country",
        description = "Update an existing country configuration",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Country updated successfully",
            content = @Content(schema = @Schema(implementation = CountryResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors or invalid boundaries"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Country not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Country with same code or name already exists"
        )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> updateCountry(
            @Parameter(description = "Country ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Country data", required = true)
            @Valid @RequestBody CountryRequest request) {
        log.info("Update country request: {}", id);
        CountryResponse country = countryService.updateCountry(id, request);
        return ResponseEntity.ok(country);
    }

    /**
     * Delete a country (soft delete)
     * DELETE /api/admin/countries/{id}
     * Accessible by ADMIN only
     */
    @Operation(
        summary = "Delete a country",
        description = "Soft delete a country by disabling it (cannot delete default country)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Country deleted successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot delete default country"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Country not found"
        )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCountry(
            @Parameter(description = "Country ID", required = true)
            @PathVariable UUID id) {
        log.info("Delete country request: {}", id);
        countryService.deleteCountry(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Set default country
     * PUT /api/admin/countries/{id}/set-default
     * Accessible by ADMIN only
     */
    @Operation(
        summary = "Set default country",
        description = "Set a country as the default country for the system",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Default country set successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Country not found"
        )
    })
    @PutMapping("/{id}/set-default")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setDefaultCountry(
            @Parameter(description = "Country ID", required = true)
            @PathVariable UUID id) {
        log.info("Set default country request: {}", id);
        countryService.setDefaultCountry(id);
        return ResponseEntity.ok().build();
    }
}
