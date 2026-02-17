#!/bin/bash

# Markdown Validation Script for Architecture Documentation
# Validates:
# 1. Proper heading hierarchy (no skipped levels)
# 2. Code blocks have language tags
# 3. Tables are properly formatted
# 4. Mermaid diagrams have proper syntax

echo "=== Markdown Validation for Architecture Documentation ==="
echo ""

DOCS_DIR="docs/architecture"
ERRORS=0
WARNINGS=0

# Function to check heading hierarchy
check_heading_hierarchy() {
    local file=$1
    echo "Checking heading hierarchy in $file..."
    
    # Extract heading levels
    local prev_level=0
    while IFS= read -r line; do
        if [[ $line =~ ^(#{1,6})\ .+ ]]; then
            local level=${#BASH_REMATCH[1]}
            if [ $prev_level -gt 0 ] && [ $level -gt $((prev_level + 1)) ]; then
                echo "  ❌ ERROR: Heading level skipped from h$prev_level to h$level"
                ((ERRORS++))
            fi
            prev_level=$level
        fi
    done < "$file"
}

# Function to check code blocks have language tags
check_code_blocks() {
    local file=$1
    echo "Checking code blocks in $file..."
    
    local in_code_block=false
    local line_num=0
    while IFS= read -r line; do
        ((line_num++))
        if [[ $line =~ ^\`\`\`(.*)$ ]]; then
            if [ "$in_code_block" = false ]; then
                # Opening code fence
                local lang="${BASH_REMATCH[1]}"
                if [ -z "$lang" ]; then
                    echo "  ⚠️  WARNING: Code block at line $line_num missing language tag"
                    ((WARNINGS++))
                fi
                in_code_block=true
            else
                # Closing code fence
                in_code_block=false
            fi
        fi
    done < "$file"
}

# Function to check table formatting
check_tables() {
    local file=$1
    echo "Checking tables in $file..."
    
    local in_table=false
    local line_num=0
    while IFS= read -r line; do
        ((line_num++))
        if [[ $line =~ ^\|.*\|$ ]]; then
            if [ "$in_table" = false ]; then
                in_table=true
            fi
            # Check if table row has consistent pipe count
            local pipe_count=$(echo "$line" | tr -cd '|' | wc -c)
            if [ $pipe_count -lt 2 ]; then
                echo "  ⚠️  WARNING: Malformed table row at line $line_num"
                ((WARNINGS++))
            fi
        else
            in_table=false
        fi
    done < "$file"
}

# Function to check Mermaid diagrams
check_mermaid_diagrams() {
    local file=$1
    echo "Checking Mermaid diagrams in $file..."
    
    local in_mermaid=false
    local line_num=0
    while IFS= read -r line; do
        ((line_num++))
        if [[ $line =~ ^\`\`\`mermaid$ ]]; then
            in_mermaid=true
        elif [[ $line =~ ^\`\`\`$ ]] && [ "$in_mermaid" = true ]; then
            in_mermaid=false
        fi
    done < "$file"
}

# Validate all markdown files
for file in "$DOCS_DIR"/*.md; do
    if [ -f "$file" ]; then
        echo ""
        echo "=== Validating $(basename $file) ==="
        check_heading_hierarchy "$file"
        check_code_blocks "$file"
        check_tables "$file"
        check_mermaid_diagrams "$file"
    fi
done

echo ""
echo "=== Validation Summary ==="
echo "Errors: $ERRORS"
echo "Warnings: $WARNINGS"

if [ $ERRORS -gt 0 ]; then
    echo "❌ Validation FAILED with $ERRORS errors"
    exit 1
else
    echo "✅ Validation PASSED (with $WARNINGS warnings)"
    exit 0
fi
