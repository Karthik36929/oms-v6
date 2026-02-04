#!/bin/bash
# Universal debug script that works with different Maven installations

echo "=========================================="
echo "Spring Boot Test Debugger"
echo "=========================================="

# Find Maven
MAVEN_CMD=""

if command -v mvn &> /dev/null; then
    MAVEN_CMD="mvn"
    echo "✓ Found Maven in PATH: $(which mvn)"
elif command -v ./mvnw &> /dev/null; then
    MAVEN_CMD="./mvnw"
    echo "✓ Found Maven wrapper: ./mvnw"
elif [ -f "mvnw" ]; then
    chmod +x mvnw
    MAVEN_CMD="./mvnw"
    echo "✓ Found Maven wrapper (made executable): ./mvnw"
else
    echo "❌ Maven not found!"
    echo ""
    echo "Please install Maven or use one of these options:"
    echo ""
    echo "Option 1: Install Maven"
    echo "  Ubuntu/Debian: sudo apt-get install maven"
    echo "  macOS: brew install maven"
    echo "  Windows: choco install maven"
    echo ""
    echo "Option 2: Use Azure DevOps pipeline to see test results"
    echo "  - Go to your pipeline run"
    echo "  - Click 'Tests' tab"
    echo "  - View failed test details"
    echo ""
    exit 1
fi

echo "Maven version:"
$MAVEN_CMD --version
echo ""

# Check if we're in a Spring Boot project
if [ ! -f "pom.xml" ]; then
    echo "❌ No pom.xml found!"
    echo "Please run this script from the root of your Spring Boot project"
    exit 1
fi

echo "✓ Found pom.xml"
echo ""

# Display project structure
echo "=========================================="
echo "Project Structure:"
echo "=========================================="
find src -type f -name "*.java" | head -20
echo ""

# Clean previous build
echo "=========================================="
echo "Step 1: Cleaning previous build..."
echo "=========================================="
$MAVEN_CMD clean

# Compile
echo ""
echo "=========================================="
echo "Step 2: Compiling source code..."
echo "=========================================="
$MAVEN_CMD compile

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ COMPILATION FAILED!"
    echo ""
    echo "Common issues:"
    echo "  - Missing imports (List, Map, ArrayList, HashMap)"
    echo "  - Syntax errors in Java code"
    echo "  - Undefined methods or variables"
    echo ""
    exit 1
fi

echo ""
echo "✅ Compilation successful!"

# Run tests with detailed output
echo ""
echo "=========================================="
echo "Step 3: Running tests..."
echo "=========================================="
$MAVEN_CMD test

TEST_EXIT_CODE=$?

# Display test results
echo ""
echo "=========================================="
echo "Test Results:"
echo "=========================================="

if [ -d "target/surefire-reports" ]; then
    echo ""
    echo "--- Text Reports ---"
    for file in target/surefire-reports/*.txt; do
        if [ -f "$file" ]; then
            echo ""
            echo "=== $(basename $file) ==="
            cat "$file"
            echo ""
        fi
    done
    
    echo ""
    echo "--- XML Reports (Detailed) ---"
    for file in target/surefire-reports/TEST-*.xml; do
        if [ -f "$file" ]; then
            echo ""
            echo "=== $(basename $file) ==="
            
            # Extract test failures using grep
            echo "Test Name:"
            grep -o 'name="[^"]*"' "$file" | head -1
            
            echo ""
            echo "Failures:"
            grep -A 5 "<failure" "$file" || echo "No failures in this file"
            
            echo ""
            echo "Errors:"
            grep -A 5 "<error" "$file" || echo "No errors in this file"
            echo ""
        fi
    done
    
    # Summary
    echo ""
    echo "=========================================="
    echo "Summary:"
    echo "=========================================="
    TOTAL=$(grep -r 'tests="' target/surefire-reports/TEST-*.xml | head -1 | grep -o 'tests="[0-9]*"' | grep -o '[0-9]*')
    FAILURES=$(grep -r 'failures="' target/surefire-reports/TEST-*.xml | head -1 | grep -o 'failures="[0-9]*"' | grep -o '[0-9]*')
    ERRORS=$(grep -r 'errors="' target/surefire-reports/TEST-*.xml | head -1 | grep -o 'errors="[0-9]*"' | grep -o '[0-9]*')
    
    echo "Total tests: ${TOTAL:-0}"
    echo "Failures: ${FAILURES:-0}"
    echo "Errors: ${ERRORS:-0}"
else
    echo "❌ No surefire-reports directory found!"
    echo "Tests may not have run at all."
fi

echo ""
echo "=========================================="

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo "✅ ALL TESTS PASSED!"
    echo "=========================================="
    exit 0
else
    echo "❌ TESTS FAILED!"
    echo "=========================================="
    echo ""
    echo "Next steps:"
    echo "  1. Review the test failures above"
    echo "  2. Check controller response format matches test expectations"
    echo "  3. Verify all endpoints return Map<String,Object> with 'message' field"
    echo ""
    exit 1
fi