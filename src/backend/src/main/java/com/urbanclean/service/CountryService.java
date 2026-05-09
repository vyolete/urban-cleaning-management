package com.urbanclean.service;

import com.urbanclean.dto.request.CountryRequest;
import com.urbanclean.dto.response.CountryResponse;
import com.urbanclean.entity.Country;
import com.urbanclean.entity.Report;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.exception.custom.ValidationException;
import com.urbanclean.repository.CountryRepository;
import com.urbanclean.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for country management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CountryService {

    private final CountryRepository countryRepository;
    private final ReportRepository reportRepository;

    /**
     * Create a new country
     */
    @Transactional
    public CountryResponse createCountry(CountryRequest request) {
        log.info("Creating new country: {}", request.getName());

        // Validate geofencing boundaries
        validateGeofencingBoundaries(request.getMinLat(), request.getMaxLat(), 
                                     request.getMinLon(), request.getMaxLon());

        // Check if country code already exists
        if (countryRepository.findByCode(request.getCode()).isPresent()) {
            throw new ValidationException("Country with code " + request.getCode() + " already exists");
        }

        // Check if country name already exists
        if (countryRepository.findByName(request.getName()).isPresent()) {
            throw new ValidationException("Country with name " + request.getName() + " already exists");
        }

        // Create country entity
        Country country = Country.builder()
                .name(request.getName())
                .code(request.getCode())
                .defaultCountry(false)  // New countries are not default by default
                .enabled(true)
                .minLat(request.getMinLat())
                .maxLat(request.getMaxLat())
                .minLon(request.getMinLon())
                .maxLon(request.getMaxLon())
                .administrativeArea(request.getAdministrativeArea())
                .municipality(request.getMunicipality())
                .centerLat(request.getCenterLat())
                .centerLon(request.getCenterLon())
                .build();

        Country savedCountry = countryRepository.save(country);
        log.info("Country created successfully: {} ({})", savedCountry.getName(), savedCountry.getCode());

        return mapToResponse(savedCountry);
    }

    /**
     * Update an existing country
     */
    @Transactional
    public CountryResponse updateCountry(UUID id, CountryRequest request) {
        log.info("Updating country: {}", id);

        Country country = countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + id));

        // Validate geofencing boundaries
        validateGeofencingBoundaries(request.getMinLat(), request.getMaxLat(), 
                                     request.getMinLon(), request.getMaxLon());

        // Check if country code is being changed and if it already exists
        if (!country.getCode().equals(request.getCode())) {
            if (countryRepository.findByCode(request.getCode()).isPresent()) {
                throw new ValidationException("Country with code " + request.getCode() + " already exists");
            }
        }

        // Check if country name is being changed and if it already exists
        if (!country.getName().equals(request.getName())) {
            if (countryRepository.findByName(request.getName()).isPresent()) {
                throw new ValidationException("Country with name " + request.getName() + " already exists");
            }
        }

        // Update country fields
        country.setName(request.getName());
        country.setCode(request.getCode());
        country.setMinLat(request.getMinLat());
        country.setMaxLat(request.getMaxLat());
        country.setMinLon(request.getMinLon());
        country.setMaxLon(request.getMaxLon());
        country.setAdministrativeArea(request.getAdministrativeArea());
        country.setMunicipality(request.getMunicipality());
        country.setCenterLat(request.getCenterLat());
        country.setCenterLon(request.getCenterLon());

        Country updatedCountry = countryRepository.save(country);
        log.info("Country updated successfully: {} ({})", updatedCountry.getName(), updatedCountry.getCode());

        return mapToResponse(updatedCountry);
    }

    /**
     * Delete a country (soft delete by disabling)
     */
    @Transactional
    public void deleteCountry(UUID id) {
        log.info("Deleting (disabling) country: {}", id);

        Country country = countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + id));

        // Prevent deletion of default country
        if (country.getDefaultCountry()) {
            throw new ValidationException("Cannot delete the default country");
        }

        // Soft delete by disabling
        country.setEnabled(false);
        countryRepository.save(country);

        log.info("Country disabled successfully: {} ({})", country.getName(), country.getCode());
    }

    /**
     * Get country by ID
     */
    @Transactional(readOnly = true)
    public CountryResponse getCountryById(UUID id) {
        log.info("Fetching country by id: {}", id);

        Country country = countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + id));

        return mapToResponse(country);
    }

    /**
     * Get all countries
     */
    @Transactional(readOnly = true)
    public List<CountryResponse> getAllCountries() {
        log.info("Fetching all countries");

        return countryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all enabled countries
     */
    @Transactional(readOnly = true)
    public List<CountryResponse> getEnabledCountries() {
        log.info("Fetching enabled countries");

        return countryRepository.findByEnabledTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get default country
     */
    @Transactional(readOnly = true)
    public CountryResponse getDefaultCountry() {
        log.info("Fetching default country");

        Country country = countryRepository.findByDefaultCountryTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No default country configured"));

        return mapToResponse(country);
    }

    /**
     * Set default country
     */
    @Transactional
    public void setDefaultCountry(UUID id) {
        log.info("Setting default country: {}", id);

        Country newDefaultCountry = countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + id));

        // Remove default flag from current default country
        countryRepository.findByDefaultCountryTrue().ifPresent(currentDefault -> {
            currentDefault.setDefaultCountry(false);
            countryRepository.save(currentDefault);
        });

        // Set new default country
        newDefaultCountry.setDefaultCountry(true);
        countryRepository.save(newDefaultCountry);

        log.info("Default country set to: {} ({})", newDefaultCountry.getName(), newDefaultCountry.getCode());
    }

    /**
     * Validate geofencing boundaries
     */
    public void validateGeofencingBoundaries(BigDecimal minLat, BigDecimal maxLat, 
                                             BigDecimal minLon, BigDecimal maxLon) {
        if (minLat.compareTo(maxLat) >= 0) {
            throw new ValidationException("Minimum latitude must be less than maximum latitude");
        }

        if (minLon.compareTo(maxLon) >= 0) {
            throw new ValidationException("Minimum longitude must be less than maximum longitude");
        }

        // Validate latitude range (-90 to 90)
        if (minLat.compareTo(BigDecimal.valueOf(-90)) < 0 || maxLat.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new ValidationException("Latitude must be between -90 and 90");
        }

        // Validate longitude range (-180 to 180)
        if (minLon.compareTo(BigDecimal.valueOf(-180)) < 0 || maxLon.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new ValidationException("Longitude must be between -180 and 180");
        }
    }

    /**
     * Migrate existing reports to default country
     */
    @Transactional
    public void migrateExistingReportsToDefaultCountry() {
        log.info("Migrating existing reports to default country");

        Country defaultCountry = countryRepository.findByDefaultCountryTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No default country configured"));

        List<Report> reportsWithoutCountry = reportRepository.findAll().stream()
                .filter(report -> report.getCountry() == null)
                .collect(Collectors.toList());

        reportsWithoutCountry.forEach(report -> {
            report.setCountry(defaultCountry);
            reportRepository.save(report);
        });

        log.info("Migrated {} reports to default country: {}", 
                reportsWithoutCountry.size(), defaultCountry.getName());
    }

    /**
     * Map Country entity to CountryResponse DTO
     */
    private CountryResponse mapToResponse(Country country) {
        return CountryResponse.builder()
                .id(country.getId())
                .name(country.getName())
                .code(country.getCode())
                .defaultCountry(country.getDefaultCountry())
                .enabled(country.getEnabled())
                .minLat(country.getMinLat())
                .maxLat(country.getMaxLat())
                .minLon(country.getMinLon())
                .maxLon(country.getMaxLon())
                .administrativeArea(country.getAdministrativeArea())
                .municipality(country.getMunicipality())
                .centerLat(country.getCenterLat())
                .centerLon(country.getCenterLon())
                .createdAt(country.getCreatedAt())
                .updatedAt(country.getUpdatedAt())
                .build();
    }
}
