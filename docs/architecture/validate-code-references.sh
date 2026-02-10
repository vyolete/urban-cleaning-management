#!/bin/bash

# Code Reference Validation Script
# Validates that all documented features have source file references

echo "=== Code Reference Validation ==="
echo ""

DOCS_DIR="docs/architecture"
BACKEND_DIR="backend/src/main/java/com/urbanclean"
FRONTEND_DIR="frontend/src"
DOCKER_DIR="docker"

ERRORS=0
WARNINGS=0

# Function to check if a source reference exists
check_source_reference() {
    local file=$1
    local ref=$2
    
    # Extract file path from reference (remove method names, line numbers, etc.)
    local clean_ref=$(echo "$ref" | sed 's/(.*//' | sed 's/:.*//' | sed 's/ .*//')
    
    # Check if file exists
    if [ -f "$clean_ref" ]; then
        return 0
    else
        return 1
    fi
}

# Function to extract source references from a document
extract_source_references() {
    local file=$1
    echo "Checking source references in $(basename $file)..."
    
    # Find all "Source Reference:" lines
    local ref_count=0
    local valid_count=0
    
    while IFS= read -r line; do
        if [[ $line =~ Source\ Reference:|source\ reference:|Source\ References: ]]; then
            ((ref_count++))
            # Extract the reference path
            local ref=$(echo "$line" | sed 's/.*Source Reference[s]*: *//' | sed 's/`//g' | sed 's/,.*//')
            
            if [ -n "$ref" ] && [ "$ref" != "_" ] && [ "$ref" != "TBD" ]; then
                if check_source_reference "$file" "$ref"; then
                    ((valid_count++))
                else
                    echo "  ⚠️  WARNING: Referenced file not found: $ref"
                    ((WARNINGS++))
                fi
            fi
        fi
    done < "$file"
    
    if [ $ref_count -gt 0 ]; then
        echo "  ✓ Found $ref_count source references ($valid_count verified)"
    else
        echo "  ⚠️  WARNING: No source references found in document"
        ((WARNINGS++))
    fi
}

# Function to check for "TBD" or placeholder content
check_for_placeholders() {
    local file=$1
    echo "Checking for placeholders in $(basename $file)..."
    
    local tbd_count=$(grep -c "TBD\|TODO\|PLACEHOLDER\|_This section will" "$file" 2>/dev/null || echo "0")
    
    if [ $tbd_count -gt 0 ]; then
        echo "  ⚠️  WARNING: Found $tbd_count placeholder markers (TBD/TODO/etc.)"
        ((WARNINGS++))
    else
        echo "  ✓ No placeholders found"
    fi
}

# Function to check for "implementation-dependent" markers
check_for_ambiguity_markers() {
    local file=$1
    echo "Checking for ambiguity markers in $(basename $file)..."
    
    local ambig_count=$(grep -c "implementation-dependent\|unclear from code\|cannot be determined" "$file" 2>/dev/null || echo "0")
    
    if [ $ambig_count -gt 0 ]; then
        echo "  ✓ Found $ambig_count ambiguity markers (properly marked uncertain items)"
    fi
}

# Validate all markdown files
for file in "$DOCS_DIR"/*.md; do
    if [ -f "$file" ] && [ "$(basename $file)" != "README.md" ]; then
        echo ""
        echo "=== Validating $(basename $file) ==="
        extract_source_references "$file"
        check_for_placeholders "$file"
        check_for_ambiguity_markers "$file"
    fi
done

echo ""
echo "=== Validation Summary ==="
echo "Errors: $ERRORS"
echo "Warnings: $WARNINGS"

if [ $ERRORS -gt 0 ]; then
    echo "❌ Validation FAILED with $ERRORS errors"
    exit 1
elif [ $WARNINGS -gt 10 ]; then
    echo "⚠️  Validation PASSED with $WARNINGS warnings (review recommended)"
    exit 0
else
    echo "✅ Validation PASSED (with $WARNINGS warnings)"
    exit 0
fi
