#!/usr/bin/env node

/**
 * Phase 4 Integration Verification Script
 * 
 * This script verifies that all Phase 4 frontend components are properly integrated
 * by checking imports, exports, and component structure.
 */

const fs = require('fs');
const path = require('path');

const FRONTEND_PATH = path.join(__dirname, 'src', 'frontend', 'src');

// ANSI color codes
const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function checkFileExists(filePath, description) {
  const fullPath = path.join(FRONTEND_PATH, filePath);
  const exists = fs.existsSync(fullPath);
  
  if (exists) {
    log(`✅ ${description}`, 'green');
    return true;
  } else {
    log(`❌ ${description} - File not found: ${filePath}`, 'red');
    return false;
  }
}

function checkFileContains(filePath, searchStrings, description) {
  const fullPath = path.join(FRONTEND_PATH, filePath);
  
  if (!fs.existsSync(fullPath)) {
    log(`❌ ${description} - File not found: ${filePath}`, 'red');
    return false;
  }
  
  const content = fs.readFileSync(fullPath, 'utf-8');
  const allFound = searchStrings.every(str => content.includes(str));
  
  if (allFound) {
    log(`✅ ${description}`, 'green');
    return true;
  } else {
    const missing = searchStrings.filter(str => !content.includes(str));
    log(`❌ ${description} - Missing: ${missing.join(', ')}`, 'red');
    return false;
  }
}

function runVerification() {
  log('\n🔍 Phase 4 Frontend Integration Verification\n', 'cyan');
  
  let passed = 0;
  let failed = 0;
  
  // Check 1: Country Service exists
  log('📦 Checking Country Service...', 'blue');
  if (checkFileExists('services/countryService.js', 'countryService.js exists')) {
    passed++;
    if (checkFileContains(
      'services/countryService.js',
      ['getEnabledCountries', 'getDefaultCountry', 'getCountryById'],
      'countryService has required methods'
    )) {
      passed++;
    } else {
      failed++;
    }
  } else {
    failed += 2;
  }
  
  // Check 2: Country Service exported
  log('\n📦 Checking Service Exports...', 'blue');
  if (checkFileContains(
    'services/index.js',
    ['export { default as countryService }'],
    'countryService exported from services/index.js'
  )) {
    passed++;
  } else {
    failed++;
  }
  
  // Check 3: CountrySelector component exists
  log('\n🧩 Checking CountrySelector Component...', 'blue');
  if (checkFileExists('components/citizen/CountrySelector.jsx', 'CountrySelector.jsx exists')) {
    passed++;
    if (checkFileContains(
      'components/citizen/CountrySelector.jsx',
      ['selectedCountryId', 'onSelectCountry', 'getEnabledCountries', 'getDefaultCountry'],
      'CountrySelector has required props and API calls'
    )) {
      passed++;
    } else {
      failed++;
    }
  } else {
    failed += 2;
  }
  
  // Check 4: CountrySelector CSS exists
  if (checkFileExists('components/citizen/CountrySelector.css', 'CountrySelector.css exists')) {
    passed++;
  } else {
    failed++;
  }
  
  // Check 5: ReportForm integration
  log('\n📝 Checking ReportForm Integration...', 'blue');
  if (checkFileContains(
    'components/citizen/ReportForm.jsx',
    [
      'import CountrySelector',
      'countryId: null',
      'handleCountrySelect',
      'onCountrySelect',
      '<CountrySelector'
    ],
    'ReportForm imports and uses CountrySelector'
  )) {
    passed++;
  } else {
    failed++;
  }
  
  if (checkFileContains(
    'components/citizen/ReportForm.jsx',
    ['countryId: formData.countryId', 'onCountrySelect: PropTypes.func'],
    'ReportForm has countryId state and PropTypes'
  )) {
    passed++;
  } else {
    failed++;
  }
  
  // Check 6: reportService validation
  log('\n🔧 Checking Report Service...', 'blue');
  if (checkFileContains(
    'services/reportService.js',
    ['if (!reportData.countryId)', 'errors.countryId'],
    'reportService validates countryId'
  )) {
    passed++;
  } else {
    failed++;
  }
  
  // Check 7: MapView integration
  log('\n🗺️  Checking MapView Integration...', 'blue');
  if (checkFileContains(
    'components/citizen/MapView.jsx',
    [
      'import { countryService }',
      'countryId = null',
      'countryData',
      'getCountryById',
      'geofencingBoundary'
    ],
    'MapView imports countryService and handles country data'
  )) {
    passed++;
  } else {
    failed++;
  }
  
  if (checkFileContains(
    'components/citizen/MapView.jsx',
    ['countryId: PropTypes.string', 'centerLatitude', 'centerLongitude'],
    'MapView has countryId prop and uses country center'
  )) {
    passed++;
  } else {
    failed++;
  }
  
  // Check 8: CitizenReportPage integration
  log('\n📄 Checking CitizenReportPage Integration...', 'blue');
  if (checkFileContains(
    'pages/CitizenReportPage.jsx',
    [
      'selectedCountryId',
      'handleCountrySelect',
      'onCountrySelect={handleCountrySelect}',
      'countryId={selectedCountryId}'
    ],
    'CitizenReportPage manages country state and passes to children'
  )) {
    passed++;
  } else {
    failed++;
  }
  
  // Summary
  log('\n' + '='.repeat(60), 'cyan');
  log('📊 Verification Summary', 'cyan');
  log('='.repeat(60), 'cyan');
  log(`✅ Passed: ${passed}`, 'green');
  log(`❌ Failed: ${failed}`, failed > 0 ? 'red' : 'green');
  log(`📈 Success Rate: ${((passed / (passed + failed)) * 100).toFixed(1)}%`, 
      failed === 0 ? 'green' : 'yellow');
  
  if (failed === 0) {
    log('\n🎉 All integration checks passed! Phase 4 is complete.', 'green');
    log('✨ Ready to proceed to Phase 5 (Testing)', 'cyan');
    return 0;
  } else {
    log('\n⚠️  Some integration checks failed. Please review the errors above.', 'yellow');
    return 1;
  }
}

// Run verification
const exitCode = runVerification();
process.exit(exitCode);
