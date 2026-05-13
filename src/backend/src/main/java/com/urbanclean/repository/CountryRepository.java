package com.urbanclean.repository;

import com.urbanclean.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Country entity operations
 */
@Repository
public interface CountryRepository extends JpaRepository<Country, UUID> {
    
    /**
     * Find the default country
     * @return Optional containing the default country if exists
     */
    Optional<Country> findByDefaultCountryTrue();
    
    /**
     * Find all enabled countries
     * @return List of enabled countries
     */
    List<Country> findByEnabledTrue();
    
    /**
     * Find country by ISO code
     * @param code ISO 3166-1 alpha-3 code
     * @return Optional containing the country if exists
     */
    Optional<Country> findByCode(String code);
    
    /**
     * Find country by name
     * @param name Country name
     * @return Optional containing the country if exists
     */
    Optional<Country> findByName(String name);
    
    /**
     * Find country by administrative area
     * @param administrativeArea Administrative area name
     * @return Optional containing the country if exists
     */
    Optional<Country> findByAdministrativeArea(String administrativeArea);
    
    /**
     * Find country by municipality
     * @param municipality Municipality name
     * @return Optional containing the country if exists
     */
    Optional<Country> findByMunicipality(String municipality);
}
