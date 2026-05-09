# Multi-Country Support - Quick Start Guide

## 🚀 Quick Start for Developers

This guide helps you quickly understand and use the multi-country support feature.

---

## 📋 Overview

The Urban Cleaning Management System now supports multiple countries with:
- Country-specific geofencing boundaries
- Country selection in report submission
- Dynamic map visualization per country
- Country-based report filtering

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CitizenReportPage                        │
│  - Manages selectedCountryId state                          │
│  - Coordinates form and map                                 │
└────────────────┬────────────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
┌───────▼──────┐  ┌──────▼────────┐
│  ReportForm  │  │    MapView    │
│              │  │               │
│  ┌─────────┐ │  │  - Displays   │
│  │ Country │ │  │    boundaries │
│  │Selector │ │  │  - Centers on │
│  └─────────┘ │  │    country    │
└──────────────┘  └───────────────┘
```

---

## 🔧 Backend Setup

### 1. Database Migration

The database schema is already migrated if you've run the application. If not:

```bash
# The migration runs automatically on startup
# Migration file: V20__add_multi_country_support.sql
```

### 2. Verify Countries

Check that countries are configured:

```sql
SELECT id, name, code, default_country, enabled 
FROM countries 
WHERE enabled = true;
```

Default countries:
- 🇪🇸 Spain (ESP) - Default
- 🇨🇴 Colombia (COL)
- 🇺🇸 United States (USA)

### 3. API Endpoints

**Public Endpoints** (no auth required):
```
GET /api/admin/countries/enabled    # Get enabled countries
GET /api/admin/countries/default    # Get default country
GET /api/admin/countries/{id}       # Get country by ID
```

**Admin Endpoints** (ADMIN role required):
```
GET    /api/admin/countries              # Get all countries
POST   /api/admin/countries              # Create country
PUT    /api/admin/countries/{id}         # Update country
DELETE /api/admin/countries/{id}         # Delete (disable) country
PUT    /api/admin/countries/{id}/set-default  # Set as default
```

---

## 💻 Frontend Usage

### Using CountrySelector Component

```jsx
import CountrySelector from './components/citizen/CountrySelector';

function MyForm() {
  const [countryId, setCountryId] = useState(null);

  return (
    <CountrySelector
      selectedCountryId={countryId}
      onSelectCountry={setCountryId}
      disabled={false}
    />
  );
}
```

### Using Country Service

```javascript
import { countryService } from './services';

// Get enabled countries
const countries = await countryService.getEnabledCountries();

// Get default country
const defaultCountry = await countryService.getDefaultCountry();

// Get country by ID
const country = await countryService.getCountryById(countryId);
```

### Submitting Reports with Country

```javascript
const reportData = {
  category: 'BASURA_ACUMULADA',
  description: 'Basura acumulada en la esquina',
  latitude: 40.4168,
  longitude: -3.7038,
  countryId: 'uuid-of-selected-country'  // Required!
};

await reportService.submitReport(reportData, photoFile);
```

### Displaying Country Boundaries on Map

```jsx
import MapView from './components/citizen/MapView';

function MyPage() {
  const [countryId, setCountryId] = useState(null);
  const [location, setLocation] = useState(null);

  return (
    <MapView
      location={location}
      countryId={countryId}
      showGeofence={true}
      height="500px"
      zoom={15}
    />
  );
}
```

---

## 🧪 Testing

### Run Integration Verification

```bash
node verify-phase4-integration.js
```

Expected output:
```
✅ Passed: 12
❌ Failed: 0
📈 Success Rate: 100.0%
```

### Build Frontend

```bash
cd src/frontend
npm run build
```

### Start Development Server

```bash
# Backend
cd src/backend
mvn spring-boot:run

# Frontend
cd src/frontend
npm run dev
```

---

## 🗺️ Country Boundary Format

Countries use GeoJSON Polygon format for boundaries:

```json
{
  "type": "Polygon",
  "coordinates": [
    [
      [-3.9, 40.3],  // [longitude, latitude]
      [-3.5, 40.3],
      [-3.5, 40.6],
      [-3.9, 40.6],
      [-3.9, 40.3]   // Close the polygon
    ]
  ]
}
```

**Important**: 
- Coordinates are `[longitude, latitude]` (not lat, lon!)
- First and last coordinate must be the same (closed polygon)
- Store as JSON string in `geofencing_boundary` column

---

## 🔍 Troubleshooting

### Issue: Countries not loading

**Check**:
1. Backend is running
2. Database has enabled countries
3. Network tab shows successful API calls
4. Console for error messages

**Solution**:
```sql
-- Verify enabled countries
SELECT * FROM countries WHERE enabled = true;

-- Enable a country if needed
UPDATE countries SET enabled = true WHERE code = 'ESP';
```

### Issue: Map not showing boundaries

**Check**:
1. Country has `geofencing_boundary` data
2. GeoJSON is valid
3. Console for parsing errors

**Solution**:
```sql
-- Check boundary data
SELECT name, geofencing_boundary FROM countries WHERE id = 'country-uuid';

-- Update boundary if needed
UPDATE countries 
SET geofencing_boundary = '{"type":"Polygon","coordinates":[[[...]]]}'
WHERE id = 'country-uuid';
```

### Issue: Submit button disabled

**Check**:
1. Country is selected (countryId not null)
2. Location is available
3. All required fields filled

**Debug**:
```javascript
// In ReportForm component
console.log('countryId:', formData.countryId);
console.log('location:', location);
console.log('disabled:', submitting || (!location && !useManualLocation) || !formData.countryId);
```

### Issue: Validation error "Country is required"

**Cause**: countryId is null or undefined

**Solution**:
1. Ensure CountrySelector is rendered
2. Check that onSelectCountry callback is working
3. Verify countryId is in formData state

---

## 📊 Database Schema Reference

### countries table

```sql
CREATE TABLE countries (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(3) NOT NULL UNIQUE,
    default_country BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    
    -- Geofencing
    geofencing_boundary TEXT,  -- GeoJSON Polygon
    min_lat DECIMAL(10, 8),
    max_lat DECIMAL(10, 8),
    min_lon DECIMAL(11, 8),
    max_lon DECIMAL(11, 8),
    
    -- Administrative
    administrative_area VARCHAR(100),
    municipality VARCHAR(100),
    
    -- Map center
    center_lat DECIMAL(10, 8),
    center_lon DECIMAL(11, 8),
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### reportes table (updated)

```sql
ALTER TABLE reportes 
ADD COLUMN country_id UUID REFERENCES countries(id);

CREATE INDEX idx_report_country ON reportes(country_id);
```

### tareas table (updated)

```sql
ALTER TABLE tareas 
ADD COLUMN country_id UUID REFERENCES countries(id);

CREATE INDEX idx_task_country ON tareas(country_id);
```

---

## 🎨 Styling Reference

### CountrySelector CSS Classes

```css
.country-selector              /* Main container */
.country-selector-loading      /* Loading state */
.country-selector-error        /* Error state */
.country-selector-empty        /* Empty state */
.country-selector-info         /* Info message */
.country-selector-hint         /* Hint text */
.spinner                       /* Loading spinner */
.retry-button                  /* Retry button */
```

### Custom Styling Example

```css
/* Override country selector styles */
.country-selector select {
  border: 2px solid #your-color;
  border-radius: 8px;
}

.country-selector-info {
  background-color: #your-bg-color;
}
```

---

## 🔐 Security Notes

### Client-Side
- Country validation before submission
- UUID format validation
- Error handling for API failures

### Server-Side
- Country existence validation
- Country enabled check
- Coordinate boundary validation
- Role-based access control for admin endpoints

---

## 📚 Additional Resources

### Documentation
- `MULTI_COUNTRY_IMPLEMENTATION_PROGRESS.md` - Full implementation details
- `PHASE_4_COMPLETION_SUMMARY.md` - Phase 4 summary
- `PHASE_4_TESTING_CHECKLIST.md` - Testing guide
- `SSL_CERTIFICATE_SETUP.md` - HTTPS configuration

### Code Examples
- `src/frontend/src/components/citizen/CountrySelector.jsx` - Component implementation
- `src/frontend/src/services/countryService.js` - API client
- `src/backend/src/main/java/com/urbanclean/controller/CountryController.java` - REST API

---

## 🆘 Getting Help

### Check Logs

**Frontend**:
```javascript
// Browser console
// Look for [MapView], [CountrySelector] prefixed logs
```

**Backend**:
```bash
# Application logs
tail -f logs/application.log | grep -i country
```

### Common Log Messages

```
[MapView] Loading country data for: {uuid}
[MapView] Country data loaded: {country}
[MapView] Using country boundary: {bounds}
[CountrySelector] Loading countries...
[CountrySelector] Countries loaded: {count}
```

---

## ✅ Checklist for New Developers

- [ ] Read this Quick Start Guide
- [ ] Verify database migration completed
- [ ] Check enabled countries in database
- [ ] Run integration verification script
- [ ] Build frontend successfully
- [ ] Start backend and frontend servers
- [ ] Test country selection in browser
- [ ] Test report submission with country
- [ ] Verify map boundaries display
- [ ] Review code examples

---

## 🎯 Key Takeaways

1. **Country is Required**: All new reports must have a countryId
2. **Auto-Selection**: Default country is auto-selected for convenience
3. **Validation**: Both client and server validate country
4. **Boundaries**: Map displays country-specific boundaries
5. **Graceful Degradation**: Falls back to env vars if country data unavailable

---

**Last Updated**: May 9, 2026  
**Version**: 1.0  
**Status**: Production Ready (pending Phase 5 testing)
